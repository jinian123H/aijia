package com.aijia.video.data.remote

import com.aijia.video.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API服务接口
 */
interface ApiService {

    // ========== 启动与配置 ==========

    /**
     * 获取启动配置
     */
    @GET("api/v1/vod/index")
    suspend fun getStartupConfig(): Response<ResponseBody>

    /**
     * 获取应用配置
     */
    @GET("api/v1/vod/index")
    suspend fun getAppConfig(): Response<ResponseBody>

    // ========== 视频内容 ==========

    /**
     * 获取视频类型列表（返回 ResponseBody 规避 R8 泛型擦除）
     */
    @GET("api/v1/vod/type")
    suspend fun getVideoTypes(
        @Query("version_id") versionId: String = ""
    ): Response<ResponseBody>

    @GET("api/v1/vod/short-video-config")
    suspend fun getShortVideoConfig(): Response<ResponseBody>

    @GET("api/v1/vod/parse-config")
    suspend fun getParseConfigs(
        @Query("from") from: String? = null
    ): Response<ResponseBody>

    @GET("api/v1/vod/parse-play-url")
    suspend fun parsePlayUrl(
        @Query("from") from: String? = null,
        @Query("url") url: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis() / 1000
    ): Response<ResponseBody>

    /**
     * 获取视频列表
     */
    @GET("api/v1/vod/list")
    suspend fun getVideoList(
        @Query("type_id") tid: Int? = null,
        @Query("area") area: String? = null,
        @Query("year") year: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "time"
    ): Response<ResponseBody>

    /**
     * 获取视频详情
     */
    @GET("api/v1/vod/detail")
    suspend fun getVideoDetail(
        @Query("id") id: Int
    ): Response<ResponseBody>

    /**
     * 获取首页轮播和推荐分组（返回 ResponseBody 规避 R8 泛型擦除）
     */
    @GET("api/v1/vod/index")
    suspend fun getHomeIndex(
        @Query("version_id") versionId: String = ""
    ): Response<ResponseBody>

    /**
     * 获取排行榜分组
     */
    @GET("api/v1/vod/rank")
    suspend fun getRankIndex(): Response<ResponseBody>

    /**
     * 获取推荐视频
     */
    @GET("api/v1/vod/index")
    suspend fun getRecommendVideos(
        @Query("type_id") typeId: Int = 0,
        @Query("sort_by") sortBy: String = "vod_hits",
        @Query("sort_order") sortOrder: String = "DESC",
        @Query("random") random: Int = 0,
        @Query("title") title: String = "推荐视频",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ResponseBody>

    /**
     * 搜索视频
     */
    @GET("api/v1/vod/search")
    suspend fun searchVideos(
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ResponseBody>

    // ========== 用户系统 ==========

    /**
     * 用户登录
     */
    @POST("api/v1/user/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<ResponseBody>

    /**
     * 用户注册
     */
    @POST("api/v1/user/register")
    @FormUrlEncoded
    suspend fun register(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("confirm_password") confirmPassword: String
    ): Response<ResponseBody>


    /**
     * 获取用户信息
     */
    @GET("api/v1/user/detail")
    suspend fun getUserInfo(): Response<ResponseBody>

    /**
     * 获取用户权限（返回 ResponseBody 规避 R8 泛型擦除）
     */
    @GET("api/v1/permission/user")
    suspend fun getUserPermission(
        @Query("version_id") versionId: String = ""
    ): Response<ResponseBody>

    /**
     * 更新用户信息
     */
    @POST("api/v1/user/update")
    @FormUrlEncoded
    suspend fun updateUserInfo(
        @Field("nickname") nickname: String? = null,
        @Field("avatar") avatar: String? = null
    ): Response<ResponseBody>

    // ========== 视频播放 ==========

    /**
     * 获取视频播放地址
     */
    @GET("api/v1/vod/play")
    suspend fun getVideoPlayUrl(
        @Query("id") id: String,
        @Query("rid") rid: Int = 1
    ): Response<ResponseBody>

    /**
     * 获取视频下载地址（需JWT + download权限）
     */
    @GET("api/v1/vod/download")
    suspend fun getVideoDownloadUrl(
        @Query("id") id: String,
        @Query("rid") rid: Int = 1
    ): Response<ResponseBody>

    // ========== 评论系统 ==========

    /**
     * 获取评论列表
     */
    @GET("api/v1/comment/index")
    suspend fun getComments(
        @Query("rid") rid: Int,
        @Query("mid") mid: Int,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ResponseBody>

    /**
     * 发表评论
     */
    @POST("api/v1/comment/index")
    @FormUrlEncoded
    suspend fun postComment(
        @Field("comment_content") commentContent: String,
        @Field("rid") rid: Int,
        @Field("mid") mid: Int
    ): Response<ResponseBody>

    // ========== 反馈系统 ==========

    /**
     * 发表反馈
     */
    @FormUrlEncoded
    @POST("api/v1/feedback/index")
    suspend fun postFeedback(
        @Field("feedback_type") type: String,
        @Field("vod_id") vodId: Int = 0
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("api/v1/urge/index")
    suspend fun postUrge(
        @Field("vod_id") vodId: Int,
        @Field("vod_name") vodName: String,
        @Field("episode_name") episodeName: String = "",
        @Field("play_from") playFrom: String = "",
        @Field("play_url") playUrl: String = "",
        @Field("resolved_url") resolvedUrl: String = "",
        @Field("cover_url") coverUrl: String = ""
    ): Response<ResponseBody>

    // ========== 弹幕系统 ==========

    /**
     * 获取弹幕配置
     */
    @GET("api/v1/danmu/config")
    suspend fun getDanmuConfig(): Response<ResponseBody>

    /**
     * 获取弹幕列表
     */
    @GET("api/v1/danmu/list")
    suspend fun getDanmuList(
        @Query("url") url: String
    ): Response<ResponseBody>

    // ========== 留言 ==========

    /**
     * 提交留言
     */
    @FormUrlEncoded
    @POST("api/v1/message/index")
    suspend fun submitMessage(
        @Field("message_content") content: String,
        @Field("message_contact") contact: String = ""
    ): Response<ResponseBody>

    /**
     * 版本检查（新架构）
     */
    @POST("api/v1/app/check-version")
    suspend fun checkAppVersion(
        @Body request: CheckVersionRequest
    ): Response<ResponseBody>

    /**
     * 强制更新检查
     */
    @POST("api/v1/app/version/check")
    suspend fun checkForceUpdate(
        @Body request: CheckForceUpdateRequest
    ): Response<ResponseBody>

    /**
     * 获取广告配置（返回 ResponseBody 规避 R8 泛型擦除）
     */
    @GET("api/v1/ad/config")
    suspend fun getAdConfig(
        @Query("version_id") versionId: String = "",
        @Query("user_id") userId: Int? = null,
        @Query("token") token: String? = null
    ): Response<ResponseBody>

    /**
     * 按类型获取广告配置（新接口）
     */
    @GET("api/v1/ad/config")
    suspend fun getAdByType(
        @Query("ad_type") adType: String,
        @Query("version_id") versionId: String = "",
        @Header("Authorization") token: String? = null
    ): Response<ResponseBody>

    // ========== Banner ==========

    /**
     * 获取 Banner 轮播视频（每日缓存模式）
     */
    @GET("api/v1/app/banner")
    suspend fun getBanner(
        @Query("version_id") versionId: String = ""
    ): Response<ResponseBody>

    // ========== 卡密系统 ==========

    /**
     * 验证卡密
     */
    @POST("api/v1/card/verify")
    @FormUrlEncoded
    suspend fun verifyCard(
        @Field("card_pwd") cardPwd: String
    ): Response<ResponseBody>

    /**
     * 使用卡密
     */
    @POST("api/v1/card/use")
    @FormUrlEncoded
    suspend fun useCard(
        @Field("card_pwd") cardPwd: String
    ): Response<ResponseBody>

    /**
     * 获取卡密使用记录
     */
    @GET("api/v1/card/records")
    suspend fun getCardRecords(): Response<ResponseBody>
}
