package com.aijia.video.data.model

import com.google.gson.annotations.SerializedName

/**
 * 版本检查请求
 */
data class CheckVersionRequest(
    @SerializedName("version_code")
    val versionCode: Int,
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("update_version_id")
    val updateVersionId: String = ""
)

/**
 * 版本检查响应（data 字段）
 */
data class CheckVersionData(
    @SerializedName("update_version_id")
    val updateVersionId: String = "",
    @SerializedName("need_update")
    val needUpdate: Boolean = false,
    @SerializedName("force_update")
    val forceUpdate: VersionUpdateInfo? = null
)

/**
 * 单个更新详情
 */
data class VersionUpdateInfo(
    @SerializedName("version_code")
    val versionCode: Int,
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("download_url")
    val downloadUrl: String,
    @SerializedName("release_notes")
    val releaseNotes: String,
    @SerializedName("update_version_id")
    val updateVersionId: String
)

/**
 * 强制更新检查请求
 */
data class CheckForceUpdateRequest(
    @SerializedName("version_code")
    val versionCode: Int,
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("update_version_id")
    val updateVersionId: String = "",
    @SerializedName("platform")
    val platform: String = "android"
)

/**
 * 强制更新内容
 */
data class ForceUpdateContent(
    @SerializedName("version_code")
    val versionCode: Int,
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("download_url")
    val downloadUrl: String,
    @SerializedName("release_notes")
    val releaseNotes: String,
    @SerializedName("min_supported_code")
    val minSupportedCode: Int
)

/**
 * 强制更新检查响应
 */
data class CheckForceUpdateResponse(
    @SerializedName("need_update")
    val needUpdate: Boolean,
    @SerializedName("force_update")
    val forceUpdate: Boolean,
    @SerializedName("update_type")
    val updateType: String? = null,
    @SerializedName("update_version_id")
    val updateVersionId: String,
    @SerializedName("content")
    val content: ForceUpdateContent? = null
)
