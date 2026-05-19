package com.aijia.video.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.aijia.video.data.model.Danmu
import android.graphics.Color.parseColor

/**
 * 弹幕视图组件
 * 根据播放时间显示对应的弹幕，只在视频区域内显示
 */
@Composable
fun DanmuView(
    danmuList: List<Danmu>,
    currentTime: Long,  // 当前播放时间（毫秒）
    videoKey: String,
    modifier: Modifier = Modifier
) {
    val timeWindow = 10f

    val currentDanmuList = remember(danmuList, currentTime) {
        danmuList.filter { danmu ->
            val danmuTimeSeconds = danmu.time / 1000f
            val currentTimeSeconds = currentTime / 1000f
            danmuTimeSeconds in (currentTimeSeconds - 0.5f)..(currentTimeSeconds + timeWindow)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val containerHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val trackCount = 5

        currentDanmuList.forEachIndexed { index, danmu ->
            key(danmu.id) {
                val trackIndex = index % trackCount
                DanmuItem(
                    danmu = danmu,
                    trackIndex = trackIndex,
                    trackCount = trackCount,
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx
                )
            }
        }
    }
}

/**
 * 单条弹幕组件
 */
@Composable
fun DanmuItem(
    danmu: Danmu,
    trackIndex: Int,
    trackCount: Int,
    containerWidthPx: Float,
    containerHeightPx: Float
) {
    val density = LocalDensity.current

    val danmuColor = remember(danmu.color) {
        try {
            Color(parseColor(danmu.color))
        } catch (e: Exception) {
            Color.White
        }
    }

    val fontSize = when (danmu.fontSize) {
        in 1..18 -> 14.sp
        in 19..22 -> 16.sp
        in 23..26 -> 18.sp
        in 27..30 -> 20.sp
        else -> 16.sp
    }

    val duration = when (danmu.fontSize) {
        in 1..18 -> 8000
        in 19..22 -> 9000
        in 23..26 -> 10000
        else -> 11000
    }

    // X 动画：从容器右边界外移动到左边界外
    val offsetX = remember { Animatable(containerWidthPx) }

    // Y 轴：按轨道等分容器高度，只用上半部分显示弹幕
    val trackHeight = (containerHeightPx * 0.6f) / trackCount
    val yOffsetPx = trackIndex * trackHeight + trackHeight * 0.1f
    val yOffsetDp = with(density) { yOffsetPx.toDp() }

    LaunchedEffect(danmu.id) {
        offsetX.animateTo(
            targetValue = -containerWidthPx,
            animationSpec = tween(duration, easing = LinearEasing)
        )
    }

    val xOffsetDp = with(density) { offsetX.value.toDp() }

    Box(
        modifier = Modifier
            .offset(x = xOffsetDp, y = yOffsetDp)
    ) {
        Text(
            text = danmu.content,
            color = danmuColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
        )
    }
}
