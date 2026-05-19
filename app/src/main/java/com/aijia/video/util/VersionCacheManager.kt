package com.aijia.video.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 版本缓存管理器
 * 管理各个接口的version_id，用于判断数据是否需要重新加载
 */
@Singleton
class VersionCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "version_cache",
        Context.MODE_PRIVATE
    )

    companion object {
        // 缓存键定义
        const val KEY_AD_CONFIG = "ad_config"
        const val KEY_HOME_INDEX = "home_index"
        const val KEY_VOD_TYPES = "vod_types"
        const val KEY_USER_PERMISSION = "user_permission"

        // 缓存时间戳键后缀
        private const val SUFFIX_TIMESTAMP = "_timestamp"

        // 缓存过期时间（30分钟，从5分钟优化到30分钟减少网络请求）
        private const val CACHE_EXPIRE_TIME = 30 * 60 * 1000L

        // 缓存版本号，用于清除旧版本的缓存
        private const val CACHE_VERSION_KEY = "cache_version"
        private const val CURRENT_CACHE_VERSION = 1
    }

    init {
        // 检查缓存版本，如果不匹配则清除所有缓存
        val savedVersion = prefs.getInt(CACHE_VERSION_KEY, 0)
        android.util.Log.d("VersionCacheManager", "init - 保存的缓存版本: $savedVersion, 当前版本: $CURRENT_CACHE_VERSION")
        if (savedVersion != CURRENT_CACHE_VERSION) {
            android.util.Log.d("VersionCacheManager", "缓存版本不匹配，清除所有缓存")
            prefs.edit().clear().commit()
            prefs.edit().putInt(CACHE_VERSION_KEY, CURRENT_CACHE_VERSION).commit()
            android.util.Log.d("VersionCacheManager", "缓存已清除，新版本已保存")
        } else {
            android.util.Log.d("VersionCacheManager", "缓存版本匹配，保留现有缓存")
        }
    }

    /**
     * 获取指定键的version_id
     * 如果缓存已过期，返回空字符串强制重新加载
     */
    fun getVersionId(key: String): String {
        val timestamp = prefs.getLong(key + SUFFIX_TIMESTAMP, 0L)
        val versionId = prefs.getString(key, "") ?: ""

        android.util.Log.d("VersionCacheManager", "getVersionId($key) - versionId: $versionId, timestamp: $timestamp")

        // 如果缓存过期，返回空字符串
        if (isCacheExpired(key)) {
            android.util.Log.d("VersionCacheManager", "getVersionId($key) - 缓存已过期，返回空字符串")
            return ""
        }

        android.util.Log.d("VersionCacheManager", "getVersionId($key) - 返回: $versionId")
        return versionId
    }

    /**
     * 保存version_id
     */
    fun saveVersionId(key: String, versionId: String) {
        prefs.edit()
            .putString(key, versionId)
            .putLong(key + SUFFIX_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    /**
     * 检查缓存是否过期
     * @return true表示已过期，需要重新加载
     */
    fun isCacheExpired(key: String): Boolean {
        val timestamp = prefs.getLong(key + SUFFIX_TIMESTAMP, 0L)
        if (timestamp == 0L) return true

        val currentTime = System.currentTimeMillis()
        return (currentTime - timestamp) > CACHE_EXPIRE_TIME
    }

    /**
     * 清除指定键的缓存
     * 使用commit()确保同步清除，避免时序问题
     */
    fun clearCache(key: String) {
        prefs.edit()
            .remove(key)
            .remove(key + SUFFIX_TIMESTAMP)
            .commit()
    }

    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        prefs.edit().clear().apply()
    }

    /**
     * 判断是否需要重新加载数据
     * @param key 缓存键
     * @return true表示需要重新加载
     */
    fun shouldReload(key: String): Boolean {
        // 如果缓存过期，需要重新加载
        if (isCacheExpired(key)) {
            return true
        }

        // 如果没有version_id，需要重新加载
        val versionId = getVersionId(key)
        return versionId.isEmpty()
    }
}
