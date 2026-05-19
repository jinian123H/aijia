package com.aijia.video.data.model

import com.google.gson.annotations.SerializedName

data class AdConfig(
    @SerializedName("startup_ad")
    val startupAd: AdItem? = null,

    @SerializedName("player_ad")
    val playerAd: AdItem? = null,

    @SerializedName("player_bottom_ad")
    val playerBottomAd: AdItem? = null,

    @SerializedName("short_video_ads")
    val shortVideoAds: List<AdItem>? = null
)

data class AdItem(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("image_url")
    val imageUrl: String = "",

    @SerializedName("link_url")
    val linkUrl: String = "",

    @SerializedName("duration")
    val duration: String = "5",

    @SerializedName("enabled")
    val enabled: Boolean = false,

    @SerializedName("sort")
    val sort: Int = 0
) {
    val durationInt: Int
        get() = duration.toIntOrNull() ?: 5
}

/**
 * 按类型获取广告的响应（新接口）
 */
data class AdResponse(
    @SerializedName("show_ad")
    val showAd: Boolean = false,

    @SerializedName("data_changed")
    val dataChanged: Boolean = false,

    @SerializedName("version_id")
    val versionId: String = "",

    @SerializedName("content")
    val content: AdItem? = null
)
