package me.weishu.kernelsu.data.remote

import android.net.LocalSocket
import android.net.LocalSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

/**
 * coverRoot 后端 (local_api) 通讯客户端。
 *
 * 后端是一个 Unix domain socket 上的极简 HTTP/1.1 服务：
 *   - 主通道:  抽象 socket "@local_api"
 *   - 调试通道: 文件 socket "/data/local/tmp/local_api.sock"
 *   - 协议:    POST /  {"action":"..."}   -> JSON 响应
 *              GET  /debug               -> JSON 响应
 *
 * 使用 android.net.LocalSocket（原生支持抽象 Unix socket 命名空间），
 * 不依赖任何第三方库。
 */
object LocalApiClient {

    /** 后端主通道（抽象 Unix socket，与 local-api/src/router.cpp kAbstractName 一致） */
    private const val ABSTRACT_SOCKET = "local_api"

    /** 后端调试通道（文件 Unix socket，与 router.cpp kDefaultSock 一致） */
    private const val FILE_SOCKET = "/data/local/tmp/local_api.sock"

    /** 连接 / 读写超时（毫秒） */
    private const val SO_TIMEOUT_MS = 5_000

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

    private fun request(method: String, path: String, body: String?): ApiResult {
        // 依次尝试: 抽象 socket（生产主通道） -> 文件 socket（调试通道）
        val response = tryConnectAndExchange(ABSTRACT_SOCKET, abstract = true, method, path, body)
            ?: tryConnectAndExchange(FILE_SOCKET, abstract = false, method, path, body)
        if (response == null) {
            return ApiResult.Unreachable(
                "无法连接后端服务（@local_api / $FILE_SOCKET 均连接失败，后端可能未运行）"
            )
        }
        return parseResponse(response)
    }

    /**
     * 连接一个 socket 并完成一次 HTTP 请求/响应交换。
     * 返回 null 表示该通道连接失败（调用方会尝试下一个通道）。
     */
    private fun tryConnectAndExchange(
        socketName: String,
        abstract: Boolean,
        method: String,
        path: String,
        body: String?,
    ): String? {
        val socket = LocalSocket()
        return try {
            socket.soTimeout = SO_TIMEOUT_MS
            val address = if (abstract) {
                LocalSocketAddress(socketName, LocalSocketAddress.Namespace.ABSTRACT)
            } else {
                LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM)
            }
            socket.connect(address)

            val output = socket.outputStream
            val sb = StringBuilder()
            sb.append(method).append(' ').append(path).append(" HTTP/1.1\r\n")
            sb.append("Host: localhost\r\n")
            if (body != null) {
                sb.append("Content-Type: application/json\r\n")
                sb.append("Content-Length: ").append(body.length).append("\r\n")
            }
            sb.append("Connection: close\r\n")
            sb.append("\r\n")
            if (body != null) sb.append(body)
            output.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
            output.flush()

            // 读取到 EOF（后端在 Connection: close 时会关闭连接）
            val input = socket.inputStream
            val buffer = ByteArray(8192)
            val chunks = StringBuilder()
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                chunks.append(String(buffer, 0, n, Charsets.ISO_8859_1))
            }
            chunks.toString()
        } catch (e: IOException) {
            null // 该通道不可达，尝试下一个
        } catch (e: Exception) {
            null
        } finally {
            runCatching { socket.close() }
        }
    }

    /** 解析极简 HTTP 响应: 状态行 + 头 + JSON body。 */
    private fun parseResponse(raw: String): ApiResult {
        val headerEnd = raw.indexOf("\r\n\r\n")
        if (headerEnd < 0) {
            return ApiResult.ApiError(-1, "后端响应格式异常（缺少 HTTP 头结束符）", null)
        }
        val head = raw.substring(0, headerEnd)
        val body = raw.substring(headerEnd + 4)

        // 状态行: "HTTP/1.1 200 OK"
        val statusLine = head.substringBefore("\r\n")
        val code = statusLine.split(" ").getOrNull(1)?.toIntOrNull() ?: -1

        // 若有 Transfer-Encoding: chunked，解码 body
        val isChunked = head.lineSequence()
            .any { it.startsWith("Transfer-Encoding:", ignoreCase = true) && it.contains("chunked", true) }
        val payload = if (isChunked) decodeChunked(body) else body

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

    /** 解码 HTTP chunked 传输编码。 */
    private fun decodeChunked(body: String): String {
        val out = StringBuilder()
        var pos = 0
        while (pos < body.length) {
            val lineEnd = body.indexOf("\r\n", pos)
            if (lineEnd < 0) break
            val size = body.substring(pos, lineEnd).trim().toIntOrNull(16) ?: break
            if (size == 0) break
            val chunkStart = lineEnd + 2
            val chunkEnd = (chunkStart + size).coerceAtMost(body.length)
            out.append(body, chunkStart, chunkEnd)
            pos = chunkEnd + 2
        }
        return out.toString()
    }
}
