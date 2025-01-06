package com.files.video.downloader.videoplayerdownloader.downloader.di.module

import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.ProgressRepository
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.ProgressRepositoryImpl
import com.files.video.downloader.videoplayerdownloader.downloader.di.qualifier.LocalData
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

}
