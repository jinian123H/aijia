package com.aijia.video.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.model.Video
import com.aijia.video.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(
        val videos: List<Video>,
        val hasMore: Boolean = false
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentPage = 1
    private var isLoadingMore = false

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun search(keyword: String) {
        if (keyword.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            currentPage = 1

            val result = videoRepository.searchVideos(keyword, page = 1, limit = 20)

            result.fold(
                onSuccess = { pagedResponse ->
                    _uiState.value = SearchUiState.Success(
                        videos = pagedResponse.data,
                        hasMore = pagedResponse.hasMore
                    )
                },
                onFailure = { error ->
                    _uiState.value = SearchUiState.Error(error.message ?: "搜索失败")
                }
            )
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState !is SearchUiState.Success || isLoadingMore || !currentState.hasMore) {
            return
        }

        val keyword = _searchQuery.value
        if (keyword.isBlank()) return

        viewModelScope.launch {
            isLoadingMore = true
            currentPage++

            val result = videoRepository.searchVideos(keyword, page = currentPage, limit = 20)

            result.fold(
                onSuccess = { pagedResponse ->
                    // 去重：过滤掉已存在的视频ID
                    val existingIds = currentState.videos.map { it.id }.toSet()
                    val uniqueNewVideos = pagedResponse.data.filter { it.id !in existingIds }
                    _uiState.value = SearchUiState.Success(
                        videos = currentState.videos + uniqueNewVideos,
                        hasMore = pagedResponse.hasMore
                    )
                },
                onFailure = {
                    currentPage--
                }
            )

            isLoadingMore = false
        }
    }
}
