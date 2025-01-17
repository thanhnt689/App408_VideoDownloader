package com.files.video.downloader.videoplayerdownloader.downloader.di

import android.content.Context
import androidx.room.Room
import com.files.video.downloader.videoplayerdownloader.downloader.data.AppDatabase
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.AdHostDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.HistoryDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.ProgressDao
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "video"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(appDatabase: AppDatabase): HistoryDao {
        return appDatabase.historyDao()
    }

    @Provides
    @Singleton
    fun provideAdHostDao(appDatabase: AppDatabase): AdHostDao {
        return appDatabase.adHostDao()
    }

    @Provides
    @Singleton
    fun provideProgressDao(appDatabase: AppDatabase): ProgressDao {
        return appDatabase.progressDao()
    }

    @Provides
    @Singleton
    fun provideVideoTaskItemDao(appDatabase: AppDatabase): VideoTaskItem {
        return appDatabase.videoTaskItemDao()
    }

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}