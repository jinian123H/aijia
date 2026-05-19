package com.aijia.video.data.repository

import android.util.Log
import com.aijia.video.data.model.AdConfig
import com.aijia.video.data.model.AdConfigVersionedData
import com.aijia.video.data.model.AdResponse
import com.aijia.video.data.model.ApiResponse
import com.aijia.video.data.remote.ApiService
import com.aijia.video.util.AdCacheManager
import com.aijia.video.util.NetworkErrorHandler
import com.aijia.video.util.VersionCacheManager
import com.aijia.video.data.repository.SessionManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AdRepository @Inject constructor(
    private val apiService: ApiService,
    private val versionCacheManager: VersionCacheManager,
    private val adCacheManager: AdCacheManager,
    private val sessionManager: SessionManager
) {
    private val gson = Gson()
    private val adConfigType = object : TypeToken<ApiResponse<AdConfigVersionedData>>() {}.type

    /**
     * 仅从本地缓存获取广告配置，不发起网络请求
     */
    fun getCachedAdConfig(): AdConfig? {
        return adCacheManager.getCachedAdConfig()
    }

    suspend fun getAdConfigForce(): Result<AdConfig?> {
        return withContext(Dispatchers.IO) {
            try {
                val token = sessionManager.getToken()
                val userInfo = sessionManager.getUserInfo()
                val userId = userInfo?.userId
                val response = apiService.getAdConfig("", userId, token)
                if (response.isSuccessful) {
                    val json = response.body()?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                    val body: ApiResponse<AdConfigVersionedData> = gson.fromJson(json, adConfigType)
                    if (body.code == 200) {
                        val versionedData = body.data
                        versionCacheManager.saveVersionId(VersionCacheManager.KEY_AD_CONFIG, versionedData.versionId)
                        val newAdConfig = versionedData.content
                        if (newAdConfig != null) {
                            adCacheManager.saveAdConfig(newAdConfig)
                            android.util.Log.d("AdRepository", "强制拉取广告配置成功，短视频广告数: ${newAdConfig.shortVideoAds?.size ?: 0}")
                            Result.success(newAdConfig)
                        } else {
                            Result.failure(Exception("广告配置内容为空"))
                        }
                    } else {
                        Result.failure(Exception(body.msg ?: "获取广告配置失败"))
                    }
                } else {
                    Result.failure(Exception("网络请求失败"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AdRepository", "强制拉取广告配置异常", e)
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }

    suspend fun getAdConfig(): Result<AdConfig?> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = VersionCacheManager.KEY_AD_CONFIG
                val versionId = versionCacheManager.getVersionId(cacheKey)
                val token = sessionManager.getToken()
                val userInfo = sessionManager.getUserInfo()
                val userId = userInfo?.userId

                android.util.Log.d("AdRepository", "getAdConfig - 发送version_id: $versionId, user_id: $userId")

                val response = apiService.getAdConfig(versionId, userId, token)
                if (response.isSuccessful) {
                    val json = response.body()?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                    val body: ApiResponse<AdConfigVersionedData> = gson.fromJson(json, adConfigType)
                    if (body.code == 200) {
                        val versionedData = body.data

                        Log.d("AdRepository", "getAdConfig - 服务端version_id: ${versionedData.versionId}")
                        Log.d("AdRepository", "getAdConfig - data_changed: ${versionedData.dataChanged}")

                        if (versionedData.dataChanged && versionedData.content != null) {
                            val newAdConfig = versionedData.content

                            Log.d("AdRepository", "广告配置数据变化")
                            Log.d("AdRepository", "短视频广告数: ${newAdConfig.shortVideoAds?.size ?: 0}")
                            newAdConfig.shortVideoAds?.forEachIndexed { index, ad ->
                                Log.d("AdRepository", "广告[$index]: id=${ad.id}, enabled=${ad.enabled}, imageUrl=${ad.imageUrl}, sort=${ad.sort}")
                            }

                            if (adCacheManager.shouldClearCache(newAdConfig)) {
                                Log.d("AdRepository", "广告ID变化，清除本地缓存")
                                adCacheManager.clearCache()
                            }

                            // 先保存内容，再保存 version_id（防止崩溃后 version_id 更新但内容丢失）
                            adCacheManager.saveAdConfig(newAdConfig)
                            if (versionedData.versionId.isNotBlank()) {
                                versionCacheManager.saveVersionId(VersionCacheManager.KEY_AD_CONFIG, versionedData.versionId)
                            }
                            Result.success(newAdConfig)
                        } else {
                            Log.d("AdRepository", "数据未变化，使用本地缓存")
                            // 仅当本地有缓存时才更新 version_id，避免问题2：本地空缓存+新version_id
                            if (adCacheManager.getCachedAdConfig() != null && versionedData.versionId.isNotBlank()) {
                                versionCacheManager.saveVersionId(VersionCacheManager.KEY_AD_CONFIG, versionedData.versionId)
                            }
                            val cachedConfig = adCacheManager.getCachedAdConfig()
                            Result.success(cachedConfig)
                        }
                    } else {
                        Result.failure(Exception(body.msg ?: "获取广告配置失败"))
                    }
                } else {
                    Result.failure(Exception("网络请求失败"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AdRepository", "getAdConfig异常", e)
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }

    /**
     * 按类型获取广告（新接口）
     * @param adType 广告类型：startup/player/player_bottom/short_video
     * @return AdResponse 包含 show_ad、data_changed、version_id、content
     */
    suspend fun getAdByType(adType: String): Result<AdResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = "ad_$adType"
                val versionId = versionCacheManager.getVersionId(cacheKey)
                val token = sessionManager.getToken()
                val authHeader = if (token != null) "Bearer $token" else null

                android.util.Log.d("AdRepository", "getAdByType - type: $adType, version_id: $versionId")

                val response = apiService.getAdByType(adType, versionId, authHeader)
                if (response.isSuccessful) {
                    val json = response.body()?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                    val type = object : TypeToken<ApiResponse<AdResponse>>() {}.type
                    val apiResponse: ApiResponse<AdResponse> = gson.fromJson(json, type)

                    if (apiResponse.code == 200) {
                        var adResponse = apiResponse.data

                        android.util.Log.d("AdRepository", "getAdByType - show_ad: ${adResponse.showAd}, data_changed: ${adResponse.dataChanged}")

                        // 兜底：data_changed=false 但本地对应广告为空，且服务器要求显示广告
                        // 说明本地缓存丢失，重新请求（清空version_id）
                        if (adResponse.showAd && !adResponse.dataChanged && adResponse.content == null) {
                            val cached = adCacheManager.getAd(adType)
                            if (cached == null) {
                                android.util.Log.w("AdRepository", "本地广告缓存为空但服务端返回data_changed=false，重试请求")
                                versionCacheManager.clearCache(cacheKey)
                                return@withContext getAdByType(adType)
                            }
                        }

                        // 先保存广告内容，再保存 version_id
                        // 防止崩溃后 version_id 已更新但内容丢失，导致下次请求跳过内容下载
                        if (adResponse.dataChanged && adResponse.content != null) {
                            Log.d("AdRepository", "广告数据变化，更新缓存: id=${adResponse.content.id}")
                            adCacheManager.saveAd(adType, adResponse.content)
                        }
                        if (adResponse.versionId.isNotBlank()) {
                            versionCacheManager.saveVersionId(cacheKey, adResponse.versionId)
                        }

                        Result.success(adResponse)
                    } else {
                        Result.failure(Exception(apiResponse.msg ?: "获取广告失败"))
                    }
                } else {
                    Result.failure(Exception("网络请求失败: ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("AdRepository", "getAdByType异常: type=$adType", e)
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }
}
