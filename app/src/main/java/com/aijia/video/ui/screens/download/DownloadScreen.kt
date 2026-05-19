package com.aijia.video.ui.screens.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijia.video.data.model.DownloadRecord
import com.aijia.video.data.repository.DownloadStatus
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (DownloadRecord) -> Unit,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val downloads = viewModel.downloads.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (downloads.value.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearCompleted) {
                            Icon(Icons.Default.Delete, contentDescription = "清理已完成")
                        }
                    }
                }
            )
        },
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) { paddingValues ->
        if (downloads.value.isEmpty()) {
            Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "下载",
                        modifier = androidx.compose.ui.Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
                    Text(
                        text = "暂无下载内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads.value, key = { it.id }) { item ->
                    DownloadItem(
                        record = item,
                        onPause = { viewModel.pauseDownload(item.id) },
                        onResume = { viewModel.resumeDownload(item.id) },
                        onDelete = { viewModel.deleteDownload(item.id) },
                        onNavigateToPlayer = { onNavigateToPlayer(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    record: DownloadRecord,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val targetFile = File(record.localPath)
    val displayTitle = buildString {
        append(record.videoTitle)
        record.episodeName?.takeIf { it.isNotBlank() }?.let {
            append(" · ")
            append(it)
        }
    }
    val fileSizeText = when {
        record.status == DownloadStatus.COMPLETED -> formatFileSize(targetFile.length())
        record.downloadedBytes > 0L && record.totalBytes > 0L -> {
            "${formatFileSize(record.downloadedBytes)} / ${formatFileSize(record.totalBytes)}"
        }
        record.downloadedBytes > 0L -> formatFileSize(record.downloadedBytes)
        else -> "等待下载"
    }
    val timeText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        .format(Date(record.updatedAt))

    Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = androidx.compose.ui.Modifier.padding(16.dp)) {
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                    Text(text = displayTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                    Text(
                        text = fileSizeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    when (record.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Default.Pause, contentDescription = "暂停")
                            }
                        }

                        DownloadStatus.PAUSED,
                        DownloadStatus.FAILED -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Default.Refresh, contentDescription = "继续")
                            }
                        }

                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onNavigateToPlayer) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                            }
                        }

                        else -> Unit
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (record.status == DownloadStatus.DOWNLOADING || record.status == DownloadStatus.PAUSED || record.status == DownloadStatus.FAILED) {
                Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { record.progress / 100f },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                Text(
                    text = "${record.progress}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
            StatusChip(status = record.status)
        }
    }
}

@Composable
private fun StatusChip(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.PENDING -> "等待中" to MaterialTheme.colorScheme.outline
        DownloadStatus.DOWNLOADING -> "下载中" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> "已暂停" to MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> "已完成" to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> "失败" to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> "已取消" to MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
        else -> String.format(Locale.getDefault(), "%.1fGB", bytes / (1024f * 1024f * 1024f))
    }
}
