package com.aijia.video.data.model

import com.google.gson.annotations.SerializedName

/**
 * 带版本信息的数据响应
 * 用于所有支持版本缓存的接口
 */
data class VersionedData<T>(
    @SerializedName("version_id")
    val versionId: String = "",

    @SerializedName("data_changed")
    val dataChanged: Boolean = true,

    @SerializedName("content")
    val content: T? = null
)

/**
 * 扩展函数：判断是否有新数据
 */
fun <T> VersionedData<T>.hasNewData(): Boolean {
    return dataChanged && content != null
}

/**
 * 扩展函数：获取数据或默认值
 */
fun <T> VersionedData<T>.getDataOrNull(): T? {
    return if (dataChanged) content else null
}

// 具名 data class：规避 Retrofit+R8 三层嵌套泛型 ParameterizedType 擦除崩溃
// 不继承 VersionedData（data class 是 final），字段名与 VersionedData 完全一致
data class VideoTypeVersionedData(
    @SerializedName("version_id") val versionId: String = "",
    @SerializedName("data_changed") val dataChanged: Boolean = true,
    @SerializedName("content") val content: List<VideoType>? = null
)

data class HomeIndexVersionedData(
    @SerializedName("version_id") val versionId: String = "",
    @SerializedName("data_changed") val dataChanged: Boolean = true,
    @SerializedName("content") val content: HomeIndexResponse? = null
)

data class AppPermissionVersionedData(
    @SerializedName("version_id") val versionId: String = "",
    @SerializedName("data_changed") val dataChanged: Boolean = true,
    @SerializedName("content") val content: AppPermission? = null
)

data class AdConfigVersionedData(
    @SerializedName("version_id") val versionId: String = "",
    @SerializedName("data_changed") val dataChanged: Boolean = true,
    @SerializedName("content") val content: AdConfig? = null
)

/**
 * 首页推荐版本化数据（每日缓存模式）
 * content 为推荐区块列表，无 banners
 */
data class HomeRecommendVersionedData(
    @SerializedName("version_id") val versionId: String = "",
    @SerializedName("data_changed") val dataChanged: Boolean = true,
    @SerializedName("content") val content: List<HomeSection>? = null
)
