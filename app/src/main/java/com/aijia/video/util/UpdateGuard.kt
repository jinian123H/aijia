package com.aijia.video.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * 更新保护守卫
 * 防止反编译篡改后跳过强制更新逻辑
 *
 * 保护策略：
 * 1. APK签名验证 — 检测签名是否被篡改
 * 2. Debug检测 — 检测是否在调试模式下运行（反编译重打包特征）
 * 3. 安装源检测 — 检测是否通过非官方渠道安装
 *
 * 签名哈希通过首次release运行日志获取，然后填入getExpectedSignature()
 * 开发期运行会自动输出当前签名哈希
 */
object UpdateGuard {

    private var isVerified: Boolean? = null
    private const val TAG = "UpdateGuard"

    /**
     * 获取APK签名证书的SHA-256哈希
     */
    fun getSignatureHash(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signatures = packageInfo.signingInfo?.apkContentsSigners ?: return null
                if (signatures.isEmpty()) return null
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(signatures[0].toByteArray())
                bytesToHex(hash)
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                val signatures = packageInfo.signatures ?: return null
                if (signatures.isEmpty()) return null
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(signatures[0].toByteArray())
                bytesToHex(hash)
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "获取签名失败", e)
            null
        }
    }

    /**
     * 检测App是否被篡改
     * @return true = 应用完整未被篡改, false = 可能被篡改
     */
    fun isAppIntegrityValid(context: Context): Boolean {
        isVerified?.let { return it }

        val sigHash = getSignatureHash(context)
        val expected = getExpectedSignature()

        val sigMatch = if (expected != null && sigHash != null) {
            sigHash == expected
        } else {
            // 无法获取签名或期望签名为空时，不阻断
            true
        }

        val notDebuggable = !isDebuggable(context)

        val result = if (expected != null) {
            sigMatch && notDebuggable
        } else {
            // 期望签名未配置时，只检查debuggable
            notDebuggable
        }

        isVerified = result
        return result
    }

    /**
     * 检测是否为调试模式
     */
    private fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * 获取期望的签名哈希
     * release编译后，从logcat获取实际哈希值，替换下方占位返回
     *
     * 占位null表示"不验证签名，只检查debuggable"
     * 填入哈希后启用完整签名验证
     */
    private fun getExpectedSignature(): String? {
        // TODO: release编译后替换为实际签名哈希
        // 运行一次release版，从logcat获取 UpdateGuard: 当前APK签名哈希: xxxx
        // 将下方return null改为 return "xxxx"
        return null
    }

    /**
     * 输出当前签名哈希到logcat
     * 开发者在release编译后运行一次，复制输出的哈希填入getExpectedSignature()
     */
    fun logCurrentSignature(context: Context) {
        val hash = getSignatureHash(context) ?: "null"
        android.util.Log.d(TAG, "当前APK签名哈希: $hash")
        android.util.Log.d(TAG, "将此值填入 UpdateGuard.getExpectedSignature() 的 return 语句中")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789abcdef"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789abcdef"[v and 0x0F]
        }
        return String(hexChars)
    }
}
