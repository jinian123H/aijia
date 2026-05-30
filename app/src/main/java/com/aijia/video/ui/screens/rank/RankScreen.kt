package com.aijia.video.ui.screens.rank

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.util.lerp
import com.aijia.video.data.model.RankSection
import com.aijia.video.data.model.Video
import com.aijia.video.ui.components.FavoriteStatusBadge
import com.aijia.video.ui.components.VideoCoverImage
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay

/**
 * 排行榜页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankScreen(
    onNavigateToPlayer: (Video) -> Unit,
    viewModel: RankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val pagerState = rememberPagerState(
        initialPage = uiState.selectedTabIndex,
        pageCount = { uiState.sections.size }
    )

    var showTitleOverlay by remember { mutableStateOf(false) }
    var overlayTitle by remember { mutableStateOf("") }
    var isFirstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.selectedTabIndex, uiState.sections.size) {
        if (uiState.sections.isNotEmpty() && pagerState.currentPage != uiState.selectedTabIndex) {
            pagerState.animateScrollToPage(uiState.selectedTabIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage, uiState.sections.size) {
        if (uiState.sections.isNotEmpty() && pagerState.settledPage in uiState.sections.indices) {
            val newIndex = pagerState.settledPage
            val section = uiState.sections[newIndex]
            if (isFirstLoad) {
                isFirstLoad = false
            } else {
                overlayTitle = section.displayTitle()
                showTitleOverlay = true
                delay(1400)
                showTitleOverlay = false
            }
            viewModel.selectTab(newIndex)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.sections.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    modifier = Modifier.height(42.dp),
                    edgePadding = 8.dp,
                    divider = {},
                    indicator = { tabPositions ->
                        if (uiState.selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier
                                    .tabIndicatorOffset(tabPositions[uiState.selectedTabIndex])
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    uiState.sections.forEachIndexed { index, section ->
                        Tab(
                            selected = index == uiState.selectedTabIndex,
                            onClick = { viewModel.selectTab(index) },
                            text = {
                                Text(
                                    text = section.displayTitle(),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                // 滑动指示器（小圆点）
                if (uiState.sections.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(uiState.sections.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == uiState.selectedTabIndex) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == uiState.selectedTabIndex)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                }
            }

            when {
                uiState.isLoading && uiState.videos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "加载失败",
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.retry() }) {
                                Text("重试")
                            }
                        }
                    }
                }

                uiState.videos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无排行榜数据",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val section = uiState.sections[page]
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                            val animatedScale = lerp(0.94f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                            val animatedAlpha = lerp(0.72f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                            val animatedTranslationX = lerp(32f, 0f, 1f - pageOffset.coerceIn(0f, 1f))

                            PullToRefreshBox(
                                state = pullToRefreshState,
                                isRefreshing = uiState.isRefreshing,
                                onRefresh = { viewModel.refresh() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                        translationX = animatedTranslationX * if (page >= pagerState.currentPage) 1f else -1f
                                    }
                                    .alpha(animatedAlpha)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                if (section.videos.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "该分类暂无排行数据",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                                        contentPadding = PaddingValues(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(
                                            items = section.videos,
                                            key = { _, video -> video.id }
                                        ) { index, video ->
                                            RankVideoItem(
                                                video = video,
                                                onClick = { onNavigateToPlayer(video) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showTitleOverlay) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.82f))
                                    .padding(horizontal = 32.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = overlayTitle,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun RankSection.displayTitle(): String {
    val metricLabel = when (rank_field) {
        "vod_hits_day" -> "日人气"
        "vod_hits_week" -> "周人气"
        "vod_hits_month" -> "月人气"
        "vod_up" -> "顶榜"
        "vod_down" -> "踩榜"
        "vod_score" -> "平均分"
        "vod_score_all" -> "总评分"
        else -> "总人气"
    }

    return if (title.isBlank()) metricLabel else "$title·$metricLabel"
}

@Composable
private fun RankVideoItem(
    video: Video,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box {
                VideoCoverImage(
                    model = video.picSlide?.takeIf { it.isNotBlank() } ?: video.pic,
                    contentDescription = video.name,
                    modifier = Modifier
                        .width(78.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop,
                    requestWidth = 78.dp,
                    requestHeight = 117.dp
                )

                if (video.isFavorite) {
                    FavoriteStatusBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val infoLine = listOfNotNull(
                    video.year?.takeIf { it.isNotBlank() },
                    video.area?.takeIf { it.isNotBlank() },
                    video.lang?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")

                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                video.actor?.takeIf { it.isNotBlank() }?.let { actor ->
                    Text(
                        text = actor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (infoLine.isNotBlank()) {
                    Text(
                        text = infoLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                video.remarks?.takeIf { it.isNotBlank() }?.let { remarks ->
                    Text(
                        text = remarks,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
