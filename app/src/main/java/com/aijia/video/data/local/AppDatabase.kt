package com.aijia.video.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.aijia.video.data.local.converters.Converters
import com.aijia.video.data.local.dao.VideoDao
import com.aijia.video.data.model.Video
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用数据库
 */
@Singleton
@Database(
    entities = [
        Video::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    
    companion object {
        const val DATABASE_NAME = "aijia_video_database"
    }
}

/**
 * 数据库提供者
 */
class DatabaseProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }
}
