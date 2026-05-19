package com.aijia.video.util

import com.aijia.video.data.model.HomeSection
import com.aijia.video.data.model.VideoType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 预加载数据管理器
 * 在启动页加载广告/视频时预加载首页数据，提升用户体验
 */
@Singleton
class PreloadManager @Inject constructor() {

    // 预加载的首页推荐数据（推荐区块列表）
    private var preloadedSections: List<HomeSection>? = null

    // 预加载的视频分类
    private var preloadedVideoTypes: List<VideoType>? = null

    // 预加载时间戳（用于判断数据是否过期）
    private var preloadTimestamp: Long = 0

    // 预加载数据有效期：5分钟
    private val cacheValidDuration: Long = 5 * 60 * 1000

    /**
     * 保存预加载的首页推荐数据
     */
    fun savePreloadedData(sections: List<HomeSection>?, videoTypes: List<VideoType>?) {
        synchronized(this) {
            preloadedSections = sections
            preloadedVideoTypes = videoTypes
            preloadTimestamp = System.currentTimeMillis()
            android.util.Log.d("PreloadManager", "预加载数据已保存 - sections: ${sections?.size}, videoTypes: ${videoTypes?.size}")
        }
    }

    /**
     * 获取预加载的首页推荐数据
     * @return 如果数据有效返回数据，否则返回null
     */
    fun getPreloadedSections(): List<HomeSection>? {
        synchronized(this) {
            if (!isCacheValid()) {
                android.util.Log.d("PreloadManager", "预加载首页数据已过期")
                return null
            }
            return preloadedSections
        }
    }

    /**
     * 获取预加载的分类数据
     * @return 如果数据有效返回数据，否则返回null
     */
    fun getPreloadedVideoTypes(): List<VideoType>? {
        synchronized(this) {
            if (!isCacheValid()) {
                android.util.Log.d("PreloadManager", "预加载分类数据已过期")
                return null
            }
            return preloadedVideoTypes
        }
    }

    /**
     * 检查是否有有效的预加载数据
     */
    fun hasPreloadedData(): Boolean {
        synchronized(this) {
            return isCacheValid() && (preloadedSections != null || preloadedVideoTypes != null)
        }
    }

    /**
     * 清除预加载数据
     */
    fun clearPreloadedData() {
        synchronized(this) {
            preloadedSections = null
            preloadedVideoTypes = null
            preloadTimestamp = 0
            android.util.Log.d("PreloadManager", "预加载数据已清除")
        }
    }

    /**
     * 检查缓存是否有效
     */
    private fun isCacheValid(): Boolean {
        if (preloadTimestamp == 0L) return false
        return System.currentTimeMillis() - preloadTimestamp < cacheValidDuration
    }
}