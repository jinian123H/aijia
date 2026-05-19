package com.aijia.video.data.model

/**
 * 首页接口响应
 */
data class HomeIndexResponse(
    val banners: List<Video> = emptyList(),
    val sections: List<HomeSection> = emptyList()
)

/**
 * 首页推荐分组
 */
data class HomeSection(
    val id: Int = 0,
    val title: String = "首页推荐",
    val display_count: Int = 0,
    val mode: Int = 0,
    val target_id: Int = 0,
    val videos: List<Video> = emptyList()
)

data class RankIndexResponse(
    val sections: List<RankSection> = emptyList()
)

data class RankSection(
    val id: Int = 0,
    val title: String = "排行榜",
    val type_id: Int = 0,
    val display_count: Int = 0,
    val rank_field: String = "vod_hits",
    val videos: List<Video> = emptyList()
)
