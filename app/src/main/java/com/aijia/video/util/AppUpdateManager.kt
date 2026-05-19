package com.aijia.video.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * 下载状态
 */
data class DownloadState(
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val error: String? = null
)

/**
 * 应用更新管理器
 */
class AppUpdateManager(private val context: Context) {

    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null
    private var progressJob: Job? = null
    private var isInstalling = false
    private var lastDownloadUrl: String = ""
    private var lastVersionName: String = ""

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /**
     * 下载并安装APK
     */
    fun downloadAndInstall(downloadUrl: String, versionName: String) {
        lastDownloadUrl = downloadUrl
        lastVersionName = versionName
        val fileName = "aijia_v${versionName}.apk"

        // 删除旧文件
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("爱家视频更新")
            setDescription("正在下载 v$versionName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        _downloadState.value = DownloadState(isDownloading = true)

        // 监听下载完成
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId && !isInstalling) {
                    isInstalling = true
                    stopProgressMonitoring()
                    _downloadState.value = DownloadState(isDownloading = false, progress = 100)
                    installApk(context, fileName)
                    unregisterReceiver()
                }
            }
        }

        context.registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )

        // 开始监控下载进度
        startProgressMonitoring(downloadManager, fileName)
    }

    /**
     * 开始监控下载进度
     * @param fileName 安装文件名（用于下载完成后安装）
     */
    private fun startProgressMonitoring(downloadManager: DownloadManager, fileName: String = "") {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.IO).launch {
            var stalledCycles = 0
            var lastProgress = -1
            var lastBytesDownloaded = -1L

            while (isActive && downloadId != -1L) {
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor: Cursor? = downloadManager.query(query)

                    cursor?.use {
                        if (it.moveToFirst()) {
                            val bytesDownloaded = it.getLong(
                                it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            )
                            val bytesTotal = it.getLong(
                                it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            )
                            val status = it.getInt(
                                it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                            )

                            when (status) {
                                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                    val progress = if (bytesTotal > 0) {
                                        ((bytesDownloaded * 100) / bytesTotal).toInt()
                                    } else {
                                        0
                                    }

                                    _downloadState.value = DownloadState(
                                        isDownloading = true,
                                        progress = progress,
                                        downloadedBytes = bytesDownloaded,
                                        totalBytes = bytesTotal
                                    )

                                    // 卡死检测：已下载字节数连续30秒无变化 → 超时报错
                                    if (bytesDownloaded == lastBytesDownloaded) {
                                        stalledCycles++
                                    } else {
                                        stalledCycles = 0
                                        lastBytesDownloaded = bytesDownloaded
                                    }
                                    if (stalledCycles >= 60) { // 30秒 = 60次×500ms
                                        _downloadState.value = DownloadState(
                                            isDownloading = false,
                                            error = "下载超时，请检查网络后重试"
                                        )
                                        stopProgressMonitoring()
                                        return@launch
                                    }
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    // 下载完成（兜底：防止BroadcastReceiver未触发）
                                    if (isInstalling) return@launch
                                    isInstalling = true
                                    _downloadState.value = DownloadState(
                                        isDownloading = false,
                                        progress = 100,
                                        downloadedBytes = bytesDownloaded,
                                        totalBytes = bytesTotal
                                    )
                                    stopProgressMonitoring()
                                    unregisterReceiver()
                                    installApk(context, fileName)
                                    return@launch
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    val reason = it.getInt(
                                        it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                                    )
                                    _downloadState.value = DownloadState(
                                        isDownloading = false,
                                        error = "下载失败，错误码：$reason"
                                    )
                                    stopProgressMonitoring()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    _downloadState.value = DownloadState(
                        isDownloading = false,
                        error = "下载异常：${e.message}"
                    )
                    stopProgressMonitoring()
                }

                delay(500) // 每500ms更新一次进度
            }
        }
    }

    /**
     * 停止监控下载进度
     */
    private fun stopProgressMonitoring() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * 安装APK
     * 优先从DownloadManager查询实际文件路径，兜底使用默认Download目录
     */
    private fun installApk(context: Context, fileName: String) {
        val file = resolveDownloadedFile(context, fileName)
        if (file == null || !file.exists()) {
            android.util.Log.e("AppUpdateManager", "APK文件不存在: $fileName")
            _downloadState.value = _downloadState.value.copy(
                error = "安装包未找到，请重新下载"
            )
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                setDataAndType(uri, "application/vnd.android.package-archive")
            } else {
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }
        }

        context.startActivity(intent)
    }

    /**
     * 从DownloadManager查询实际下载文件路径
     */
    private fun resolveDownloadedFile(context: Context, fileName: String): File? {
        // 先通过DownloadManager查询实际路径
        if (downloadId != -1L) {
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                cursor?.use {
                    if (it.moveToFirst()) {
                        // API 29+ 用 COLUMN_LOCAL_URI
                        if (Build.VERSION.SDK_INT >= 29) {
                            val colIdx = it.getColumnIndex("local_uri")
                            if (colIdx >= 0) {
                                val uriStr = it.getString(colIdx)
                                if (uriStr != null) {
                                    val path = Uri.parse(uriStr).path
                                    if (path != null) {
                                        val f = File(path)
                                        if (f.exists()) return f
                                    }
                                }
                            }
                        }
                        // API < 29 用 local_filename
                        val colIdxLegacy = it.getColumnIndex("local_filename")
                        if (colIdxLegacy >= 0) {
                            val path = it.getString(colIdxLegacy)
                            if (path != null) {
                                val f = File(path)
                                if (f.exists()) return f
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("AppUpdateManager", "查询下载路径失败", e)
            }
        }

        // 兜底：默认Download目录
        val fallback = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        if (fallback.exists()) return fallback

        return null
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
            downloadId = -1
        }
        isInstalling = false
        stopProgressMonitoring()
        _downloadState.value = DownloadState()
        unregisterReceiver()
    }

    /**
     * 重置下载状态
     */
    fun resetDownloadState() {
        isInstalling = false
        downloadId = -1
        stopProgressMonitoring()
        try { unregisterReceiver() } catch (_: Exception) {}
        _downloadState.value = DownloadState()
    }

    /**
     * 重新下载（超时/失败后重试）
     */
    fun retryDownload() {
        cancelDownload()
        if (lastDownloadUrl.isNotBlank() && lastVersionName.isNotBlank()) {
            downloadAndInstall(lastDownloadUrl, lastVersionName)
        }
    }

    private fun unregisterReceiver() {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // Receiver already unregistered
            }
            downloadReceiver = null
        }
    }
}
