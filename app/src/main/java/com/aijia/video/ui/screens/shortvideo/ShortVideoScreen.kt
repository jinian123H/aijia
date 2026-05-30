package com.aijia.video.ui.screens.shortvideo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.model.AppPermission
import com.aijia.video.data.remote.ApiSecurity
import com.aijia.video.data.model.PlayUrl
import com.aijia.video.data.model.Video
import com.aijia.video.data.repository.VideoRepository
import com.aijia.video.player.Media3Player
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShortVideoUiState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
    val shortVideoTypeId: Int? = null
)

data class ShortVideoActionState(
    val likedVideoIds: Set<String> = emptySet(),
    val favoriteVideoIds: Set<String> = emptySet(),
    val likeCounts: Map<String, Int> = emptyMap(),
    val shareCounts: Map<String, Int> = emptyMap()
)

@HiltViewModel
class ShortVideoViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val media3Player: Media3Player
) : ViewModel() {
    companion object {
        private const val TAG = "ShortVideoVM"
    }

    private val _uiState = MutableStateFlow(ShortVideoUiState(isLoading = true))
    val uiState: StateFlow<ShortVideoUiState> = _uiState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val isBuffering: StateFlow<Boolean> = media3Player.isBuffering
    val playbackEndedCount: StateFlow<Int> = media3Player.playbackEndedCount
    val currentPosition: StateFlow<Long> = media3Player.currentPosition
    val duration: StateFlow<Long> = media3Player.duration

    private val _actionState = MutableStateFlow(ShortVideoActionState())
    val actionState: StateFlow<ShortVideoActionState> = _actionState.asStateFlow()

    private var currentPage = 1
    private val pageSize = 10

    private var currentUrl: String? = null
    private var configuredTypeId: Int? = null
    private var configuredInitialVideoId: String? = null

    fun ensureConfigured(typeId: Int? = null, initialVideoId: String? = null) {
        android.util.Log.d(TAG, "ensureConfigured typeId=$typeId initialVideoId=$initialVideoId configuredTypeId=$configuredTypeId")
        if (configuredTypeId == typeId && configuredInitialVideoId == initialVideoId && (_uiState.value.videos.isNotEmpty() || _uiState.value.errorMessage != null)) return
        configuredTypeId = typeId
        configuredInitialVideoId = initialVideoId
        loadShortVideos(typeId, initialVideoId)
    }

    fun loadShortVideos(preferredTypeId: Int? = configuredTypeId, initialVideoId: String? = configuredInitialVideoId) {
        viewModelScope.launch {
            currentPage = 1
            _uiState.value = _uiState.value.copy(isLoading = true, isLoadingMore = false, errorMessage = null)
            android.util.Log.d(TAG, "loadShortVideos start preferredTypeId=$preferredTypeId initialVideoId=$initialVideoId")

            val shortVideoTypeId = resolveShortVideoTypeId(preferredTypeId)
            android.util.Log.d(TAG, "loadShortVideos resolvedTypeId=$shortVideoTypeId")
            if (shortVideoTypeId == null) {
                android.util.Log.e(TAG, "loadShortVideos failed: shortVideoTypeId is null")
                _uiState.value = ShortVideoUiState(
                    videos = emptyList(),
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = false,
                    errorMessage = null
                )
                return@launch
            }

            android.util.Log.d(TAG, "request short video list typeId=$shortVideoTypeId page=$currentPage limit=$pageSize")
            val result = videoRepository.getVideoList(
                type = shortVideoTypeId,
                page = currentPage,
                limit = pageSize,
                sort = "time",
                refresh = true
            )
            if (result.isSuccess) {
                val sourceVideos = result.getOrNull()?.data ?: emptyList()
                val videos = mergeInitialVideo(
                    initialVideoId = initialVideoId,
                    typeId = shortVideoTypeId,
                    videos = sourceVideos.filter { video -> !extractPlayUrl(video).isNullOrBlank() }
                )
                android.util.Log.d(TAG, "loadShortVideos success sourceCount=${sourceVideos.size} playableCount=${videos.size}")
                _uiState.value = ShortVideoUiState(
                    videos = videos,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = videos.size >= pageSize,
                    errorMessage = if (videos.isEmpty()) "暂无短视频内容" else null,
                    shortVideoTypeId = shortVideoTypeId
                )
            } else {
                android.util.Log.e(TAG, "loadShortVideos request failed", result.exceptionOrNull())
                _uiState.value = ShortVideoUiState(
                    videos = emptyList(),
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "加载短视频失败",
                    shortVideoTypeId = shortVideoTypeId
                )
            }
        }
    }

    private suspend fun mergeInitialVideo(initialVideoId: String?, typeId: Int, videos: List<Video>): List<Video> {
        val targetVideoId = initialVideoId?.takeIf { it.isNotBlank() } ?: return videos
        if (videos.any { it.id == targetVideoId }) return videos

        android.util.Log.d(TAG, "mergeInitialVideo targetVideoId=$targetVideoId missing in first page, loading detail")

        val targetVideo = videoRepository.getVideoDetail(targetVideoId)
            .getOrNull()
            ?.video
            ?.takeIf { it.typeId == typeId && !extractPlayUrl(it).isNullOrBlank() }

        return if (targetVideo != null) {
            listOf(targetVideo) + videos.filterNot { it.id == targetVideo.id }
        } else {
            videos
        }
    }

    fun preloadMoreIfNeeded(currentIndex: Int) {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        if (currentIndex < state.videos.size - 3) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val nextPage = currentPage + 1
            val shortVideoTypeId = state.shortVideoTypeId ?: resolveShortVideoTypeId(configuredTypeId)
            if (shortVideoTypeId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    hasMore = false,
                    errorMessage = null
                )
                return@launch
            }

            val result = videoRepository.getVideoList(
                type = shortVideoTypeId,
                page = nextPage,
                limit = pageSize,
                sort = "time",
                refresh = true
            )

            if (result.isSuccess) {
                val newVideos = (result.getOrNull()?.data ?: emptyList())
                    .filter { video -> !extractPlayUrl(video).isNullOrBlank() }
                currentPage = nextPage
                _uiState.value = _uiState.value.copy(
                    videos = (_uiState.value.videos + newVideos).distinctBy { it.id },
                    isLoadingMore = false,
                    hasMore = newVideos.size >= pageSize,
                    shortVideoTypeId = shortVideoTypeId
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    hasMore = false,
                    shortVideoTypeId = shortVideoTypeId
                )
            }
        }
    }

    private suspend fun resolveShortVideoTypeId(preferredTypeId: Int? = null): Int? {
        preferredTypeId?.takeIf { it > 0 }?.let {
            android.util.Log.d(TAG, "resolveShortVideoTypeId use preferredTypeId=$it")
            return it
        }

        videoRepository.getShortVideoConfig()
            .getOrNull()
            ?.typeId
            ?.takeIf { it > 0 }
            ?.let {
                android.util.Log.d(TAG, "resolveShortVideoTypeId use serverConfigTypeId=$it")
                return it
            }

        val fallbackTypeId = videoRepository.getVideoTypes()
            .getOrNull()
            ?.firstOrNull { it.typeEn.equals("duanju", ignoreCase = true) }
            ?.id
        android.util.Log.d(TAG, "resolveShortVideoTypeId fallback duanjuTypeId=$fallbackTypeId")
        return fallbackTypeId
    }

    fun playVideo(video: Video) {
        val url = extractPlayUrl(video) ?: return
        playUrl(video, url)
    }

    fun playEpisode(video: Video, episode: PlayUrl) {
        playUrl(video, episode.url)
    }

    private fun playUrl(video: Video, url: String) {
        if (currentUrl == url) {
            _isPlaying.value = media3Player.isPlaying.value
            return
        }
        android.util.Log.d(TAG, "playVideo videoId=${video.id} videoName=${video.name} url=${url.take(160)}")
        currentUrl = url
        media3Player.preparePlayer(url)
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        media3Player.togglePlayPause()
        _isPlaying.value = media3Player.isPlaying.value
    }

    fun pausePlayback() {
        media3Player.pause()
        _isPlaying.value = false
    }

    fun seekTo(positionMs: Long) {
        media3Player.seekTo(positionMs)
    }

    fun resumePlayback() {
        media3Player.play()
        _isPlaying.value = true
    }

    fun toggleLike(videoId: String) {
        val current = _actionState.value
        val liked = videoId in current.likedVideoIds
        val baseCount = current.likeCounts[videoId] ?: 0
        _actionState.value = current.copy(
            likedVideoIds = if (liked) current.likedVideoIds - videoId else current.likedVideoIds + videoId,
            likeCounts = current.likeCounts + (videoId to (if (liked) (baseCount - 1).coerceAtLeast(0) else baseCount + 1))
        )
    }

    fun toggleFavorite(video: Video) {
        val current = _actionState.value
        val wasFavorite = video.id in current.favoriteVideoIds || video.isFavorite
        _actionState.value = current.copy(
            favoriteVideoIds = if (wasFavorite) current.favoriteVideoIds - video.id else current.favoriteVideoIds + video.id
        )

        viewModelScope.launch {
            val result = videoRepository.toggleFavorite(video.copy(isFavorite = wasFavorite))
            if (result.isFailure) {
                val rollback = _actionState.value
                _actionState.value = rollback.copy(
                    favoriteVideoIds = if (wasFavorite) rollback.favoriteVideoIds + video.id else rollback.favoriteVideoIds - video.id
                )
            }
        }
    }

    fun registerShare(videoId: String) {
        val current = _actionState.value
        val count = current.shareCounts[videoId] ?: 0
        _actionState.value = current.copy(
            shareCounts = current.shareCounts + (videoId to (count + 1))
        )
    }

    fun getTextureView(): TextureView = media3Player.getTextureView()

    private fun extractPlayUrl(video: Video): String? = parseEpisodes(video).firstOrNull()?.url

    fun parseEpisodes(video: Video): List<PlayUrl> {
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
                val url = parts.getOrNull(1)?.trim().orEmpty()
                if (url.isBlank()) {
                    null
                } else {
                    PlayUrl(
                        name = name.ifBlank { "第1集" },
                        url = url
                    )
                }
            }
            .orEmpty()
    }

    override fun onCleared() {
        super.onCleared()
        media3Player.pause()
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ShortVideoScreen(
    initialVideoId: String? = null,
    initialTypeId: Int? = null,
    appPermission: AppPermission? = null,
    onControlsVisibilityChange: (Boolean) -> Unit = {},
    viewModel: ShortVideoViewModel = hiltViewModel(),
    adViewModel: com.aijia.video.ui.ad.AdViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val playbackEndedCount by viewModel.playbackEndedCount.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val videoDuration by viewModel.duration.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val adConfig by adViewModel.adConfig.collectAsStateWithLifecycle()
    val permissionState = appPermission ?: AppPermission.guestDefault()
    val context = LocalContext.current
    var currentCommentVideo by remember { mutableStateOf<Video?>(null) }
    var pendingInitialVideoId by remember(initialVideoId) { mutableStateOf(initialVideoId) }
    var currentVideoIndex by rememberSaveable { mutableIntStateOf(0) }
    var adShown by rememberSaveable { mutableStateOf(false) }
    val selectedEpisodeIndices = remember { mutableStateMapOf<String, Int>() }
    var showEpisodeSheet by rememberSaveable { mutableStateOf(false) }
    var handledPlaybackEndedCount by remember { mutableIntStateOf(playbackEndedCount) }
    var autoPlayNextEpisodeLabel by remember { mutableStateOf<String?>(null) }
    var autoPlayLoadingLabel by remember { mutableStateOf<String?>(null) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var interactionVersion by remember { mutableIntStateOf(0) }
    val latestOnControlsVisibilityChange by rememberUpdatedState(onControlsVisibilityChange)

    fun showControls() {
        controlsVisible = true
        interactionVersion += 1
    }

    fun registerInteraction() {
        controlsVisible = true
        interactionVersion += 1
    }

    @Suppress("DEPRECATION")
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else {
            null
        }
        val previousStatusBarColor = window?.statusBarColor
        val previousNavigationBarColor = window?.navigationBarColor
        val previousLightStatusBars = insetsController?.isAppearanceLightStatusBars
        val previousLightNavigationBars = insetsController?.isAppearanceLightNavigationBars

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            window.statusBarColor = android.graphics.Color.BLACK
            window.navigationBarColor = android.graphics.Color.BLACK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val attributes = window.attributes
                attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                window.attributes = attributes
            }
        }
        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = false
        insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (window != null && previousStatusBarColor != null) {
                window.statusBarColor = previousStatusBarColor
            }
            if (window != null && previousNavigationBarColor != null) {
                window.navigationBarColor = previousNavigationBarColor
                WindowCompat.setDecorFitsSystemWindows(window, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val attributes = window.attributes
                    attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    window.attributes = attributes
                }
            }
            if (previousLightStatusBars != null) {
                insetsController?.isAppearanceLightStatusBars = previousLightStatusBars
            }
            if (previousLightNavigationBars != null) {
                insetsController?.isAppearanceLightNavigationBars = previousLightNavigationBars
            }
        }
    }

    LaunchedEffect(initialTypeId, initialVideoId) {
        viewModel.ensureConfigured(initialTypeId, initialVideoId)
    }

    LaunchedEffect(controlsVisible) {
        latestOnControlsVisibilityChange(controlsVisible)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pausePlayback()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        uiState.errorMessage != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.errorMessage ?: "加载失败", color = Color.White)
                    Text(
                        text = "点击重试",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clickable { viewModel.loadShortVideos() }
                    )
                }
            }
        }

        else -> {
            LaunchedEffect(uiState.videos, pendingInitialVideoId) {
                val targetVideoId = pendingInitialVideoId ?: return@LaunchedEffect
                val targetIndex = uiState.videos.indexOfFirst { it.id == targetVideoId }
                if (targetIndex >= 0) {
                    currentVideoIndex = targetIndex
                }
                if (targetIndex >= 0 || !uiState.isLoading) {
                    pendingInitialVideoId = null
                }
            }

            if (currentVideoIndex !in uiState.videos.indices) {
                currentVideoIndex = 0
            }

            // 短视频广告逻辑：进入页面后只显示一次广告
            val enabledAds = adConfig?.shortVideoAds
                ?.filter { it.enabled && it.imageUrl.isNotBlank() }
                ?.sortedBy { it.sort }
                ?: emptyList()

            val currentAd = if (adViewModel.shouldShowAds() && !adShown && enabledAds.isNotEmpty()) enabledAds.firstOrNull() else null

            val currentVideo = if (currentAd != null) null else uiState.videos.getOrNull(currentVideoIndex)
            val currentEpisodes = currentVideo?.let(viewModel::parseEpisodes).orEmpty()
            val selectedEpisodeIndex = currentVideo
                ?.let { video ->
                    selectedEpisodeIndices[video.id]
                        ?.coerceIn(0, (currentEpisodes.lastIndex).coerceAtLeast(0))
                        ?: 0
                }
                ?: 0
            val selectedEpisode = currentEpisodes.getOrNull(selectedEpisodeIndex)

            LaunchedEffect(interactionVersion, showEpisodeSheet, currentCommentVideo, currentVideo?.id) {
                if (showEpisodeSheet || currentCommentVideo != null) {
                    controlsVisible = true
                    return@LaunchedEffect
                }
                delay(5000)
                controlsVisible = false
            }

            LaunchedEffect(currentVideo?.id) {
                handledPlaybackEndedCount = playbackEndedCount
                autoPlayNextEpisodeLabel = null
                autoPlayLoadingLabel = null
                showControls()
            }

            LaunchedEffect(playbackEndedCount, currentVideo?.id, selectedEpisodeIndex, currentEpisodes.size) {
                val video = currentVideo ?: return@LaunchedEffect
                if (playbackEndedCount <= handledPlaybackEndedCount) return@LaunchedEffect
                handledPlaybackEndedCount = playbackEndedCount

                if (selectedEpisodeIndex < currentEpisodes.lastIndex) {
                    val nextEpisodeIndex = selectedEpisodeIndex + 1
                    val nextEpisodeName = currentEpisodes.getOrNull(nextEpisodeIndex)
                        ?.name
                        ?.ifBlank { "第${nextEpisodeIndex + 1}集" }
                        ?: "第${nextEpisodeIndex + 1}集"
                    autoPlayNextEpisodeLabel = "1s 后播放 $nextEpisodeName"
                    delay(1000)
                    autoPlayNextEpisodeLabel = null
                    autoPlayLoadingLabel = nextEpisodeName
                    selectedEpisodeIndices[video.id] = nextEpisodeIndex
                }
            }

            LaunchedEffect(autoPlayLoadingLabel, isBuffering, isPlaying) {
                if (autoPlayLoadingLabel != null && !isBuffering && isPlaying) {
                    autoPlayLoadingLabel = null
                }
            }

            LaunchedEffect(currentVideo?.id, selectedEpisode?.url) {
                val video = currentVideo ?: return@LaunchedEffect
                val episode = selectedEpisode ?: return@LaunchedEffect
                if (permissionState.hasPermission("video")) {
                    viewModel.playEpisode(video, episode)
                } else {
                    Toast.makeText(context, permissionState.tipFor("video", "当前账号暂无视频权限，无法观看视频内容"), Toast.LENGTH_SHORT).show()
                }
                viewModel.preloadMoreIfNeeded(currentVideoIndex)
            }

            currentVideo?.let { video ->
                val textureView = remember { viewModel.getTextureView() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { textureView },
                        modifier = Modifier.fillMaxSize()
                    )

                    ShortVideoPage(
                        video = video,
                        currentEpisode = selectedEpisode,
                        currentEpisodeIndex = selectedEpisodeIndex,
                        totalEpisodes = currentEpisodes.size,
                        autoPlayNextEpisodeLabel = autoPlayNextEpisodeLabel,
                        autoPlayLoadingLabel = autoPlayLoadingLabel,
                        controlsVisible = controlsVisible,
                        isPlaying = isPlaying,
                        isFavorite = video.id in actionState.favoriteVideoIds || video.isFavorite,
                        currentPosition = currentPosition,
                        videoDuration = videoDuration,
                        onTogglePlay = {
                            if (controlsVisible) {
                                if (permissionState.hasPermission("video")) {
                                    registerInteraction()
                                    viewModel.togglePlayPause()
                                } else {
                                    Toast.makeText(context, permissionState.tipFor("video", "当前账号暂无视频权限，无法观看视频内容"), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                showControls()
                            }
                        },
                        onFavorite = {
                            registerInteraction()
                            viewModel.toggleFavorite(video)
                        },
                        onEpisodeClick = {
                            registerInteraction()
                            showEpisodeSheet = true
                        },
                        onSeek = { positionMs ->
                            registerInteraction()
                            viewModel.seekTo(positionMs)
                        },
                        onGestureNavigate = { horizontalDelta, verticalDelta ->
                            registerInteraction()
                            val horizontalThreshold = 120f
                            val verticalThreshold = 120f
                            when {
                                kotlin.math.abs(horizontalDelta) >= kotlin.math.abs(verticalDelta) && kotlin.math.abs(horizontalDelta) > horizontalThreshold -> {
                                    if (horizontalDelta < 0 && currentVideoIndex < uiState.videos.lastIndex) {
                                        currentVideoIndex += 1
                                    } else if (horizontalDelta > 0 && currentVideoIndex > 0) {
                                        currentVideoIndex -= 1
                                    }
                                }

                                kotlin.math.abs(verticalDelta) > verticalThreshold && currentEpisodes.isNotEmpty() -> {
                                    val nextEpisodeIndex = when {
                                        verticalDelta < 0 && selectedEpisodeIndex < currentEpisodes.lastIndex -> selectedEpisodeIndex + 1
                                        verticalDelta > 0 && selectedEpisodeIndex > 0 -> selectedEpisodeIndex - 1
                                        else -> selectedEpisodeIndex
                                    }
                                    if (nextEpisodeIndex != selectedEpisodeIndex) {
                                        selectedEpisodeIndices[video.id] = nextEpisodeIndex
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // 短视频广告 - 全屏显示
            if (currentAd != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    // 广告页面也支持手势切换
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val horizontalThreshold = 120f
                                    if (kotlin.math.abs(dragAmount.x) > horizontalThreshold) {
                                        registerInteraction()
                                        if (dragAmount.x < 0 && currentVideoIndex < uiState.videos.size - 1) {
                                            currentVideoIndex += 1
                                        } else if (dragAmount.x > 0 && currentVideoIndex > 0) {
                                            currentVideoIndex -= 1
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    com.aijia.video.ui.components.AdView(
                        imageUrl = currentAd.imageUrl,
                        linkUrl = currentAd.linkUrl,
                        duration = currentAd.durationInt,
                        onAdClick = { url ->
                            registerInteraction()
                            adShown = true
                            adViewModel.handleAdClick(url, context)
                        },
                        onAdSkip = {
                            registerInteraction()
                            adShown = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (showEpisodeSheet && currentVideo != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showEpisodeSheet = false
                        registerInteraction()
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "选集 · ${currentVideo.name}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                                .padding(bottom = 24.dp)
                        ) {
                            itemsIndexed(
                                items = currentEpisodes,
                                key = { index, episode -> "${currentVideo.id}_$index" }
                            ) { index, episode ->
                                val selected = index == selectedEpisodeIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        .clickable {
                                            registerInteraction()
                                            selectedEpisodeIndices[currentVideo.id] = index
                                            showEpisodeSheet = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = episode.name.ifBlank { "第${index + 1}集" },
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (selected) {
                                        Text(
                                            text = "播放中",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun ShortVideoPage(
    video: Video,
    currentEpisode: PlayUrl?,
    currentEpisodeIndex: Int,
    totalEpisodes: Int,
    autoPlayNextEpisodeLabel: String?,
    autoPlayLoadingLabel: String?,
    controlsVisible: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    currentPosition: Long,
    videoDuration: Long,
    onTogglePlay: () -> Unit,
    onFavorite: () -> Unit,
    onEpisodeClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onGestureNavigate: (horizontalDelta: Float, verticalDelta: Float) -> Unit
) {
    var dragPreviewOffsetY by remember(video.id, currentEpisodeIndex) { mutableStateOf(0f) }
    var episodeChangeBanner by remember(video.id) { mutableStateOf<String?>(null) }
    var initializedEpisodeIndex by remember(video.id) { mutableStateOf(false) }
    val dragPreviewAlpha by animateFloatAsState(
        targetValue = if (kotlin.math.abs(dragPreviewOffsetY) > 24f) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "dragPreviewAlpha"
    )
    val bannerAlpha by animateFloatAsState(
        targetValue = if (episodeChangeBanner != null) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "episodeBannerAlpha"
    )
    val autoPlayPromptAlpha by animateFloatAsState(
        targetValue = if (autoPlayNextEpisodeLabel != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "autoPlayPromptAlpha"
    )

    LaunchedEffect(video.id, currentEpisodeIndex, totalEpisodes) {
        if (!initializedEpisodeIndex) {
            initializedEpisodeIndex = true
            return@LaunchedEffect
        }
        if (totalEpisodes > 1) {
            episodeChangeBanner = "已切换到 ${currentEpisode?.name?.ifBlank { "第${currentEpisodeIndex + 1}集" } ?: "第${currentEpisodeIndex + 1}集"}"
            delay(1100)
            episodeChangeBanner = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.only(androidx.compose.foundation.layout.WindowInsetsSides.Top))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(Color.Black)
        )

        var totalHorizontalDrag by remember { mutableStateOf(0f) }
        var totalVerticalDrag by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(video.id, currentEpisodeIndex, totalEpisodes) {
                    detectDragGestures(
                        onDragStart = {
                            totalHorizontalDrag = 0f
                            totalVerticalDrag = 0f
                        },
                        onDragCancel = {
                            totalHorizontalDrag = 0f
                            totalVerticalDrag = 0f
                            dragPreviewOffsetY = 0f
                        },
                        onDragEnd = {
                            onGestureNavigate(totalHorizontalDrag, totalVerticalDrag)
                            totalHorizontalDrag = 0f
                            totalVerticalDrag = 0f
                            dragPreviewOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalHorizontalDrag += dragAmount.x
                            totalVerticalDrag += dragAmount.y
                            dragPreviewOffsetY = (dragPreviewOffsetY + dragAmount.y).coerceIn(-220f, 220f)
                        }
                    )
                }
                .clickable { onTogglePlay() }
        )

        if (controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }

        if (dragPreviewAlpha > 0f) {
            val swipeUp = dragPreviewOffsetY < 0
            val previewLabel = when {
                swipeUp && currentEpisodeIndex < totalEpisodes - 1 -> "上滑切到下一集"
                !swipeUp && currentEpisodeIndex > 0 -> "下滑返回上一集"
                swipeUp -> "已经是最后一集"
                else -> "已经是第一集"
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(0, (dragPreviewOffsetY * 0.35f).toInt()) }
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (swipeUp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = dragPreviewAlpha),
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = previewLabel,
                    color = Color.White.copy(alpha = dragPreviewAlpha),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (totalEpisodes > 1) {
                    Text(
                        text = "当前第${currentEpisodeIndex + 1}集 / 共${totalEpisodes}集",
                        color = Color.White.copy(alpha = 0.86f * dragPreviewAlpha),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        episodeChangeBanner?.let { bannerText ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black.copy(alpha = 0.68f * bannerAlpha))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = bannerText,
                    color = Color.White.copy(alpha = bannerAlpha),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        autoPlayNextEpisodeLabel?.let { nextEpisodeLabel ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Color.Black.copy(alpha = 0.78f * autoPlayPromptAlpha))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "本集播放完成",
                    color = Color.White.copy(alpha = autoPlayPromptAlpha),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = nextEpisodeLabel,
                    color = Color.White.copy(alpha = autoPlayPromptAlpha),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "自动连播中",
                    color = Color.White.copy(alpha = 0.82f * autoPlayPromptAlpha),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        autoPlayLoadingLabel?.let { nextEpisodeLabel ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.6.dp,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "正在加载 $nextEpisodeLabel",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.22f)
                )
                Text(
                    text = "即将流畅播放",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (controlsVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f),
                                Color.Black.copy(alpha = 0.34f),
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
                    .padding(top = 140.dp, bottom = 0.dp)
            )

            // 底部统一布局：右侧按钮 + 视频信息 + 进度条，从下往上排列
            val progress = if (videoDuration > 0) currentPosition.toFloat() / videoDuration.toFloat() else 0f
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom))
                    .padding(bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 右侧按钮 + 左侧视频信息
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 左侧：视频名称 + 集数信息
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = video.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        currentEpisode?.let {
                            Text(
                                text = "当前：${it.name.ifBlank { "第${currentEpisodeIndex + 1}集" }}${if (totalEpisodes > 1) " / 共${totalEpisodes}集" else ""}",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (totalEpisodes > 1) {
                            Text(
                                text = "上下滑动切集，左右滑动切视频",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // 右侧：操作按钮
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ShortVideoActionButton(
                            icon = Icons.Default.Star,
                            label = if (isFavorite) "已藏" else "收藏",
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else Color.White,
                            onClick = onFavorite
                        )
                        ShortVideoActionButton(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            label = if (totalEpisodes > 1) "选集" else "单集",
                            tint = Color.White,
                            onClick = onEpisodeClick
                        )
                    }
                }

                // 时间 + 进度条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatShortTime(currentPosition),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .pointerInput(videoDuration) {
                                detectTapGestures { offset ->
                                    if (videoDuration > 0) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        onSeek((fraction * videoDuration).toLong())
                                    }
                                }
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White.copy(alpha = 0.3f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White)
                        )
                    }
                    Text(
                        text = formatShortTime(videoDuration),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        if (controlsVisible) {
            if (!isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(84.dp)
                )
            } else {
                var showPauseHint by remember { mutableStateOf(false) }
                LaunchedEffect(isPlaying) {
                    showPauseHint = true
                    delay(800)
                    showPauseHint = false
                }
                if (showPauseHint) {
                    Icon(
                        imageVector = Icons.Default.PauseCircle,
                        contentDescription = "暂停",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(84.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortVideoActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun formatShortTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
