package com.files.video.downloader.videoplayerdownloader.downloader.di.module

import com.files.video.downloader.videoplayerdownloader.downloader.util.scheduler.BaseSchedulers
import com.files.video.downloader.videoplayerdownloader.downloader.util.scheduler.BaseSchedulersImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SchedulerModule {

    @Provides
    fun provideBaseSchedulers(): BaseSchedulers {
        return BaseSchedulersImpl()
    }
}
