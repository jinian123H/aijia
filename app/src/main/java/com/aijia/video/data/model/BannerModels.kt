package com.aijia.video.data.model

import com.google.gson.annotations.SerializedName

/**
 * Banner 视频项（每日缓存模式）
 */
data class BannerItem(
    val id: Int = 0,
    val title: String = "",
    val cover: String = "",
    @SerializedName("vod_id")
    val vodId: Int = 0
)

/**
 * Banner 版本化响应（每日缓存模式）
 */
data class BannerVersionedData(
    @SerializedName("version_id") val versionId: String = "",
    @SerializedName("data_changed") val dataChanged: Boolean = true,
    @SerializedName("content") val content: List<BannerItem>? = null
)
