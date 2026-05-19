package com.aijia.video.data.model

/**
 * 推荐视频响应数据
 */
data class RecommendResponse(
    val title: String = "推荐视频",
    val type_id: Int = 0,
    val type_name: String = "",
    val sort_by: String = "vod_hits",
    val sort_order: String = "DESC",
    val random: Int = 0,
    val videos: List<Video> = emptyList()
)