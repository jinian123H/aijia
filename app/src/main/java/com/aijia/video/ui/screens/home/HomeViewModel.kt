package com.aijia.video.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.model.BannerItem
import com.aijia.video.data.model.HomeSection
import com.aijia.video.data.model.Video
import com.aijia.video.data.model.VideoType
import com.aijia.video.data.repository.VideoRepository
import com.aijia.video.util.PreloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val preloadManager: PreloadManager
) : ViewModel() {

    companion object {
        private const val CATEGORY_PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val categoryCache = mutableMapOf<Int, List<Video>>()

    init {
        loadHomeData()
    }

    /**
     * 加载 Banner 轮播数据
     */
    private fun loadBanner() {
        viewModelScope.launch {
            val result = videoRepository.getBanner()
            result.onSuccess { items ->
                val currentState = _uiState.value as? HomeUiState.Success ?: return@launch
                if (items != null) {
                    _uiState.value = currentState.copy(
                        banners = items.map { item ->
                            Video(
                                id = item.vodId.toString(),
                                name = item.title,
                                picSlide = item.cover,
                                pic = item.cover
                            )
                        }
                    )
                    android.util.Log.d("HomeViewModel", "loadBanner - 加载完成: " + items.size + "项")
                }
            }.onFailure {
                android.util.Log.e("HomeViewModel", "loadBanner失败: " + it.message)
            }
        }
    }

    /**
     * 加载首页数据
     */
    fun loadHomeData() {
        viewModelScope.launch {
            val previousState = _uiState.value as? HomeUiState.Success
            _uiState.value = previousState ?: HomeUiState.Loading

            android.util.Log.d("HomeViewModel", "loadHomeData - 开始加载")

            // 尝试使用预加载数据（仅在首次加载时）
            if (previousState == null && preloadManager.hasPreloadedData()) {
                android.util.Log.d("HomeViewModel", "loadHomeData - 发现预加载数据，直接使用")
                val preloadedSections = preloadManager.getPreloadedSections()
                val preloadedVideoTypes = preloadManager.getPreloadedVideoTypes()

                if (preloadedSections != null || preloadedVideoTypes != null) {
                    val videoTypes = if (preloadedVideoTypes != null) {
                        mutableListOf(VideoType(id = 0, name = "全部")).apply {
                            addAll(preloadedVideoTypes)
                        }
                    } else {
                        mutableListOf(VideoType(id = 0, name = "全部"))
                    }

                    _uiState.value = HomeUiState.Success(
                        banners = emptyList(),
                        sections = preloadedSections ?: emptyList(),
                        videoTypes = videoTypes,
                        selectedCategoryId = 0,
                        selectedCategoryName = "全部",
                        categoryVideos = emptyList(),
                        selectedArea = "全部",
                        selectedYear = "全部",
                        selectedSort = "最新",
                        currentCategoryPage = 1,
                        hasMoreCategoryVideos = false,
                        isCategoryLoading = false,
                        isCategoryLoadingMore = false
                    )

                    android.util.Log.d("HomeViewModel", "loadHomeData - 使用预加载数据完成")

                    loadBanner()
                    preloadManager.clearPreloadedData()
                    return@launch
                }
            }

            try {
                val (sectionsResult, videoTypesResult) = coroutineScope {
                    val sectionsDeferred = async(kotlinx.coroutines.Dispatchers.IO) { videoRepository.getHomeRecommend() }
                    val videoTypesDeferred = async(kotlinx.coroutines.Dispatchers.IO) { videoRepository.getVideoTypes() }
                    sectionsDeferred.await() to videoTypesDeferred.await()
                }

                when {
                    sectionsResult.isSuccess && videoTypesResult.isSuccess -> {
                        val sections = sectionsResult.getOrNull()
                        val newVideoTypes = videoTypesResult.getOrNull()

                        val videoTypes = if (newVideoTypes != null) {
                            mutableListOf(VideoType(id = 0, name = "全部")).apply {
                                addAll(newVideoTypes)
                            }
                        } else {
                            previousState?.videoTypes ?: mutableListOf(VideoType(id = 0, name = "全部"))
                        }

                        val previousTabIndex = _selectedTabIndex.value
                            .coerceIn(0, videoTypes.lastIndex.coerceAtLeast(0))
                        val previousCategoryId = previousState?.selectedCategoryId ?: 0
                        val selectedType = videoTypes.getOrNull(previousTabIndex)
                        val shouldRestoreCategory = previousCategoryId > 0 && selectedType?.id == previousCategoryId

                        _uiState.value = HomeUiState.Success(
                            banners = (previousState?.banners) ?: emptyList(),
                            sections = sections ?: previousState?.sections ?: emptyList(),
                            videoTypes = videoTypes,
                            selectedCategoryId = if (shouldRestoreCategory) previousCategoryId else 0,
                            selectedCategoryName = if (shouldRestoreCategory) {
                                previousState?.selectedCategoryName ?: (selectedType?.name ?: "全部")
                            } else {
                                "全部"
                            },
                            categoryVideos = if (shouldRestoreCategory) previousState?.categoryVideos ?: emptyList() else emptyList(),
                            selectedArea = if (shouldRestoreCategory) previousState?.selectedArea ?: "全部" else "全部",
                            selectedYear = if (shouldRestoreCategory) previousState?.selectedYear ?: "全部" else "全部",
                            selectedSort = if (shouldRestoreCategory) previousState?.selectedSort ?: "最新" else "最新",
                            currentCategoryPage = if (shouldRestoreCategory) previousState?.currentCategoryPage ?: 1 else 1,
                            hasMoreCategoryVideos = if (shouldRestoreCategory) previousState?.hasMoreCategoryVideos ?: false else false,
                            isCategoryLoading = false,
                            isCategoryLoadingMore = false
                        )

                        loadBanner()
                        _selectedTabIndex.value = if (shouldRestoreCategory) previousTabIndex else 0

                        if (shouldRestoreCategory && previousState?.categoryVideos.isNullOrEmpty()) {
                            val restoredState = _uiState.value as? HomeUiState.Success
                            if (restoredState != null) {
                                _uiState.value = restoredState.copy(
                                    isCategoryLoading = true,
                                    hasMoreCategoryVideos = true
                                )
                                loadCategoryVideos(reset = true)
                            }
                        }
                    }
                    else -> {
                        val errorMessage = listOf(
                            sectionsResult.exceptionOrNull(),
                            videoTypesResult.exceptionOrNull()
                        ).firstNotNullOfOrNull { it?.message } ?: "加载失败"
                        _uiState.value = HomeUiState.Error(errorMessage)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("网络错误: " + (e.message ?: ""))
            }
        }
    }

    /**
     * 选择Tab
     */
    fun selectTab(index: Int) {
        _selectedTabIndex.value = index

        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val selectedType = currentState.videoTypes.getOrNull(index) ?: return

        if (selectedType.id == 0) {
            _uiState.value = currentState.copy(
                selectedCategoryId = 0,
                selectedCategoryName = "全部",
                categoryVideos = emptyList(),
                selectedArea = "全部",
                selectedYear = "全部",
                selectedSort = "最新",
                currentCategoryPage = 1,
                hasMoreCategoryVideos = false,
                isCategoryLoading = false,
                isCategoryLoadingMore = false
            )
            return
        }

        val cached = categoryCache[selectedType.id]
        if (cached != null && cached.isNotEmpty()) {
            _uiState.value = currentState.copy(
                selectedCategoryId = selectedType.id,
                selectedCategoryName = selectedType.name,
                categoryVideos = cached,
                selectedArea = "全部",
                selectedYear = "全部",
                selectedSort = "最新",
                currentCategoryPage = (cached.size / CATEGORY_PAGE_SIZE).coerceAtLeast(1),
                hasMoreCategoryVideos = cached.size >= CATEGORY_PAGE_SIZE,
                isCategoryLoading = false,
                isCategoryLoadingMore = false
            )
            return
        }

        _uiState.value = currentState.copy(
            selectedCategoryId = selectedType.id,
            selectedCategoryName = selectedType.name,
            categoryVideos = emptyList(),
            selectedArea = "全部",
            selectedYear = "全部",
            selectedSort = "最新",
            currentCategoryPage = 1,
            hasMoreCategoryVideos = true,
            isCategoryLoading = true,
            isCategoryLoadingMore = false
        )

        loadCategoryVideos(reset = true)
    }

    fun selectArea(area: String) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        if (currentState.selectedArea == area) return

        _uiState.value = currentState.copy(
            selectedArea = area,
            currentCategoryPage = 1,
            hasMoreCategoryVideos = true,
            isCategoryLoading = true,
            isCategoryLoadingMore = false,
            categoryVideos = emptyList()
        )
        loadCategoryVideos(reset = true)
    }

    fun selectYear(year: String) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        if (currentState.selectedYear == year) return

        _uiState.value = currentState.copy(
            selectedYear = year,
            currentCategoryPage = 1,
            hasMoreCategoryVideos = true,
            isCategoryLoading = true,
            isCategoryLoadingMore = false,
            categoryVideos = emptyList()
        )
        loadCategoryVideos(reset = true)
    }

    fun selectSort(sort: String) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        if (currentState.selectedSort == sort) return

        _uiState.value = currentState.copy(
            selectedSort = sort,
            currentCategoryPage = 1,
            hasMoreCategoryVideos = true,
            isCategoryLoading = true,
            isCategoryLoadingMore = false,
            categoryVideos = emptyList()
        )
        loadCategoryVideos(reset = true)
    }

    fun loadMoreCategoryVideos() {
        val currentState = _uiState.value as? HomeUiState.Success ?: return

        android.util.Log.d("HomeViewModel", "loadMoreCategoryVideos - selectedCategoryId=" + currentState.selectedCategoryId + ", hasMore=" + currentState.hasMoreCategoryVideos + ", isLoading=" + currentState.isCategoryLoading + ", isLoadingMore=" + currentState.isCategoryLoadingMore)

        if (currentState.selectedCategoryId == 0 || currentState.isCategoryLoading || currentState.isCategoryLoadingMore || !currentState.hasMoreCategoryVideos) {
            android.util.Log.d("HomeViewModel", "loadMoreCategoryVideos - 跳过加载")
            return
        }

        android.util.Log.d("HomeViewModel", "loadMoreCategoryVideos - 开始加载下一页")
        _uiState.value = currentState.copy(isCategoryLoadingMore = true)
        loadCategoryVideos(reset = false)
    }

    fun openHomeSection(section: HomeSection) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return

        if (section.mode == 2 && section.target_id > 0) {
            val matchedIndex = currentState.videoTypes.indexOfFirst { it.id == section.target_id }
            if (matchedIndex >= 0) {
                _selectedTabIndex.value = matchedIndex
            }

            _uiState.value = currentState.copy(
                selectedCategoryId = section.target_id,
                selectedCategoryName = section.title,
                categoryVideos = emptyList(),
                selectedArea = "全部",
                selectedYear = "全部",
                selectedSort = "最新",
                currentCategoryPage = 1,
                hasMoreCategoryVideos = true,
                isCategoryLoading = true,
                isCategoryLoadingMore = false
            )
            loadCategoryVideos(reset = true)
            return
        }

        _uiState.value = currentState.copy(
            selectedCategoryId = -section.id,
            selectedCategoryName = section.title,
            categoryVideos = section.videos,
            selectedArea = "全部",
            selectedYear = "全部",
            selectedSort = "最新",
            currentCategoryPage = 1,
            hasMoreCategoryVideos = false,
            isCategoryLoading = false,
            isCategoryLoadingMore = false
        )
    }
    fun getCachedCategoryVideos(categoryId: Int): List<Video>? = categoryCache[categoryId]

    private fun mapSort(sort: String): String {
        return when (sort) {
            "最热" -> "hits"
            "最赞" -> "up"
            else -> "time"
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        loadHomeData()
    }

    private fun loadCategoryVideos(reset: Boolean) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val categoryId = currentState.selectedCategoryId
        if (categoryId <= 0) return

        val page = if (reset) 1 else currentState.currentCategoryPage + 1
        val limit = CATEGORY_PAGE_SIZE

        android.util.Log.d("HomeViewModel", "loadCategoryVideos - categoryId=" + categoryId + ", page=" + page + ", limit=" + limit + ", reset=" + reset)

        viewModelScope.launch {
            _uiState.value = currentState.copy(isCategoryLoading = reset, isCategoryLoadingMore = !reset)

            val result = videoRepository.getVideoList(
                type = categoryId,
                area = currentState.selectedArea.takeIf { it != "全部" },
                year = currentState.selectedYear.takeIf { it != "全部" },
                page = page,
                limit = limit,
                sort = mapSort(currentState.selectedSort),
                refresh = reset
            )

            result.onSuccess { pagedResponse ->
                android.util.Log.d("HomeViewModel", "loadCategoryVideos成功 - 返回" + pagedResponse.data.size + "条, total=" + pagedResponse.total + ", hasMore=" + pagedResponse.hasMore)

                val videos = pagedResponse.data
                val currentVideos = if (reset) {
                    videos
                } else {
                    val existingIds = currentState.categoryVideos.map { it.id }.toSet()
                    currentState.categoryVideos + videos.filter { it.id !in existingIds }
                }
                val hasMore = pagedResponse.hasMore

                android.util.Log.d("HomeViewModel", "loadCategoryVideos - 当前总数=" + currentVideos.size + ", hasMore=" + hasMore)

                categoryCache[categoryId] = currentVideos

                _uiState.value = currentState.copy(
                    categoryVideos = currentVideos,
                    currentCategoryPage = page,
                    hasMoreCategoryVideos = hasMore,
                    isCategoryLoading = false,
                    isCategoryLoadingMore = false
                )
            }.onFailure {
                android.util.Log.e("HomeViewModel", "loadCategoryVideos失败: " + it.message)
                _uiState.value = currentState.copy(isCategoryLoading = false, isCategoryLoadingMore = false)
            }
        }
    }
}

/**
 * 首页UI状态
 */
sealed class HomeUiState {
    object Loading : HomeUiState()

    data class Success(
        val banners: List<Video>,
        val sections: List<HomeSection>,
        val videoTypes: List<VideoType> = emptyList(),
        val selectedCategoryId: Int = 0,
        val selectedCategoryName: String = "全部",
        val categoryVideos: List<Video> = emptyList(),
        val selectedArea: String = "全部",
        val selectedYear: String = "全部",
        val selectedSort: String = "最新",
        val currentCategoryPage: Int = 1,
        val hasMoreCategoryVideos: Boolean = false,
        val isCategoryLoading: Boolean = false,
        val isCategoryLoadingMore: Boolean = false,
        val bannerItems: List<BannerItem> = emptyList()
    ) : HomeUiState() {
        val availableAreas: List<String>
            get() = listOf("全部") + categoryVideos.mapNotNull { it.area?.takeIf(String::isNotBlank) }.distinct()

        val availableYears: List<String>
            get() = listOf("全部") + categoryVideos.mapNotNull { it.year?.takeIf(String::isNotBlank) }.distinct().sortedDescending()
    }

    data class Error(val message: String) : HomeUiState()
}
