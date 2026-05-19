package com.aijia.video.util

import android.util.Log
import com.aijia.video.BuildConfig
import com.aijia.video.data.model.AdConfig
import com.aijia.video.data.repository.AdRepository
import com.aijia.video.data.repository.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 广告配置共享管理器
 * - 单例，全局只加载一次广告配置
 * - 响应式观察广告权限变化
 * - 提供组合状态避免上游多次收集
 */
@Singleton
class AdStateManager @Inject constructor(
    private val adRepository: AdRepository,
    sessionManager: SessionManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 原始广告配置 */
    private val _adConfig = MutableStateFlow<AdConfig?>(null)
    val adConfig: StateFlow<AdConfig?> = _adConfig.asStateFlow()

    /** 是否有广告权限（响应式） */
    val hasAdPermission: StateFlow<Boolean> = sessionManager.permissionFlow
        .map { it.hasPermission("ad") }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    /** 广告是否已在加载中 */
    private var loadAttempted = false

    /** 加载广告配置（仅首次生效） */
    fun loadIfNeeded() {
        if (loadAttempted) return
        loadAttempted = true
        scope.launch {
            if (BuildConfig.DEBUG) Log.d("AdStateManager", "首次加载广告配置")
            val result = adRepository.getAdConfig()
            if (result.isSuccess) {
                _adConfig.value = result.getOrNull()
                if (BuildConfig.DEBUG) {
                    val config = result.getOrNull()
                    Log.d("AdStateManager", "广告配置加载成功: " +
                            "startup=${config?.startupAd?.enabled}, " +
                            "player=${config?.playerAd?.enabled}, " +
                            "bottom=${config?.playerBottomAd?.enabled}, " +
                            "shortVideo=${config?.shortVideoAds?.size ?: 0}个")
                }
            } else {
                // 加载失败时尝试使用缓存
                if (BuildConfig.DEBUG) Log.w("AdStateManager", "网络加载失败，尝试缓存")
                val cached = adRepository.getCachedAdConfig()
                if (cached != null) {
                    _adConfig.value = cached
                    if (BuildConfig.DEBUG) Log.d("AdStateManager", "使用缓存广告配置")
                }
            }
        }
    }

    /** 强制重新加载（登录/权限变更时） */
    fun forceReload() {
        loadAttempted = false
        loadIfNeeded()
    }

    companion object {
        private const val TAG = "AdStateManager"
    }
}
