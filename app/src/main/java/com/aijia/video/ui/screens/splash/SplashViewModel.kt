package com.aijia.video.ui.screens.splash

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.aijia.video.VideoApplication
import com.aijia.video.data.model.AdConfig
import com.aijia.video.data.repository.AdRepository
import com.aijia.video.data.repository.SessionManager
import com.aijia.video.data.repository.VideoRepository
import com.aijia.video.util.PreloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val adRepository: AdRepository,
    private val sessionManager: SessionManager,
    private val preloadManager: PreloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        loadPermissionAndData()
    }

    /**
     * 启动流程：先获取权限，再根据权限决定是否加载广告
     */
    private fun loadPermissionAndData() {
        viewModelScope.launch {
            try {
                android.util.Log.d("SplashViewModel", "=== 启动流程开始 ===")

                // 第一步：获取用户权限
                android.util.Log.d("SplashViewModel", "1. 获取用户权限...")
                val permissionResult = videoRepository.getUserPermission()
                val permission = permissionResult.getOrNull() ?: sessionManager.getPermission()

                android.util.Log.d("SplashViewModel", "权限获取完成: group_id=${permission.groupId}, ad=${permission.ad}")

                // 第二步：根据权限决定是否请求广告
                val shouldRequestAd = permission.ad == 1
                android.util.Log.d("SplashViewModel", "2. 是否请求广告: $shouldRequestAd")

                // 第三步：并行加载数据
                android.util.Log.d("SplashViewModel", "3. 并行加载数据...")
                coroutineScope {
                    // 并行请求首页数据
                    val sectionsDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                        videoRepository.getHomeRecommend()
                    }
                    val videoTypesDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                        videoRepository.getVideoTypes()
                    }

                    // 如果需要广告，并行请求启动页广告
                    val adDeferred = if (shouldRequestAd) {
                        async(kotlinx.coroutines.Dispatchers.IO) {
                            adRepository.getAdByType("startup")
                        }
                    } else {
                        null
                    }

                    // 等待首页推荐数据
                    val sectionsResult = sectionsDeferred.await()
                    val videoTypesResult = videoTypesDeferred.await()

                    // 保存预加载数据
                    val sections = sectionsResult.getOrNull()
                    val videoTypes = videoTypesResult.getOrNull()
                    if (sectionsResult.isSuccess || videoTypesResult.isSuccess) {
                        preloadManager.savePreloadedData(sections, videoTypes)
                        android.util.Log.d("SplashViewModel", "首页推荐预加载完成 - sections: ${sections?.size}, videoTypes: ${videoTypes?.size}")
                    }

                    // 处理广告
                    if (adDeferred != null) {
                        val adResult = adDeferred.await()
                        if (adResult.isSuccess) {
                            val adResponse = adResult.getOrNull()
                            if (adResponse != null && adResponse.showAd) {
                                // 需要显示广告
                                val adItem = if (adResponse.dataChanged && adResponse.content != null) {
                                    android.util.Log.d("SplashViewModel", "使用新广告: id=${adResponse.content.id}")
                                    adResponse.content
                                } else {
                                    // 使用本地缓存
                                    val cached = adRepository.getCachedAdConfig()?.startupAd
                                    android.util.Log.d("SplashViewModel", "使用缓存广告: id=${cached?.id}")
                                    cached
                                }

                                if (adItem != null && adItem.enabled) {
                                    val adConfig = AdConfig(startupAd = adItem)
                                    _uiState.value = _uiState.value.copy(
                                        adConfig = adConfig,
                                        shouldShowAds = true
                                    )
                                    // 预加载广告图片
                                    if (adItem.imageUrl.isNotBlank()) {
                                        preloadSingleAdImage(adItem.imageUrl)
                                    }
                                    android.util.Log.d("SplashViewModel", "启动页广告已设置")
                                } else {
                                    _uiState.value = _uiState.value.copy(shouldShowAds = false)
                                    android.util.Log.d("SplashViewModel", "广告未启用")
                                }
                            } else {
                                _uiState.value = _uiState.value.copy(shouldShowAds = false)
                                android.util.Log.d("SplashViewModel", "不显示广告 (show_ad=false)")
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(shouldShowAds = false)
                            android.util.Log.e("SplashViewModel", "加载广告失败: ${adResult.exceptionOrNull()?.message}")
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(shouldShowAds = false)
                        android.util.Log.d("SplashViewModel", "跳过广告请求 (permission.ad=0)")
                    }
                }

                android.util.Log.d("SplashViewModel", "=== 启动流程完成 ===")
            } catch (e: Exception) {
                android.util.Log.e("SplashViewModel", "启动流程异常", e)
                _uiState.value = _uiState.value.copy(shouldShowAds = false)
            }
        }
    }

    private suspend fun preloadSingleAdImage(imageUrl: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = VideoApplication.INSTANCE
                val imageLoader = ImageLoader.Builder(context).build()
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .build()
                imageLoader.execute(request)
                android.util.Log.d("SplashViewModel", "预加载广告图片: $imageUrl")
            } catch (e: Exception) {
                android.util.Log.e("SplashViewModel", "预加载广告图片失败", e)
            }
        }
    }

    private suspend fun preloadAdImages(adConfig: AdConfig) {
        // 使用 Dispatchers.IO 避免阻塞主线程
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val context = VideoApplication.INSTANCE
            val imageLoader = ImageLoader.Builder(context).build()

            // 收集所有广告图片URL
            val imageUrls = mutableListOf<String>()

            adConfig.startupAd?.let {
                if (it.enabled && it.imageUrl.isNotBlank()) {
                    imageUrls.add(it.imageUrl)
                }
            }

            adConfig.playerAd?.let {
                if (it.enabled && it.imageUrl.isNotBlank()) {
                    imageUrls.add(it.imageUrl)
                }
            }

            adConfig.playerBottomAd?.let {
                if (it.enabled && it.imageUrl.isNotBlank()) {
                    imageUrls.add(it.imageUrl)
                }
            }

            adConfig.shortVideoAds?.forEach { ad ->
                if (ad.enabled && ad.imageUrl.isNotBlank()) {
                    imageUrls.add(ad.imageUrl)
                }
            }

            // 预加载所有图片（并行）
            coroutineScope {
                imageUrls.forEach { url ->
                    launch {
                        try {
                            val request = ImageRequest.Builder(context)
                                .data(url)
                                .build()
                            imageLoader.execute(request)
                        } catch (e: Exception) {
                            // 预加载失败不影响主流程
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun handleAdClick(linkUrl: String, context: Context) {
        // linkUrl为空时不跳转
        if (linkUrl.isBlank()) {
            android.util.Log.d("SplashViewModel", "广告linkUrl为空，不执行跳转")
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("SplashViewModel", "打开广告链接失败", e)
            e.printStackTrace()
        }
    }

    data class SplashUiState(
        val isReady: Boolean = false,
        val adConfig: AdConfig? = null,
        val shouldShowAds: Boolean = true
    )
}