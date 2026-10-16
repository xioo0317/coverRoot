package me.weishu.kernelsu.data.remote

import android.net.LocalSocket
import android.net.LocalSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.Signature

/**
 * coverRoot 后端 (local_api) 通讯客户端。
 *
 * 后端是 Unix domain socket 上的极简 HTTP/1.1 服务（Content-Length + keep-alive）：
 *   - 主通道:  抽象 socket "@local_api"
 *   - 调试通道: 文件 socket "/data/local/tmp/local_api.sock"
 *   - 协议:    POST /  {"action":"..."}   -> JSON 响应
 *              GET  /debug               -> JSON 响应
 *              POST /auth {"sig":hex}    -> ECDSA 挑战握手
 *
 * 安全握手（对齐 local-api/src/router.cpp）：
 *   1. 内核 SO_PEERCRED uid 校验（root 直通 / 白名单 / 拒绝）
 *   2. 非 root peer: 连接建立后服务端立即主动发送
 *      401 {"status":"auth_required","challenge":hex}；
 *      客户端用 EC P-256 私钥对 challenge 做 SHA256withECDSA 签名
 *      （Java 标准输出 = ASN.1 DER，与服务端 mbedtls_pk_verify 兼容），
 *      同连接 POST /auth {"sig":hex}；200 后该连接进入已认证状态，
 *      继续以 keep-alive 发送业务请求。
 *   免握手对端（root peer / 信任模式）不会主动发送任何数据，
 *   客户端以短暂读超时探测后直接进入业务请求。
 */
object LocalApiClient {

    /** 后端主通道（抽象 Unix socket，与 router.cpp kAbstractName 一致） */
    private const val ABSTRACT_SOCKET = "local_api"

    /** 后端调试通道（文件 Unix socket，与 router.cpp kDefaultSock 一致） */
    private const val FILE_SOCKET = "/data/local/tmp/local_api.sock"

    /** 连接 / 读写超时（毫秒） */
    private const val SO_TIMEOUT_MS = 5_000

    /** 等待服务端主动 challenge 的探测窗口；超时 = 对端免握手 */
    private const val HANDSHAKE_PROBE_TIMEOUT_MS = 1_500

    /** 响应头大小上限（与服务端 kMaxHead 对齐的防御值） */
    private const val MAX_HEADER_BYTES = 64 * 1024

    sealed class ApiResult {
        /** 成功收到后端 JSON 响应 */
        data class Success(val json: JSONObject) : ApiResult()

        /** 后端可达但返回了错误（HTTP 非 2xx 或业务错误） */
        data class ApiError(val code: Int, val message: String, val json: JSONObject?) : ApiResult()

        /** 连不上后端（socket 不存在 / 超时 / 权限拒绝） */
        data class Unreachable(val reason: String) : ApiResult()
    }

    /** 执行一个 action。对应后端协议: POST / {"action":"..."}。 */
    suspend fun executeAction(action: String): ApiResult = withContext(Dispatchers.IO) {
        request("POST", "/", "{\"action\":\"$action\"}")
    }

    /** 获取后端调试信息。对应后端协议: GET /debug。 */
    suspend fun fetchDebugInfo(): ApiResult = withContext(Dispatchers.IO) {
        request("GET", "/debug", null)
    }

    // ------------------------------------------------------------------
    // 请求编排
    // ------------------------------------------------------------------

    /** 依次尝试抽象 socket（生产主通道）-> 文件 socket（调试通道）。 */
    private fun request(method: String, path: String, body: String?): ApiResult {
        for ((name, abstract) in listOf(ABSTRACT_SOCKET to true, FILE_SOCKET to false)) {
            val result = exchangeWithAuth(name, abstract, method, path, body)
            if (result != null) return result // 通道可达（含业务错误），直接返回
        }
        return ApiResult.Unreachable(
            "无法连接后端服务（@local_api / $FILE_SOCKET 均连接失败，后端可能未运行）"
        )
    }

    /**
     * 单连接上的完整会话：握手探测 -> （必要时签名认证）-> 业务请求。
     * 返回 null 表示通道不可达（连接/IO 失败，调用方尝试下一通道）。
     */
    private fun exchangeWithAuth(
        socketName: String,
        abstract: Boolean,
        method: String,
        path: String,
        body: String?,
    ): ApiResult? {
        val socket = LocalSocket()
        try {
            socket.soTimeout = SO_TIMEOUT_MS
            val address = if (abstract) {
                LocalSocketAddress(socketName, LocalSocketAddress.Namespace.ABSTRACT)
            } else {
                LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM)
            }
            socket.connect(address)
            val output = socket.outputStream
            val input = socket.inputStream

            // ---- 阶段 1: 握手探测 ----
            // need_auth 的服务端在 accept 后立刻发送 401+challenge；
            // 免握手的服务端保持沉默，读超时即视为免握手并复用该连接。
            socket.soTimeout = HANDSHAKE_PROBE_TIMEOUT_MS
            val probe = try {
                readResponse(input)
            } catch (e: IOException) {
                null
            } finally {
                socket.soTimeout = SO_TIMEOUT_MS
            }
            if (probe != null) {
                val (probeCode, probeBody) = probe
                val challenge = runCatching { JSONObject(probeBody).optString("challenge", "") }
                    .getOrDefault("")
                if (probeCode == 401 && challenge.isNotEmpty()) {
                    // 正常握手路径：签名并认证
                    val auth = handshake(output, input, challenge)
                    if (auth != null) return auth // 握手被拒，无需尝试下一通道
                } else {
                    // 主动到达的响应不是 challenge（异常形态），按普通响应交付
                    return parseResponse(probeCode, probeBody)
                }
            }

            // ---- 阶段 2: 业务请求（keep-alive，已认证或免握手） ----
            writeRequest(output, method, path, body)
            var (code, payload) = readResponse(input)
            if (code == 401) {
                // 极端情况：认证态在服务端被重置，补一次握手后重试业务请求
                val challenge = runCatching { JSONObject(payload).optString("challenge", "") }
                    .getOrDefault("")
                if (challenge.isEmpty()) return parseResponse(code, payload)
                val auth = handshake(output, input, challenge)
                if (auth != null) return auth
                writeRequest(output, method, path, body)
                val retry = readResponse(input)
                code = retry.first
                payload = retry.second
            }
            return parseResponse(code, payload)
        } catch (e: IOException) {
            return null // 通道不可达，尝试下一通道
        } catch (e: Exception) {
            // 客户端自身异常（如私钥解析失败）：显式暴露而非伪装成不可达
            return ApiResult.ApiError(-1, "客户端异常: ${e.message}", null)
        } finally {
            runCatching { socket.close() }
        }
    }

    /** 对 challenge 签名并完成 POST /auth；成功返回 null，失败返回错误结果。 */
    private fun handshake(output: OutputStream, input: InputStream, challenge: String): ApiResult? {
        val sig = sign(challenge)
        writeRequest(output, "POST", "/auth", "{\"sig\":\"$sig\"}")
        val (code, body) = readResponse(input)
        if (code == 200) return null
        val msg = runCatching { JSONObject(body).optString("status", "auth failed") }
            .getOrDefault("auth failed")
        return ApiResult.ApiError(code, "后端握手失败: $msg", runCatching { JSONObject(body) }.getOrNull())
    }

    // ------------------------------------------------------------------
    // ECDSA P-256 签名（SHA256withECDSA -> ASN.1 DER，与服务端 mbedtls 兼容）
    // ------------------------------------------------------------------

    private fun sign(challengeHex: String): String {
        val challenge = challengeHex.hexToBytes()
        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(ManagerIdentity.identityKey)
            update(challenge)
        }.sign()
        return signature.toHexString()
    }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "hex string must have even length" }
        return ByteArray(length / 2) { i ->
            ((Character.digit(this[i * 2], 16) shl 4) + Character.digit(this[i * 2 + 1], 16)).toByte()
        }
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    // ------------------------------------------------------------------
    // HTTP/1.1 over LocalSocket（Content-Length 帧读取，keep-alive 复用）
    // ------------------------------------------------------------------

    private fun writeRequest(output: OutputStream, method: String, path: String, body: String?) {
        val sb = StringBuilder()
        sb.append(method).append(' ').append(path).append(" HTTP/1.1\r\n")
        sb.append("Host: localhost\r\n")
        if (body != null) {
            sb.append("Content-Type: application/json\r\n")
            sb.append("Content-Length: ").append(body.toByteArray(Charsets.UTF_8).size).append("\r\n")
        }
        sb.append("Connection: keep-alive\r\n\r\n")
        if (body != null) sb.append(body)
        output.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        output.flush()
    }

    /** 精确读取一帧 HTTP 响应（头 + Content-Length body），返回 (code, body)。 */
    private fun readResponse(input: InputStream): Pair<Int, String> {
        val acc = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var headerEnd = -1
        var head = ""
        var contentLength = -1
        while (headerEnd < 0) {
            val n = input.read(buf)
            if (n < 0) throw IOException("connection closed before response complete")
            acc.write(buf, 0, n)
            val raw = acc.toByteArray()
            headerEnd = indexOfHeaderEnd(raw)
            if (headerEnd >= 0) {
                head = String(raw, 0, headerEnd, Charsets.ISO_8859_1)
                contentLength = head.lineSequence()
                    .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                    ?.substringAfter(':')?.trim()?.toIntOrNull() ?: -1
            } else if (acc.size() > MAX_HEADER_BYTES) {
                throw IOException("response header too large")
            }
        }
        val raw = acc.toByteArray()
        val body = ByteArrayOutputStream()
        body.write(raw, headerEnd + 4, raw.size - (headerEnd + 4))
        while (contentLength >= 0 && body.size() < contentLength) {
            val n = input.read(buf, 0, minOf(buf.size, contentLength - body.size()))
            if (n < 0) throw IOException("connection closed in body")
            body.write(buf, 0, n)
        }
        val bodyText = if (contentLength >= 0) {
            String(body.toByteArray(), 0, minOf(body.size(), contentLength), Charsets.UTF_8)
        } else {
            String(body.toByteArray(), Charsets.UTF_8)
        }
        val code = head.substringBefore("\r\n").split(" ").getOrNull(1)?.toIntOrNull() ?: -1
        return code to bodyText
    }

    private fun indexOfHeaderEnd(raw: ByteArray): Int {
        val pattern = byteArrayOf(13, 10, 13, 10) // "\r\n\r\n"
        outer@ for (i in 0..raw.size - pattern.size) {
            for (j in pattern.indices) {
                if (raw[i + j] != pattern[j]) continue@outer
            }
            return i
        }
        return -1
    }

    // ------------------------------------------------------------------
    // 响应解析
    // ------------------------------------------------------------------

    /** 解析状态码 + JSON body 为 ApiResult。 */
    private fun parseResponse(code: Int, payload: String): ApiResult {
        val json = try {
            JSONObject(payload)
        } catch (e: Exception) {
            null
        }
        if (json == null) {
            return ApiResult.ApiError(code, "后端响应不是有效 JSON: ${payload.take(200)}", null)
        }
        if (code in 200..299 && !json.has("error")) {
            return ApiResult.Success(json)
        }
        return ApiResult.ApiError(
            code = code,
            message = json.optString("error", json.optString("status", "unknown error")),
            json = json,
        )
    }
}
