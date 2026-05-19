package com.aijia.video.data.local.dao

import androidx.room.*
import com.aijia.video.data.model.Video
import kotlinx.coroutines.flow.Flow

/**
 * 视频数据访问对象
 */
@Dao
interface VideoDao {
    
    /**
     * 插入视频列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<Video>)
    
    /**
     * 插入单个视频
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: Video)
    
    /**
     * 根据ID获取视频
     */
    @Query("SELECT * FROM videos WHERE id = :vodId")
    suspend fun getVideoById(vodId: String): Video?

    @Query("SELECT * FROM videos WHERE id IN (:vodIds)")
    suspend fun getVideosByIds(vodIds: List<String>): List<Video>
    
    /**
     * 根据类型获取视频列表
     */
    @Query("SELECT * FROM videos WHERE typeId = :type ORDER BY hits DESC LIMIT :limit OFFSET :offset")
    suspend fun getVideosByType(type: Int?, limit: Int, offset: Int): List<Video>
    
    /**
     * 获取收藏视频
     */
    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY lastWatchTime DESC")
    fun getFavoriteVideos(): Flow<List<Video>>
    
    /**
     * 获取播放历史
     */
    @Query("SELECT * FROM videos WHERE progress > 0 ORDER BY lastWatchTime DESC")
    fun getPlayHistory(): Flow<List<Video>>
    
    /**
     * 获取热门视频
     */
    @Query("SELECT * FROM videos WHERE isHot = 1 ORDER BY hits DESC LIMIT :limit")
    suspend fun getHotVideos(limit: Int = 20): List<Video>
    
    /**
     * 获取推荐视频
     */
    @Query("SELECT * FROM videos WHERE isFiery = 1 ORDER BY hits DESC LIMIT :limit")
    suspend fun getRecommendVideos(limit: Int = 20): List<Video>
    
    /**
     * 搜索视频
     */
    @Query("SELECT * FROM videos WHERE name LIKE '%' || :keyword || '%' OR actor LIKE '%' || :keyword || '%' OR director LIKE '%' || :keyword || '%' ORDER BY hits DESC LIMIT :limit")
    suspend fun searchVideos(keyword: String, limit: Int = 20): List<Video>
    
    /**
     * 更新视频播放进度
     */
    @Query("UPDATE videos SET progress = :progress, lastWatchTime = :lastWatchTime, progressDuration = :duration, lastPlayUrl = :playUrl, lastEpisodeName = :episodeName WHERE id = :vodId")
    suspend fun updateVideoProgress(
        vodId: String,
        progress: Long,
        duration: Long,
        playUrl: String?,
        episodeName: String?,
        lastWatchTime: Long
    )

    /**
     * 更新收藏状态
     */
    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :vodId")
    suspend fun updateFavoriteStatus(vodId: String, isFavorite: Boolean)

    @Query("UPDATE videos SET isFavorite = 0 WHERE isFavorite = 1")
    suspend fun clearAllFavorites()
    
    /**
     * 清除播放历史
     */
    @Query("UPDATE videos SET progress = 0, lastWatchTime = 0, progressDuration = 0, lastPlayUrl = NULL, lastEpisodeName = NULL")
    suspend fun clearPlayHistory()

    /**
     * 删除单个播放历史
     */
    @Query("UPDATE videos SET progress = 0, lastWatchTime = 0, progressDuration = 0, lastPlayUrl = NULL, lastEpisodeName = NULL WHERE id = :vodId")
    suspend fun clearSinglePlayHistory(vodId: String)
    
    /**
     * 获取所有视频数量
     */
    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int
    
    /**
     * 删除视频
     */
    @Delete
    suspend fun deleteVideo(video: Video)
    
    /**
     * 清空所有视频
     */
    @Query("DELETE FROM videos")
    suspend fun clearAllVideos()
}
