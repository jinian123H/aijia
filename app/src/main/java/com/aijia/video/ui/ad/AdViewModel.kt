package com.aijia.video.ui.ad

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.aijia.video.BuildConfig
import com.aijia.video.data.model.AdConfig
import com.aijia.video.util.AdStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 广告ViewModel
 * 轻量委托层：实际状态由 [AdStateManager] 单例管理
 * 保留此层便于 Compose 的 hiltViewModel() 注入和生命周期绑定
 */
@HiltViewModel
class AdViewModel @Inject constructor(
    private val adStateManager: AdStateManager
) : ViewModel() {

    init {
        // 触发全局广告配置加载（仅首次有效）
        adStateManager.loadIfNeeded()
        if (BuildConfig.DEBUG) Log.d("AdViewModel", "初始化，委托 AdStateManager 加载广告配置")
    }

    /** 广告配置 */
    val adConfig: StateFlow<AdConfig?> = adStateManager.adConfig

    /** 是否有广告权限 */
    val shouldShowAds: StateFlow<Boolean> = adStateManager.hasAdPermission

    /** 检查是否应显示广告（同步方法，用于非 Flow 场景） */
    fun shouldShowAds(): Boolean {
        return adStateManager.hasAdPermission.value
    }

    /** 打开广告链接 */
    fun handleAdClick(url: String, context: Context) {
        if (url.isBlank()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }.onFailure {
            if (BuildConfig.DEBUG) Log.e("AdViewModel", "打开广告链接失败", it)
        }
    }
}
