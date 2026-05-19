package com.aijia.video.util

import java.security.MessageDigest

/**
 * 配置解密密钥持有器
 * 明文存储完整密钥，用于解密GIF隐写配置
 */
object SecureKeyHolder {

    // ═══════════════════════════════════════════════════════════════
    // 完整密钥（明文存储）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 配置解密密钥（SHA-256哈希后的32字节AES密钥）
     * 组成：AijiaGifConf2024SecretKey!@FixedSalt12345
     */
    private const val FULL_KEY: String = "AijiaGifConf2024SecretKey!@FixedSalt12345"

    // ═══════════════════════════════════════════════════════════════
    // 密钥派生
    // ═══════════════════════════════════════════════════════════════

    /**
     * 获取完整的配置解密密钥
     * 对完整密钥进行SHA-256哈希
     *
     * @return SHA-256哈希后的32字节AES密钥
     */
    fun getKey(): ByteArray {
        return MessageDigest.getInstance("SHA-256")
            .digest(FULL_KEY.toByteArray(Charsets.UTF_8))
    }

    /**
     * 获取完整密钥字符串（用于加密工具生成GIF）
     * 注意：此方法仅用于离线工具，APP运行时应使用getKey()
     *
     * @return 完整密钥字符串
     */
    fun getKeyForTool(): String {
        return FULL_KEY
    }

    /**
     * 验证密钥是否有效（用于调试）
     */
    fun validate(): Boolean {
        val key = getKey()
        return key.size == 32
    }
}
