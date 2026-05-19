package com.aijia.video.data.remote

import android.util.Base64
import android.util.Log
import com.aijia.video.BuildConfig
import com.aijia.video.util.GifUrlReader
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * API安全模块
 * 提供AES-256-GCM加密和HMAC-SHA256签名功能
 * 密钥从GIF隐写配置中动态获取
 */
object ApiSecurity {

    // ═══════════════════════════════════════════════════════════════
    // 动态密钥配置
    // ═══════════════════════════════════════════════════════════════

    private var _config: GifUrlReader.Config? = null

    /**
     * 初始化配置（Application启动时调用）
     */
    fun init(config: GifUrlReader.Config) {
        _config = config
        // 正式版不输出敏感信息
        if (BuildConfig.DEBUG) {
            Log.d("ApiSecurity", "ApiSecurity初始化成功")
        }
    }

    /** AES加密KEY_PREFIX */
    private val KEY_PREFIX: String
        get() = _config?.keyPrefix ?: error("ApiSecurity not initialized")

    /** AES加密KEY_SALT */
    private val KEY_SALT: String
        get() = _config?.keySalt ?: error("ApiSecurity not initialized")

    /** 密钥滚动周期(毫秒) */
    private val KEY_TIMEOUT: Long
        get() = (_config?.keyTimeout ?: error("ApiSecurity not initialized")) * 1000L

    /** HMAC签名SIGN_PREFIX */
    private val SIGN_PREFIX: String
        get() = _config?.signPrefix ?: error("ApiSecurity not initialized")

    /** HMAC签名SIGN_KEY */
    private val SIGN_KEY: String
        get() = _config?.signKey ?: error("ApiSecurity not initialized")

    // ═══════════════════════════════════════════════════════════════
    // AES-256-GCM 加密/解密
    // ═══════════════════════════════════════════════════════════════

    /**
     * 生成动态AES密钥
     * 基于时间槽滚动，每KEY_TIMEOUT秒更换一次
     *
     * @param timeOffsetSeconds 时间偏移（用于解密时尝试上一个时间槽）
     * @return 32字节SHA-256哈希密钥
     */
    fun generateDynamicKey(timeOffsetSeconds: Long = 0): ByteArray {
        val nowSec = System.currentTimeMillis() / 1000 + timeOffsetSeconds
        val slot = nowSec / (KEY_TIMEOUT / 1000L)
        val datePart = SimpleDateFormat("yyyyMMddHHmm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(nowSec * 1000))

        val raw = KEY_PREFIX + datePart + slot + KEY_SALT
        if (BuildConfig.DEBUG) Log.d("ApiSecurity", "密钥生成 - timeOffset=$timeOffsetSeconds, datePart=$datePart, slot=$slot, raw=$raw")

        val key = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))

        if (BuildConfig.DEBUG) Log.d("ApiSecurity", "生成的密钥SHA256: " + key.joinToString("") { "%02x".format(it) })
        return key
    }

    /**
     * AES-GCM加密
     *
     * @param plaintext 明文
     * @return Base64编码的密文（格式：IV + Tag + Ciphertext）
     */
    fun encrypt(plaintext: String): String {
        val key = generateDynamicKey()
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

        // 格式：IV + Tag + Ciphertext（与PHP保持一致）
        val packed = iv + tag + ciphertext

        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    /**
     * AES-GCM解密
     *
     * @param encoded Base64编码的密文
     * @return 明文，解密失败返回原始内容
     */
    fun decrypt(encoded: String): String {
        if (BuildConfig.DEBUG) Log.d("ApiSecurity", "开始解密，输入长度: ${encoded.length}")

        // 拆分时效签名格式：<base64>|<exp>|<sig>
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

        if (!isValidBase64(actualEncoded)) {
            if (BuildConfig.DEBUG) Log.d("ApiSecurity", "不是有效的Base64，直接返回")
            return encoded
        }

        val raw = Base64.decode(actualEncoded, Base64.NO_WRAP)
        if (BuildConfig.DEBUG) Log.d("ApiSecurity", "Base64解码后长度: ${raw.size}")

        if (raw.size <= 28) {
            if (BuildConfig.DEBUG) Log.d("ApiSecurity", "数据太短，不是有效密文")
            return encoded
        }

        val iv = raw.copyOfRange(0, 12)
        val tag = raw.copyOfRange(12, 28)
        val ciphertext = raw.copyOfRange(28, raw.size)

        if (BuildConfig.DEBUG) Log.d("ApiSecurity", "IV长度: ${iv.size}, Tag长度: ${tag.size}, Ciphertext长度: ${ciphertext.size}")

        val timeoutSec = KEY_TIMEOUT / 1000L
        val offsets = listOf(0L, -timeoutSec, -timeoutSec * 2, timeoutSec, -timeoutSec * 3)

        for (offset in offsets) {
            try {
                if (BuildConfig.DEBUG) Log.d("ApiSecurity", "尝试解密，timeOffset=$offset 秒")
                val key = generateDynamicKey(offset)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")

                val cipherWithTag = ciphertext + tag
                if (BuildConfig.DEBUG) Log.d("ApiSecurity", "cipherWithTag长度: ${cipherWithTag.size}")

                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(key, "AES"),
                    GCMParameterSpec(128, iv)
                )
                val plaintext = cipher.doFinal(cipherWithTag)
                val result = String(plaintext, Charsets.UTF_8)
                if (BuildConfig.DEBUG) Log.d("ApiSecurity", "解密成功: $result")
                return result
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e("ApiSecurity", "解密失败(timeOffset=$offset): ${e.message}")
            }
        }

        if (BuildConfig.DEBUG) Log.d("ApiSecurity", "所有时间槽都尝试失败，返回原始内容")
        return encoded
    }

    /**
     * 检查是否是有效的Base64字符串
     */
    private fun isValidBase64(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.length % 4 != 0) {
            return false
        }
        return try {
            Base64.decode(trimmed, Base64.NO_WRAP)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HMAC-SHA256 签名
    // ═══════════════════════════════════════════════════════════════

    /**
     * 生成HMAC-SHA256签名
     *
     * @param method HTTP方法 (GET/POST/PUT/DELETE)
     * @param url 请求URL路径
     * @param timestamp 时间戳(秒)
     * @param nonce 随机字符串
     * @param body 请求体字节
     * @return 十六进制签名字符串
     */
    fun sign(
        method: String,
        url: String,
        timestamp: Long,
        nonce: String,
        body: ByteArray
    ): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val secret = (SIGN_PREFIX + dateStr + SIGN_KEY).toByteArray(Charsets.UTF_8)

        val bodyHash = MessageDigest.getInstance("SHA-256")
            .digest(body)
            .joinToString("") { "%02x".format(it) }

        val payload = "${method.uppercase()}\n$url\n$timestamp\n$nonce\n$bodyHash"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))

        return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * 生成随机Nonce
     */
    fun generateNonce(): String =
        UUID.randomUUID().toString().replace("-", "")
}