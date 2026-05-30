package com.aijia.video.ui.components

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 启动页视频播放器组件
 * 使用 Android MediaPlayer 替代 ExoPlayer，避免启动页加载 ExoPlayer 解码栈
 *
 * 释放顺序：surface=null → surface.release → stop → reset → release
 */
@Composable
fun SplashVideoPlayer(
    videoResId: Int,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mediaPlayer = remember {
        MediaPlayer().apply {
            setDataSource(context, Uri.parse("android.resource://${context.packageName}/${videoResId}"))
            setOnCompletionListener {
                onComplete()
            }
            setOnPreparedListener {
                start()
            }
            prepareAsync()
        }
    }

    var currentSurface = remember<Surface?> { null }

    DisposableEffect(Unit) {
        onDispose {
            // 先断开 surface，再释放 MediaPlayer，避免 BufferQueueProducer 报错
            try { mediaPlayer.setSurface(null) } catch (_: Exception) { }
            currentSurface?.release()
            currentSurface = null
            try { mediaPlayer.stop() } catch (_: Exception) { }
            try { mediaPlayer.reset() } catch (_: Exception) { }
            try { mediaPlayer.release() } catch (_: Exception) { }
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        val surface = Surface(st)
                        currentSurface = surface
                        mediaPlayer.setSurface(surface)
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        // TextureView 销毁时断开 surface 引用，但不手动 release SurfaceTexture（return true 交给系统）
                        try { mediaPlayer.setSurface(null) } catch (_: Exception) { }
                        currentSurface?.release()
                        currentSurface = null
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}
