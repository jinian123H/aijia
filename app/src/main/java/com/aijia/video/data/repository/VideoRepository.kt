package com.aijia.video.data.repository

import coil.ImageLoader
import coil.request.ImageRequest
import android.content.Context
import android.content.SharedPreferences
import com.aijia.video.VideoApplication
import com.aijia.video.data.local.dao.VideoDao
import com.aijia.video.data.model.*
import com.aijia.video.data.remote.ApiSecurity
import com.aijia.video.data.remote.ApiService
import com.aijia.video.util.NetworkErrorHandler
import com.aijia.video.util.VersionCacheManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视频数据仓库
 */
@Singleton
class VideoRepository @Inject constructor(
    private val apiService: ApiService,
    private val videoDao: VideoDao,
    private val sessionManager: SessionManager,
    private val versionCacheManager: VersionCacheManager,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val homeCachePrefs: SharedPreferences = context.getSharedPreferences("home_recommend_cache", Context.MODE_PRIVATE)
    private val bannerPrefs: SharedPreferences = context.getSharedPreferences("banner_cache", Context.MODE_PRIVATE)
    private val videoTypePrefs: SharedPreferences = context.getSharedPreferences("video_type_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CACHED_VERSION_ID = "home_index_version_id"
        private const val KEY_CACHED_DATA = "home_index_data"
        private const val KEY_BANNER_VERSION_ID = "banner_version_id"
        private const val KEY_BANNER_DATA = "banner_data"
        private const val KEY_VIDEO_TYPE_VERSION_ID = "video_type_version_id"
        private const val KEY_VIDEO_TYPE_DATA = "video_type_data"
    }

    /**
     * 获取视频类型列表（带本地缓存，与getHomeRecommend/getBanner一致的缓存模式）
     */
    suspend fun getVideoTypes(): Result<List<VideoType>?> {
        return try {
            val versionId = videoTypePrefs.getString(KEY_VIDEO_TYPE_VERSION_ID, "") ?: ""

            android.util.Log.d("VideoRepository", "getVideoTypes - 发送version_id: $versionId")

            val response = apiService.getVideoTypes(versionId)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<VideoTypeVersionedData>>() {}.type
                val apiResponse: ApiResponse<VideoTypeVersionedData> = gson.fromJson(json, type)
                android.util.Log.d("VideoRepository", "getVideoTypes - 响应码: ${apiResponse.code}")

                if (apiResponse.code == 200) {
                    val versionedData = apiResponse.data

                    android.util.Log.d("VideoRepository", "getVideoTypes - 服务端version_id: ${versionedData.versionId}")
                    android.util.Log.d("VideoRepository", "getVideoTypes - data_changed: ${versionedData.dataChanged}")

                    if (versionedData.dataChanged && versionedData.content != null) {
                        videoTypePrefs.edit()
                            .putString(KEY_VIDEO_TYPE_VERSION_ID, versionedData.versionId)
                            .putString(KEY_VIDEO_TYPE_DATA, gson.toJson(versionedData.content))
                            .apply()
                        android.util.Log.d("VideoRepository", "getVideoTypes - 数据已更新，version_id: ${versionedData.versionId}")
                        Result.success(versionedData.content)
                    } else {
                        val cachedJson = videoTypePrefs.getString(KEY_VIDEO_TYPE_DATA, null)
                        if (cachedJson != null) {
                            val listType = object : TypeToken<List<VideoType>>() {}.type
                            val cachedData: List<VideoType> = gson.fromJson(cachedJson, listType)
                            android.util.Log.d("VideoRepository", "getVideoTypes - 无更新，使用本地缓存")
                            Result.success(cachedData)
                        } else {
                            if (versionId.isNotEmpty()) {
                                videoTypePrefs.edit().putString(KEY_VIDEO_TYPE_VERSION_ID, "").apply()
                                return getVideoTypes()
                            }
                            android.util.Log.d("VideoRepository", "getVideoTypes - 无缓存可用，返回null")
                            Result.success(null)
                        }
                    }
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "getVideoTypes - 异常", e)
            val cachedJson = videoTypePrefs.getString(KEY_VIDEO_TYPE_DATA, null)
            if (cachedJson != null) {
                val listType = object : TypeToken<List<VideoType>>() {}.type
                val cachedData: List<VideoType> = gson.fromJson(cachedJson, listType)
                android.util.Log.d("VideoRepository", "getVideoTypes - 异常，使用本地缓存")
                Result.success(cachedData)
            } else {
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }

    suspend fun getShortVideoConfig(): Result<ShortVideoConfig> {
        return try {
            val response = apiService.getShortVideoConfig()
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<ShortVideoConfig>>() {}.type
                val apiResponse: ApiResponse<ShortVideoConfig> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun getCurrentUserInfo(): Result<UserInfo> {
        return try {
            val response = apiService.getUserInfo()
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<UserInfo>>() {}.type
                val apiResponse: ApiResponse<UserInfo> = gson.fromJson(json, type)
                if (apiResponse.code == 200 || apiResponse.code == 1) {
                    sessionManager.updateUserInfo(apiResponse.data)
                    getUserPermission().getOrNull()
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else if (response.code() == 403) {
                sessionManager.clearSession()
                val errorMsg = try {
                    val errorBody = response.errorBody()?.string()
                    errorBody?.let {
                        com.google.gson.Gson().fromJson(it, com.google.gson.JsonObject::class.java)
                            ?.get("msg")?.asString
                    }
                } catch (_: Exception) { null }
                Result.failure(Exception(errorMsg ?: "账号已被禁用"))
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun login(username: String, password: String): Result<UserInfo> {
        return try {
            val encUsername = ApiSecurity.encrypt(username)
            val encPassword = ApiSecurity.encrypt(password)
            val response = apiService.login(encUsername, encPassword)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<LoginResponse>>() {}.type
                val apiResponse: ApiResponse<LoginResponse> = gson.fromJson(json, type)
                if (apiResponse.code == 200 || apiResponse.code == 1) {
                    val loginResponse = apiResponse.data
                    val userInfo = UserInfo(
                        userId = loginResponse.userId,
                        groupId = loginResponse.groupId,
                        groupName = loginResponse.groupName,
                        userName = loginResponse.userName,
                        nickName = loginResponse.nickName,
                        phone = loginResponse.phone,
                        vipEndTime = loginResponse.userEndTime,
                        status = 1
                    )
                    sessionManager.saveSession(loginResponse.token, userInfo)
                    versionCacheManager.clearCache(VersionCacheManager.KEY_USER_PERMISSION)
                    getUserPermission().getOrNull()
                    Result.success(userInfo)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    errorBody?.let {
                        com.google.gson.Gson().fromJson(it, com.google.gson.JsonObject::class.java)
                            ?.get("msg")?.asString
                    }
                } catch (_: Exception) { null }
                Result.failure(Exception(errorMsg ?: "登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun register(username: String, password: String, confirmPassword: String): Result<UserInfo> {
        return try {
            val encUsername = ApiSecurity.encrypt(username)
            val encPassword = ApiSecurity.encrypt(password)
            val encConfirmPassword = ApiSecurity.encrypt(confirmPassword)
            val response = apiService.register(encUsername, encPassword, encConfirmPassword)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<RegisterResponse>>() {}.type
                val apiResponse: ApiResponse<RegisterResponse> = gson.fromJson(json, type)
                if (apiResponse.code == 200 || apiResponse.code == 1) {
                    val registerResponse = apiResponse.data
                    val userInfo = UserInfo(
                        userId = registerResponse.userId,
                        groupId = registerResponse.groupId,
                        groupName = registerResponse.groupName,
                        userName = registerResponse.userName,
                        nickName = registerResponse.nickName,
                        status = 1
                    )
                    sessionManager.saveSession(registerResponse.token, userInfo)
                    versionCacheManager.clearCache(VersionCacheManager.KEY_USER_PERMISSION)
                    getUserPermission().getOrNull()
                    Result.success(userInfo)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    errorBody?.let {
                        com.google.gson.Gson().fromJson(it, com.google.gson.JsonObject::class.java)
                            ?.get("msg")?.asString
                    }
                } catch (_: Exception) { null }
                Result.failure(Exception(errorMsg ?: "注册失败"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun getCachedUserInfo(): UserInfo? = sessionManager.getUserInfo()

    fun observeUserPermission(): Flow<AppPermission> = sessionManager.permissionFlow

    fun getCachedUserPermission(): AppPermission = sessionManager.getPermission()
    fun isLoggedIn(): Boolean = !sessionManager.getToken().isNullOrBlank()

    suspend fun getUserPermission(): Result<AppPermission?> {
        return try {
            val cacheKey = VersionCacheManager.KEY_USER_PERMISSION
            val versionId = versionCacheManager.getVersionId(cacheKey)

            val response = apiService.getUserPermission(versionId)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<AppPermissionVersionedData>>() {}.type
                val apiResponse: ApiResponse<AppPermissionVersionedData> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    val versionedData = apiResponse.data

                    if (versionedData.versionId.isNotBlank()) {
                        versionCacheManager.saveVersionId(cacheKey, versionedData.versionId)
                    } else {
                        android.util.Log.w("VideoRepository", "getUserPermission - 服务端返回空version_id，跳过保存")
                    }

                    if (versionedData.dataChanged && versionedData.content != null) {
                        sessionManager.savePermission(versionedData.content)
                        Result.success(versionedData.content)
                    } else {
                        Result.success(sessionManager.getPermission())
                    }
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun getParseConfigs(from: String? = null): Result<List<ParseConfig>> {
        return try {
            val response = apiService.getParseConfigs(from)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<ParseConfigListResponse>>() {}.type
                val apiResponse: ApiResponse<ParseConfigListResponse> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    Result.success(apiResponse.data.list)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun resolvePlayUrl(from: String?, url: String): Result<ResolvedPlayUrl> {
        return try {
            val response = apiService.parsePlayUrl(from, url)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<ResolvedPlayUrl>>() {}.type
                val apiResponse: ApiResponse<ResolvedPlayUrl> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun submitPlaybackFeedback(
        vodId: Int,
        videoTitle: String,
        type: String,
        contact: String = ""
    ): Result<String> {
        return try {
            val response = apiService.postFeedback(type = type, vodId = vodId)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val apiResponse: ApiResponse<*> = gson.fromJson(json, ApiResponse::class.java)
                if (apiResponse.code == 200 || apiResponse.code == 1) {
                    Result.success("反馈成功")
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                val errorMsg = try {
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrBlank()) {
                        org.json.JSONObject(errorBody).optString("msg", "提交失败")
                    } else "提交失败"
                } catch (e: Exception) { "提交失败" }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun submitVideoUrge(
        vodId: Int,
        videoTitle: String,
        episodeName: String = "",
        playFrom: String = "",
        playUrl: String = "",
        resolvedUrl: String = "",
        coverUrl: String = ""
    ): Result<String> {
        return try {
            val response = apiService.postUrge(
                vodId = vodId,
                vodName = videoTitle,
                episodeName = episodeName,
                playFrom = playFrom,
                playUrl = playUrl,
                resolvedUrl = resolvedUrl,
                coverUrl = coverUrl
            )
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val apiResponse: ApiResponse<*> = gson.fromJson(json, ApiResponse::class.java)
                if (apiResponse.code == 200 || apiResponse.code == 1) {
                    Result.success(apiResponse.msg.ifBlank { "催更成功" })
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun getVideoList(
        type: Int? = null,
        area: String? = null,
        year: String? = null,
        page: Int = 1,
        limit: Int = 20,
        sort: String = "time",
        refresh: Boolean = false
    ): Result<PagedResponse<Video>> {
        return try {
            android.util.Log.d("VideoRepository", "getVideoList - type=$type, area=$area, year=$year, page=$page, limit=$limit, sort=$sort, refresh=$refresh")

            val response = apiService.getVideoList(type, area, year, page, limit, sort)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))

                val newFormatType = object : TypeToken<ApiResponse<VideoListData>>() {}.type
                try {
                    val apiResponse: ApiResponse<VideoListData> = gson.fromJson(json, newFormatType)
                    if (apiResponse.code == 200) {
                        val data = apiResponse.data
                        val pagedResponse = PagedResponse(
                            data = data.list,
                            total = data.total,
                            page = data.page,
                            limit = data.limit,
                            hasMore = data.list.size >= limit && (page * limit < data.total)
                        )

                        android.util.Log.d("VideoRepository", "getVideoList成功 - 返回${data.list.size}条, total=${data.total}, page=${data.page}, hasMore=${pagedResponse.hasMore}")

                        if (page == 1) {
                            try {
                                videoDao.insertVideos(mergeLocalVideoState(pagedResponse.data))
                            } catch (e: Exception) {
                                android.util.Log.e("VideoRepository", "缓存数据失败", e)
                            }
                        }

                        return Result.success(pagedResponse)
                    }
                } catch (e: Exception) {
                    android.util.Log.d("VideoRepository", "尝试旧格式解析", e)
                }

                val listType = object : TypeToken<ApiResponse<List<Video>>>() {}.type
                val apiResponse: ApiResponse<List<Video>> = gson.fromJson(json, listType)
                if (apiResponse.code == 200) {
                    val pagedResponse = PagedResponse.from(apiResponse, page = page, limit = limit)

                    if (page == 1) {
                        try {
                            videoDao.insertVideos(mergeLocalVideoState(pagedResponse.data))
                        } catch (e: Exception) {
                            android.util.Log.e("VideoRepository", "缓存数据失败", e)
                        }
                    }

                    return Result.success(pagedResponse)
                } else {
                    return Result.failure(Exception(apiResponse.msg))
                }
            } else {
                return Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "getVideoList异常", e)
            val localVideos = videoDao.getVideosByType(type, limit, (page - 1) * limit)
            if (localVideos.isNotEmpty()) {
                Result.success(
                    PagedResponse(
                        data = localVideos,
                        total = localVideos.size,
                        page = page,
                        limit = limit,
                        hasMore = localVideos.size >= limit
                    )
                )
            } else {
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }

    suspend fun getVideoDetail(vodId: String): Result<VideoDetail> {
        return try {
            val response = apiService.getVideoDetail(vodId.toInt())
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<VideoDetail>>() {}.type
                val apiResponse: ApiResponse<VideoDetail> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    val videoDetail = apiResponse.data
                    val mergedVideo = mergeLocalVideoState(videoDetail.video)
                    videoDao.insertVideo(mergedVideo)
                    Result.success(videoDetail.copy(video = mergedVideo))
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            val localVideo = videoDao.getVideoById(vodId)
            if (localVideo != null) {
                Result.success(
                    VideoDetail(
                        video = localVideo,
                        relatedVideos = emptyList(),
                        playUrls = emptyList(),
                        comments = emptyList()
                    )
                )
            } else {
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }

    suspend fun searchVideos(
        keyword: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<PagedResponse<Video>> {
        return try {
            val response = apiService.searchVideos(keyword, page, limit)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<SearchData<Video>>>() {}.type
                val apiResponse: ApiResponse<SearchData<Video>> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    Result.success(PagedResponse.fromSearch(apiResponse, page, limit))
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun getRecommendVideos(
        typeId: Int = 0,
        sortBy: String = "vod_hits",
        sortOrder: String = "DESC",
        random: Int = 0,
        title: String = "推荐视频",
        page: Int = 1,
        limit: Int = 20
    ): Result<RecommendResponse> {
        return getVideoList(
            type = typeId.takeIf { it > 0 },
            page = page,
            limit = limit,
            sort = sortBy.removePrefix("vod_"),
            refresh = true
        ).map { paged ->
            RecommendResponse(
                title = title,
                type_id = typeId,
                sort_by = sortBy,
                sort_order = sortOrder,
                random = random,
                videos = paged.data
            )
        }
    }

    suspend fun getHomeIndex(): Result<HomeIndexResponse?> {
        return try {
            val cacheKey = VersionCacheManager.KEY_HOME_INDEX
            val versionId = versionCacheManager.getVersionId(cacheKey)

            val response = apiService.getHomeIndex(versionId)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<HomeIndexVersionedData>>() {}.type
                val apiResponse: ApiResponse<HomeIndexVersionedData> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    val versionedData = apiResponse.data

                    if (versionedData.versionId.isNotBlank()) {
                        versionCacheManager.saveVersionId(cacheKey, versionedData.versionId)
                    } else {
                        android.util.Log.w("VideoRepository", "getHomeIndex - 服务端返回空version_id，跳过保存")
                    }

                    if (versionedData.dataChanged && versionedData.content != null) {
                        Result.success(versionedData.content)
                    } else {
                        Result.success(null)
                    }
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun getHomeRecommend(): Result<List<HomeSection>?> {
        return try {
            val versionId = homeCachePrefs.getString(KEY_CACHED_VERSION_ID, "") ?: ""

            android.util.Log.d("VideoRepository", "getHomeRecommend - 发送version_id: " + versionId)

            val response = apiService.getHomeIndex(versionId)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<HomeIndexVersionedData>>() {}.type
                val apiResponse: ApiResponse<HomeIndexVersionedData> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    val versionedData = apiResponse.data

                    if (versionedData.dataChanged && versionedData.content != null) {
                        homeCachePrefs.edit()
                            .putString(KEY_CACHED_VERSION_ID, versionedData.versionId)
                            .putString(KEY_CACHED_DATA, gson.toJson(versionedData.content?.sections))
                            .apply()
                        android.util.Log.d("VideoRepository", "getHomeRecommend - 数据已更新，version_id: " + versionedData.versionId)
                        Result.success(versionedData.content?.sections)
                    } else {
                        val cachedJson = homeCachePrefs.getString(KEY_CACHED_DATA, null)
                        if (cachedJson != null) {
                            val listType = object : TypeToken<List<HomeSection>>() {}.type
                            val cachedData: List<HomeSection> = gson.fromJson(cachedJson, listType)
                            android.util.Log.d("VideoRepository", "getHomeRecommend - 无更新，使用本地缓存")
                            Result.success(cachedData)
                        } else {
                            if (versionId.isNotEmpty()) {
                                homeCachePrefs.edit().putString(KEY_CACHED_VERSION_ID, "").apply()
                                return getHomeRecommend()
                            }
                            Result.success(emptyList())
                        }
                    }
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: " + response.code()))
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "getHomeRecommend异常", e)
            val cachedJson = homeCachePrefs.getString(KEY_CACHED_DATA, null)
            if (cachedJson != null) {
                val listType = object : TypeToken<List<HomeSection>>() {}.type
                val cachedData: List<HomeSection> = gson.fromJson(cachedJson, listType)
                Result.success(cachedData)
            } else {
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }

    /**
     * 获取 Banner 轮播视频（每日缓存模式）
     */
    suspend fun getBanner(): Result<List<BannerItem>?> {
        return try {
            val versionId = bannerPrefs.getString(KEY_BANNER_VERSION_ID, "") ?: ""

            android.util.Log.d("VideoRepository", "getBanner - 发送version_id: " + versionId)

            // Banner 从首页数据 data.content.banners 中提取，无需独立接口
            val response = apiService.getHomeIndex(versionId)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<HomeIndexVersionedData>>() {}.type
                val apiResponse: ApiResponse<HomeIndexVersionedData> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    val versionedData = apiResponse.data
                    val banners = versionedData.content?.banners?.map { video ->
                        BannerItem(
                            id = video.id.toIntOrNull() ?: 0,
                            title = video.name,
                            cover = video.picSlide ?: "",
                            vodId = video.id.toIntOrNull() ?: 0
                        )
                    }

                    if (versionedData.dataChanged && banners != null) {
                        bannerPrefs.edit()
                            .putString(KEY_BANNER_VERSION_ID, versionedData.versionId)
                            .putString(KEY_BANNER_DATA, gson.toJson(banners))
                            .apply()
                        android.util.Log.d("VideoRepository", "getBanner - 数据已更新，version_id: " + versionedData.versionId)
                        Result.success(banners)
                    } else {
                        val cachedJson = bannerPrefs.getString(KEY_BANNER_DATA, null)
                        if (cachedJson != null) {
                            val listType = object : TypeToken<List<BannerItem>>() {}.type
                            val cachedData: List<BannerItem> = gson.fromJson(cachedJson, listType)
                            android.util.Log.d("VideoRepository", "getBanner - 无更新，使用本地缓存")
                            Result.success(cachedData)
                        } else {
                            if (versionId.isNotEmpty()) {
                                bannerPrefs.edit().putString(KEY_BANNER_VERSION_ID, "").apply()
                                return getBanner()
                            }
                            Result.success(emptyList())
                        }
                    }
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: " + response.code()))
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "getBanner异常", e)
            val cachedJson = bannerPrefs.getString(KEY_BANNER_DATA, null)
            if (cachedJson != null) {
                val listType = object : TypeToken<List<BannerItem>>() {}.type
                val cachedData: List<BannerItem> = gson.fromJson(cachedJson, listType)
                Result.success(cachedData)
            } else {
                Result.failure(NetworkErrorHandler.formatError(e))
            }
        }
    }

    suspend fun getRankIndex(): Result<RankIndexResponse> {
        return try {
            val response = apiService.getRankIndex()
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<RankIndexResponse>>() {}.type
                val apiResponse: ApiResponse<RankIndexResponse> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun updateVideoProgress(
        videoId: String,
        progress: Long,
        duration: Long,
        playUrl: String?,
        episodeName: String?,
        lastWatchTime: Long
    ): Result<String> {
        return try {
            videoDao.updateVideoProgress(
                vodId = videoId,
                progress = progress,
                duration = duration.coerceAtLeast(0L),
                playUrl = playUrl?.takeIf { it.isNotBlank() },
                episodeName = episodeName?.takeIf { it.isNotBlank() },
                lastWatchTime = lastWatchTime
            )
            Result.success("Progress saved locally")
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun watchVideo(
        vodId: String,
        progress: Long,
        duration: Long,
        playUrl: String? = null,
        episodeName: String? = null
    ): Result<String> {
        return try {
            videoDao.updateVideoProgress(
                vodId = vodId,
                progress = progress,
                duration = duration.coerceAtLeast(0L),
                playUrl = playUrl?.takeIf { it.isNotBlank() },
                episodeName = episodeName?.takeIf { it.isNotBlank() },
                lastWatchTime = System.currentTimeMillis()
            )
            Result.success("Progress saved locally")
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun getVideoProgress(vodId: String): Result<PlayProgress> {
        return try {
            val localVideo = videoDao.getVideoById(vodId)
            if (localVideo != null && localVideo.progress > 0) {
                val duration = localVideo.progressDuration.coerceAtLeast(0L)
                val percentage = if (duration > 0L) {
                    (localVideo.progress.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Result.success(
                    PlayProgress(
                        vodId = vodId.toInt(),
                        progress = localVideo.progress,
                        duration = duration,
                        lastWatchTime = localVideo.lastWatchTime,
                        percentage = percentage
                    )
                )
            } else {
                Result.success(
                    PlayProgress(
                        vodId = vodId.toInt(),
                        progress = 0L,
                        lastWatchTime = 0L
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFavoriteVideos(): Flow<List<Video>> {
        return videoDao.getFavoriteVideos()
    }

    suspend fun removeFavorite(vodId: String): Result<Unit> {
        return try {
            videoDao.updateFavoriteStatus(vodId, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllFavorites(): Result<Unit> {
        return try {
            videoDao.clearAllFavorites()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(video: Video): Result<Boolean> {
        return try {
            val localVideo = videoDao.getVideoById(video.id)
            val currentFavorite = localVideo?.isFavorite ?: video.isFavorite
            val mergedVideo = mergeLocalVideoState(video, localVideo)
            val updatedVideo = mergedVideo.copy(isFavorite = !currentFavorite)
            updatedVideo.parseConfigs = mergedVideo.parseConfigs
            videoDao.insertVideo(updatedVideo)
            Result.success(updatedVideo.isFavorite)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPlayHistory(): Flow<List<Video>> {
        return videoDao.getPlayHistory()
    }

    suspend fun getStoredVideo(vodId: String): Video? {
        return videoDao.getVideoById(vodId)
    }

    private suspend fun mergeLocalVideoState(videos: List<Video>): List<Video> {
        if (videos.isEmpty()) return emptyList()
        val localMap = videoDao.getVideosByIds(videos.map { it.id }).associateBy { it.id }
        return videos.map { mergeLocalVideoState(it, localMap[it.id]) }
    }

    private suspend fun mergeLocalVideoState(video: Video): Video {
        return mergeLocalVideoState(video, videoDao.getVideoById(video.id))
    }

    private fun mergeLocalVideoState(video: Video, localVideo: Video?): Video {
        localVideo ?: return video
        val merged = video.copy(
            isFavorite = localVideo.isFavorite,
            progress = localVideo.progress,
            lastWatchTime = localVideo.lastWatchTime,
            progressDuration = localVideo.progressDuration,
            lastPlayUrl = localVideo.lastPlayUrl,
            lastEpisodeName = localVideo.lastEpisodeName
        )
        merged.parseConfigs = video.parseConfigs
        return merged
    }

    suspend fun clearPlayHistory(): Result<Unit> {
        return try {
            videoDao.clearPlayHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFromPlayHistory(vodId: String): Result<Unit> {
        return try {
            videoDao.clearSinglePlayHistory(vodId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatNetworkError(e: Exception): Exception {
        return NetworkErrorHandler.formatError(e)
    }

    // ========== 卡密系统 ==========

    suspend fun verifyCard(cardPwd: String): Result<CardResponse> {
        return try {
            val response = apiService.verifyCard(cardPwd)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<CardResponse>>() {}.type
                val apiResponse: ApiResponse<CardResponse> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                if (response.code() == 403) {
                    Result.failure(Exception("请先登录"))
                } else {
                    Result.failure(Exception("Network error: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun useCard(cardPwd: String): Result<CardResponse> {
        return try {
            val response = apiService.useCard(cardPwd)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<CardResponse>>() {}.type
                val apiResponse: ApiResponse<CardResponse> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    getCurrentUserInfo().getOrNull()
                    getUserPermission().getOrNull()
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = try {
                    errorBody?.let {
                        com.google.gson.Gson().fromJson(it, com.google.gson.JsonObject::class.java)
                            ?.get("msg")?.asString
                    }
                } catch (_: Exception) { null }
                if (response.code() == 403) {
                    Result.failure(Exception(errorMsg ?: "请先登录"))
                } else {
                    Result.failure(Exception(errorMsg ?: "卡密兑换失败"))
                }
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    suspend fun getCardRecords(): Result<List<CardRecord>> {
        return try {
            val response = apiService.getCardRecords()
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<List<CardRecord>>>() {}.type
                val apiResponse: ApiResponse<List<CardRecord>> = gson.fromJson(json, type)
                if (apiResponse.code == 200) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                Result.failure(Exception("Network error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }
}
