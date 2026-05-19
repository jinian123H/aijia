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
 * 释放顺序：pause → stop → reset → release → surface=null
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

    val surfaceHolder = remember { mutableMapOf<String, Surface?>() }

    DisposableEffect(Unit) {
        onDispose {
            // 标准释放流程
            try { mediaPlayer.stop() } catch (_: Exception) { }
            try { mediaPlayer.reset() } catch (_: Exception) { }
            try { mediaPlayer.release() } catch (_: Exception) { }
            surfaceHolder["surface"]?.release()
            surfaceHolder.clear()
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        val surface = Surface(st)
                        surfaceHolder["surface"] = surface
                        mediaPlayer.setSurface(surface)
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        mediaPlayer.setSurface(null)
                        surfaceHolder["surface"]?.release()
                        surfaceHolder.clear()
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
}
