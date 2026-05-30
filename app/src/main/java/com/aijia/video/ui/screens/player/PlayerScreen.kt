package com.aijia.video.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aijia.video.R
import com.aijia.video.data.model.*
import com.aijia.video.data.remote.ApiSecurity
import com.aijia.video.data.repository.AppThemeDefaults
import com.aijia.video.ui.ad.AdViewModel
import com.aijia.video.ui.components.*
import com.aijia.video.ui.screens.download.DownloadViewModel
import com.aijia.video.data.repository.DownloadStatus
import com.aijia.video.data.repository.SessionManager
import kotlinx.coroutines.delay

private fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PlayerSessionManagerEntryPoint {
    fun sessionManager(): SessionManager
}

/**
 * 视频播放页Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoId: Int,
    onNavigateBack: () -> Unit,
    appPermission: AppPermission? = null,
    localVideoPath: String? = null,
    localVideoTitle: String? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
    downloadViewModel: DownloadViewModel = hiltViewModel(),
    adViewModel: AdViewModel = hiltViewModel()
) {
    val video by viewModel.video.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentError by viewModel.commentError.collectAsStateWithLifecycle()
    val isLoadingComments by viewModel.isLoadingComments.collectAsStateWithLifecycle()
    val danmuList by viewModel.danmuList.collectAsStateWithLifecycle()
    val showDanmu by viewModel.showDanmu.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
    val playbackEndedCount by viewModel.playbackEndedCount.collectAsStateWithLifecycle()
    val resolvedVideoUrl by viewModel.resolvedVideoUrl.collectAsStateWithLifecycle()
    val playerErrorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isSubmittingFeedback by viewModel.isSubmittingFeedback.collectAsStateWithLifecycle()
    val feedbackResultMessage by viewModel.feedbackResultMessage.collectAsStateWithLifecycle()
    val urgeResultMessage by viewModel.urgeResultMessage.collectAsStateWithLifecycle()
    val savedPlayUrl by viewModel.savedPlayUrl.collectAsStateWithLifecycle()
    val feedbackDescription by viewModel.feedbackDescription.collectAsStateWithLifecycle()
    val downloads by downloadViewModel.downloads.collectAsStateWithLifecycle(emptyList())
    val permissionState = appPermission ?: AppPermission.guestDefault()
    val hasServerVideoPermission by viewModel.hasServerVideoPermission.collectAsStateWithLifecycle()
    val adConfig by adViewModel.adConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sessionManager = remember(context.applicationContext) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerSessionManagerEntryPoint::class.java
        ).sessionManager()
    }
    val navigationStyle by sessionManager.navigationStyleFlow.collectAsStateWithLifecycle()
    val latestCurrentPosition by rememberUpdatedState(currentPosition)
    val latestDuration by rememberUpdatedState(duration)

    fun showPermissionTip(key: String, fallback: String) {
        Toast.makeText(context, permissionState.tipFor(key, fallback), Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(isFullscreen) {
        val activity = context.findActivity()
        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else {
            null
        }

        if (isFullscreen) {
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val attributes = window.attributes
                    attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    window.attributes = attributes
                }
            }
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val attributes = window.attributes
                    attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    window.attributes = attributes
                }
            }
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopPlayback()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            viewModel.ensureWindowedMode()
            context.findActivity()?.let { activity ->
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val attributes = activity.window.attributes
                    attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    activity.window.attributes = attributes
                }
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    ?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val hasVideoPermission = hasServerVideoPermission ?: permissionState.hasPermission("video")

    // 实时从服务端获取视频权限
    LaunchedEffect(Unit) {
        viewModel.fetchVideoPermission()
    }

    val playSources = remember(video?.id, video?.playFrom, video?.playUrl) {
        parsePlaySources(video?.playFrom, video?.playShow, video?.playUrl, video?.parseConfigs.orEmpty())
    }

    var selectedSourceIndex by remember(video?.id) { mutableIntStateOf(0) }
    var selectedEpisodeIndex by remember(video?.id) { mutableIntStateOf(0) }
    var handledEndedCount by remember(video?.id) { mutableIntStateOf(playbackEndedCount) }
    val playUrls = playSources.getOrNull(selectedSourceIndex)?.urls ?: emptyList()

    LaunchedEffect(playSources) {
        if (selectedSourceIndex !in playSources.indices) {
            selectedSourceIndex = 0
        }
    }

    LaunchedEffect(playSources, savedPlayUrl, video?.id) {
        val targetUrl = savedPlayUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        playSources.forEachIndexed { sourceIndex, source ->
            val episodeIndex = source.urls.indexOfFirst { it.url == targetUrl }
            if (episodeIndex >= 0) {
                if (selectedSourceIndex != sourceIndex) {
                    selectedSourceIndex = sourceIndex
                }
                if (selectedEpisodeIndex != episodeIndex) {
                    selectedEpisodeIndex = episodeIndex
                }
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(selectedSourceIndex, playUrls) {
        if (selectedEpisodeIndex !in playUrls.indices) {
            selectedEpisodeIndex = 0
        }
    }

    LaunchedEffect(video?.id) {
        handledEndedCount = playbackEndedCount
    }

    val selectedRawVideoUrl = playUrls.getOrNull(selectedEpisodeIndex)?.url ?: ""
    val selectedPlayFrom = playSources.getOrNull(selectedSourceIndex)?.fromKey
    val selectedPlayFromName = playSources.getOrNull(selectedSourceIndex)?.name
    val selectedEpisodeName = playUrls.getOrNull(selectedEpisodeIndex)?.name
    val selectedParseApiUrl = playSources.getOrNull(selectedSourceIndex)?.parseApiUrl.orEmpty()

    LaunchedEffect(selectedEpisodeName) {
        viewModel.updateEpisodeContext(selectedEpisodeName)
    }

    LaunchedEffect(currentPosition, duration, selectedRawVideoUrl) {
        if (selectedRawVideoUrl.isNotBlank() && currentPosition >= 1_000L) {
            viewModel.saveWatchProgress(
                progressMs = currentPosition,
                durationMs = duration,
                playUrl = selectedRawVideoUrl,
                force = duration > 0L && duration - currentPosition <= 2_000L
            )
        }
    }

    DisposableEffect(selectedRawVideoUrl) {
        onDispose {
            if (selectedRawVideoUrl.isNotBlank()) {
                viewModel.saveWatchProgress(
                    progressMs = latestCurrentPosition,
                    durationMs = latestDuration,
                    playUrl = selectedRawVideoUrl,
                    force = true
                )
            }
        }
    }

    LaunchedEffect(videoId, localVideoPath) {
        viewModel.ensureWindowedMode()
        if (localVideoPath != null && localVideoTitle != null) {
            Log.d("PlayerScreen", "Playing local video: $localVideoTitle")
            viewModel.initializeLocalPlayer(localVideoPath, localVideoTitle)
        } else {
            Log.d("PlayerScreen", "Loading video info for videoId: $videoId")
            viewModel.loadVideoInfo(videoId.toString())
        }
    }

    // 只在选中剧集地址改变且不为空时初始化播放器（非本地视频时）
    LaunchedEffect(selectedRawVideoUrl, selectedPlayFrom, selectedParseApiUrl, localVideoPath) {
        if (localVideoPath != null) {
            // 本地视频，不使用这个逻辑
            return@LaunchedEffect
        }
        if (selectedRawVideoUrl.isNotBlank() && hasVideoPermission) {
            Log.d("PlayerScreen", "Initializing player with raw URL: $selectedRawVideoUrl, from=$selectedPlayFrom, parseApi=$selectedParseApiUrl")
            viewModel.initializePlayer(selectedRawVideoUrl, selectedPlayFrom, selectedParseApiUrl)
            Log.d("PlayerScreen", "Loading danmu for videoId: ${video?.id ?: videoId}, videoUrl: $selectedRawVideoUrl")
            viewModel.loadDanmu(video?.id ?: videoId.toString(), selectedRawVideoUrl)
        } else if (selectedRawVideoUrl.isNotBlank() && !hasVideoPermission) {
            Log.w("PlayerScreen", "User has no video permission")
        } else {
            Log.w("PlayerScreen", "Video URL is blank, waiting for data...")
        }
    }

    LaunchedEffect(playbackEndedCount, playUrls.size) {
        if (playbackEndedCount > handledEndedCount) {
            handledEndedCount = playbackEndedCount
            if (selectedEpisodeIndex < playUrls.lastIndex) {
                selectedEpisodeIndex += 1
            }
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(PlayerPageRowTab.Intro) }
    var showDanmuPanel by rememberSaveable { mutableStateOf(false) }
    var showDownloadSheet by rememberSaveable { mutableStateOf(false) }
    var showFeedbackSheet by rememberSaveable { mutableStateOf(false) }
    var selectedFeedbackType by rememberSaveable(videoId) { mutableStateOf(PlaybackFeedbackOptions.first()) }
    var feedbackTypeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(feedbackResultMessage) {
        val message = feedbackResultMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        if (message.contains("成功")) {
            showFeedbackSheet = false
            selectedFeedbackType = PlaybackFeedbackOptions.first()
        }
        viewModel.clearFeedbackResultMessage()
    }

    LaunchedEffect(urgeResultMessage) {
        val message = urgeResultMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.clearUrgeResultMessage()
    }

    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (hasVideoPermission) {
                VideoPlayer(
                    videoUrl = resolvedVideoUrl ?: "",
                    title = video?.name.orEmpty(),
                    onBackClick = onNavigateBack,
                    onBackFromFullscreen = { viewModel.toggleFullscreen() },
                    onEnterPictureInPicture = { enterPictureInPicture(context) },
                    onPreviousEpisode = {
                        if (selectedEpisodeIndex > 0) {
                            selectedEpisodeIndex -= 1
                        }
                    },
                    onNextEpisode = {
                        if (selectedEpisodeIndex < playUrls.lastIndex) {
                            selectedEpisodeIndex += 1
                        }
                    },
                    hasPreviousEpisode = selectedEpisodeIndex > 0,
                    hasNextEpisode = selectedEpisodeIndex < playUrls.lastIndex,
                    fullscreenLayout = isFullscreen,
                    episodeList = playUrls.map { it.name },
                    currentEpisodeIndex = selectedEpisodeIndex,
                    onEpisodeClick = { index ->
                        selectedEpisodeIndex = index
                    },
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )

                if (showDanmu) {
                    Log.d("PlayerScreen", "Showing DanmuView - Count: ${danmuList.size}, Current time: $currentPosition")
                    DanmuView(
                        danmuList = danmuList,
                        currentTime = currentPosition,
                        videoKey = video?.id ?: videoId.toString(),
                        modifier = Modifier
                            .fillMaxSize()
                            .displayCutoutPadding()
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "权限不足",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "权限不足",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = permissionState.tipFor("video", "当前账号暂无视频权限，无法观看视频内容"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("返回")
                        }
                    }
                }
            }
        }
    } else {
        if (hasVideoPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val hasAdPermission by adViewModel.shouldShowAds.collectAsStateWithLifecycle(false)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    VideoPlayer(
                        videoUrl = resolvedVideoUrl ?: "",
                        title = video?.name.orEmpty(),
                        onBackClick = onNavigateBack,
                        onBackFromFullscreen = { viewModel.toggleFullscreen() },
                        onEnterPictureInPicture = { enterPictureInPicture(context) },
                        onPreviousEpisode = {
                            if (selectedEpisodeIndex > 0) {
                                selectedEpisodeIndex -= 1
                            }
                        },
                        onNextEpisode = {
                            if (selectedEpisodeIndex < playUrls.lastIndex) {
                                selectedEpisodeIndex += 1
                            }
                        },
                        hasPreviousEpisode = selectedEpisodeIndex > 0,
                        hasNextEpisode = selectedEpisodeIndex < playUrls.lastIndex,
                        fullscreenLayout = false,
                        episodeList = playUrls.map { it.name },
                        currentEpisodeIndex = selectedEpisodeIndex,
                        onEpisodeClick = { index ->
                            selectedEpisodeIndex = index
                        },
                        modifier = Modifier.fillMaxSize(),
                        viewModel = viewModel
                    )

                    if (showDanmu) {
                        DanmuView(
                            danmuList = danmuList,
                            currentTime = currentPosition,
                            videoKey = video?.id ?: videoId.toString(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 播放器广告 - 使用 rememberSaveable 保证每个页面实例独立
                    val playerAd = adConfig?.playerAd
                    var showPlayerAd by rememberSaveable { mutableStateOf(true) }

                    if (showPlayerAd && playerAd?.enabled == true &&
                        playerAd.imageUrl.isNotBlank() && hasAdPermission) {
                        AdView(
                            imageUrl = playerAd.imageUrl,
                            linkUrl = playerAd.linkUrl,
                            duration = playerAd.durationInt,
                            onAdClick = { url -> adViewModel.handleAdClick(url, context) },
                            onAdSkip = { showPlayerAd = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                PlayerPageActionRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    isDanmuEnabled = showDanmu,
                    onDanmuClick = {
                        if (!showDanmu && !permissionState.hasPermission("danmaku")) {
                            showPermissionTip("danmaku", "当前账号暂无弹幕权限")
                        } else if (showDanmu) {
                            showDanmuPanel = false
                            viewModel.toggleDanmu()
                        } else {
                            viewModel.toggleDanmu()
                        }
                    },
                    onDanmuInputClick = {
                        if (permissionState.hasPermission("danmaku")) {
                            showDanmuPanel = true
                        } else {
                            showPermissionTip("danmaku", "当前账号暂无弹幕权限")
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 1.dp, bottom = 1.dp)
                )

                // 使用 HorizontalPager 实现滑动切换
                val pagerState = rememberPagerState(
                    initialPage = if (selectedTab == PlayerPageRowTab.Intro) 0 else 1,
                    pageCount = { 2 }
                )

                // 同步 pagerState 和 selectedTab
                LaunchedEffect(pagerState.currentPage) {
                    selectedTab = if (pagerState.currentPage == 0) {
                        PlayerPageRowTab.Intro
                    } else {
                        PlayerPageRowTab.Comment
                    }
                }

                // 当点击按钮切换时，滚动到对应页面
                LaunchedEffect(selectedTab) {
                    val targetPage = if (selectedTab == PlayerPageRowTab.Intro) 0 else 1
                    if (pagerState.currentPage != targetPage) {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    when (page) {
                        0 -> {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(bottom = 16.dp)
                            ) {
                                // 根据导航样式选择简介组件
                                if (navigationStyle == AppThemeDefaults.NAVIGATION_FIXED) {
                                    // 主题二：紧凑布局，操作按钮内联，无缩略图
                                    IntroSlidePosterTheme2(
                                        video = video,
                                        isFavorite = video?.isFavorite == true,
                                        playSources = playSources,
                                        selectedSourceIndex = selectedSourceIndex,
                                        selectedEpisodeIndex = selectedEpisodeIndex,
                                        downloadRecords = downloads,
                                        onFavoriteClick = viewModel::toggleFavorite,
                                        onSourceSelected = {
                                            selectedSourceIndex = it
                                            selectedEpisodeIndex = 0
                                        },
                                        onEpisodeSelected = { selectedEpisodeIndex = it },
                                        onDownloadClick = {
                                            if (permissionState.hasPermission("download")) {
                                                showDownloadSheet = true
                                            } else {
                                                showPermissionTip("download", "当前账号暂无下载权限")
                                            }
                                        },
                                        onFeedbackClick = {
                                            if (permissionState.hasPermission("feedback")) {
                                                val videoId = video?.id ?: ""
                                                if (viewModel.canSubmitFeedback(videoId)) {
                                                    showFeedbackSheet = true
                                                } else {
                                                    Toast.makeText(context, "今天已经提交过反馈，请明天再试", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                showPermissionTip("feedback", "当前账号暂无反馈权限")
                                            }
                                        },
                                        onUrgeClick = {
                                            if (permissionState.hasPermission("urge")) {
                                                viewModel.submitUrge()
                                            } else {
                                                showPermissionTip("urge", "当前账号暂无催更权限")
                                            }
                                        },
                                        onEpisodeDownload = { episodeIndex, episode ->
                                            if (permissionState.hasPermission("download")) {
                                                downloadViewModel.startDownload(
                                                    videoId = video?.id ?: videoId.toString(),
                                                    videoTitle = video?.name.orEmpty(),
                                                    episodeName = episode.name.ifBlank { "第${episodeIndex + 1}集" },
                                                    videoUrl = episode.url,
                                                    coverUrl = video?.pic
                                                )
                                                showDownloadSheet = true
                                            } else {
                                                showPermissionTip("download", "当前账号暂无下载权限")
                                            }
                                        },
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp)
                                    )
                                } else {
                                    // 主题一：原有布局，有缩略图，底部固定按钮栏
                                    IntroSlidePoster(
                                        video = video,
                                        isFavorite = video?.isFavorite == true,
                                        playSources = playSources,
                                        selectedSourceIndex = selectedSourceIndex,
                                        selectedEpisodeIndex = selectedEpisodeIndex,
                                        downloadRecords = downloads,
                                        onFavoriteClick = viewModel::toggleFavorite,
                                        onSourceSelected = {
                                            selectedSourceIndex = it
                                            selectedEpisodeIndex = 0
                                        },
                                        onEpisodeSelected = { selectedEpisodeIndex = it },
                                        onDownloadClick = {
                                            if (permissionState.hasPermission("download")) {
                                                showDownloadSheet = true
                                            } else {
                                                showPermissionTip("download", "当前账号暂无下载权限")
                                            }
                                        },
                                        onFeedbackClick = {
                                            if (permissionState.hasPermission("feedback")) {
                                                val videoId = video?.id ?: ""
                                                if (viewModel.canSubmitFeedback(videoId)) {
                                                    showFeedbackSheet = true
                                                } else {
                                                    Toast.makeText(context, "今天已经提交过反馈，请明天再试", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                showPermissionTip("feedback", "当前账号暂无反馈权限")
                                            }
                                        },
                                        onUrgeClick = {
                                            if (permissionState.hasPermission("urge")) {
                                                viewModel.submitUrge()
                                            } else {
                                                showPermissionTip("urge", "当前账号暂无催更权限")
                                            }
                                        },
                                        onEpisodeDownload = { episodeIndex, episode ->
                                            if (permissionState.hasPermission("download")) {
                                                downloadViewModel.startDownload(
                                                    videoId = video?.id ?: videoId.toString(),
                                                    videoTitle = video?.name.orEmpty(),
                                                    episodeName = episode.name.ifBlank { "第${episodeIndex + 1}集" },
                                                    videoUrl = episode.url,
                                                    coverUrl = video?.pic
                                                )
                                                showDownloadSheet = true
                                            } else {
                                                showPermissionTip("download", "当前账号暂无下载权限")
                                            }
                                        },
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp)
                                    )
                                }

                                // 播放器底部广告（选集下面的广告）
                                val bottomAd = adConfig?.playerBottomAd
                                val showBottomAd = remember(bottomAd?.id, hasAdPermission) {
                                    hasAdPermission &&
                                        bottomAd?.enabled == true &&
                                        bottomAd?.imageUrl?.isNotBlank() == true
                                }

                                if (showBottomAd && bottomAd != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .clickable {
                                                if (bottomAd.linkUrl.isNotBlank()) {
                                                    adViewModel.handleAdClick(bottomAd.linkUrl, context)
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(bottomAd.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "广告",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            CommentSection(
                                comments = comments,
                                onSendComment = {
                                    if (permissionState.hasPermission("comment")) {
                                        viewModel.sendComment(it)
                                    } else {
                                        showPermissionTip("comment", "当前账号暂无评论权限")
                                    }
                                },
                                onLoadMore = { viewModel.loadMoreComments() },
                                isLoading = isLoadingComments,
                                errorMessage = commentError,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // 底部固定按钮栏（只在主题一、简介页面显示）
                if (selectedTab == PlayerPageRowTab.Intro && navigationStyle == AppThemeDefaults.NAVIGATION_FLOATING) {
                    PlayerBottomActionBar(
                        isFavorite = video?.isFavorite == true,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onDownloadClick = {
                            if (permissionState.hasPermission("download")) {
                                showDownloadSheet = true
                            } else {
                                showPermissionTip("download", "当前账号暂无下载权限")
                            }
                        },
                        onFeedbackClick = {
                            if (permissionState.hasPermission("feedback")) {
                                val videoId = video?.id ?: ""
                                if (viewModel.canSubmitFeedback(videoId)) {
                                    showFeedbackSheet = true
                                } else {
                                    Toast.makeText(context, "今天已经提交过反馈，请明天再试", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                showPermissionTip("feedback", "当前账号暂无反馈权限")
                            }
                        },
                        onUrgeClick = {
                            if (permissionState.hasPermission("urge")) {
                                viewModel.submitUrge()
                            } else {
                                showPermissionTip("urge", "当前账号暂无催更权限")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    )
                }

                // 弹幕输入面板
                if (showDanmuPanel) {
                    val danmuSheetHeight = rememberDanmuInputSheetHeight()
                    ModalBottomSheet(
                        onDismissRequest = { showDanmuPanel = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .height(danmuSheetHeight)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "发送弹幕",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                TextButton(
                                    onClick = { showDanmuPanel = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("关闭", style = MaterialTheme.typography.labelLarge)
                                }
                            }

                            DanmuInput(
                                isExpanded = true,
                                onSendDanmu = { content, color, fontSize ->
                                    if (permissionState.hasPermission("danmaku")) {
                                        viewModel.sendDanmu(content, color.hashCode(), fontSize)
                                        showDanmuPanel = false
                                    } else {
                                        showPermissionTip("danmaku", "当前账号暂无弹幕权限")
                                    }
                                },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                // 下载面板
                if (showDownloadSheet) {
                    val currentSource = playSources.getOrNull(selectedSourceIndex)
                    val configuration = LocalConfiguration.current
                    val screenHeight = configuration.screenHeightDp.dp
                    val sheetHeight = screenHeight * 0.6f
                    ModalBottomSheet(
                        onDismissRequest = { showDownloadSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .height(sheetHeight)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "缓存剧集 · ${currentSource?.name ?: "当前线路"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { showDownloadSheet = false }) {
                                    Text("关闭")
                                }
                            }

                            if (currentSource == null || currentSource.urls.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "暂无可缓存剧集",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(currentSource.urls) { index, episode ->
                                        val displayName = episode.name.ifBlank { "第${index + 1}集" }
                                        val record = downloads.firstOrNull { it.remoteUrl == episode.url }
                                        DownloadEpisodeRow(
                                            episodeName = displayName,
                                            isCurrentEpisode = index == selectedEpisodeIndex,
                                            record = record,
                                            onClick = {
                                                when (record?.status) {
                                                    DownloadStatus.PAUSED,
                                                    DownloadStatus.FAILED -> downloadViewModel.resumeDownload(record.id)

                                                    DownloadStatus.DOWNLOADING,
                                                    DownloadStatus.COMPLETED -> Unit

                                                    else -> downloadViewModel.startDownload(
                                                        videoId = video?.id ?: videoId.toString(),
                                                        videoTitle = video?.name.orEmpty(),
                                                        episodeName = displayName,
                                                        videoUrl = episode.url,
                                                        coverUrl = video?.pic
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 反馈面板
                if (showFeedbackSheet) {
                    val configuration = LocalConfiguration.current
                    val screenHeight = configuration.screenHeightDp.dp
                    val sheetHeight = screenHeight * 0.6f
                    ModalBottomSheet(
                        onDismissRequest = { showFeedbackSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .height(sheetHeight)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "问题反馈",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { showFeedbackSheet = false }) {
                                    Text("关闭")
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "反馈类型",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // 使用简单的点击展开列表替代 ExposedDropdownMenuBox，提升性能
                                Box {
                                    Button(
                                        onClick = { feedbackTypeExpanded = !feedbackTypeExpanded },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(selectedFeedbackType)
                                            Icon(
                                                imageVector = if (feedbackTypeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = if (feedbackTypeExpanded) "收起" else "展开"
                                            )
                                        }
                                    }

                                    if (feedbackTypeExpanded) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .shadow(8.dp),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Column {
                                                PlaybackFeedbackOptions.forEach { option ->
                                                    TextButton(
                                                        onClick = {
                                                            selectedFeedbackType = option
                                                            feedbackTypeExpanded = false
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 16.dp)
                                                    ) {
                                                        Text(option)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.submitFeedback(selectedFeedbackType)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSubmittingFeedback
                            ) {
                                if (isSubmittingFeedback) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("提交反馈")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "权限不足",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "权限不足",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = permissionState.tipFor("video", "当前账号暂无视频权限，无法观看视频内容"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("返回")
                    }
                }
            }
        }
    }
}

/**
 * 播放器页面操作行
 */
@Composable
private fun PlayerPageActionRow(
    selectedTab: PlayerPageRowTab,
    onTabSelected: (PlayerPageRowTab) -> Unit,
    isDanmuEnabled: Boolean,
    onDanmuClick: () -> Unit,
    onDanmuInputClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                PlayerPageRowTab.values().forEach { tab ->
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (selectedTab == tab) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .clickable {
                                onTabSelected(tab)
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }

            // 弹幕状态和操作
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 弹幕开启时显示"发送弹幕"按钮
                if (isDanmuEnabled) {
                    TextButton(
                        onClick = onDanmuInputClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "发送弹幕",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 弹幕开关图标
                IconButton(onClick = onDanmuClick) {
                    Image(
                        painter = painterResource(id = if (isDanmuEnabled) R.drawable.dmk else R.drawable.dmg),
                        contentDescription = if (isDanmuEnabled) "关闭弹幕" else "开启弹幕",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 黑色横线分隔
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
    }
}

/**
 * 播放器页面标签
 */
enum class PlayerPageRowTab(val title: String) {
    Intro("简介"),
    Comment("评论")
}

/**
 * 播放器底部操作栏（收藏、下载、反馈、催更）
 */
@Composable
private fun PlayerBottomActionBar(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onUrgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 收藏按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFavoriteClick() }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消收藏" else "收藏",
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFavorite) "已收藏" else "收藏",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 下载按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDownloadClick() }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "下载",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "下载",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 反馈按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFeedbackClick() }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Feedback,
                    contentDescription = "反馈",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "反馈",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 催更按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onUrgeClick() }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "催更",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "催更",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 播放器更多选项弹窗尺寸
 */
@Composable
private fun rememberPlayerMoreSheetMetrics() = remember {
    object {
        val sheetHeight = 600.dp
    }
}

/**
 * 弹幕输入面板高度
 */
@Composable
private fun rememberDanmuInputSheetHeight() = remember {
    250.dp
}

/**
 * 播放反馈选项
 */
private val PlaybackFeedbackOptions = listOf(
    "播放卡顿",
    "画面模糊",
    "无声音",
    "无法播放",
    "其他问题"
)

/**
 * 进入画中画模式
 */
private fun enterPictureInPicture(context: Context) {
    val activity = context.findActivity() ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            val params = PictureInPictureParams.Builder().build()
            activity.enterPictureInPictureMode(params)
        }
    }
}

/**
 * 解析播放源
 */
private fun parsePlaySources(
    playFrom: String?,
    playShow: String?,
    playUrl: String?,
    parseConfigs: List<ParseConfig>
): List<PlaySource> {
    val sources = mutableListOf<PlaySource>()

    // 先解密 playUrl（后端AES-256-GCM加密，防TVBox抓包）
    val decryptedPlayUrl = try {
        playUrl?.let { rawUrl ->
            android.util.Log.d("PlayerScreen", "尝试解密 playUrl: $rawUrl")
            val decrypted = ApiSecurity.decrypt(rawUrl)
            android.util.Log.d("PlayerScreen", "解密成功: $decrypted")
            decrypted ?: rawUrl
        }
    } catch (e: Exception) {
        android.util.Log.e("PlayerScreen", "解密失败，使用原始URL", e)
        playUrl // 解密失败降级使用原值
    }

    // 处理播放源
    if (!playFrom.isNullOrBlank() && !decryptedPlayUrl.isNullOrBlank()) {
        val froms = playFrom.split("$$$")
        val urls = decryptedPlayUrl.split("$$$")
        val shows = playShow?.split("$$$") ?: emptyList()

        froms.forEachIndexed { index, from ->
            if (index < urls.size) {
                val show = if (index < shows.size) shows[index] else ""
                val sourceUrls = parsePlayUrls(urls[index], show)
                android.util.Log.d("PlayerScreen", "Source[$index] from=$from, show=$show, urls count=${sourceUrls.size}")
                if (sourceUrls.isNotEmpty()) {
                    // 优先使用 playShow 中的名称，如果为空则从 parseConfigs 查找，最后才使用 fromKey
                    val parseConfig = parseConfigs.find { it.from == from }
                    android.util.Log.d("PlayerScreen", "Source[$index] 查找解析配置: from='$from', parseConfigs.size=${parseConfigs.size}")
                    parseConfigs.forEachIndexed { idx, cfg ->
                        android.util.Log.d("PlayerScreen", "  parseConfig[$idx]: from='${cfg.from}', show='${cfg.show}', parse='${cfg.parse}'")
                    }
                    android.util.Log.d("PlayerScreen", "Source[$index] 匹配结果: parseConfig=${parseConfig != null}, parse='${parseConfig?.parse}'")

                    val sourceName = if (show.isNotBlank()) {
                        show
                    } else {
                        parseConfig?.show ?: from
                    }
                    // 获取解析API地址
                    val parseApiUrl = parseConfig?.parse?.trim().orEmpty()
                    android.util.Log.d("PlayerScreen", "Source[$index] parseApiUrl=$parseApiUrl")

                    sources.add(
                        PlaySource(
                            name = sourceName,
                            fromKey = from,
                            urls = sourceUrls,
                            parseApiUrl = parseApiUrl
                        )
                    )
                }
            }
        }
    }

    return sources
}

/**
 * 解析播放地址
 * 格式：第1集$http://url1#第2集$http://url2
 */
private fun parsePlayUrls(urls: String, show: String): List<PlayUrl> {
    val result = mutableListOf<PlayUrl>()
    val urlList = urls.split("#")

    urlList.forEachIndexed { index, item ->
        if (item.isNotBlank()) {
            // 按 $ 分割集名和URL
            val parts = item.split("$", limit = 2)
            val name = parts.getOrNull(0)?.trim() ?: ""
            val url = parts.getOrNull(1)?.trim() ?: item.trim()

            // 如果没有集名，使用默认名称
            val episodeName = name.ifBlank { "第${index + 1}集" }

            if (url.isNotBlank()) {
                result.add(PlayUrl(episodeName, url))
            }
        }
    }

    return result
}

/**
 * 获取播放源名称
 */
private fun getPlayFromName(fromKey: String, parseConfigs: List<ParseConfig>): String {
    return parseConfigs.find { it.from == fromKey }?.show ?: fromKey
}

/**
 * 播放源
 */
data class PlaySource(
    val name: String,
    val fromKey: String,
    val urls: List<PlayUrl>,
    val parseApiUrl: String = ""  // 解析API地址，为空则直接播放
)