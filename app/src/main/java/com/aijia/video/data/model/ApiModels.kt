package com.aijia.video.data.model

import com.google.gson.annotations.SerializedName

/**
 * 分页响应包装器
 */
data class PaginatedResponse<T>(
    val page: Int,
    val total: Int,
    val limit: Int,
    val list: List<T>
)

/**
 * 视频播放地址 - 新API格式 (GET /vod/play 返回数组元素)
 */
data class PlayUrlInfo(
    @SerializedName("name")
    val play_name: String,
    @SerializedName("url")
    val play_url: String,
    val play_from: String = ""
)


data class AppPermission(
    @SerializedName("is_logged_in")
    val isLoggedIn: Boolean = false,
    @SerializedName("user_id")
    val userId: Int = 0,
    @SerializedName("group_id")
    val groupId: Int = 1,
    @SerializedName("group_name")
    val groupName: String = "游客",
    @SerializedName("user_end_time")
    val userEndTime: Long = 0,
    val video: Int = 1,
    val danmaku: Int = 0,
    val comment: Int = 0,
    val feedback: Int = 0,
    val urge: Int = 0,
    val search: Int = 0,
    val download: Int = 0,
    val ad: Int = 1,
    val extend: Int = 0,
    @SerializedName("discover_visible")
    val discoverVisible: Int = 0,
    val tips: Map<String, String> = emptyMap()
) {
    fun hasPermission(key: String): Boolean {
        return when (key) {
            "video" -> video == 1
            "danmaku" -> danmaku == 1
            "comment" -> comment == 1
            "feedback" -> feedback == 1
            "urge" -> urge == 1
            "search" -> search == 1
            "download" -> download == 1
            "ad" -> ad == 1
            "extend" -> extend == 1
            else -> false
        }
    }

    fun tipFor(key: String, fallback: String): String {
        return tips[key]?.takeIf { it.isNotBlank() } ?: fallback
    }

    companion object {
        fun guestDefault(): AppPermission = AppPermission(
            isLoggedIn = false,
            userId = 0,
            groupId = 1,
            groupName = "游客",
            video = 0,
            danmaku = 0,
            comment = 0,
            feedback = 0,
            urge = 0,
            search = 0,
            download = 0,
            ad = 1,
            extend = 0,
            discoverVisible = 0,
            tips = mapOf(
                "video" to "当前账号暂无视频权限",
                "danmaku" to "当前账号暂无弹幕权限",
                "comment" to "当前账号暂无评论权限",
                "feedback" to "当前账号暂无反馈权限",
                "urge" to "当前账号暂无催更权限",
                "search" to "当前账号暂无搜索权限",
                "download" to "当前账号暂无下载权限",
                "ad" to "游客默认显示广告",
                "extend" to "当前账号暂无发现权限"
            )
        )

        /**
         * 根据 group_id 生成本地备用权限
         * 离线时不给任何额外权限，必须联网从服务端获取
         */
        fun fromGroupId(groupId: Int, groupName: String, isLoggedIn: Boolean): AppPermission {
            return guestDefault().copy(
                isLoggedIn = isLoggedIn,
                groupId = groupId,
                groupName = groupName.ifBlank { "用户" }
            )
        }
    }
}

// ========== 反馈 ==========

/**
 * 反馈请求
 */
data class FeedbackRequest(
    val feedback_title: String,
    val feedback_content: String,
    val contact: String = ""
)

// ========== 弹幕 ==========

/**
 * 弹幕配置
 */
data class DanmuConfig(
    @SerializedName("danmu_api_url")
    val danmuApiUrl: String,
    @SerializedName("enabled")
    val enabled: Boolean = true,
    @SerializedName("danmu_api_description")
    val danmuApiDescription: String? = null
)

/**
 * 弹幕API信息
 */
data class DanmuApiInfo(
    @SerializedName("danmu_api_url")
    val danmuApiUrl: String,
    @SerializedName("danmu_api_full_url")
    val danmuApiFullUrl: String,
    @SerializedName("video_url")
    val videoUrl: String
)

/**
 * 第三方弹幕API响应
 */
data class DanmuApiResponse(
    val code: Int,
    val name: String,
    val danum: Int,
    val danmuku: List<List<Any>>
)

/**
 * 弹幕响应
 */
data class DanmuResponse(
    val danmuList: List<Danmu>
)

// ========== 卡密系统 ==========

/**
 * 卡密响应
 */
data class CardResponse(
    @SerializedName("card_id")
    val cardId: Int,
    @SerializedName("card_pwd")
    val cardPwd: String = "",
    @SerializedName("card_money")
    val cardMoney: Int = 0,
    @SerializedName("card_points")
    val cardPoints: Int = 0
)

/**
 * 卡密记录
 */
data class CardRecord(
    @SerializedName("card_id")
    val cardId: Int,
    @SerializedName("card_no")
    val cardNo: String = "",
    @SerializedName("card_days")
    val cardDays: Int = 0,
    @SerializedName("use_time")
    val useTime: Int = 0,
    @SerializedName("start_time")
    val startTime: Int = 0,
    @SerializedName("end_time")
    val endTime: Int = 0
)

/**
 * 卡密记录响应
 */
data class CardRecordsResponse(
    val records: List<CardRecord>
)