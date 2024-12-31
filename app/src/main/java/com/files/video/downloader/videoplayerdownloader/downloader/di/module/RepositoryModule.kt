package com.files.video.downloader.videoplayerdownloader.downloader.di.module

import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.AdBlockHostsRemoteDataSource
import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.VideoRemoteDataSource
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.AdBlockHostsRepository
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.AdBlockHostsRepositoryImpl
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.ProgressRepository
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.ProgressRepositoryImpl
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.VideoRepository
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.VideoRepositoryImpl
import com.files.video.downloader.videoplayerdownloader.downloader.di.qualifier.RemoteData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindVideoLocalDataSource(localDataSource: VideoLocalDataSource): VideoRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindVideoRemoteDataSource(remoteDataSource: VideoRemoteDataSource): VideoRepository

    @Singleton
    @Binds
    abstract fun bindVideoRepositoryImpl(videoRepository: VideoRepositoryImpl): VideoRepository



    @Singleton
    @Binds
    abstract fun bindProgressRepositoryImpl(progressRepository: ProgressRepositoryImpl): ProgressRepository

//    @Singleton
//    @Binds
//    @LocalData
//    abstract fun bindAdBlockHostsLocalDataSource(adBlockHostsLocalDataSource: AdBlockHostsLocalDataSource): AdBlockHostsRepository

    @Singleton
    @Binds
    @RemoteData
    abstract fun bindAdBlockHostsRemoteDataSource(adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource): AdBlockHostsRepository

    @Singleton
    @Binds
    abstract fun bindAdBlockHostsRepositoryImpl(adBlockHostsRepository: AdBlockHostsRepositoryImpl): AdBlockHostsRepository

}
