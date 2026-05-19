package com.aijia.video.ui.screens.filter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aijia.video.data.model.VideoType
import com.aijia.video.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (com.aijia.video.data.model.Video) -> Unit,
    viewModel: FilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedArea by viewModel.selectedArea.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadFilterData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视频筛选") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterOptions(
                videoTypes = uiState.videoTypes,
                areas = uiState.areas,
                years = uiState.years,
                sorts = uiState.sorts,
                selectedType = selectedType,
                selectedArea = selectedArea,
                selectedYear = selectedYear,
                selectedSort = selectedSort,
                onTypeSelected = { viewModel.selectType(it) },
                onAreaSelected = { viewModel.selectArea(it) },
                onYearSelected = { viewModel.selectYear(it) },
                onSortSelected = { viewModel.selectSort(it) },
                onApplyFilter = { viewModel.applyFilter() }
            )

            HorizontalDivider()

            when {
                uiState.isLoading -> {
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
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "加载失败",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.applyFilter() }) {
                                Text("重试")
                            }
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.videos,
                            key = { video -> video.id }
                        ) { video ->
                            VideoCard(
                                video = video,
                                onClick = { onNavigateToPlayer(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOptions(
    videoTypes: List<VideoType>,
    areas: List<String>,
    years: List<String>,
    sorts: List<String>,
    selectedType: Int,
    selectedArea: String,
    selectedYear: String,
    selectedSort: String,
    onTypeSelected: (Int) -> Unit,
    onAreaSelected: (String) -> Unit,
    onYearSelected: (String) -> Unit,
    onSortSelected: (String) -> Unit,
    onApplyFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FilterSectionType(
            title = "类型",
            options = listOf(VideoType(0, "全部")) + videoTypes,
            selectedOption = selectedType,
            onOptionSelected = onTypeSelected
        )

        FilterSectionString(
            title = "地区",
            options = listOf("全部") + areas,
            selectedOption = selectedArea,
            onOptionSelected = onAreaSelected
        )

        FilterSectionString(
            title = "年份",
            options = listOf("全部") + years,
            selectedOption = selectedYear,
            onOptionSelected = onYearSelected
        )

        FilterSectionString(
            title = "排序",
            options = listOf("全部") + sorts,
            selectedOption = selectedSort,
            onOptionSelected = onSortSelected
        )

        Button(
            onClick = onApplyFilter,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("应用筛选")
        }
    }
}

@Composable
private fun FilterSectionType(
    title: String,
    options: List<VideoType>,
    selectedOption: Int,
    onOptionSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                val isSelected = option.id == selectedOption
                FilterChip(
                    selected = isSelected,
                    onClick = { onOptionSelected(option.id) },
                    label = { Text(option.name) }
                )
            }
        }
    }
}

@Composable
private fun FilterSectionString(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                val isSelected = option == selectedOption
                FilterChip(
                    selected = isSelected,
                    onClick = { onOptionSelected(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}
