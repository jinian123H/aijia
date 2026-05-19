package com.aijia.video.ui.screens.download

import androidx.lifecycle.ViewModel
import com.aijia.video.data.model.DownloadRecord
import com.aijia.video.data.repository.DownloadRepository
import com.aijia.video.data.repository.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val downloads: Flow<List<DownloadRecord>> = downloadRepository.downloads
    val downloadStatus: Flow<Map<String, DownloadStatus>> = downloadRepository.downloadStatus
    val downloadProgress: Flow<Map<String, Int>> = downloadRepository.downloadProgress

    fun startDownload(
        videoId: String,
        videoTitle: String,
        episodeName: String?,
        videoUrl: String,
        coverUrl: String? = null
    ): String {
        return downloadRepository.startDownload(
            videoId = videoId,
            videoTitle = videoTitle,
            episodeName = episodeName,
            videoUrl = videoUrl,
            coverUrl = coverUrl
        )
    }

    fun pauseDownload(downloadId: String) {
        downloadRepository.pauseDownload(downloadId)
    }

    fun resumeDownload(downloadId: String) {
        downloadRepository.resumeDownload(downloadId)
    }

    fun deleteDownload(downloadId: String) {
        downloadRepository.cancelDownload(downloadId)
    }

    fun clearCompleted() {
        downloadRepository.clearCompleted()
    }
}
