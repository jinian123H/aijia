package com.aijia.video.ui.components

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aijia.video.ui.components.DanmuView
import com.aijia.video.ui.screens.player.PlayerViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    title: String,
    onBackClick: () -> Unit,
    onBackFromFullscreen: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onPreviousEpisode: () -> Unit = {},
    onNextEpisode: () -> Unit = {},
    hasPreviousEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    fullscreenLayout: Boolean = true,
    episodeList: List<String> = emptyList(),
    currentEpisodeIndex: Int = 0,
    onEpisodeClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var showEpisodePanel by remember { mutableStateOf(false) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var brightness by remember {
        val window = (context as? android.app.Activity)?.window
        val current = window?.attributes?.screenBrightness ?: -1f
        mutableStateOf(if (current < 0f) 0.5f else current)
    }
    var volume by remember {
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableStateOf(cur.toFloat() / maxVolume.toFloat())
    }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var gestureStartY by remember { mutableStateOf(0f) }

    val playerView = remember {
        viewModel.getPlayerView(context).apply {
            useController = false
            keepScreenOn = true
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    // 监听播放器状态
    DisposableEffect(playerView) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && playerView.player?.playWhenReady == false) {
                    playerView.player?.playWhenReady = true
                }
            }
        }
        playerView.player?.addListener(listener)
        onDispose {
            // 先移除listener
            playerView.player?.removeListener(listener)
            // 清空player引用，释放Surface
            playerView.player = null
            // 手动触发Surface释放（关键修复）
            try {
                val surfaceHolderField = PlayerView::class.java.getDeclaredField("surfaceView")
                surfaceHolderField.isAccessible = true
                val surfaceView = surfaceHolderField.get(playerView) as? android.view.SurfaceView
                surfaceView?.holder?.surface?.release()
            } catch (e: Exception) {
                // 反射失败不影响主流程
                android.util.Log.w("VideoPlayer", "Failed to release surface via reflection", e)
            }
        }
    }

    // 更新进度
    LaunchedEffect(Unit) {
        while (isActive) {
            playerView.player?.let { player ->
                if (!isSeeking) {
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                    duration = player.duration.coerceAtLeast(0L)
                    viewModel.updateCurrentPosition(currentPosition)
                    viewModel.updateDuration(duration)
                }
            }
            delay(500)
        }
    }

    // 自动隐藏控制条
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = if (fullscreenLayout) {
            modifier
                .fillMaxSize()
                .background(Color.Black)
        } else {
            modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        }
    ) {
        AndroidView<PlayerView>(
            factory = { playerView },
            update = { view ->
                view.useController = false
                view.keepScreenOn = true
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
        )

        // 全屏手势层：左半屏亮度，右半屏音量，点击切换控件
        if (fullscreenLayout) {
            var isLeftSide by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isLeftSide = offset.x < size.width / 2
                                if (isLeftSide) showBrightnessOverlay = true else showVolumeOverlay = true
                            },
                            onDragEnd = {
                                showBrightnessOverlay = false
                                showVolumeOverlay = false
                            },
                            onDragCancel = {
                                showBrightnessOverlay = false
                                showVolumeOverlay = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount.y / size.height
                                if (isLeftSide) {
                                    brightness = (brightness + delta).coerceIn(0f, 1f)
                                    (context as? android.app.Activity)?.window?.let { window ->
                                        val params = window.attributes
                                        params.screenBrightness = brightness
                                        window.attributes = params
                                    }
                                } else {
                                    volume = (volume + delta).coerceIn(0f, 1f)
                                    val newVol = (volume * maxVolume).toInt().coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                }
                            }
                        )
                    }
            )
        }

        // 弹幕显示区域
        if (viewModel.showDanmu.collectAsStateWithLifecycle().value) {
            val danmuList by viewModel.danmuList.collectAsStateWithLifecycle()
            DanmuView(
                danmuList = danmuList,
                currentTime = currentPosition,
                videoKey = "video",
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showControls) {
            // 左上角返回按钮和当前集数显示
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = if (fullscreenLayout) onBackFromFullscreen else onBackClick
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                
                if (episodeList.isNotEmpty()) {
                    Text(
                        text = episodeList.getOrNull(currentEpisodeIndex) ?: "第${currentEpisodeIndex + 1}集",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            // 全屏状态下的集数按钮
            if (fullscreenLayout) {
                TextButton(
                    onClick = { showEpisodePanel = !showEpisodePanel },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(
                        text = "选集",
                        fontSize = 14.sp
                    )
                }

                // 集数面板
                if (showEpisodePanel && episodeList.isNotEmpty()) {
                    EpisodePanel(
                        episodeList = episodeList,
                        currentEpisodeIndex = currentEpisodeIndex,
                        onEpisodeClick = onEpisodeClick,
                        onClose = { showEpisodePanel = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 60.dp, end = 16.dp)
                    )
                }
            }

            // 中间播放控制按钮（上一集 + 播放/暂停 + 下一集）
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 上一集按钮
                IconButton(
                    onClick = onPreviousEpisode,
                    enabled = hasPreviousEpisode,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一集",
                        tint = if (hasPreviousEpisode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 播放/暂停按钮
                IconButton(
                    onClick = {
                        playerView.player?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                if (player.playbackState == Player.STATE_IDLE ||
                                    player.playbackState == Player.STATE_ENDED) {
                                    player.prepare()
                                }
                                player.play()
                            }
                        }
                    },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // 下一集按钮
                IconButton(
                    onClick = onNextEpisode,
                    enabled = hasNextEpisode,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一集",
                        tint = if (hasNextEpisode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 底部控制栏（进度条 + 全屏）
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 当前时间
                Text(
                    text = formatTime(currentPosition),
                    color = Color.White,
                    fontSize = 12.sp
                )

                // 进度条
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() else 0f,
                    onValueChange = { value ->
                        isSeeking = true
                        currentPosition = value.toLong()
                    },
                    onValueChangeFinished = {
                        playerView.player?.seekTo(currentPosition)
                        isSeeking = false
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.weight(1f),
                    thumb = { }
                )

                // 总时长
                Text(
                    text = formatTime(duration),
                    color = Color.White,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.width(4.dp))

                // 全屏按钮
                IconButton(
                    onClick = {
                        if (fullscreenLayout) {
                            onBackFromFullscreen()
                        } else {
                            viewModel.toggleFullscreen()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (fullscreenLayout) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (fullscreenLayout) "退出全屏" else "全屏",
                        tint = Color.White
                    )
                }
            }

            // 亮度/音量手势反馈
            if (showBrightnessOverlay) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 32.dp)
                        .width(48.dp)
                        .height(120.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessHigh,
                            contentDescription = "亮度",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "${(brightness * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(60.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(brightness.coerceIn(0f, 1f))
                                    .background(Color.White, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            if (showVolumeOverlay) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 32.dp)
                        .width(48.dp)
                        .height(120.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "音量",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(60.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(volume.coerceIn(0f, 1f))
                                    .background(Color.White, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodePanel(
    episodeList: List<String>,
    currentEpisodeIndex: Int,
    onEpisodeClick: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(16.dp)
            .width(220.dp)
            .heightIn(max = 400.dp)
    ) {
        // 关闭按钮
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }

        // 集数网格（可滚动的4列网格）
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 28.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(episodeList) { episode ->
                val index = episodeList.indexOf(episode)
                EpisodeButton(
                    episodeName = episode,
                    episodeIndex = index,
                    isCurrent = index == currentEpisodeIndex,
                    onClick = { onEpisodeClick(index) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeButton(
    episodeName: String,
    episodeIndex: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = (episodeIndex + 1).toString(),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
