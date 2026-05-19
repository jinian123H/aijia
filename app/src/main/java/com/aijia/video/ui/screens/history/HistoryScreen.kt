package com.aijia.video.ui.screens.history

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijia.video.data.model.Video
import com.aijia.video.ui.components.PlayHistoryCard

/**
 * 观看历史Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Video) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val playHistory by viewModel.playHistory.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    
    LaunchedEffect(Unit) {
        viewModel.loadPlayHistory()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("观看历史") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (playHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearPlayHistory() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空历史")
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        if (playHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "历史",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无观看历史",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = playHistory,
                    key = { video -> video.id }
                ) { video ->
                    HistoryItem(
                        video = video,
                        onClick = { onNavigateToPlayer(video) },
                        onDelete = { viewModel.deleteFromHistory(video.id) }
                    )
                }
            }
        }
    }
}

/**
 * 历史记录项组件
 */
@Composable
private fun HistoryItem(
    video: Video,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    PlayHistoryCard(
        video = video,
        onPlayClick = onClick,
        onCloseClick = onDelete
    )
}
