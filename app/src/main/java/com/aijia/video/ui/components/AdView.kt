package com.aijia.video.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@Composable
fun AdView(
    imageUrl: String,
    linkUrl: String,
    duration: Int,
    onAdClick: (String) -> Unit,
    onAdSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var remainingTime by remember { mutableIntStateOf(duration) }

    // 调试日志
    LaunchedEffect(imageUrl) {
        Log.d("AdView", "Loading ad image: $imageUrl")
    }

    // 倒计时逻辑：duration=0 表示不自动跳过
    LaunchedEffect(Unit) {
        if (duration > 0) {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
            onAdSkip()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (linkUrl.isNotBlank()) {
                    onAdClick(linkUrl)
                }
            }
    ) {
        // 广告图片
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .listener(
                    onError = { request, error ->
                        Log.e("AdView", "Ad image load error: ${error.throwable?.message}")
                    },
                    onSuccess = { request, result ->
                        Log.d("AdView", "Ad image loaded successfully")
                    }
                )
                .build(),
            contentDescription = "广告",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onError = {
                Log.e("AdView", "AsyncImage error: ${it.result.throwable?.message}")
            }
        )

        // 加载失败时显示占位
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 这里可以添加加载状态判断
        }

        // 跳过按钮：duration>0 显示倒计时，duration=0 直接显示跳过
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(20.dp))
                .clickable { onAdSkip() }
        ) {
            Text(
                text = if (duration > 0 && remainingTime > 0) "跳过 $remainingTime" else "跳过",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}