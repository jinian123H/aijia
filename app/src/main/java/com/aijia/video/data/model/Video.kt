package com.aijia.video.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * 视频数据模型
 */
@Entity(tableName = "videos")
data class Video(
    @PrimaryKey
    @SerializedName("vod_id")
    val id: String,
    
    @SerializedName("vod_name")
    val name: String = "",
    
    @SerializedName("vod_sub")
    val subTitle: String? = null,
    
    @SerializedName("vod_pic")
    val pic: String? = null,
    
    @SerializedName("vod_pic_thumb")
    val picThumb: String? = null,
    
    @SerializedName("vod_pic_slide")
    val picSlide: String? = null,
    
    @SerializedName("vod_actor")
    val actor: String? = null,
    
    @SerializedName("vod_director")
    val director: String? = null,
    
    @SerializedName("vod_content")
    val content: String? = null,
    
    @SerializedName("vod_year")
    val year: String? = null,
    
    @SerializedName("vod_area")
    val area: String? = null,
    
    @SerializedName("vod_lang")
    val lang: String? = null,
    
    @SerializedName("vod_duration")
    val duration: String? = null,
    
    @SerializedName("vod_score")
    val score: String? = null,
    
    @SerializedName("vod_hits")
    val hits: String = "0",

    @SerializedName("vod_hits_day")
    val hitsDay: String = "0",

    @SerializedName("vod_hits_week")
    val hitsWeek: String = "0",

    @SerializedName("vod_hits_month")
    val hitsMonth: String = "0",
    
    @SerializedName("vod_is_hot")
    val isHot: Int = 0,
    
    @SerializedName("vod_is_fiery")
    val isFiery: Int = 0,
    
    @SerializedName("type_id")
    val typeId: Int = 0,
    
    @SerializedName("type_name")
    val typeName: String? = null,
    
    @SerializedName("vod_play_url")
    val playUrl: String? = null,
    
    @SerializedName("vod_play_from")
    val playFrom: String? = null,

    @SerializedName("vod_play_show")
    val playShow: String? = null,
    
    @SerializedName("vod_remarks")
    val remarks: String? = null,
    
    @SerializedName("vod_total")
    val total: Int = 0,
    
    @SerializedName("vod_serial")
    val serial: String? = null,
    
    @SerializedName("comment_num")
    val commentNum: Int = 0,

    // 本地字段
    val isFavorite: Boolean = false,
    val progress: Long = 0L,
    val lastWatchTime: Long = 0L,
    val progressDuration: Long = 0L,
    val lastPlayUrl: String? = null,
    val lastEpisodeName: String? = null
) {
    // 解析配置（从 API 返回，不持久化到数据库）
    // 使用 @Ignore 让 Room 忽略，Gson 仍然可以序列化
    @Ignore
    @SerializedName("parse_configs")
    var parseConfigs: List<ParseConfig> = emptyList()
}

/**
 * 视频播放链接
 */
data class PlayUrl(
    val name: String,
    val url: String
)

data class ParseConfig(
    val id: Int = 0,
    val from: String,
    @SerializedName("vod_play_from")
    val vodPlayFrom: String? = null,
    val show: String,
    val parse: String,
    val status: Int = 0,
    val sort: Int = 0
)

data class ResolvedPlayUrl(
    val from: String = "",
    val show: String = "",
    val parse: String = "",
    @SerializedName("raw_url")
    val rawUrl: String = "",
    val url: String,
    val direct: Boolean = false,
    val type: String? = null,
    val system: String? = null,
    val msg: String? = null
)

/**
 * 视频类型
 */
data class VideoType(
    @SerializedName("type_id")
    val id: Int,

    @SerializedName("type_name")
    val name: String,

    @SerializedName("type_en")
    val typeEn: String? = null,

    @SerializedName("type_pid")
    val parentId: Int = 0
)

data class ShortVideoConfig(
    @SerializedName("type_id")
    val typeId: Int,
    @SerializedName("type_name")
    val typeName: String? = null
)

data class ParseConfigListResponse(
    val from: String? = null,
    val list: List<ParseConfig> = emptyList()
)

/**
 * API响应基础模型 - 新API格式
 */
data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 搜索响应数据
 */
data class SearchData<T>(
    val list: List<T>,
    val total: Int
)

/**
 * 分页响应
 */
data class PagedResponse<T>(
    val data: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
) {
    companion object {
        fun <T> from(apiResponse: ApiResponse<List<T>>, total: Int = 0, page: Int = 1, limit: Int = 20): PagedResponse<T> {
            return PagedResponse(
                data = apiResponse.data,
                total = total,
                page = page,
                limit = limit,
                hasMore = apiResponse.data.size >= limit
            )
        }

        // 新增：支持搜索接口的响应格式
        fun <T> fromSearch(apiResponse: ApiResponse<SearchData<T>>, page: Int = 1, limit: Int = 20): PagedResponse<T> {
            return PagedResponse(
                data = apiResponse.data.list,
                total = apiResponse.data.total,
                page = page,
                limit = limit,
                hasMore = apiResponse.data.list.size >= limit
            )
        }
    }
}

/**
 * 视频列表响应数据（新格式）
 */
data class VideoListData(
    val list: List<Video>,
    val total: Int,
    val page: Int,
    val limit: Int
)
