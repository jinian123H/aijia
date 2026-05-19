package com.aijia.video.data.model

import com.google.gson.annotations.SerializedName

/**
 * 评论数据模型
 */
data class Comment(
    val id: Int,
    val content: String,
    val user: CommentUser,
    val videoId: Int,
    val parentId: Int? = null,
    val likes: Int = 0,
    val createTime: String,
    val replyCount: Int = 0,
    val isLiked: Boolean = false,
    val isSystem: Boolean = false
) {
    // 兼容旧字段访问
    val commentId: Int get() = id
    val commentContent: String get() = content
    val commentName: String get() = user.username
    val commentTime: Long get() = parseTimeToTimestamp(createTime)

    private fun parseTimeToTimestamp(timeStr: String): Long {
        return try {
            // 如果是相对时间描述，返回当前时间
            if (timeStr.contains("前") || timeStr == "刚刚") {
                System.currentTimeMillis() / 1000
            } else {
                // 尝试解析时间戳
                timeStr.toLongOrNull() ?: (System.currentTimeMillis() / 1000)
            }
        } catch (e: Exception) {
            System.currentTimeMillis() / 1000
        }
    }
}

/**
 * 评论用户信息
 */
data class CommentUser(
    val id: Int,
    val username: String,
    val avatar: String? = null,
    val level: Int = 1,
    val vipLevel: Int = 0
)

/**
 * 弹幕数据模型
 */
data class Danmu(
    val id: String,
    val content: String,
    val videoId: Int,
    val userId: Int,
    val username: String,
    val color: String = "#FFFFFF",
    val fontSize: Int = 25,
    val time: Long, // 弹幕出现时间(毫秒)
    val type: DanmuType = DanmuType.SCROLL
)

/**
 * 弹幕类型
 */
enum class DanmuType {
    SCROLL,    // 滚动弹幕
    TOP,       // 顶部弹幕
    BOTTOM,    // 底部弹幕
    SPECIAL    // 特殊弹幕
}

/**
 * 评论列表响应
 */
data class CommentResponse(
    val code: Int,
    val msg: String,
    val data: CommentData
)

/**
 * 评论数据
 */
data class CommentData(
    val comments: List<Comment>,
    val total: Int,
    val page: Int,
    val limit: Int
)

data class CommentApiListData(
    @SerializedName("page")
    val page: Int = 1,
    @SerializedName("total")
    val total: Int = 0,
    @SerializedName("limit")
    val limit: Int = 20,
    @SerializedName("list")
    val list: List<CommentApiItem> = emptyList()
)

data class CommentApiItem(
    @SerializedName("comment_id")
    val commentId: Int = 0,
    @SerializedName("comment_content")
    val commentContent: String = "",
    @SerializedName("comment_name")
    val commentName: String = "",
    @SerializedName("user_portrait")
    val userPortrait: String? = null,
    @SerializedName("comment_time")
    val commentTime: Long = 0,
    @SerializedName("user_id")
    val userId: Int = 0,
    @SerializedName("likes")
    val likes: Int = 0,
    @SerializedName("is_liked")
    val isLiked: Boolean = false,
    @SerializedName("is_system")
    val isSystem: Boolean = false
)
