package me.weishu.kernelsu.data.remote

import java.security.PrivateKey

/**
 * manager 身份私钥（EC P-256, PKCS#8 DER, base64）。
 *
 * 与 local-api/include/embedded_pubkey.hpp 嵌入的服务端公钥配对；
 * 私钥原件: localapi-staging/manager-api-key.pem（SEC1 -> PKCS#8 转换）。
 *
 * 信任模型: UDS 仅本机可达 + 内核 SO_PEERCRED 为第一层（root 直通/白名单），
 * 私钥签名用于向后端证明调用方身份（第二层）。私钥嵌入 APK 在该模型下
 * 不扩大攻击面：能读取 APK 的攻击者已能对本机 socket 做同等操作。
 */
internal object ManagerIdentity {

    const val ECDSA_PRIVATE_KEY_B64: String =
        "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgH8SRwzQHw6cLLjDd6d3QR8KskFjKN/DysRpjcGShCTahRANCAAQ2eE9N1irhErDeV9G0ifebqBuXNHaQTF8KvBniuj75QLl0QYR6RzoYa0I8xIvYPUp23GRz32sLMHR4V+6tMnSE"

    /** 懒加载解析私钥；解析失败在首次握手时显式暴露。 */
    val identityKey: PrivateKey by lazy {
        val der = android.util.Base64.decode(ECDSA_PRIVATE_KEY_B64, android.util.Base64.DEFAULT)
        java.security.KeyFactory.getInstance("EC")
            .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(der))
    }
}
