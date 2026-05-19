package com.aijia.video.cast

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 投屏管理器
 */
@Singleton
class CastManager @Inject constructor(
    @ApplicationContext private val _context: Context
) {
    // 投屏状态
    private val _castState = MutableStateFlow<CastState>(CastState.NO_DEVICES_AVAILABLE)
    val castState: StateFlow<CastState> = _castState.asStateFlow()
    
    // 当前会话
    private val _castSession = MutableStateFlow<CastDevice?>(null)
    val castSession: StateFlow<CastDevice?> = _castSession.asStateFlow()
    
    // 投屏设备列表
    private val _castDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    val castDevices: StateFlow<List<CastDevice>> = _castDevices.asStateFlow()
    
    // 投屏媒体项
    private val _castMediaItem = MutableStateFlow<CastMediaItem?>(null)
    val castMediaItem: StateFlow<CastMediaItem?> = _castMediaItem.asStateFlow()

    // 播放状态
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    
    init {
        setupCastListener()
    }
    
    /**
     * 设置投屏监听器
     */
    private fun setupCastListener() {
        // 当前实现使用本地状态模拟投屏会话，不依赖 Google Cast SDK
    }
    
    /**
     * 扫描投屏设备
     */
    fun scanDevices() {
        // 这里应该扫描本地网络中的投屏设备
        // 暂时返回模拟设备列表
        val mockDevices = listOf(
            CastDevice.Builder("Living Room TV", "192.168.1.100")
                .setIconUrl("https://example.com/tv-icon.png")
                .setCapabilities(listOf("video", "audio"))
                .build(),
            CastDevice.Builder("Samsung TV", "192.168.1.101")
                .setIconUrl("https://example.com/samsung-icon.png")
                .setCapabilities(listOf("video", "audio"))
                .build()
        )
        _castDevices.value = mockDevices
    }
    
    /**
     * 连接投屏设备
     */
    fun connectToDevice(device: CastDevice) {
        _castState.value = CastState.CONNECTING
        _castSession.value = device.copy(isConnected = true)
        _castState.value = CastState.CONNECTED
    }
    
    /**
     * 断开投屏连接
     */
    fun disconnect() {
        _castState.value = CastState.DISCONNECTING
        _castSession.value = null
        _castMediaItem.value = null
        _castState.value = CastState.NO_DEVICES_AVAILABLE
    }
    
    /**
     * 投屏视频
     */
    fun castVideo(
        title: String,
        url: String,
        imageUrl: String? = null,
        duration: Long? = null
    ) {
        val mediaItem = CastMediaItem(
            title = title,
            url = url,
            imageUrl = imageUrl,
            duration = duration ?: 0L
        )
        
        _castMediaItem.value = mediaItem
        if (_castSession.value != null) {
            _playbackState.value = PlaybackState.PLAYING
        }
    }
    
    /**
     * 控制播放
     */
    fun play() {
        if (_castSession.value != null) {
            _playbackState.value = PlaybackState.PLAYING
        }
    }
    
    /**
     * 暂停播放
     */
    fun pause() {
        if (_castSession.value != null) {
            _playbackState.value = PlaybackState.PAUSED
        }
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        if (_castSession.value != null) {
            _playbackState.value = PlaybackState.IDLE
        }
    }
    
    /**
     * 跳转到指定位置
     */
    fun seekTo(positionMs: Long) {
        // 模拟实现，当前不维护具体进度
    }
    
    /**
     * 设置音量
     */
    fun setVolume(volume: Float) {
        // 模拟实现，当前不维护音量状态
    }
    
    /**
     * 静音/取消静音
     */
    fun setMute(muted: Boolean) {
        // 模拟实现，当前不维护静音状态
    }
    
    /**
     * 获取当前播放状态
     */
    fun getPlaybackState(): PlaybackState? {
        return _playbackState.value
    }
    
    /**
     * 释放资源
     */
    fun release() {
        _castSession.value = null
        _castState.value = CastState.NO_DEVICES_AVAILABLE
        _castDevices.value = emptyList()
        _castMediaItem.value = null
        _playbackState.value = PlaybackState.IDLE
    }
}
data class CastMediaItem(
    val title: String,
    val url: String,
    val imageUrl: String? = null,
    val duration: Long = 0L
)

/**
 * 投屏状态
 */
enum class CastState {
    NO_DEVICES_AVAILABLE,    // 无可用设备
    SCANNING,              // 扫描中
    CONNECTING,             // 连接中
    CONNECTED,              // 已连接
    DISCONNECTING,          // 断开连接中
    ERROR                    // 错误
}

/**
 * 播放状态
 */
enum class PlaybackState {
    PLAYING,    // 播放中
    PAUSED,     // 已暂停
    BUFFERING,  // 缓冲中
    IDLE,       // 空闲
    UNKNOWN     // 未知
}

/**
 * 投屏设备数据类
 */
data class CastDevice(
    val name: String,
    val address: String,
    val iconUrl: String? = null,
    val capabilities: List<String> = emptyList(),
    val isConnected: Boolean = false
) {
    class Builder(
        private val name: String,
        private val address: String
    ) {
        private var iconUrl: String? = null
        private var capabilities: List<String> = emptyList()
        
        fun setIconUrl(url: String): Builder {
            this.iconUrl = url
            return this
        }
        
        fun setCapabilities(caps: List<String>): Builder {
            this.capabilities = caps
            return this
        }
        
        fun build(): CastDevice {
            return CastDevice(
                name = name,
                address = address,
                iconUrl = iconUrl,
                capabilities = capabilities,
                isConnected = false
            )
        }
    }
}
