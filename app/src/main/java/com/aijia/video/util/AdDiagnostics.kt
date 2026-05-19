package com.aijia.video.util

import android.util.Log
import com.aijia.video.data.model.AdConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 广告诊断工具
 * 用于检查广告配置和图片加载问题
 */
object AdDiagnostics {
    private const val TAG = "AdDiagnostics"

    /**
     * 诊断广告配置
     */
    suspend fun diagnoseAdConfig(adConfig: AdConfig?) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "========== 广告配置诊断 ==========")

            if (adConfig == null) {
                Log.e(TAG, "❌ 广告配置为 null")
                return@withContext
            }

            Log.d(TAG, "✓ 广告配置已加载")

            // 检查启动页广告
            Log.d(TAG, "\n--- 启动页广告 ---")
            adConfig.startupAd?.let {
                Log.d(TAG, "ID: ${it.id}, 启用: ${it.enabled}, URL: ${it.imageUrl}")
            } ?: Log.d(TAG, "未配置")

            // 检查播放器广告
            Log.d(TAG, "\n--- 播放器广告 ---")
            adConfig.playerAd?.let {
                Log.d(TAG, "ID: ${it.id}, 启用: ${it.enabled}, URL: ${it.imageUrl}")
            } ?: Log.d(TAG, "未配置")

            // 检查播放器底部广告
            Log.d(TAG, "\n--- 播放器底部广告 ---")
            adConfig.playerBottomAd?.let {
                Log.d(TAG, "ID: ${it.id}, 启用: ${it.enabled}, URL: ${it.imageUrl}")
            } ?: Log.d(TAG, "未配置")

            // 检查短视频广告
            Log.d(TAG, "\n--- 短视频广告 ---")
            val shortVideoAds = adConfig.shortVideoAds
            if (shortVideoAds.isNullOrEmpty()) {
                Log.d(TAG, "未配置")
            } else {
                Log.d(TAG, "总数: ${shortVideoAds.size}")
                shortVideoAds.forEachIndexed { index, ad ->
                    Log.d(TAG, "[$index] ID: ${ad.id}, 启用: ${ad.enabled}, Sort: ${ad.sort}, URL: ${ad.imageUrl}")
                }
            }

            Log.d(TAG, "\n========== 诊断完成 ==========")
        }
    }
}

