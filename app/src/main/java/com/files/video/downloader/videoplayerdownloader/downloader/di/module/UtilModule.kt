package com.files.video.downloader.videoplayerdownloader.downloader.di.module

import android.app.DownloadManager
import android.content.Context
import com.files.video.downloader.videoplayerdownloader.downloader.Application
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.VideoTaskItemRepository
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.IntentUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.NotificationsHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.SystemUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    @Singleton
    @Provides
    fun bindDownloadManager(application: android.app.Application): DownloadManager =
        application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    @Singleton
    @Provides
    fun bindSystemUtil() = SystemUtils()

    @Singleton
    @Provides
    fun bindIntentUtil(fileUtil: FileUtil) = IntentUtil(fileUtil)

    @Singleton
    @Provides
    fun bindAppUtil() = AppUtil()

    @Provides
    @Singleton
    fun provideNotificationsHelper(@ApplicationContext application: Context, videoTaskItemRepository: VideoTaskItemRepository): NotificationsHelper {
        return NotificationsHelper(application,videoTaskItemRepository)
    }

    @Singleton
    @Provides
    fun bindFileUtil() = FileUtil()
}
