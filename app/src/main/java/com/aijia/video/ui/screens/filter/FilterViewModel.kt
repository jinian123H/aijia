package com.aijia.video.ui.screens.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.model.Video
import com.aijia.video.data.model.VideoType
import com.aijia.video.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 筛选页面ViewModel
 */
@HiltViewModel
class FilterViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilterUiState())
    val uiState: StateFlow<FilterUiState> = _uiState.asStateFlow()

    private val _selectedType = MutableStateFlow(0)
    val selectedType: StateFlow<Int> = _selectedType.asStateFlow()

    private val _selectedArea = MutableStateFlow("全部")
    val selectedArea: StateFlow<String> = _selectedArea.asStateFlow()

    private val _selectedYear = MutableStateFlow("全部")
    val selectedYear: StateFlow<String> = _selectedYear.asStateFlow()

    private val _selectedSort = MutableStateFlow("最新")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    /**
     * 加载筛选数据
     */
    fun loadFilterData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val videoTypesResult = videoRepository.getVideoTypes()

                if (videoTypesResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        videoTypes = videoTypesResult.getOrNull() ?: emptyList(),
                        areas = listOf("内地", "香港", "台湾", "日本", "韩国", "美国", "英国", "法国", "德国"),
                        years = listOf("2025", "2024", "2023", "2022", "2021", "2020", "2019"),
                        sorts = listOf("最新", "最热", "评分", "时间"),
                        isLoading = false
                    )
                    applyFilter()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = videoTypesResult.exceptionOrNull()?.message ?: "加载失败",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "加载失败",
                    isLoading = false
                )
            }
        }
    }

    /**
     * 选择类型
     */
    fun selectType(typeId: Int) {
        _selectedType.value = typeId
    }

    /**
     * 选择地区
     */
    fun selectArea(area: String) {
        _selectedArea.value = area
    }

    /**
     * 选择年份
     */
    fun selectYear(year: String) {
        _selectedYear.value = year
    }

    /**
     * 选择排序
     */
    fun selectSort(sort: String) {
        _selectedSort.value = sort
    }

    /**
     * 应用筛选
     */
    fun applyFilter() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val type = if (_selectedType.value == 0) null else _selectedType.value
                val result = videoRepository.getVideoList(
                    type = type,
                    page = 1,
                    limit = 20,
                    refresh = true
                )

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        videos = result.getOrNull()?.data ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "加载失败",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "加载失败",
                    isLoading = false
                )
            }
        }
    }
}

/**
 * 筛选页面UI状态
 */
data class FilterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val videoTypes: List<VideoType> = emptyList(),
    val areas: List<String> = emptyList(),
    val years: List<String> = emptyList(),
    val sorts: List<String> = emptyList(),
    val videos: List<Video> = emptyList()
)
