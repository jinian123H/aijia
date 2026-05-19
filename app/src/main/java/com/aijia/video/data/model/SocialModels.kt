package com.aijia.video.data.model

data class GroupChat(
    val group_id: Int = 0,
    val group_name: String = "",
    val group_desc: String = "",
    val group_avatar: String = "",
    val member_count: Int = 0,
    val message_count: Int = 0,
    val create_time: Long = 0L
)

data class GroupMessage(
    val message_id: Int = 0,
    val group_id: Int = 0,
    val user_id: Int = 0,
    val user_name: String = "",
    val user_avatar: String = "",
    val content: String = "",
    val type: Int = 1,
    val create_time: Long = 0L
)

data class Topic(
    val topic_id: Int = 0,
    val topic_title: String = "",
    val topic_content: String = "",
    val topic_pic: String = "",
    val user_id: Int = 0,
    val user_name: String = "",
    val comment_num: Int = 0,
    val like_num: Int = 0,
    val create_time: Long = 0L
)

data class GuestBook(
    val gbook_id: Int = 0,
    val gbook_title: String = "",
    val gbook_content: String = "",
    val gbook_type: Int = 0,
    val gbook_name: String = "",
    val gbook_reply: String = "",
    val create_time: Long = 0L
)

data class GuestBookType(
    val id: Int = 0,
    val name: String = ""
)
