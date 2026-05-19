package com.aijia.video.util

import android.content.Context
import android.util.Log
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * GIF隐写配置读取器
 * 支持V1(明文URL)和V2(加密JSON)两种格式
 */
object GifUrlReader {

    private const val TAG = "GifUrlReader"

    // ═══════════════════════════════════════════════════════════════
    // 格式标记
    // ═══════════════════════════════════════════════════════════════

    /** V1标记: KMU - 明文URL */
    private val MARKER_V1 = byteArrayOf(0x4B, 0x4D, 0x55)

    /** V2标记: KMU\x02 - AES-GCM加密JSON */
    private val MARKER_V2 = byteArrayOf(0x4B, 0x4D, 0x55, 0x02)

    // ═══════════════════════════════════════════════════════════════
    // 配置数据结构
    // ═══════════════════════════════════════════════════════════════

    /**
     * GIF隐写配置
     *
     * @param url 后端API地址
     * @param keyPrefix AES加密KEY_PREFIX
     * @param keySalt AES加密KEY_SALT
     * @param keyTimeout 密钥滚动周期(秒)
     * @param signPrefix HMAC签名SIGN_PREFIX
     * @param signKey HMAC签名SIGN_KEY
     */
    data class Config(
        val url: String,
        val keyPrefix: String,
        val keySalt: String,
        val keyTimeout: Long,
        val signPrefix: String,
        val signKey: String
    )

    // ═══════════════════════════════════════════════════════════════
    // 公开API
    // ═══════════════════════════════════════════════════════════════

    /**
     * 从assets中读取完整配置
     *
     * @param context Android Context
     * @param assetName assets中的GIF文件名
     * @return Config对象，读取失败返回null
     */
    fun readConfig(context: Context, assetName: String = "loading_placeholder.gif"): Config? {
        return try {
            val bytes = context.assets.open(assetName).readBytes()
            parseConfig(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "读取GIF配置失败: ${e.message}")
            null
        }
    }

    /**
     * 从assets中读取URL（兼容旧代码）
     *
     * @param context Android Context
     * @param assetName assets中的GIF文件名
     * @return URL字符串，读取失败返回null
     */
    fun readUrl(context: Context, assetName: String = "loading_placeholder.gif"): String? {
        return readConfig(context, assetName)?.url
    }

    // ═══════════════════════════════════════════════════════════════
    // 解析逻辑
    // ═══════════════════════════════════════════════════════════════

    /**
     * 解析GIF字节数据中的配置
     * 优先尝试V2格式，失败则回退到V1格式
     */
    private fun parseConfig(bytes: ByteArray): Config? {
        // 先尝试V2格式（加密JSON）
        val v2Pos = findMarker(bytes, MARKER_V2)
        if (v2Pos >= 0) {
            if (com.aijia.video.BuildConfig.DEBUG) {
                Log.d(TAG, "检测到V2加密格式，偏移: $v2Pos")
            }
            return parseV2(bytes, v2Pos)
        }

        // 回退到V1格式（明文URL）
        val v1Pos = findMarker(bytes, MARKER_V1)
        if (v1Pos >= 0) {
            if (com.aijia.video.BuildConfig.DEBUG) {
                Log.d(TAG, "检测到V1明文格式，偏移: $v1Pos")
            }
            return parseV1(bytes, v1Pos)
        }

        Log.w(TAG, "未找到配置标记")
        return null
    }

    /**
     * 解析V2格式：AES-GCM加密的JSON
     */
    private fun parseV2(bytes: ByteArray, markerPos: Int): Config? {
        return try {
            val ivStart = markerPos + MARKER_V2.size

            // IV: 12字节
            if (ivStart + 12 > bytes.size) {
                Log.e(TAG, "V2格式数据不完整：缺少IV")
                return null
            }
            val iv = bytes.copyOfRange(ivStart, ivStart + 12)

            // 密文: 从IV结束到文件末尾（最后1字节是GIF Trailer 0x3B，跳过）
            val ciphertextStart = ivStart + 12
            val ciphertextEnd = if (bytes.last() == 0x3B.toByte()) bytes.size - 1 else bytes.size

            if (ciphertextEnd <= ciphertextStart) {
                Log.e(TAG, "V2格式数据不完整：缺少密文")
                return null
            }

            val ciphertext = bytes.copyOfRange(ciphertextStart, ciphertextEnd)

            // 解密
            val json = decryptAesGcm(iv, ciphertext)

            // 解析JSON
            parseJsonConfig(json).also {
                if (com.aijia.video.BuildConfig.DEBUG) {
                    Log.d(TAG, "V2解密成功: url=${it?.url}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "V2格式解密失败: ${e.message}")
            null
        }
    }

    /**
     * 解析V1格式：明文URL
     */
    private fun parseV1(bytes: ByteArray, markerPos: Int): Config? {
        return try {
            val urlStart = markerPos + MARKER_V1.size
            var urlEnd = urlStart

            while (urlEnd < bytes.size && bytes[urlEnd] != 0x00.toByte() && bytes[urlEnd] != 0x3B.toByte()) {
                urlEnd++
            }

            if (urlEnd <= urlStart) {
                Log.e(TAG, "V1格式数据不完整：缺少URL")
                return null
            }

            val url = String(bytes, urlStart, urlEnd - urlStart, Charsets.UTF_8)

            // 返回默认配置
            Config(
                url = url,
                keyPrefix = "kumiao_enc_v3_",
                keySalt = "K8#mZ!qL9vX2",
                keyTimeout = 300L,
                signPrefix = "kumiao_sign_v3_",
                signKey = "N5@pR#wY7kJ1"
            ).also {
                Log.d(TAG, "V1解析成功: url=$url")
            }
        } catch (e: Exception) {
            Log.e(TAG, "V1格式解析失败: ${e.message}")
            null
        }
    }

    /**
     * 查找标记位置
     */
    private fun findMarker(bytes: ByteArray, marker: ByteArray): Int {
        for (i in 0..(bytes.size - marker.size)) {
            var found = true
            for (j in marker.indices) {
                if (bytes[i + j] != marker[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }

    // ═══════════════════════════════════════════════════════════════
    // 解密
    // ═══════════════════════════════════════════════════════════════

    /**
     * AES-GCM解密
     */
    private fun decryptAesGcm(iv: ByteArray, ciphertext: ByteArray): String {
        val keyBytes = SecureKeyHolder.getKey()
        val key = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    /**
     * 解析JSON配置
     */
    private fun parseJsonConfig(json: String): Config {
        val obj = JSONObject(json)
        return Config(
            url = obj.getString("url"),
            keyPrefix = obj.optString("kp", "kumiao_enc_v3_"),
            keySalt = obj.optString("ks", "K8#mZ!qL9vX2"),
            keyTimeout = obj.optLong("kt", 300L),
            signPrefix = obj.optString("sp", "kumiao_sign_v3_"),
            signKey = obj.optString("sk", "N5@pR#wY7kJ1")
        )
    }
}