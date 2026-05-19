package com.aijia.video.util

import android.content.Context
import android.content.SharedPreferences
import com.aijia.video.data.model.AdConfig
import com.aijia.video.data.model.AdItem
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 广告缓存管理器
 * 管理广告配置的本地缓存，并验证广告ID是否变化
 */
@Singleton
class AdCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ad_cache",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_AD_CONFIG = "ad_config"
        private const val KEY_STARTUP_AD_ID = "startup_ad_id"
        private const val KEY_PLAYER_AD_ID = "player_ad_id"
        private const val KEY_PLAYER_BOTTOM_AD_ID = "player_bottom_ad_id"

        // 新增：按类型缓存广告
        private const val KEY_AD_PREFIX = "ad_"
    }

    /**
     * 保存广告配置到本地缓存
     */
    fun saveAdConfig(adConfig: AdConfig) {
        val json = gson.toJson(adConfig)
        prefs.edit()
            .putString(KEY_AD_CONFIG, json)
            .putInt(KEY_STARTUP_AD_ID, adConfig.startupAd?.id ?: 0)
            .putInt(KEY_PLAYER_AD_ID, adConfig.playerAd?.id ?: 0)
            .putInt(KEY_PLAYER_BOTTOM_AD_ID, adConfig.playerBottomAd?.id ?: 0)
            .apply()

        android.util.Log.d("AdCacheManager", "保存广告配置: startup_id=${adConfig.startupAd?.id}, player_id=${adConfig.playerAd?.id}")
    }

    /**
     * 获取缓存的广告配置
     */
    fun getCachedAdConfig(): AdConfig? {
        val json = prefs.getString(KEY_AD_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, AdConfig::class.java)
        } catch (e: Exception) {
            android.util.Log.e("AdCacheManager", "解析缓存广告配置失败", e)
            null
        }
    }

    /**
     * 验证广告ID是否变化
     * @return true表示ID有变化，需要清除缓存
     */
    fun shouldClearCache(newAdConfig: AdConfig): Boolean {
        val cachedStartupId = prefs.getInt(KEY_STARTUP_AD_ID, 0)
        val cachedPlayerId = prefs.getInt(KEY_PLAYER_AD_ID, 0)
        val cachedPlayerBottomId = prefs.getInt(KEY_PLAYER_BOTTOM_AD_ID, 0)

        val newStartupId = newAdConfig.startupAd?.id ?: 0
        val newPlayerId = newAdConfig.playerAd?.id ?: 0
        val newPlayerBottomId = newAdConfig.playerBottomAd?.id ?: 0

        val shouldClear = cachedStartupId != newStartupId ||
                cachedPlayerId != newPlayerId ||
                cachedPlayerBottomId != newPlayerBottomId

        if (shouldClear) {
            android.util.Log.d("AdCacheManager", "广告ID变化，需要清除缓存")
            android.util.Log.d("AdCacheManager", "启动页: $cachedStartupId -> $newStartupId")
            android.util.Log.d("AdCacheManager", "播放器: $cachedPlayerId -> $newPlayerId")
            android.util.Log.d("AdCacheManager", "播放器底部: $cachedPlayerBottomId -> $newPlayerBottomId")
        }

        return shouldClear
    }

    /**
     * 清除广告缓存
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        android.util.Log.d("AdCacheManager", "已清除广告缓存")
    }

    // ========== 新增：按类型缓存广告 ==========

    /**
     * 保存单个广告到本地缓存
     * @param adType 广告类型：startup/player/player_bottom/short_video
     * @param adItem 广告内容
     */
    fun saveAd(adType: String, adItem: AdItem) {
        val key = "$KEY_AD_PREFIX$adType"
        val json = gson.toJson(adItem)
        prefs.edit().putString(key, json).apply()
        android.util.Log.d("AdCacheManager", "保存广告: type=$adType, id=${adItem.id}")
    }

    /**
     * 获取单个广告缓存
     * @param adType 广告类型：startup/player/player_bottom/short_video
     * @return 广告内容，如果不存在返回 null
     */
    fun getAd(adType: String): AdItem? {
        val key = "$KEY_AD_PREFIX$adType"
        val json = prefs.getString(key, null) ?: return null
        return try {
            gson.fromJson(json, AdItem::class.java)
        } catch (e: Exception) {
            android.util.Log.e("AdCacheManager", "解析缓存广告失败: type=$adType", e)
            null
        }
    }

    /**
     * 清除单个广告缓存
     * @param adType 广告类型
     */
    fun clearAd(adType: String) {
        val key = "$KEY_AD_PREFIX$adType"
        prefs.edit().remove(key).apply()
        android.util.Log.d("AdCacheManager", "清除广告缓存: type=$adType")
    }
}
