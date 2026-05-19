package com.aijia.video.di

import com.aijia.video.player.Media3Player
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context

/**
 * 应用依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideMedia3Player(@ApplicationContext context: Context): Media3Player {
        return Media3Player(context)
    }
}
