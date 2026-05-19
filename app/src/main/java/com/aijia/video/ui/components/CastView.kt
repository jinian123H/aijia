package com.aijia.video.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijia.video.cast.CastManager
import com.aijia.video.cast.CastState
import com.aijia.video.cast.PlaybackState

/**
 * 投屏视图组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastView(
    modifier: Modifier = Modifier,
    castManager: CastManager,
    onNavigateBack: () -> Unit
) {
    val castState by castManager.castState.collectAsStateWithLifecycle()
    val castDevices by castManager.castDevices.collectAsStateWithLifecycle()
    val castSession by castManager.castSession.collectAsStateWithLifecycle()
    var playbackState by remember { mutableStateOf<PlaybackState?>(null) }
    
    LaunchedEffect(castSession) {
        playbackState = castManager.getPlaybackState()
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部栏
        TopAppBar(
            title = { Text("投屏") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { castManager.scanDevices() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "扫描设备")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (castState) {
            CastState.NO_DEVICES_AVAILABLE -> {
                NoDevicesView(
                    onScan = { castManager.scanDevices() }
                )
            }
            
            CastState.SCANNING -> {
                ScanningView()
            }
            
            CastState.CONNECTING -> {
                ConnectingView()
            }
            
            CastState.CONNECTED -> {
                ConnectedView(
                    castSession = castSession,
                    castManager = castManager,
                    playbackState = playbackState
                )
            }
            
            CastState.DISCONNECTING -> {
                DisconnectingView()
            }
            
            CastState.ERROR -> {
                ErrorView(
                    onRetry = { castManager.scanDevices() }
                )
            }
        }
    }
}

/**
 * 无设备视图
 */
@Composable
private fun NoDevicesView(
    onScan: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CastConnected,
                contentDescription = "无设备",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "未发现投屏设备",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "请确保您的电视或投屏设备与手机在同一网络",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("扫描设备")
            }
        }
    }
}

/**
 * 扫描中视图
 */
@Composable
private fun ScanningView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "正在扫描投屏设备...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 连接中视图
 */
@Composable
private fun ConnectingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "正在连接设备...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 已连接视图
 */
@Composable
private fun ConnectedView(
    castSession: com.aijia.video.cast.CastDevice?,
    castManager: CastManager,
    playbackState: PlaybackState?
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 设备信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "已连接到 ${castSession?.name ?: "未知设备"}",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 播放控制
                PlaybackControls(
                    playbackState = playbackState,
                    onPlay = { castManager.play() },
                    onPause = { castManager.pause() },
                    onStop = { castManager.stop() },
                    onDisconnect = { castManager.disconnect() }
                )
            }
        }
    }
}

/**
 * 播放控制组件
 */
@Composable
private fun PlaybackControls(
    playbackState: PlaybackState?,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (playbackState) {
            PlaybackState.PLAYING -> {
                IconButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = "暂停")
                }
            }
            
            PlaybackState.PAUSED, PlaybackState.IDLE -> {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                }
            }
            
            PlaybackState.BUFFERING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
            
            else -> {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                }
            }
        }
        
        IconButton(onClick = onStop) {
            Icon(Icons.Default.Stop, contentDescription = "停止")
        }
        
        IconButton(onClick = onDisconnect) {
            Icon(Icons.Default.Close, contentDescription = "断开连接")
        }
    }
}

/**
 * 断开连接视图
 */
@Composable
private fun DisconnectingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "正在断开连接...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误视图
 */
@Composable
private fun ErrorView(
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "错误",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "连接失败",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "请检查网络连接或重试",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("重试")
            }
        }
    }
}
