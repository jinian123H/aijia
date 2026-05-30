package com.aijia.video.data.remote

import android.content.Context
import android.util.Base64
import android.util.Log
import com.aijia.video.BuildConfig
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * API安全模块
 * 提供AES-256-GCM加密/解密功能
 * 使用固定密钥明文配置
 */
object ApiSecurity {

    @Volatile
    private var aesKey: ByteArray? = null

    /**
     * 从assets读取明文配置并初始化
     */
    fun init(context: Context, configName: String = "api_config.json") {
        try {
            val json = context.assets.open(configName).bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val passphrase = obj.getString("aes_passphrase")
            aesKey = deriveKey(passphrase)
            if (BuildConfig.DEBUG) {
                Log.d("ApiSecurity", "初始化成功，密钥长度: ${aesKey!!.size}")
            }
        } catch (e: Exception) {
            Log.e("ApiSecurity", "初始化失败: ${e.message}")
        }
    }

    /**
     * 用指定密码初始化（用于测试）
     */
    fun initWithPassphrase(passphrase: String) {
        aesKey = deriveKey(passphrase)
    }

    private fun deriveKey(passphrase: String): ByteArray {
        return MessageDigest.getInstance("SHA-256")
            .digest(passphrase.toByteArray(Charsets.UTF_8))
    }

    private fun getKey(): ByteArray {
        return aesKey ?: error("ApiSecurity not initialized")
    }

    // ═══════════════════════════════════════════════════════════════
    // AES-256-GCM 加密/解密
    // ═══════════════════════════════════════════════════════════════

    /**
     * AES-GCM加密
     * @param plaintext 明文
     * @return Base64编码的密文（格式：IV[12] + Tag[16] + Ciphertext）
     */
    fun encrypt(plaintext: String): String {
        val key = getKey()
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, iv)
        )

        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val tag = encrypted.copyOfRange(encrypted.size - 16, encrypted.size)
        val ciphertext = encrypted.copyOfRange(0, encrypted.size - 16)

        // 格式：IV + Tag + Ciphertext
        val packed = iv + tag + ciphertext
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    /**
     * AES-GCM解密
     * @param encoded Base64编码的密文
     * @return 明文，解密失败返回null
     */
    fun decrypt(encoded: String): String? {
        if (!isValidBase64(encoded)) return null

        val raw = Base64.decode(encoded, Base64.NO_WRAP)
        if (raw.size <= 28) return null

        val iv = raw.copyOfRange(0, 12)
        val tag = raw.copyOfRange(12, 28)
        val ciphertext = raw.copyOfRange(28, raw.size)

        return try {
            val key = getKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val cipherWithTag = ciphertext + tag
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, iv)
            )
            String(cipher.doFinal(cipherWithTag), Charsets.UTF_8)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("ApiSecurity", "解密失败: ${e.message}")
            null
        }
    }

    /**
     * 解密播放地址（支持时效签名格式 base64|exp|sig）
     */
    fun decryptPlayUrl(encoded: String): String {
        // 拆分时效签名格式
        val parts = encoded.split("|")
        val actualEncoded = if (parts.size == 3) {
            val exp = parts[1].toLongOrNull() ?: 0L
            if (exp > 0 && System.currentTimeMillis() / 1000 > exp) {
                if (BuildConfig.DEBUG) Log.w("ApiSecurity", "播放地址已过期(exp=$exp)")
                return encoded
            }
            parts[0]
        } else {
            encoded
        }

        return decrypt(actualEncoded) ?: encoded
    }

    private fun isValidBase64(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.length % 4 != 0) return false
        return try {
            Base64.decode(trimmed, Base64.NO_WRAP)
            true
        } catch (_: Exception) {
            false
        }
    }
}
