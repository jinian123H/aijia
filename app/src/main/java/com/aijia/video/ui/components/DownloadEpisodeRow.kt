package com.aijia.video.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aijia.video.data.model.DownloadRecord
import com.aijia.video.data.repository.DownloadStatus

@Composable
fun DownloadEpisodeRow(
    episodeName: String,
    isCurrentEpisode: Boolean,
    record: DownloadRecord?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = episodeName,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isCurrentEpisode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        record?.let {
            Text(
                text = when (it.status) {
                    DownloadStatus.PENDING -> "等待中"
                    DownloadStatus.DOWNLOADING -> "下载中"
                    DownloadStatus.PAUSED -> "已暂停"
                    DownloadStatus.COMPLETED -> "已完成"
                    DownloadStatus.FAILED -> "失败"
                    DownloadStatus.CANCELLED -> "已取消"
                    else -> "未知"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
