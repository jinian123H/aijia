package com.aijia.video.data.model

import com.aijia.video.data.repository.DownloadStatus

data class DownloadRecord(
    val id: String,
    val videoId: String,
    val videoTitle: String,
    val episodeName: String? = null,
    val coverUrl: String? = null,
    val remoteUrl: String,
    val localPath: String,
    val fileType: DownloadFileType,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class DownloadFileType {
    MP4,
    M3U8
}
