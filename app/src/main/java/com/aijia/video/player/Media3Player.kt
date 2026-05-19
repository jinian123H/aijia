package com.aijia.video.player

import android.content.Context
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aijia.video.util.NetworkErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media3播放器封装
 */
@Singleton
class Media3Player @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var textureView: TextureView? = null
    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    
    // 播放状态
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    // 当前播放位置
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    // 视频时长
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    // 缓冲状态
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()
    
    // 播放错误
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 播放结束计数
    private val _playbackEndedCount = MutableStateFlow(0)
    val playbackEndedCount: StateFlow<Int> = _playbackEndedCount.asStateFlow()
    
    // 播放速度
    var playbackSpeed by mutableStateOf(1.0f)
        private set
    
    // 音量
    var volume by mutableStateOf(1.0f)
        private set
    
    // 全屏状态
    var isFullscreen by mutableStateOf(false)
        private set
    
    /**
     * 初始化播放器
     */
    fun initializePlayer(): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                // 监听播放状态
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> _isBuffering.value = true
                            Player.STATE_READY -> {
                                _isBuffering.value = false
                                _errorMessage.value = null
                            }
                            Player.STATE_ENDED -> {
                                _isBuffering.value = false
                                _playbackEndedCount.value = _playbackEndedCount.value + 1
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("Media3Player", "Playback failed", error)
                        _isBuffering.value = false
                        _errorMessage.value = buildPlaybackErrorMessage(error)
                    }
                })
            }
        }
        return exoPlayer!!
    }
    
    /**
     * 准备播放
     */
    fun preparePlayer(videoUrl: String) {
        val player = initializePlayer()
        _errorMessage.value = null
        _isBuffering.value = true
        player.stop()
        player.clearMediaItems()
        val mediaSource = createMediaSource(videoUrl)
        player.setMediaSource(mediaSource)
        player.prepare()
        playerView?.player = player
        textureView?.let { player.setVideoTextureView(it) }

        // 自动开始播放
        player.playWhenReady = true
        startProgressUpdates()

        android.util.Log.d("Media3Player", "Player prepared and starting playback for URL: $videoUrl")
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                    _duration.value = player.duration.takeIf { it > 0L } ?: 0L
                    _isPlaying.value = player.isPlaying
                }
                delay(500)
            }
        }
    }
    
    /**
     * 创建媒体源
     */
    private fun createMediaSource(videoUrl: String): MediaSource {
        val uri = Uri.parse(videoUrl)
        val normalizedUrl = videoUrl.lowercase()
        val isLocalFile = uri.scheme == "file" || videoUrl.startsWith("/")
        
        val dataSourceFactory = if (isLocalFile) {
            DefaultDataSource.Factory(context)
        } else {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("AijiaPlayer/1.0")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(20_000)
            DefaultDataSource.Factory(context, httpDataSourceFactory)
        }
        
        // 更智能的格式判断
        return when {
            // HLS (m3u8 或包含 playlist 参数)
            normalizedUrl.contains(".m3u8") || normalizedUrl.contains("playlist") || normalizedUrl.contains("hls") -> {
                android.util.Log.d("Media3Player", "Detected HLS stream for URL: $videoUrl")
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            }
            // DASH (mpd)
            normalizedUrl.contains(".mpd") -> {
                android.util.Log.d("Media3Player", "Detected DASH stream for URL: $videoUrl")
                DashMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            }
            // FLV (flv)
            normalizedUrl.contains(".flv") -> {
                android.util.Log.d("Media3Player", "Detected FLV stream for URL: $videoUrl")
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            }
            // MP4/AVI/MKV 等渐进式下载
            normalizedUrl.contains(".mp4") || normalizedUrl.contains(".avi") || normalizedUrl.contains(".mkv") || 
            normalizedUrl.contains(".wmv") || normalizedUrl.contains(".mov") -> {
                android.util.Log.d("Media3Player", "Detected progressive download stream for URL: $videoUrl")
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            }
            // 无法识别格式，尝试渐进式下载
            else -> {
                android.util.Log.w("Media3Player", "Unrecognized format, trying progressive playback for URL: $videoUrl")
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            }
        }
    }

    private fun buildPlaybackErrorMessage(error: androidx.media3.common.PlaybackException): String {
        val fullMessage = buildString {
            append(error.message.orEmpty())
            var cause: Throwable? = error.cause
            while (cause != null) {
                if (cause.message?.isNotBlank() == true) {
                    append(" | ")
                    append(cause.message)
                }
                cause = cause.cause
            }
        }

        val normalized = fullMessage.lowercase()
        return when {
            "sslhandshakeexception" in normalized || "sslprotocolexception" in normalized || "tlsv1_alert" in normalized -> {
                "当前线路暂时不可播放，请切换线路或稍后重试"
            }
            "unable to connect" in normalized || "failed to connect" in normalized || "connection refused" in normalized || "connection timed out" in normalized -> {
                NetworkErrorHandler.formatError(Exception(fullMessage)).message ?: "服务暂时不可用"
            }
            "cleartext" in normalized -> {
                "播放源被系统拦截，当前不支持该明文地址"
            }
            "unrecognizedinputformat" in normalized || "none of the available extractors" in normalized -> {
                "该视频格式不支持播放，请切换播放源"
            }
            "source error" in normalized -> {
                "视频源格式异常，请尝试其他播放源"
            }
            else -> error.message ?: "播放失败，请稍后重试"
        }
    }
    
    /**
     * 播放/暂停
     */
    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }
    
    /**
     * 播放
     */
    fun play() {
        exoPlayer?.play()
    }
    
    /**
     * 暂停
     */
    fun pause() {
        exoPlayer?.pause()
    }
    
    /**
     * 停止
     */
    fun stop() {
        exoPlayer?.stop()
    }
    
    /**
     * 跳转到指定位置
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
    
    /**
     * 设置播放速度
     */
    fun updatePlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        exoPlayer?.setPlaybackSpeed(speed)
    }
    
    /**
     * 设置音量
     */
    fun updateVolume(volumeLevel: Float) {
        volume = volumeLevel.coerceIn(0f, 1f)
        exoPlayer?.volume = volume
    }
    
    /**
     * 快进
     */
    fun fastForward(seconds: Int = 10) {
        exoPlayer?.let { player ->
            val newPosition = (player.currentPosition + seconds * 1000).coerceAtMost(player.duration)
            player.seekTo(newPosition)
        }
    }
    
    /**
     * 快退
     */
    fun rewind(seconds: Int = 10) {
        exoPlayer?.let { player ->
            val newPosition = (player.currentPosition - seconds * 1000).coerceAtLeast(0)
            player.seekTo(newPosition)
        }
    }
    
    /**
     * 切换全屏
     */
    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
    }

    fun updateFullscreenState(fullscreen: Boolean) {
        isFullscreen = fullscreen
    }
    
    /**
     * 释放播放器
     */
    fun release() {
        progressJob?.cancel()
        progressJob = null
        playerView?.player = null
        textureView?.let { exoPlayer?.clearVideoTextureView(it) }
        exoPlayer?.release()
        exoPlayer = null
        playerView = null
        textureView = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _playbackEndedCount.value = 0
    }
    
    /**
     * 获取PlayerView
     */
    fun getPlayerView(): PlayerView {
        val player = initializePlayer()
        val view = playerView ?: PlayerView(context).apply {
            useController = false
            controllerAutoShow = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            keepScreenOn = true
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }.also {
            playerView = it
        }

        (view.parent as? ViewGroup)?.removeView(view)
        view.player = player
        view.keepScreenOn = true
        return view
    }

    fun getTextureView(): TextureView {
        val player = initializePlayer()
        val view = textureView ?: TextureView(context).apply {
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
            keepScreenOn = true
        }.also {
            textureView = it
        }

        (view.parent as? ViewGroup)?.removeView(view)
        player.setVideoTextureView(view)
        view.keepScreenOn = true
        return view
    }

    /**
     * 设置视频缩放模式：短视频竖屏填满用 crop=true，横向视频用 crop=false
     */
    fun setScaleCrop(crop: Boolean) {
        exoPlayer?.videoScalingMode = if (crop) {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        } else {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }
}
