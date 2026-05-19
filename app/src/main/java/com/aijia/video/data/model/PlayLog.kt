package com.aijia.video.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * 播放记录数据模型
 */
@Entity(tableName = "play_logs")
data class PlayLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @SerializedName("vod_id")
    val vodId: String,

    @SerializedName("vod_name")
    val vodName: String,

    @SerializedName("vod_pic")
    val vodPic: String? = null,

    @SerializedName("episode")
    val episode: Int = 1,

    @SerializedName("episode_name")
    val episodeName: String? = null,

    @SerializedName("progress")
    val progress: Long = 0L,

    @SerializedName("duration")
    val duration: Long = 0L,

    @SerializedName("last_watch_time")
    val lastWatchTime: Long = System.currentTimeMillis(),

    @SerializedName("percentage")
    val percentage: Float = 0f,

    @SerializedName("play_url")
    val playUrl: String? = null
) {
    /**
     * 是否已观看完成
     */
    val isCompleted: Boolean
        get() = percentage >= 0.95f

    /**
     * 格式化进度显示
     */
    fun getProgressText(): String {
        val progressMinutes = progress / 60000
        val durationMinutes = duration / 60000
        return "$progressMinutes/$durationMinutes 分钟"
    }
}
