package com.aijia.video.data.repository

import android.content.Context
import com.aijia.video.data.model.*
import com.aijia.video.data.remote.ApiService
import com.aijia.video.util.NetworkErrorHandler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 评论弹幕仓库
 */
@Singleton
class CommentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val gson: Gson
) {
    private val danmuPrefs by lazy {
        context.getSharedPreferences("player_local_danmu", Context.MODE_PRIVATE)
    }

    
    // 弹幕列表
    private val _danmuList = MutableStateFlow<List<Danmu>>(emptyList())
    val danmuList: Flow<List<Danmu>> = _danmuList.asStateFlow()
    
    // 评论列表
    private val _commentList = MutableStateFlow<List<Comment>>(emptyList())
    val commentList: Flow<List<Comment>> = _commentList.asStateFlow()
    
    // 发送弹幕状态
    private val _sendDanmuStatus = MutableStateFlow<Result<Unit>?>(null)
    val sendDanmuStatus: Flow<Result<Unit>?> = _sendDanmuStatus.asStateFlow()
    
    // 发送评论状态
    private val _sendCommentStatus = MutableStateFlow<Result<Unit>?>(null)
    val sendCommentStatus: Flow<Result<Unit>?> = _sendCommentStatus.asStateFlow()
    
    /**
     * 获取视频评论列表
     */
    suspend fun getVideoComments(videoId: Int, page: Int = 1, limit: Int = 20): Result<CommentResponse> {
        return try {
            val response = apiService.getComments(videoId, 1, page, limit)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val type = object : TypeToken<ApiResponse<CommentApiListData>>() {}.type
                val body: ApiResponse<CommentApiListData> = gson.fromJson(json, type)
                if (body.code == 1 || body.code == 200) {
                    val mappedComments = body.data.list.map { item ->
                        Comment(
                            id = item.commentId,
                            content = item.commentContent,
                            user = CommentUser(
                                id = item.userId,
                                username = item.commentName,
                                avatar = item.userPortrait
                            ),
                            videoId = videoId,
                            likes = item.likes,
                            createTime = formatCommentTime(item.commentTime),
                            replyCount = 0,
                            isLiked = item.isLiked,
                            isSystem = item.isSystem
                        )
                    }
                    val data = CommentData(
                        comments = mappedComments,
                        total = body.data.total,
                        page = body.data.page,
                        limit = body.data.limit
                    )
                    data.let {
                        val currentComments = _commentList.value.toMutableList()
                        if (page == 1) {
                            _commentList.value = data.comments
                        } else {
                            currentComments.addAll(data.comments)
                            _commentList.value = currentComments
                        }
                    }
                    Result.success(
                        CommentResponse(
                            code = body.code,
                            msg = body.msg,
                            data = data
                        )
                    )
                } else {
                    Result.failure(Exception("获取评论失败: ${body.msg}"))
                }
            } else {
                Result.failure(Exception("获取评论失败: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }
    
    suspend fun sendComment(videoId: Int, content: String, parentId: Int? = null): Result<Unit> {
        return try {
            val response = apiService.postComment(content, videoId, 1)
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val apiResponse: com.aijia.video.data.model.ApiResponse<*> = gson.fromJson(json, com.aijia.video.data.model.ApiResponse::class.java)
                if (apiResponse.code == 1 || apiResponse.code == 200) {
                    getVideoComments(videoId, 1)
                    _sendCommentStatus.value = Result.success(Unit)
                    Result.success(Unit)
                } else {
                    val error = Exception(apiResponse.msg)
                    _sendCommentStatus.value = Result.failure(error)
                    Result.failure(error)
                }
            } else {
                val errorMsg = if (response.errorBody() != null) {
                    try {
                        val errorBody = response.errorBody()!!.string()
                        val errorResponse = gson.fromJson(errorBody, ApiErrorResponse::class.java)
                        errorResponse.msg
                    } catch (e: Exception) {
                        response.message()
                    }
                } else {
                    response.message()
                }
                val error = Exception(errorMsg)
                _sendCommentStatus.value = Result.failure(error)
                Result.failure(error)
            }
        } catch (e: Exception) {
            val formattedError = NetworkErrorHandler.formatError(e)
            _sendCommentStatus.value = Result.failure(formattedError)
            Result.failure(formattedError)
        }
    }
    
    data class ApiErrorResponse(
        val code: Int,
        val msg: String,
        val data: Any?
    )
    
    /**
     * 点赞评论
     */
    suspend fun likeComment(commentId: Int): Result<Unit> {
        return try {
            updateCommentLikeStatus(commentId, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 取消点赞
     */
    suspend fun unlikeComment(commentId: Int): Result<Unit> {
        return try {
            updateCommentLikeStatus(commentId, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVideoDanmu(videoId: Int, videoUrl: String): Result<com.aijia.video.data.model.DanmuResponse> {
        val localDanmu = getLocalDanmu(videoId)
        return try {
            // 先获取弹幕API配置
            val configResponse = apiService.getDanmuConfig()
            if (!configResponse.isSuccessful) {
                _danmuList.value = localDanmu
                return Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu))
            }
            val configJson = configResponse.body()?.string() ?: run {
                _danmuList.value = localDanmu
                return Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu))
            }
            val configApiResponse: com.aijia.video.data.model.ApiResponse<*> = gson.fromJson(configJson, com.aijia.video.data.model.ApiResponse::class.java)
            if (configApiResponse.code != 200) {
                _danmuList.value = localDanmu
                return Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu))
            }

            // 获取弹幕API信息
            val danmuInfoResponse = apiService.getDanmuList(videoUrl)
            if (!danmuInfoResponse.isSuccessful) {
                _danmuList.value = localDanmu
                return Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu))
            }
            val danmuInfoJson = danmuInfoResponse.body()?.string() ?: run {
                _danmuList.value = localDanmu
                return Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu))
            }
            val danmuInfoType = object : TypeToken<com.aijia.video.data.model.ApiResponse<DanmuApiInfo>>() {}.type
            val danmuInfoApiResponse: com.aijia.video.data.model.ApiResponse<DanmuApiInfo> = gson.fromJson(danmuInfoJson, danmuInfoType)
            if (danmuInfoApiResponse.code != 200) {
                _danmuList.value = localDanmu
                return Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu))
            }

            val danmuApiInfo = danmuInfoApiResponse.data
            val fullUrl = danmuApiInfo?.danmuApiFullUrl ?: return Result.success(
                com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu)
            )

            // 从第三方API获取弹幕
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(fullUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                _danmuList.value = localDanmu
                return Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu))
            }

            val jsonString = response.body?.string() ?: ""
            val danmuApiResponse = gson.fromJson(jsonString, DanmuApiResponse::class.java)

            // 解析弹幕数据
            val remoteDanmu = parseDanmuApiResponse(danmuApiResponse, videoId)

            // 合并本地和远程弹幕
            val mergedDanmu = mergeDanmu(localDanmu, remoteDanmu)
            _danmuList.value = mergedDanmu

            Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = mergedDanmu))
        } catch (e: Exception) {
            _danmuList.value = localDanmu
            Result.success(com.aijia.video.data.model.DanmuResponse(danmuList = localDanmu))
        }
    }

    /**
     * 解析第三方弹幕API响应
     */
    private fun parseDanmuApiResponse(response: DanmuApiResponse, videoId: Int): List<Danmu> {
        return response.danmuku.mapIndexedNotNull { index, item ->
            try {
                if (item.size < 5) return@mapIndexedNotNull null

                val time = (item[0] as? Number)?.toLong() ?: 0L
                val position = item[1] as? String ?: "right"
                val color = item[2] as? String ?: "#FFFFFF"
                val content = item[4] as? String ?: ""
                val fontSize = if (item.size > 7) {
                    val sizeStr = item[7] as? String ?: "24px"
                    sizeStr.replace("px", "").toIntOrNull() ?: 24
                } else {
                    24
                }

                if (content.isBlank()) return@mapIndexedNotNull null

                Danmu(
                    id = "remote_${response.code}_${index}",
                    content = content,
                    videoId = videoId,
                    userId = 0,
                    username = "弹幕用户",
                    color = color,
                    fontSize = fontSize,
                    time = time * 1000L,
                    type = when (position) {
                        "top" -> DanmuType.TOP
                        "bottom" -> DanmuType.BOTTOM
                        else -> DanmuType.SCROLL
                    }
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 发送弹幕
     */
    suspend fun sendDanmu(
        videoId: Int,
        content: String,
        color: String = "#FFFFFF",
        fontSize: Int = DEFAULT_DANMU_FONT_SIZE,
        timeMs: Long = 0L
    ): Result<Unit> {
        return try {
            val localRecord = LocalDanmuRecord(
                time = (timeMs.coerceAtLeast(0L) / 1000L),
                color = color,
                type = "0",
                content = content,
                fontSize = normalizeDanmuFontSize(fontSize)
            )
            val newDanmu = Danmu(
                id = System.currentTimeMillis().toString(),
                content = content,
                videoId = videoId,
                userId = 0,
                username = "当前用户",
                color = color,
                fontSize = localRecord.fontSize,
                time = localRecord.time * 1000L,
                type = DanmuType.SCROLL
            )
            val mergedDanmu = mergeDanmu(_danmuList.value, listOf(newDanmu))
            _danmuList.value = mergedDanmu
            saveLocalDanmuRecords(
                videoId,
                mergeLocalDanmuRecords(getLocalDanmuRecords(videoId), listOf(localRecord))
            )

            _sendDanmuStatus.value = Result.success(Unit)
            Result.success(Unit)
        } catch (e: Exception) {
            _sendDanmuStatus.value = Result.failure(e)
            Result.failure(e)
        }
    }

    private fun getLocalDanmu(videoId: Int): List<Danmu> {
        val json = danmuPrefs.getString(localDanmuKey(videoId), null) ?: return emptyList()
        val localRecordType = object : TypeToken<List<LocalDanmuRecord>>() {}.type
        val localRecords = runCatching { gson.fromJson<List<LocalDanmuRecord>>(json, localRecordType) }
            .getOrNull()
            .orEmpty()

        if (localRecords.isNotEmpty()) {
            return localRecords.mapIndexed { index, record ->
                record.toDanmu(videoId = videoId, index = index)
            }
        }

        val legacyType = object : TypeToken<List<Danmu>>() {}.type
        return runCatching { gson.fromJson<List<Danmu>>(json, legacyType).orEmpty() }
            .getOrDefault(emptyList())
    }

    private fun getLocalDanmuRecords(videoId: Int): List<LocalDanmuRecord> {
        val json = danmuPrefs.getString(localDanmuKey(videoId), null) ?: return emptyList()
        val type = object : TypeToken<List<LocalDanmuRecord>>() {}.type
        return runCatching { gson.fromJson<List<LocalDanmuRecord>>(json, type).orEmpty() }
            .getOrDefault(emptyList())
    }

    private fun saveLocalDanmuRecords(videoId: Int, danmuList: List<LocalDanmuRecord>) {
        danmuPrefs.edit()
            .putString(localDanmuKey(videoId), gson.toJson(danmuList))
            .apply()
    }

    private fun localDanmuKey(videoId: Int): String = "video_danmu_$videoId"

    private fun mergeDanmu(primary: List<Danmu>, secondary: List<Danmu>): List<Danmu> {
        return (primary + secondary)
            .distinctBy { danmu -> "${danmu.id}_${danmu.time}_${danmu.content}" }
            .sortedBy { it.time }
    }

    private fun mergeLocalDanmuRecords(
        primary: List<LocalDanmuRecord>,
        secondary: List<LocalDanmuRecord>
    ): List<LocalDanmuRecord> {
        return (primary + secondary)
            .distinctBy { record -> "${record.time}_${record.color}_${record.type}_${record.content}_${record.fontSize}" }
            .sortedBy { it.time }
    }

    private fun normalizeDanmuFontSize(fontSize: Int): Int {
        return fontSize.coerceIn(MIN_DANMU_FONT_SIZE, MAX_DANMU_FONT_SIZE)
    }
    
    /**
     * 更新评论点赞状态
     */
    private fun updateCommentLikeStatus(commentId: Int, isLiked: Boolean) {
        val currentComments = _commentList.value.toMutableList()
        val commentIndex = currentComments.indexOfFirst { it.id == commentId }
        if (commentIndex != -1) {
            val comment = currentComments[commentIndex]
            currentComments[commentIndex] = comment.copy(
                isLiked = isLiked,
                likes = if (isLiked) comment.likes + 1 else comment.likes - 1
            )
            _commentList.value = currentComments
        }
    }
    
    /**
     * 清空状态
     */
    fun clearStatus() {
        _sendDanmuStatus.value = null
        _sendCommentStatus.value = null
    }
    
    /**
     * 提交反馈
     */
    suspend fun postFeedback(type: String, vodId: Int = 0): Result<Unit> {
        return try {
            val response = apiService.postFeedback(
                type = type,
                vodId = vodId
            )
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val apiResponse: com.aijia.video.data.model.ApiResponse<*> = gson.fromJson(json, com.aijia.video.data.model.ApiResponse::class.java)
                if (apiResponse.code == 1 || apiResponse.code == 200) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                val errorMsg = if (response.errorBody() != null) {
                    try {
                        val errorBody = response.errorBody()!!.string()
                        val errorResponse = gson.fromJson(errorBody, ApiErrorResponse::class.java)
                        errorResponse.msg
                    } catch (e: Exception) {
                        response.message()
                    }
                } else {
                    response.message()
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }
    
    /**
     * 提交催更
     */
    suspend fun postUrge(
        vodId: Int,
        vodName: String,
        episodeName: String = "",
        playFrom: String = "",
        playUrl: String = "",
        resolvedUrl: String = "",
        coverUrl: String = ""
    ): Result<String> {
        return try {
            val response = apiService.postUrge(
                vodId = vodId,
                vodName = vodName,
                episodeName = episodeName,
                playFrom = playFrom,
                playUrl = playUrl,
                resolvedUrl = resolvedUrl,
                coverUrl = coverUrl
            )
            if (response.isSuccessful) {
                val json = response.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val apiResponse: com.aijia.video.data.model.ApiResponse<*> = gson.fromJson(json, com.aijia.video.data.model.ApiResponse::class.java)
                if (apiResponse.code == 1 || apiResponse.code == 200) {
                    Result.success(apiResponse.msg.ifEmpty { "催更成功" })
                } else {
                    Result.failure(Exception(apiResponse.msg))
                }
            } else {
                val errorMsg = if (response.errorBody() != null) {
                    try {
                        val errorBody = response.errorBody()!!.string()
                        val errorResponse = gson.fromJson(errorBody, ApiErrorResponse::class.java)
                        errorResponse.msg
                    } catch (e: Exception) {
                        response.message()
                    }
                } else {
                    response.message()
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.formatError(e))
        }
    }

    private fun formatCommentTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timestamp * 1000))
    }
}

/**
 * 弹幕响应
 */
data class DanmuResponse(
    override val code: Int,
    override val msg: String,
    override val data: DanmuData
) : BaseResult<DanmuData>()

/**
 * 弹幕数据
 */
data class DanmuData(
    val danmuList: List<Danmu>
)

private data class LocalDanmuRecord(
    val time: Long,
    val color: String,
    val type: String,
    val content: String,
    val fontSize: Int = DEFAULT_DANMU_FONT_SIZE
) {
    fun toDanmu(videoId: Int, index: Int): Danmu {
        return Danmu(
            id = "local_${videoId}_${time}_$index",
            content = content,
            videoId = videoId,
            userId = 0,
            username = "当前用户",
            color = color,
            fontSize = fontSize.coerceIn(MIN_DANMU_FONT_SIZE, MAX_DANMU_FONT_SIZE),
            time = time * 1000L,
            type = DanmuType.SCROLL
        )
    }
}

private const val MIN_DANMU_FONT_SIZE = 18
private const val MAX_DANMU_FONT_SIZE = 34
private const val DEFAULT_DANMU_FONT_SIZE = 25
