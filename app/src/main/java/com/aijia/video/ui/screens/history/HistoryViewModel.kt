package com.aijia.video.ui.screens.history

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

/**
 * 观看历史ViewModel
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _playHistory = MutableStateFlow<List<Video>>(emptyList())
    val playHistory: StateFlow<List<Video>> = _playHistory.asStateFlow()

    /**
     * 加载观看历史
     */
    fun loadPlayHistory() {
        viewModelScope.launch {
            videoRepository.getPlayHistory().collect { history ->
                _playHistory.value = history
            }
        }
    }

    /**
     * 从历史记录中删除
     */
    fun deleteFromHistory(videoId: String) {
        viewModelScope.launch {
            videoRepository.deleteFromPlayHistory(videoId)
        }
    }

    /**
     * 清空历史记录
     */
    fun clearPlayHistory() {
        viewModelScope.launch {
            videoRepository.clearPlayHistory()
        }
    }
}
