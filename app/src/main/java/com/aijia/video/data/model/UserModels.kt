package com.aijia.video.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户信息
 */
data class UserInfo(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("group_id")
    val groupId: Int = 1,

    @SerializedName("group_name")
    val groupName: String? = null,
    
    @SerializedName(value = "user_name", alternate = ["username"])
    val userName: String? = null,

    @SerializedName(value = "user_nick_name", alternate = ["nickname"])
    val nickName: String? = null,
    
    @SerializedName("user_phone")
    val phone: String? = null,
    
    @SerializedName(value = "user_avatar", alternate = ["user_portrait", "avatar"])
    val avatar: String? = null,
    
    @SerializedName(value = "user_points", alternate = ["points"])
    val points: Int = 0,
    
    @SerializedName("user_gold")
    val gold: Int = 0,
    
    @SerializedName("user_level")
    val level: Int = 1,
    
    @SerializedName("user_vip")
    val isVip: Int = 0,
    
    @SerializedName(value = "vip_end_time", alternate = ["user_end_time"])
    val vipEndTime: Long? = null,
    
    @SerializedName("user_status")
    val status: Int = 1,
    
    @SerializedName("create_time")
    val createTime: Long = 0L
)

/**
 * 登录响应
 */
data class LoginResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("group_id")
    val groupId: Int = 1,

    @SerializedName("group_name")
    val groupName: String? = null,

    @SerializedName(value = "user_name", alternate = ["username"])
    val userName: String? = null,

    @SerializedName(value = "user_nick_name", alternate = ["nickname"])
    val nickName: String? = null,

    @SerializedName("user_phone")
    val phone: String? = null,

    @SerializedName(value = "user_end_time", alternate = ["vip_end_time"])
    val userEndTime: Long? = null,

    @SerializedName("expire_time")
    val expireTime: Long = 0L,

    // 登录时服务端内嵌的权限字段（减少额外请求）
    val video: Int = 1,
    val danmaku: Int = 0,
    val comment: Int = 0,
    val feedback: Int = 0,
    val urge: Int = 0,
    val search: Int = 0,
    val download: Int = 0,
    val ad: Int = 1,
    val extend: Int = 0
)

data class RegisterResponse(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("group_id")
    val groupId: Int = 2,

    @SerializedName("group_name")
    val groupName: String? = null,

    @SerializedName(value = "username", alternate = ["user_name"])
    val userName: String,

    @SerializedName(value = "nickname", alternate = ["user_nick_name"])
    val nickName: String,

    @SerializedName("token")
    val token: String,

    // 注册时服务端内嵌的权限字段
    val video: Int = 1,
    val danmaku: Int = 0,
    val comment: Int = 0,
    val feedback: Int = 0,
    val urge: Int = 0,
    val search: Int = 0,
    val download: Int = 0,
    val ad: Int = 1,
    val extend: Int = 0
)

/**
 * 启动配置
 */
data class StartupConfig(
    @SerializedName("app_config")
    val appConfig: AppConfig
)

/**
 * 应用配置
 */
data class AppConfig(
    @SerializedName("enable_download")
    val enableDownload: Boolean = true,
    
    @SerializedName("enable_cast")
    val enableCast: Boolean = true,
    
    @SerializedName("max_quality")
    val maxQuality: String = "1080p",
    
    @SerializedName("supported_formats")
    val supportedFormats: List<String> = emptyList(),
    
    @SerializedName("enable_danmu")
    val enableDanmu: Boolean = true,
    
    @SerializedName("enable_comment")
    val enableComment: Boolean = true,
    
    @SerializedName("enable_share")
    val enableShare: Boolean = true
)

/**
 * 视频详情（扩展）
 */
data class VideoDetail(
    val video: Video,
    val relatedVideos: List<Video> = emptyList(),
    val playUrls: List<PlayUrl> = emptyList(),
    val comments: List<Comment> = emptyList()
)

/**
 * 播放进度
 */
data class PlayProgress(
    @SerializedName("vod_id")
    val vodId: Int,
    
    @SerializedName("progress")
    val progress: Long = 0L,
    
    @SerializedName("duration")
    val duration: Long = 0L,
    
    @SerializedName("last_watch_time")
    val lastWatchTime: Long = 0L,
    
    @SerializedName("percentage")
    val percentage: Float = 0f
)

/**
 * 评论
 */
data class LegacyComment(
    @SerializedName("comment_id")
    val id: Int,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("user_name")
    val userName: String,
    
    @SerializedName("user_avatar")
    val userAvatar: String? = null,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("like_count")
    val likeCount: Int = 0,
    
    @SerializedName("reply_count")
    val replyCount: Int = 0,
    
    @SerializedName("create_time")
    val createTime: Long,
    
    @SerializedName("is_liked")
    val isLiked: Boolean = false
)

/**
 * 弹幕
 */
data class LegacyDanmu(
    @SerializedName("danmu_id")
    val id: Int,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("user_name")
    val userName: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("color")
    val color: String = "#FFFFFF",
    
    @SerializedName("time")
    val time: Double = 0.0,
    
    @SerializedName("create_time")
    val createTime: Long
)

/**
 * 视频评分
 */
data class VideoScore(
    @SerializedName("vod_id")
    val vodId: Int,
    
    @SerializedName("score")
    val score: Float,
    
    @SerializedName("score_count")
    val scoreCount: Int = 0,
    
    @SerializedName("user_score")
    val userScore: Float? = null
)

// ========== 多端注册登录 ==========

data class PhoneAuthConfig(
    @SerializedName("device_type") val deviceType: String = "phone",
    @SerializedName("oneclick_login") val oneclickLogin: Boolean = false,
    @SerializedName("manual_register") val manualRegister: Boolean = true,
    @SerializedName("auto_register_trial_days") val autoRegisterTrialDays: Int = 7,
    @SerializedName("manual_register_trial_days") val manualRegisterTrialDays: Int = 7
)

data class OneClickLoginResponse(
    @SerializedName("is_new") val isNew: Boolean = false,
    @SerializedName("need_register") val needRegister: Boolean = false,
    @SerializedName("username") val username: String = "",
    @SerializedName("token") val token: String = "",
    @SerializedName("user_id") val userId: Int = 0,
    @SerializedName("group_id") val groupId: Int = 1,
    @SerializedName("group_name") val groupName: String? = null,
    @SerializedName("user_end_time") val userEndTime: Long = 0,
    @SerializedName("login_mode") val loginMode: String = ""
)

data class QRConfirmRequest(
    @SerializedName("token") val token: String
)
