package com.aijia.video.ui.screens.rank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijia.video.data.model.RankSection
import com.aijia.video.data.model.Video
import com.aijia.video.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RankUiState(
    val sections: List<RankSection> = emptyList(),
    val selectedTabIndex: Int = 0,
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RankViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankUiState())
    val uiState: StateFlow<RankUiState> = _uiState.asStateFlow()

    init {
        loadRankIndex()
    }

    fun selectTab(index: Int) {
        val state = _uiState.value
        if (index == state.selectedTabIndex || index !in state.sections.indices) {
            return
        }

        val selectedSection = state.sections[index]
        _uiState.value = state.copy(
            selectedTabIndex = index,
            videos = selectedSection.videos,
            errorMessage = null
        )
    }

    fun refresh() {
        loadRankIndex(refresh = true)
    }

    fun retry() {
        refresh()
    }

    private fun loadRankIndex(refresh: Boolean = false) {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                isLoading = !refresh && currentState.sections.isEmpty(),
                isRefreshing = refresh,
                errorMessage = null
            )

            val result = videoRepository.getRankIndex()

            if (result.isSuccess) {
                val sections = result.getOrNull()?.sections.orEmpty()
                val selectedIndex = if (sections.isEmpty()) 0 else currentState.selectedTabIndex.coerceIn(0, sections.lastIndex)
                val selectedVideos = sections.getOrNull(selectedIndex)?.videos.orEmpty()

                _uiState.value = currentState.copy(
                    sections = sections,
                    selectedTabIndex = if (sections.isEmpty()) 0 else selectedIndex,
                    videos = selectedVideos,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null
                )
            } else {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "加载排行榜失败"
                )
            }
        }
    }
}
