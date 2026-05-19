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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aijia.video.data.model.Video
import com.aijia.video.data.model.PlayUrl
import com.aijia.video.data.model.DownloadRecord
import com.aijia.video.ui.screens.player.PlaySource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroSlidePoster(
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
    var showFullIntro by remember { mutableStateOf(false) }
    var showFullActor by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var videoNameHeight by remember { mutableStateOf(0.dp) }
    val commentButtonHeight = 48.dp // 评论按钮高度
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val episodeSheetHeight = screenHeight * 3f / 5f // 屏幕五分之三高度

    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        video?.let {
            // 视频名称
            Text(
                text = it.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    videoNameHeight = with(density) {
                        coordinates.size.height.toDp()
                    }
                }
            )

            // 视频图片和信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 视频图片（9:16竖向）
                AsyncImage(
                    model = it.pic,
                    contentDescription = it.name,
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                // 视频信息和简介
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp * 16f / 9f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "年份：${it.year ?: "未知"}",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "地区：${it.area ?: "未知"}",
                        fontSize = 14.sp
                    )

                    // 演员（最多1行，超出显示更多按钮）
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "演员：${it.actor ?: "未知"}",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if ((it.actor?.length ?: 0) > 15) {
                            Text(
                                text = "更多",
                                fontSize = 13.sp,
                                color = Color(0xFF2196F3),
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clickable { showFullActor = true }
                            )
                        }
                    }

                    // 简介（缩进显示，最多3行）
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "简介：",
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = cleanIntroContent(it.content ?: "暂无简介"),
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "更多",
                            fontSize = 13.sp,
                            color = Color(0xFF2196F3),
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable { showFullIntro = true }
                        )
                    }
                }
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
                                        color = if (index == selectedSourceIndex) Color(0xFF2196F3) else Color.Gray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onSourceSelected(index) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = source.name,
                                    fontSize = 13.sp,
                                    color = if (index == selectedSourceIndex) Color(0xFF2196F3) else Color.Black
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
                        if (episodes.size > 1) {
                            Text(
                                text = "全${episodes.size}集",
                                fontSize = 13.sp,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.clickable { showEpisodeSheet = true }
                            )
                        } else {
                            Text(
                                text = "全${episodes.size}集",
                                fontSize = 13.sp,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }

                    if (episodes.size > 1) {
                        // 多集时显示可点击的选集列表
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(episodes) { index, episode ->
                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = if (index == selectedEpisodeIndex) Color(0xFF2196F3) else Color.Gray,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onEpisodeSelected(index) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = episode.name,
                                        fontSize = 13.sp,
                                        color = if (index == selectedEpisodeIndex) Color(0xFF2196F3) else Color.Black
                                    )
                                }
                            }
                        }
                    } else {
                        // 单集时直接显示集名
                        Text(
                            text = episodes.firstOrNull()?.name ?: "第1集",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // 简介弹窗
    if (showFullIntro && video != null) {
        ModalBottomSheet(
            onDismissRequest = { showFullIntro = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(episodeSheetHeight)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "简介",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = cleanIntroContent(video.content ?: "暂无简介"),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }

    // 演员弹窗
    if (showFullActor && video != null) {
        ModalBottomSheet(
            onDismissRequest = { showFullActor = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(episodeSheetHeight)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "演员",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = video.actor ?: "暂无演员信息",
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
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(episodeSheetHeight)
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
                                    color = if (index == selectedEpisodeIndex) Color(0xFF2196F3) else Color.Gray,
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
                                color = if (index == selectedEpisodeIndex) Color(0xFF2196F3) else Color.Black,
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

// 清理简介内容中的HTML标签
private fun cleanIntroContent(content: String): String {
    return content
        .replace("<p>", "")
        .replace("</p>", "\n")
        .replace(Regex("<[^>]*>"), "")
        .trim()
}
