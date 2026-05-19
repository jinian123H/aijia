package com.aijia.video.data.repository

import android.content.Context
import com.aijia.video.data.model.DownloadFileType
import com.aijia.video.data.model.DownloadRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.io.File
import java.net.URLConnection
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadPrefs by lazy {
        context.getSharedPreferences("video_download_records", Context.MODE_PRIVATE)
    }
    private val activeJobs = linkedMapOf<String, kotlinx.coroutines.Job>()
    private val pendingActions = mutableMapOf<String, DownloadStatus>()

    private val _downloads = MutableStateFlow(loadPersistedDownloads())
    val downloads: Flow<List<DownloadRecord>> = _downloads.asStateFlow()

    val downloadStatus: Flow<Map<String, DownloadStatus>> = downloads.map { list ->
        list.associate { it.id to it.status }
    }

    val downloadProgress: Flow<Map<String, Int>> = downloads.map { list ->
        list.associate { it.id to it.progress }
    }

    fun startDownload(
        videoId: String,
        videoTitle: String,
        episodeName: String?,
        videoUrl: String,
        coverUrl: String? = null
    ): String {
        val normalizedUrl = videoUrl.trim()
        require(normalizedUrl.isNotBlank()) { "videoUrl is blank" }

        val downloadId = buildDownloadId(videoId, normalizedUrl)
        val fileType = if (normalizedUrl.contains(".m3u8", ignoreCase = true)) {
            DownloadFileType.M3U8
        } else {
            DownloadFileType.MP4
        }
        val localPath = when (fileType) {
            DownloadFileType.MP4 -> File(downloadRootDir(), "$downloadId.mp4").absolutePath
            DownloadFileType.M3U8 -> File(downloadRootDir(), downloadId).resolve("local.m3u8").absolutePath
        }

        val existing = _downloads.value.firstOrNull { it.id == downloadId }
        if (existing?.status == DownloadStatus.DOWNLOADING) return downloadId

        val nextRecord = (existing ?: DownloadRecord(
            id = downloadId,
            videoId = videoId,
            videoTitle = videoTitle,
            episodeName = episodeName,
            coverUrl = coverUrl,
            remoteUrl = normalizedUrl,
            localPath = localPath,
            fileType = fileType
        )).copy(
            videoTitle = videoTitle,
            episodeName = episodeName,
            coverUrl = coverUrl,
            remoteUrl = normalizedUrl,
            localPath = localPath,
            fileType = fileType,
            status = DownloadStatus.PENDING,
            updatedAt = System.currentTimeMillis()
        )

        upsertRecord(nextRecord)
        launchDownload(nextRecord)
        return downloadId
    }

    fun pauseDownload(downloadId: String) {
        pendingActions[downloadId] = DownloadStatus.PAUSED
        activeJobs.remove(downloadId)?.cancel()
    }

    fun resumeDownload(downloadId: String) {
        val record = _downloads.value.firstOrNull { it.id == downloadId } ?: return
        if (record.status == DownloadStatus.DOWNLOADING) return
        val resetRecord = record.copy(
            status = DownloadStatus.PENDING,
            progress = 0,
            downloadedBytes = 0L,
            totalBytes = 0L,
            updatedAt = System.currentTimeMillis()
        )
        clearDownloadOutput(resetRecord)
        upsertRecord(resetRecord)
        launchDownload(resetRecord)
    }

    fun clearAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        pendingActions.clear()
        _downloads.value.forEach { clearDownloadOutput(it) }
        _downloads.value = emptyList()
        downloadPrefs.edit().clear().apply()
    }

    fun cancelDownload(downloadId: String) {
        pendingActions[downloadId] = DownloadStatus.CANCELLED
        activeJobs.remove(downloadId)?.cancel()
        val record = _downloads.value.firstOrNull { it.id == downloadId } ?: return
        clearDownloadOutput(record)
        removeRecord(downloadId)
    }

    fun clearCompleted() {
        _downloads.value.filter { it.status == DownloadStatus.COMPLETED }
            .forEach { record ->
                clearDownloadOutput(record)
                removeRecord(record.id)
            }
    }

    fun getDownload(downloadId: String): DownloadRecord? {
        return _downloads.value.firstOrNull { it.id == downloadId }
    }

    fun getDownloads(): List<DownloadRecord> {
        return _downloads.value.sortedByDescending { it.updatedAt }
    }

    private fun launchDownload(record: DownloadRecord) {
        activeJobs.remove(record.id)?.cancel()
        activeJobs[record.id] = appScope.launch {
            try {
                updateRecord(record.id) {
                    it.copy(status = DownloadStatus.DOWNLOADING, updatedAt = System.currentTimeMillis())
                }
                when (record.fileType) {
                    DownloadFileType.MP4 -> downloadMp4(record.id)
                    DownloadFileType.M3U8 -> downloadM3u8(record.id)
                }
            } catch (_: CancellationException) {
                val finalStatus = pendingActions.remove(record.id) ?: DownloadStatus.PAUSED
                if (finalStatus == DownloadStatus.PAUSED) {
                    updateRecord(record.id) {
                        it.copy(status = DownloadStatus.PAUSED, updatedAt = System.currentTimeMillis())
                    }
                }
            } catch (_: Exception) {
                updateRecord(record.id) {
                    it.copy(status = DownloadStatus.FAILED, updatedAt = System.currentTimeMillis())
                }
            } finally {
                activeJobs.remove(record.id)
                pendingActions.remove(record.id)
            }
        }
    }

    private suspend fun downloadMp4(downloadId: String) = withContext(Dispatchers.IO) {
        val record = requireRecord(downloadId)
        val outputFile = File(record.localPath)
        outputFile.parentFile?.mkdirs()

        val request = Request.Builder().url(record.remoteUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            val totalBytes = body.contentLength().coerceAtLeast(0L)
            outputFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    var downloadedBytes = 0L
                    while (input.read(buffer).also { read = it } >= 0) {
                        ensureActive()
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val progress = if (totalBytes > 0L) {
                            ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        updateRecord(downloadId) {
                            it.copy(
                                progress = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
        }

        updateRecord(downloadId) {
            it.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100,
                downloadedBytes = outputFile.length(),
                totalBytes = outputFile.length(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private suspend fun downloadM3u8(downloadId: String) = withContext(Dispatchers.IO) {
        val record = requireRecord(downloadId)
        val rootDir = File(downloadRootDir(), downloadId)
        if (rootDir.exists()) {
            rootDir.deleteRecursively()
        }
        rootDir.mkdirs()

        val mediaPlaylistUrl = resolveMediaPlaylistUrl(record.remoteUrl)
        val playlistText = httpGetText(mediaPlaylistUrl)
        val localPlaylist = buildLocalPlaylist(mediaPlaylistUrl, playlistText, rootDir, downloadId)
        File(record.localPath).writeText(localPlaylist)

        updateRecord(downloadId) {
            val totalSize = folderSize(rootDir)
            it.copy(
                status = DownloadStatus.COMPLETED,
                progress = 100,
                downloadedBytes = totalSize,
                totalBytes = totalSize,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private suspend fun resolveMediaPlaylistUrl(initialUrl: String): String {
        var currentUrl = initialUrl
        repeat(4) {
            val text = httpGetText(currentUrl)
            val variantUrl = parseMasterPlaylistVariant(currentUrl, text) ?: return currentUrl
            currentUrl = variantUrl
        }
        return currentUrl
    }

    private suspend fun buildLocalPlaylist(
        playlistUrl: String,
        playlistText: String,
        rootDir: File,
        downloadId: String
    ): String {
        val lines = playlistText.lines()
        val segmentLines = lines.mapIndexedNotNull { index, line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) index to trimmed else null
        }

        val keyMap = mutableMapOf<String, String>()
        val mapMap = mutableMapOf<String, String>()
        val rewrittenLines = mutableListOf<String>()
        var segmentIndex = 0

        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#EXT-X-KEY", ignoreCase = true) -> {
                    val rawKeyUri = extractQuotedAttribute(trimmed, "URI")
                    val remoteKey = rawKeyUri?.let { resolveUrl(playlistUrl, it) }
                    if (remoteKey != null) {
                        val localName = keyMap.getOrPut(remoteKey) {
                            val fileName = "key_${keyMap.size}.key"
                            val target = rootDir.resolve(fileName)
                            downloadBinaryFile(remoteKey, target)
                            fileName
                        }
                        rewrittenLines += line.replace(rawKeyUri, localName)
                    } else {
                        rewrittenLines += line
                    }
                }

                trimmed.startsWith("#EXT-X-MAP", ignoreCase = true) -> {
                    val rawMapUri = extractQuotedAttribute(trimmed, "URI")
                    val remoteMap = rawMapUri?.let { resolveUrl(playlistUrl, it) }
                    if (remoteMap != null) {
                        val localName = mapMap.getOrPut(remoteMap) {
                            val suffix = inferSuffix(remoteMap, ".mp4")
                            val fileName = "init_${mapMap.size}$suffix"
                            val target = rootDir.resolve(fileName)
                            downloadBinaryFile(remoteMap, target)
                            fileName
                        }
                        rewrittenLines += line.replace(rawMapUri, localName)
                    } else {
                        rewrittenLines += line
                    }
                }

                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    val remoteSegment = resolveUrl(playlistUrl, trimmed)
                    val suffix = inferSuffix(remoteSegment, ".ts")
                    val localName = String.format("seg_%04d%s", segmentIndex, suffix)
                    downloadBinaryFile(remoteSegment, rootDir.resolve(localName))
                    segmentIndex += 1
                    val progress = if (segmentLines.isNotEmpty()) {
                        ((segmentIndex * 100f) / segmentLines.size).toInt().coerceIn(1, 99)
                    } else {
                        0
                    }
                    updateRecord(downloadId) {
                        it.copy(
                            progress = progress,
                            downloadedBytes = folderSize(rootDir),
                            totalBytes = 0L,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    rewrittenLines += localName
                }

                else -> rewrittenLines += line
            }
        }

        return rewrittenLines.joinToString(separator = "\n")
    }

    private fun parseMasterPlaylistVariant(playlistUrl: String, playlistText: String): String? {
        val lines = playlistText.lines()
        val variants = mutableListOf<Pair<Int, String>>()
        for (index in lines.indices) {
            val line = lines[index].trim()
            if (line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
                val bandwidth = extractNumericAttribute(line, "BANDWIDTH") ?: 0
                val nextUri = lines.drop(index + 1)
                    .firstOrNull { next -> next.trim().isNotEmpty() && !next.trim().startsWith("#") }
                    ?.trim()
                if (nextUri != null) {
                    variants += bandwidth to resolveUrl(playlistUrl, nextUri)
                }
            }
        }
        return variants.maxByOrNull { it.first }?.second
    }

    private fun downloadBinaryFile(remoteUrl: String, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        val request = Request.Builder().url(remoteUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty body")
            outputFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } >= 0) {
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    private fun httpGetText(url: String): String {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("Empty body")
        }
    }

    private fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
        val httpUrl = baseUrl.toHttpUrlOrNull() ?: return relativeOrAbsolute
        return httpUrl.resolve(relativeOrAbsolute)?.toString() ?: relativeOrAbsolute
    }

    private fun extractQuotedAttribute(line: String, key: String): String? {
        val regex = Regex("$key=\"([^\"]+)\"")
        return regex.find(line)?.groupValues?.getOrNull(1)
    }

    private fun extractNumericAttribute(line: String, key: String): Int? {
        val regex = Regex("$key=(\\d+)")
        return regex.find(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun inferSuffix(url: String, fallback: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val extension = path.substringAfterLast('.', "")
        return if (extension.isNotBlank() && extension.length <= 5) {
            ".${extension.lowercase()}"
        } else {
            fallback
        }
    }

    private fun clearDownloadOutput(record: DownloadRecord) {
        val target = File(record.localPath)
        when (record.fileType) {
            DownloadFileType.MP4 -> if (target.exists()) target.delete()
            DownloadFileType.M3U8 -> target.parentFile?.takeIf { it.exists() }?.deleteRecursively()
        }
    }

    private fun requireRecord(downloadId: String): DownloadRecord {
        return _downloads.value.firstOrNull { it.id == downloadId }
            ?: error("Missing download record: $downloadId")
    }

    private fun upsertRecord(record: DownloadRecord) {
        val mutable = _downloads.value.toMutableList()
        val index = mutable.indexOfFirst { it.id == record.id }
        if (index >= 0) {
            mutable[index] = record
        } else {
            mutable += record
        }
        _downloads.value = mutable.sortedByDescending { it.updatedAt }
        persistDownloads()
    }

    private fun updateRecord(downloadId: String, transform: (DownloadRecord) -> DownloadRecord) {
        val mutable = _downloads.value.toMutableList()
        val index = mutable.indexOfFirst { it.id == downloadId }
        if (index < 0) return
        mutable[index] = transform(mutable[index])
        _downloads.value = mutable.sortedByDescending { it.updatedAt }
        persistDownloads()
    }

    private fun removeRecord(downloadId: String) {
        _downloads.value = _downloads.value.filterNot { it.id == downloadId }
        persistDownloads()
    }

    private fun persistDownloads() {
        downloadPrefs.edit()
            .putString(DOWNLOAD_RECORDS_KEY, gson.toJson(_downloads.value))
            .apply()
    }

    private fun loadPersistedDownloads(): List<DownloadRecord> {
        val json = downloadPrefs.getString(DOWNLOAD_RECORDS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<DownloadRecord>>() {}.type
        val records = runCatching { gson.fromJson<List<DownloadRecord>>(json, type).orEmpty() }
            .getOrDefault(emptyList())
        return records.mapNotNull { record ->
            val fileExists = File(record.localPath).exists()
            when {
                record.status == DownloadStatus.COMPLETED && !fileExists -> null
                record.status == DownloadStatus.DOWNLOADING -> record.copy(status = DownloadStatus.PAUSED)
                else -> record
            }
        }.sortedByDescending { it.updatedAt }
    }

    private fun buildDownloadId(videoId: String, url: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(url.toByteArray())
        val suffix = digest.joinToString(separator = "") { "%02x".format(it) }.take(10)
        return "${videoId}_$suffix"
    }

    private fun downloadRootDir(): File {
        return File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    companion object {
        private const val DOWNLOAD_RECORDS_KEY = "download_records"
    }
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
