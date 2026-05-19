package com.aijia.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun VideoCoverImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    requestWidth: Dp? = null,
    requestHeight: Dp? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val isInPreview = LocalInspectionMode.current
    val requestWidthPx = remember(requestWidth, density) { requestWidth?.let { with(density) { it.roundToPx() } } }
    val requestHeightPx = remember(requestHeight, density) { requestHeight?.let { with(density) { it.roundToPx() } } }

    // 如果 model 为 null，显示占位图
    if (model == null) {
        Box(
            modifier = modifier.background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = contentDescription,
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
        }
        return
    }

    // 使用稳定的 key 确保 recompose 时不会重新加载
    val imageKey = remember(model) { model.toString() }

    val request = remember(imageKey, requestWidthPx, requestHeightPx, isInPreview) {
        ImageRequest.Builder(context)
            .data(imageKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)  // 关闭淡入动画，减少渲染开销
            .apply {
                if (requestWidthPx != null && requestHeightPx != null) {
                    size(requestWidthPx, requestHeightPx)
                }
            }
            .build()
    }

    Box(
        modifier = modifier.background(Color.Gray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            loading = {
                AnimatedGifPlaceholder(
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "加载失败",
                    tint = Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
            }
        )
    }
}

@Composable
private fun AnimatedGifPlaceholder(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(context) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/loading_placeholder.gif")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}