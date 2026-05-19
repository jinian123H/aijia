package com.aijia.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aijia.video.data.model.Video
import com.aijia.video.data.model.PlayUrl
import com.aijia.video.data.model.DownloadRecord
import com.aijia.video.ui.screens.player.PlaySource

/**
 * 主题二简介组件 - 紧凑布局
 * 特点：无缩略图、名称右侧简介按钮、年份地区独立行、操作按钮内联
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroSlidePosterTheme2(
    video: Video?,
    isFavorite: Boolean,
    playSources: List<PlaySource>,
    selectedSourceIndex: Int,
    selectedEpisodeIndex: Int,
    downloadRecords: List<DownloadRecord>,
    onFavoriteClick: () -> Unit,
    onSourceSelected: (Int) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onDownloadClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onUrgeClick: () -> Unit,
    onEpisodeDownload: (Int, PlayUrl) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetailSheet by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val sheetHeight = screenHeight * 3f / 5f

    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        video?.let {
            // 第一行：视频名称 + 简介按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = it.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // 简介按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable { showDetailSheet = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "简介",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 第二行：年份 | 地区
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "年份: ${it.year ?: "未知"}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "|",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "地区: ${it.area ?: "未知"}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 第三行：操作按钮（收藏、下载、反馈、催更）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 收藏按钮
                ActionButtonCompact(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (isFavorite) "已收藏" else "收藏",
                    tint = if (isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onFavoriteClick,
                    modifier = Modifier.weight(1f)
                )

                // 下载按钮
                ActionButtonCompact(
                    icon = Icons.Default.Download,
                    label = "下载",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onDownloadClick,
                    modifier = Modifier.weight(1f)
                )

                // 反馈按钮
                ActionButtonCompact(
                    icon = Icons.Default.Feedback,
                    label = "反馈",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onFeedbackClick,
                    modifier = Modifier.weight(1f)
                )

                // 催更按钮
                ActionButtonCompact(
                    icon = Icons.Default.Notifications,
                    label = "催更",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onUrgeClick,
                    modifier = Modifier.weight(1f)
                )
            }

            // 播放源
            if (playSources.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "播放源",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " 播放失败请切换",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(playSources) { index, source ->
                        Box(
                            modifier = Modifier.padding(top = 8.dp, end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = if (index == selectedSourceIndex) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onSourceSelected(index) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = source.name,
                                    fontSize = 13.sp,
                                    color = if (index == selectedSourceIndex) MaterialTheme.colorScheme.primary else Color.Black
                                )
                            }
                            // 集数标记（固定在右上角）
                            if (source.urls.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .background(Color.Red, CircleShape)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = source.urls.size.toString(),
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 选集
            if (playSources.isNotEmpty() && selectedSourceIndex in playSources.indices) {
                val episodes = playSources[selectedSourceIndex].urls
                if (episodes.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "选集",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (episodes.size > 20) {
                            Text(
                                text = "全${episodes.size}集",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showEpisodeSheet = true }
                            )
                        } else {
                            Text(
                                text = "全${episodes.size}集",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (episodes.size > 20) {
                        // 多集时显示可点击的选集列表（限制显示数量）
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(episodes.take(20)) { index, episode ->
                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = if (index == selectedEpisodeIndex) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onEpisodeSelected(index) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = episode.name,
                                        fontSize = 13.sp,
                                        color = if (index == selectedEpisodeIndex) MaterialTheme.colorScheme.primary else Color.Black
                                    )
                                }
                            }
                            // 更多按钮
                            if (episodes.size > 20) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable { showEpisodeSheet = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "更多...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // 少于20集时显示全部
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(episodes) { index, episode ->
                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = if (index == selectedEpisodeIndex) MaterialTheme.colorScheme.primary else Color.Gray,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onEpisodeSelected(index) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = episode.name,
                                        fontSize = 13.sp,
                                        color = if (index == selectedEpisodeIndex) MaterialTheme.colorScheme.primary else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 简介详情弹窗（包含简介、演员）
    if (showDetailSheet && video != null) {
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 视频名称
                Text(
                    text = video.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 年份、地区、评分
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "年份: ${video.year ?: "未知"}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "地区: ${video.area ?: "未知"}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    video.score?.let { score ->
                        Text(
                            text = "评分: $score",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 演员
                Text(
                    text = "演员",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = video.actor ?: "暂无演员信息",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 导演
                video.director?.takeIf { it.isNotBlank() }?.let { director ->
                    Text(
                        text = "导演",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = director,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // 简介
                Text(
                    text = "简介",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = cleanIntroContent(video.content ?: "暂无简介"),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }

    // 选集弹窗
    if (showEpisodeSheet && playSources.isNotEmpty() && selectedSourceIndex in playSources.indices) {
        val episodes = playSources[selectedSourceIndex].urls
        ModalBottomSheet(
            onDismissRequest = { showEpisodeSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight)
                    .padding(16.dp)
            ) {
                Text(
                    text = "选集 (全${episodes.size}集)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(episodes) { index, episode ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (index == selectedEpisodeIndex) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable {
                                    onEpisodeSelected(index)
                                    showEpisodeSheet = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = episode.name,
                                fontSize = 12.sp,
                                color = if (index == selectedEpisodeIndex) MaterialTheme.colorScheme.primary else Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 紧凑型操作按钮组件
 */
@Composable
private fun ActionButtonCompact(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = tint
        )
    }
}

// 清理简介内容中的HTML标签
private fun cleanIntroContent(content: String): String {
    return content
        .replace("<p>", "")
        .replace("</p>", "\n")
        .replace(Regex("<[^>]*>"), "")
        .trim()
}