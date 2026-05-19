package com.aijia.video

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.aijia.video.BuildConfig
import com.aijia.video.data.remote.ApiSecurity
import com.aijia.video.util.GifUrlReader
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 视频应用Application类
 */
@HiltAndroidApp
class VideoApplication : Application(), ImageLoaderFactory {

    companion object {
        lateinit var INSTANCE: VideoApplication
            private set
        private const val TAG = "VideoApplication"
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // Root/Hook 检测：检测到后仅降级，不强退（强退容易被patch）
        if (!BuildConfig.DEBUG && SecurityCheck.isCompromised()) {
            Log.w(TAG, "设备安全检查未通过，降级为游客模式")
            SecurityCheck.compromised = true
        }

        // 初始化API安全模块（从GIF隐写配置加载密钥）
        initApiSecurity()
    }

    /**
     * 初始化API安全模块
     * 从assets/loading_placeholder.gif读取加密配置
     */
    private fun initApiSecurity() {
        try {
            val config = GifUrlReader.readConfig(this, "loading_placeholder.gif")
            if (config != null) {
                ApiSecurity.init(config)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "API安全模块初始化成功")
                }
            } else {
                Log.e(TAG, "GIF加密配置读取失败，API请求将无法加密签名")
            }
        } catch (e: Exception) {
            Log.e(TAG, "API安全模块初始化异常: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        // 为图片加载创建单独的OkHttp客户端，不添加签名拦截器
        val imageOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(imageOkHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}

object SecurityCheck {
    @Volatile var compromised: Boolean = false

    fun isCompromised(): Boolean {
        return try {
            throw Exception()
        } catch (e: Exception) {
            e.stackTrace.any { it.className.contains("xposed", ignoreCase = true) }
        } || isRooted() || isFridaDetected()
    }

    private fun isRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su"
        )
        return paths.any { java.io.File(it).exists() }
    }

    private fun isFridaDetected(): Boolean {
        return try {
            val maps = java.io.File("/proc/self/maps").readText()
            maps.contains("frida") || maps.contains("gum-js-loop")
        } catch (_: Exception) { false }
    }
}
