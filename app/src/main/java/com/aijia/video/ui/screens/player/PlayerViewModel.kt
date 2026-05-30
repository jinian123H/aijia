package com.aijia.video.ui.screens.player

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.aijia.video.data.model.Video
import com.aijia.video.data.model.PlayUrl
import com.aijia.video.data.model.Comment
import com.aijia.video.data.remote.ApiSecurity
import com.aijia.video.data.model.Danmu
import com.aijia.video.data.model.DanmuType
import com.aijia.video.data.repository.SessionManager
import com.aijia.video.data.repository.VideoRepository
import com.aijia.video.data.repository.CommentRepository
import com.aijia.video.util.DanmuStorageManager
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val application: Application,
    private val videoRepository: VideoRepository,
    private val commentRepository: CommentRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private fun formatTime(millis: Long): String {
        if (millis <= 0) return "00:00"
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    private val _video = MutableStateFlow<Video?>(null)
    val video = _video.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _commentError = MutableStateFlow<String?>(null)
    val commentError = _commentError.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments = _isLoadingComments.asStateFlow()

    private var _commentPage = 1
    private var _hasMoreComments = true

    private val _danmuList = MutableStateFlow<List<Danmu>>(emptyList())
    val danmuList = _danmuList.asStateFlow()

    private val _showDanmu = MutableStateFlow(false)  // 默认关闭弹幕
    val showDanmu = _showDanmu.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen = _isFullscreen.asStateFlow()

    private val _playbackEndedCount = MutableStateFlow(0)
    val playbackEndedCount = _playbackEndedCount.asStateFlow()

    private val _resolvedVideoUrl = MutableStateFlow<String?>(null)
    val resolvedVideoUrl = _resolvedVideoUrl.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isSubmittingFeedback = MutableStateFlow(false)
    val isSubmittingFeedback = _isSubmittingFeedback.asStateFlow()

    private val _feedbackResultMessage = MutableStateFlow<String?>(null)
    val feedbackResultMessage = _feedbackResultMessage.asStateFlow()

    private val _urgeResultMessage = MutableStateFlow<String?>(null)
    val urgeResultMessage = _urgeResultMessage.asStateFlow()

    private val _savedPlayUrl = MutableStateFlow<String?>(null)
    val savedPlayUrl = _savedPlayUrl.asStateFlow()

    private val _feedbackDescription = MutableStateFlow("")
    val feedbackDescription = _feedbackDescription.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _hasServerVideoPermission = MutableStateFlow<Boolean?>(null)
    val hasServerVideoPermission = _hasServerVideoPermission.asStateFlow()

    suspend fun fetchVideoPermission(): Boolean {
        return try {
            val result = videoRepository.getUserPermission()
            val perm = result.getOrNull()
            if (perm != null) {
                _hasServerVideoPermission.value = perm.hasPermission("video")
                perm.hasPermission("video")
            } else {
                // 服务端返回未变更（version cache），用本地缓存
                val cached = sessionManager.getPermission()
                _hasServerVideoPermission.value = cached.hasPermission("video")
                cached.hasPermission("video")
            }
        } catch (e: Exception) {
            Log.e("PlayerVM", "fetchVideoPermission error", e)
            val cached = sessionManager.getPermission()
            _hasServerVideoPermission.value = cached.hasPermission("video")
            cached.hasPermission("video")
        }
    }

    private var exoPlayer: ExoPlayer? = null

    // ExoPlayer延迟初始化，减少启动压力
    private fun ensurePlayerInitialized() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(application).build()
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun ensureWindowedMode() {
        _isFullscreen.value = false
    }

    fun setShowDanmu(show: Boolean) {
        _showDanmu.value = show
    }

    fun updateCurrentPosition(position: Long) {
        _currentPosition.value = position
    }

    fun updateDuration(duration: Long) {
        _duration.value = duration
    }

    fun setPlaybackEnded() {
        _playbackEndedCount.value += 1
    }

    fun setResolvedVideoUrl(url: String?) {
        _resolvedVideoUrl.value = url
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun setFeedbackResultMessage(message: String?) {
        _feedbackResultMessage.value = message
    }

    fun setUrgeResultMessage(message: String?) {
        _urgeResultMessage.value = message
    }

    fun setFeedbackDescription(description: String) {
        _feedbackDescription.value = description
    }

    fun canSubmitFeedback(videoId: String): Boolean {
        val sharedPreferences = application.getSharedPreferences("feedback_prefs", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
        val key = "feedback_${videoId}_$today"
        return !sharedPreferences.getBoolean(key, false)
    }

    private fun markFeedbackSubmitted(videoId: String) {
        val sharedPreferences = application.getSharedPreferences("feedback_prefs", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
        val key = "feedback_${videoId}_$today"
        sharedPreferences.edit().putBoolean(key, true).apply()
    }

    fun submitFeedback(type: String) {
        viewModelScope.launch {
            val video = _video.value
            val videoId = video?.id ?: ""
            
            if (!canSubmitFeedback(videoId)) {
                _feedbackResultMessage.value = "今天已经提交过反馈，请明天再试"
                return@launch
            }
            
            _isSubmittingFeedback.value = true
            try {
                val vodId = video?.id?.toIntOrNull() ?: 0
                
                val response = commentRepository.postFeedback(type, vodId)
                response.onSuccess {
                    _feedbackResultMessage.value = "反馈提交成功"
                    markFeedbackSubmitted(videoId)
                }.onFailure {
                    _feedbackResultMessage.value = "反馈提交失败: ${it.message}"
                }
            } catch (e: Exception) {
                _feedbackResultMessage.value = "反馈提交失败: ${e.message}"
            } finally {
                _isSubmittingFeedback.value = false
            }
        }
    }

    fun submitUrge() {
        viewModelScope.launch {
            try {
                val video = _video.value
                if (video != null) {
                    val response = commentRepository.postUrge(
                        vodId = video.id.toIntOrNull() ?: 0,
                        vodName = video.name,
                        episodeName = "",
                        playFrom = "",
                        playUrl = "",
                        resolvedUrl = _resolvedVideoUrl.value.orEmpty(),
                        coverUrl = video.pic.orEmpty()
                    )
                    response.onSuccess { message ->
                        _urgeResultMessage.value = message
                    }.onFailure {
                        _urgeResultMessage.value = "催更失败: ${it.message}"
                    }
                } else {
                    _urgeResultMessage.value = "催更失败: 视频信息不存在"
                }
            } catch (e: Exception) {
                _urgeResultMessage.value = "催更失败: ${e.message}"
            }
        }
    }

    fun sendComment(content: String) {
        viewModelScope.launch {
            val videoId = _video.value?.id?.toIntOrNull() ?: return@launch
            
            _commentError.value = null
            val result = commentRepository.sendComment(videoId, content)
            
            result.onSuccess {
                // 发送成功后，刷新评论列表
                val commentsResult = commentRepository.getVideoComments(videoId, page = 1)
                commentsResult.onSuccess { response ->
                    _comments.value = response.data.comments
                }.onFailure { error ->
                    _commentError.value = "刷新评论列表失败: ${error.message}"
                }
            }.onFailure { error ->
                _commentError.value = "发送评论失败: ${error.message}"
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentVideo = _video.value ?: return@launch
            val result = videoRepository.toggleFavorite(currentVideo)
            result.onSuccess { isFavorite ->
                // 从数据库获取更新后的视频信息，确保所有本地字段都是最新的
                val updatedVideo = videoRepository.getStoredVideo(currentVideo.id)
                if (updatedVideo != null) {
                    _video.value = updatedVideo
                } else {
                    // 如果获取失败，至少更新收藏状态
                    _video.value = currentVideo.copy(isFavorite = isFavorite).also {
                        it.parseConfigs = currentVideo.parseConfigs
                    }
                }
                // 显示Toast提示
                val message = if (isFavorite) "已收藏" else "已取消收藏"
                android.widget.Toast.makeText(application, message, android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                android.widget.Toast.makeText(application, "操作失败: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadVideo(videoId: Int) {
        viewModelScope.launch {
            // 简化实现
        }
    }

    fun retry() {
        viewModelScope.launch {
            // 简化实现
        }
    }

    fun resumePlayback() {
        exoPlayer?.play()
    }

    fun getPlayerView(context: Context): androidx.media3.ui.PlayerView {
        ensurePlayerInitialized()
        return androidx.media3.ui.PlayerView(context).apply {
            player = exoPlayer
        }
    }

    fun parseEpisodes(video: Video?): List<PlayUrl> {
        if (video == null) return emptyList()

        // 解密播放链接（后端AES-256-GCM加密，防TVBox抓包）
        val decryptedPlayUrl = try {
            video.playUrl?.let { ApiSecurity.decrypt(it) ?: it }
        } catch (_: Exception) {
            video.playUrl // 解密失败降级使用原值
        }

        return decryptedPlayUrl
            ?.split("#")
            ?.mapNotNull { item ->
                val parts = item.split("$", limit = 2)
                val name = parts.getOrNull(0)?.trim().orEmpty()
                val rawUrl = parts.getOrNull(1)?.trim().orEmpty()
                if (rawUrl.isBlank()) {
                    null
                } else {
                    // 对每集URL也尝试解密（防止整体解密失败时每集URL仍是密文）
                    val url = try { ApiSecurity.decrypt(rawUrl) ?: rawUrl } catch (_: Exception) { rawUrl }
                    PlayUrl(
                        name = name.ifBlank { "第1集" },
                        url = url
                    )
                }
            }
            .orEmpty()
    }

    private var currentVideoKey: String = "video"

    fun sendDanmu(content: String, color: Int, fontSize: Float) {
        viewModelScope.launch {
            android.util.Log.d("PlayerViewModel", "Sending danmu: $content")
            // 生成唯一ID
            val newDanmu = Danmu(
                id = System.currentTimeMillis().toString(),
                content = content,
                videoId = _video.value?.id?.toIntOrNull() ?: 0,
                userId = 0, // 临时用户ID，实际应该从用户登录信息获取
                username = "匿名用户",
                color = String.format("#%06X", 0xFFFFFF and color), // 转换为RGB颜色字符串
                fontSize = fontSize.toInt(),
                time = currentPosition.value, // 使用当前播放时间
                type = DanmuType.SCROLL // 默认滚动弹幕
            )
            // 发送到服务器
            // TODO: 实现服务器发送逻辑
            
            // 保存到本地存储
            DanmuStorageManager.saveDanmu(
                application.applicationContext,
                currentVideoKey, // 使用当前视频的key
                newDanmu
            )
            
            // 直接添加到本地弹幕列表，以便立即显示
            val updatedDanmuList = _danmuList.value.toMutableList()
            updatedDanmuList.add(newDanmu)
            _danmuList.value = updatedDanmuList

            // 自动开启弹幕显示，让用户能看到刚发送的弹幕
            _showDanmu.value = true

            android.util.Log.d("PlayerViewModel", "Danmu sent and added to list, showDanmu enabled")
        }
    }

    fun loadDanmu(videoId: String, videoUrl: String) {
        // 设置当前视频的key
        currentVideoKey = if (videoId.isNotEmpty()) videoId else "video"
        
        viewModelScope.launch {
            try {
                android.util.Log.d("PlayerViewModel", "Loading danmu for videoId: $videoId, videoUrl: $videoUrl")
                // 先加载本地存储的弹幕
                val localDanmu = DanmuStorageManager.loadDanmuList(
                    application.applicationContext,
                    currentVideoKey
                )
                android.util.Log.d("PlayerViewModel", "Local danmu loaded: ${localDanmu.size} items")
                
                // 再加载服务器弹幕
                val danmuResult = commentRepository.getVideoDanmu(videoId.toIntOrNull() ?: 0, videoUrl)
                danmuResult.onSuccess { response ->
                    android.util.Log.d("PlayerViewModel", "Danmu loaded successfully: ${response.danmuList.size} items")
                    val convertedDanmu = response.danmuList.map { dataDanmu ->
                        Danmu(
                            id = dataDanmu.id,
                            content = dataDanmu.content,
                            videoId = dataDanmu.videoId,
                            userId = dataDanmu.userId,
                            username = dataDanmu.username,
                            color = dataDanmu.color,
                            fontSize = dataDanmu.fontSize,
                            time = dataDanmu.time,
                            type = dataDanmu.type
                        )
                    }
                    // 合并本地和服务器弹幕
                    val allDanmu = localDanmu + convertedDanmu
                    _danmuList.value = allDanmu
                    android.util.Log.d("PlayerViewModel", "Danmu list updated to ${allDanmu.size} items")
                }.onFailure { error ->
                    android.util.Log.e("PlayerViewModel", "加载弹幕失败: ${error.message}")
                    // 如果服务器弹幕加载失败，只显示本地弹幕
                    _danmuList.value = localDanmu
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerViewModel", "加载弹幕异常: ${e.message}")
                // 异常情况下，显示本地弹幕
                _danmuList.value = DanmuStorageManager.loadDanmuList(
                    application.applicationContext,
                    currentVideoKey
                )
            }
        }
    }

    private val _selectedEpisodeName = MutableStateFlow<String?>(null)
    val selectedEpisodeName = _selectedEpisodeName.asStateFlow()
    
    fun updateEpisodeContext(episodeName: String?) {
        _selectedEpisodeName.value = episodeName
    }

    private var lastSavedProgressMs: Long = 0L
    private companion object {
        private const val PROGRESS_SAVE_INTERVAL_MS = 5_000L // 每5秒写一次
    }

    fun saveWatchProgress(progressMs: Long, durationMs: Long, playUrl: String, force: Boolean = false) {
        // 非强制保存时，间隔不足5秒直接跳过
        if (!force && progressMs - lastSavedProgressMs < PROGRESS_SAVE_INTERVAL_MS) return

        viewModelScope.launch {
            try {
                val video = _video.value ?: return@launch

                // 保存观看进度到数据库
                videoRepository.updateVideoProgress(
                    videoId = video.id,
                    progress = progressMs,
                    duration = durationMs,
                    playUrl = playUrl,
                    episodeName = _selectedEpisodeName.value,
                    lastWatchTime = System.currentTimeMillis()
                )

                // 更新当前视频状态
                _video.value = video.copy(
                    progress = progressMs,
                    lastWatchTime = System.currentTimeMillis(),
                    progressDuration = durationMs,
                    lastPlayUrl = playUrl,
                    lastEpisodeName = _selectedEpisodeName.value
                ).also {
                    it.parseConfigs = video.parseConfigs
                }

                lastSavedProgressMs = progressMs

                Log.d("PlayerViewModel", "Watch progress saved: videoId=${video.id}, progress=${formatTime(progressMs)}, duration=${formatTime(durationMs)}")
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to save watch progress", e)
            }
        }
    }

    fun initializeLocalPlayer(localVideoPath: String, localVideoTitle: String) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                _resolvedVideoUrl.value = localVideoPath

                // 创建一个临时Video对象用于显示标题
                _video.value = Video(
                    id = "0",
                    name = localVideoTitle,
                    pic = null,
                    typeId = 0,
                    typeName = "本地视频"
                )

                // 先停止并清理旧的播放状态
                exoPlayer?.apply {
                    stop()
                    clearMediaItems()
                }

                // 初始化ExoPlayer
                exoPlayer?.apply {
                    val mediaItem = MediaItem.fromUri(localVideoPath)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "初始化本地播放器失败"
            }
        }
    }

    fun loadVideoInfo(videoId: String) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val result = videoRepository.getVideoDetail(videoId)
                result.onSuccess { videoDetail ->
                    _video.value = videoDetail.video
                    if (videoDetail.comments.isNotEmpty()) {
                        _comments.value = videoDetail.comments
                    } else {
                        loadComments(videoId)
                    }
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "加载视频信息失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "加载视频信息失败"
            }
        }
    }

    private fun loadComments(videoId: String) {
        viewModelScope.launch {
            val videoIdInt = videoId.toIntOrNull() ?: 0
            if (videoIdInt <= 0) return@launch

            _commentPage = 1
            _hasMoreComments = true
            commentRepository.getVideoComments(videoIdInt, page = 1, limit = 20)
                .onSuccess { response ->
                    _comments.value = response.data.comments
                    val total = response.data.total
                    _hasMoreComments = response.data.comments.size >= 20
                }
                .onFailure { error ->
                    Log.w("PlayerViewModel", "加载评论失败: ${error.message}")
                }
        }
    }

    fun loadMoreComments() {
        val videoId = _video.value?.id?.toIntOrNull() ?: return
        if (_isLoadingComments.value || !_hasMoreComments) return

        viewModelScope.launch {
            _isLoadingComments.value = true
            _commentPage++
            commentRepository.getVideoComments(videoId, page = _commentPage, limit = 20)
                .onSuccess { response ->
                    if (response.data.comments.isNotEmpty()) {
                        _comments.value = _comments.value + response.data.comments
                    }
                    _hasMoreComments = response.data.comments.size >= 20
                }
                .onFailure { error ->
                    _commentPage--
                    Log.w("PlayerViewModel", "加载更多评论失败: ${error.message}")
                }
            _isLoadingComments.value = false
        }
    }

    fun initializePlayer(rawUrl: String, playFrom: String?, parseApiUrl: String = "") {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                _isLoading.value = true

                // 延迟初始化ExoPlayer
                ensurePlayerInitialized()

                // 先停止并清理旧的播放状态，避免 Surface 缓冲区冲突
                exoPlayer?.apply {
                    stop()
                    clearMediaItems()
                }

                // 判断是否需要解析：parseApiUrl有值才需要解析，为空则直接播放
                val needsParsing = parseApiUrl.isNotBlank()
                val finalUrl = if (needsParsing) {
                    // 调用解析接口获取真实播放地址
                    Log.d("PlayerViewModel", "URL需要解析: $rawUrl, parseApi: $parseApiUrl")
                    val result = videoRepository.resolvePlayUrl(playFrom, rawUrl)
                    result.fold(
                        onSuccess = { resolved ->
                            Log.d("PlayerViewModel", "解析成功: ${resolved.url}, type=${resolved.type}")
                            // 验证解析后的URL格式
                            if (isValidMediaUrl(resolved.url)) {
                                resolved.url
                            } else {
                                Log.e("PlayerViewModel", "解析后的URL不是有效的媒体格式: ${resolved.url}")
                                _errorMessage.value = "解析后的视频格式不支持播放"
                                null
                            }
                        },
                        onFailure = { error ->
                            Log.e("PlayerViewModel", "解析失败: ${error.message}")
                            // 解析失败，显示错误
                            _errorMessage.value = "解析失败: ${error.message}"
                            null
                        }
                    )
                } else {
                    // 不需要解析，直接播放（URL已在PlayerScreen中解密）
                    Log.d("PlayerViewModel", "直接播放URL: $rawUrl")
                    if (isValidMediaUrl(rawUrl)) {
                        rawUrl
                    } else {
                        Log.e("PlayerViewModel", "URL不是有效的媒体格式: $rawUrl")
                        _errorMessage.value = "视频格式不支持播放，请检查播放源配置"
                        null
                    }
                }

                if (finalUrl.isNullOrBlank()) {
                    if (_errorMessage.value == null) {
                        _errorMessage.value = "无法获取播放地址"
                    }
                    _isLoading.value = false
                    return@launch
                }

                _resolvedVideoUrl.value = finalUrl

                // 初始化ExoPlayer
                exoPlayer?.apply {
                    val mediaItem = MediaItem.fromUri(finalUrl)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = false
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "初始化播放器失败"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 判断URL是否是有效的媒体文件地址
     */
    private fun isValidMediaUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.startsWith("http://") ||
                lowerUrl.startsWith("https://") ||
                lowerUrl.contains(".m3u8") ||
                lowerUrl.contains(".mp4") ||
                lowerUrl.contains(".mkv") ||
                lowerUrl.contains(".avi") ||
                lowerUrl.contains(".mov") ||
                lowerUrl.contains(".flv") ||
                lowerUrl.contains(".webm") ||
                lowerUrl.contains(".mpd") ||
                lowerUrl.startsWith("file://") ||
                lowerUrl.startsWith("/")
    }

    fun clearFeedbackResultMessage() {
        _feedbackResultMessage.value = null
    }

    fun clearUrgeResultMessage() {
        _urgeResultMessage.value = null
    }

    fun toggleDanmu() {
        _showDanmu.value = !_showDanmu.value
    }

    fun updateFeedbackDescription(description: String) {
        _feedbackDescription.value = description
    }

    fun stopPlayback() {
        exoPlayer?.apply {
            stop()
            clearMediaItems()  // 清理 MediaItem，释放 Surface 缓冲区
        }
    }

    fun releasePlayer() {
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            release()
        }
        exoPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}


