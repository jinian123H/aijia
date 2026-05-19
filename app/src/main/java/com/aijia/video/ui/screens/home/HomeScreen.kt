package com.aijia.video.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijia.video.R
import com.aijia.video.data.model.HomeSection
import com.aijia.video.data.model.Video
import com.aijia.video.ui.components.BannerCarousel
import com.aijia.video.ui.components.CategoryTabBar
import com.aijia.video.ui.components.HorizontalVideoCard
import com.aijia.video.ui.components.VideoCard
import com.aijia.video.ui.components.VideoSectionHeader
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision

/**
 * 首页Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: (Video) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDownload: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val successState = uiState as? HomeUiState.Success
    var horizontalDragAccumulation by remember { mutableStateOf(0f) }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                videoTypes = successState?.videoTypes,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { viewModel.selectTab(it) },
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToHistory = onNavigateToHistory
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        val state = successState
        val pageCount = state?.videoTypes?.size ?: 0
        val pagerState = rememberPagerState(
            initialPage = selectedTabIndex,
            pageCount = { pageCount }
        )

        // 同步 pagerState 和 selectedTabIndex（单向同步，避免冲突）
        var lastSyncedPage by remember { mutableIntStateOf(selectedTabIndex) }

        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
            if (!pagerState.isScrollInProgress && pagerState.currentPage != lastSyncedPage) {
                lastSyncedPage = pagerState.currentPage
                viewModel.selectTab(pagerState.currentPage)
            }
        }

        LaunchedEffect(selectedTabIndex) {
            if (selectedTabIndex != pagerState.currentPage && selectedTabIndex != lastSyncedPage) {
                lastSyncedPage = selectedTabIndex
                pagerState.animateScrollToPage(
                    page = selectedTabIndex,
                    animationSpec = androidx.compose.animation.core.spring(
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                    )
                )
            }
        }

        when (val state = uiState) {
            is HomeUiState.Loading -> Unit
            
            is HomeUiState.Success -> {
                val bannerPrefetchUrls = remember(state.banners) {
                    state.banners
                        .mapNotNull { it.coverImageUrl() }
                        .distinct()
                        .take(3)
                }
                
                PrefetchVideoCovers(
                    urls = bannerPrefetchUrls,
                    targetWidth = 720.dp,
                    targetHeight = 352.dp
                )

                // 监听滑动，预热相邻页封面
                LaunchedEffect(pagerState.currentPage, state.videoTypes, state.sections) {
                    val current = pagerState.currentPage
                    listOf(current - 1, current + 1).forEach { neighborPage ->
                        val neighborType = state.videoTypes.getOrNull(neighborPage) ?: return@forEach
                        val neighborId = neighborType.id
                        val urls = if (neighborId == 0) {
                            state.sections.asSequence()
                                .flatMap { it.videos.asSequence().take(6) }
                                .mapNotNull { it.coverImageUrl() }
                                .distinct().take(12).toList()
                        } else {
                            viewModel.getCachedCategoryVideos(neighborId)
                                ?.mapNotNull { it.coverImageUrl() }
                                ?.distinct()?.take(12)
                                ?: emptyList()
                        }
                        urls.forEach { url ->
                            context.imageLoader.enqueue(
                                ImageRequest.Builder(context)
                                    .data(url)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) { page ->
                    val videoType = state.videoTypes[page]
                    val categoryId = videoType.id

                    val cardPrefetchUrls = remember(categoryId, state.sections, state.categoryVideos) {
                        if (categoryId == 0) {
                            state.sections
                                .asSequence()
                                .flatMap { section -> section.videos.asSequence().take(6) }
                                .mapNotNull { it.coverImageUrl() }
                                .distinct()
                                .take(12)
                                .toList()
                        } else {
                            state.categoryVideos
                                .mapNotNull { it.coverImageUrl() }
                                .distinct()
                                .take(12)
                        }
                    }
                    
                    PrefetchVideoCovers(
                        urls = cardPrefetchUrls,
                        targetWidth = 240.dp,
                        targetHeight = 426.dp
                    )

                    if (categoryId == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // 优先显示Banner，延迟渲染分组内容
                            var showSections by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(150)
                                showSections = true
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 760.dp),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 72.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 立即显示Banner，不延迟
                                item(key = "home_banner") {
                                    BannerCarousel(
                                        banners = state.banners,
                                        onVideoClick = onNavigateToPlayer
                                    )
                                }

                                // 延迟显示分组内容，优先渲染Banner
                                if (showSections) {
                                    state.sections.forEach { section ->
                                        item(key = "section_${section.id}_${section.title}") {
                                            Column(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                VideoSectionHeader(
                                                    title = section.title,
                                                    horizontalPadding = 10.dp,
                                                    modifier = Modifier.padding(bottom = 0.dp),
                                                    onMoreClick = if (section.videos.size > (section.display_count.takeIf { it > 0 } ?: section.videos.size)) {
                                                        { viewModel.openHomeSection(section) }
                                                    } else {
                                                        null
                                                    }
                                                )

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    val visibleVideos = if (section.display_count > 0) {
                                                        section.videos.take(section.display_count)
                                                    } else {
                                                        section.videos
                                                    }

                                                    visibleVideos.chunked(3).forEach { rowVideos ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            rowVideos.forEach { video ->
                                                                HorizontalVideoCard(
                                                                    video = video,
                                                                    onClick = { onNavigateToPlayer(video) },
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }

                                                            repeat(3 - rowVideos.size) {
                                                                Spacer(modifier = Modifier.weight(1f))
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
                    } else {
                        val gridState = rememberLazyGridState()

                        val loadMoreTrigger by remember {
                            derivedStateOf {
                                val s = uiState as? HomeUiState.Success ?: return@derivedStateOf false
                                if (!s.hasMoreCategoryVideos || s.isCategoryLoading || s.isCategoryLoadingMore) {
                                    return@derivedStateOf false
                                }

                                val layoutInfo = gridState.layoutInfo
                                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                                val totalCount = layoutInfo.totalItemsCount
                                val lastVisibleIndex = lastVisibleItem?.index ?: -1
                                val viewportEnd = layoutInfo.viewportEndOffset
                                val isLastItemFullyVisible = lastVisibleItem != null &&
                                    lastVisibleItem.offset.y + lastVisibleItem.size.height <= viewportEnd

                                totalCount > 0 &&
                                    lastVisibleIndex >= totalCount - 1 &&
                                    isLastItemFullyVisible
                            }
                        }

                        LaunchedEffect(loadMoreTrigger) {
                            if (loadMoreTrigger) {
                                viewModel.loadMoreCategoryVideos()
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f),
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryFilterPanel(
                                areaOptions = state.availableAreas,
                                yearOptions = state.availableYears,
                                selectedArea = state.selectedArea,
                                selectedYear = state.selectedYear,
                                selectedSort = state.selectedSort,
                                onAreaSelected = viewModel::selectArea,
                                onYearSelected = viewModel::selectYear,
                                onSortSelected = viewModel::selectSort,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                state = gridState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .widthIn(max = 760.dp)
                                    .align(Alignment.CenterHorizontally),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (state.isCategoryLoading && state.categoryVideos.isEmpty()) {
                                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                } else {
                                    gridItems(
                                        items = state.categoryVideos,
                                        key = { video -> "cat_${video.id}" }
                                    ) { video ->
                                        HorizontalVideoCard(
                                            video = video,
                                            onClick = { onNavigateToPlayer(video) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (state.isCategoryLoadingMore) {
                                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadHomeData() }) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefetchVideoCovers(
    urls: List<String>,
    targetWidth: Dp,
    targetHeight: Dp
) {
    if (urls.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = with(density) { targetWidth.roundToPx() }
    val heightPx = with(density) { targetHeight.roundToPx() }

    LaunchedEffect(urls, widthPx, heightPx) {
        urls.forEach { url ->
            context.imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(widthPx, heightPx)
                    .precision(Precision.INEXACT)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            )
        }
    }
}

private fun Video.coverImageUrl(): String? {
    return picSlide?.takeIf { it.isNotBlank() } ?: pic?.takeIf { it.isNotBlank() }
}

@Composable
private fun CategoryFilterPanel(
    areaOptions: List<String>,
    yearOptions: List<String>,
    selectedArea: String,
    selectedYear: String,
    selectedSort: String,
    onAreaSelected: (String) -> Unit,
    onYearSelected: (String) -> Unit,
    onSortSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (areaOptions.size > 1) {
            FilterChipRow(
                options = areaOptions,
                selectedOption = selectedArea,
                onOptionSelected = onAreaSelected
            )
        }

        if (yearOptions.size > 1) {
            FilterChipRow(
                options = yearOptions,
                selectedOption = selectedYear,
                onOptionSelected = onYearSelected
            )
        }

        FilterChipRow(
            options = listOf("最新", "最热", "最赞"),
            selectedOption = selectedSort,
            onOptionSelected = onSortSelected
        )
    }
}

@Composable
private fun FilterChipRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        items(options) { option ->
            FilterChip(
                selected = option == selectedOption,
                onClick = { onOptionSelected(option) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = option == selectedOption,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
                ),
                modifier = Modifier.height(30.dp),
                label = {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    videoTypes: List<com.aijia.video.data.model.VideoType>?,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable(onClick = onNavigateToSearch)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            shape = MaterialTheme.shapes.extraLarge
                        ),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "搜索视频、演员、影片",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.History, contentDescription = "历史")
                }
            }

            videoTypes?.let {
                CategoryTabBar(
                    categories = it,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
        }
    }
}
