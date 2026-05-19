package com.aijia.video.data.model

import com.google.gson.annotations.SerializedName

/**
 * 反馈数据模型
 */
data class Feedback(
    @SerializedName("feedback_id")
    val id: Int,
    @SerializedName("feedback_title")
    val title: String = "",
    @SerializedName("feedback_content")
    val content: String,
    @SerializedName("feedback_name")
    val name: String = "",
    @SerializedName("feedback_contact")
    val contact: String = "",
    @SerializedName("feedback_time")
    val time: Long,
    @SerializedName("feedback_status")
    val status: Int = 0,
    @SerializedName("feedback_reply")
    val reply: String = "",
    @SerializedName("reply_time")
    val replyTime: Long = 0
)

/**
 * 反馈列表响应
 */
data class FeedbackListResponse(
    val code: Int,
    val msg: String,
    val data: FeedbackListData
)

/**
 * 反馈列表数据
 */
data class FeedbackListData(
    val page: Int,
    val total: Int,
    val limit: Int,
    val list: List<Feedback>
)
