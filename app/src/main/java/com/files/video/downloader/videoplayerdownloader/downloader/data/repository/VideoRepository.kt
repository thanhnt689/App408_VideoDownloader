package com.files.video.downloader.videoplayerdownloader.downloader.data.repository

import androidx.annotation.VisibleForTesting
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.di.qualifier.RemoteData
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

interface VideoRepository {

    fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean = false): VideoInfo?

    fun saveVideoInfo(videoInfo: VideoInfo)
}

@Singleton
class VideoRepositoryImpl @Inject constructor(
    @RemoteData private val remoteDataSource: VideoRepository
) : VideoRepository {

    @VisibleForTesting
    internal var cachedVideos: MutableMap<String, VideoInfo> = mutableMapOf()

    override fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        cachedVideos[url.url.toString()]?.let { return it }

        return getAndCacheRemoteVideo(url, isM3u8OrMpd)
    }

    override fun saveVideoInfo(videoInfo: VideoInfo) {
        cachedVideos[videoInfo.originalUrl] = videoInfo
    }

    private fun getAndCacheRemoteVideo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        val videoInfo = remoteDataSource.getVideoInfo(url, isM3u8OrMpd)
        if (videoInfo != null) {
            videoInfo.originalUrl = url.url.toString()
            cachedVideos[videoInfo.originalUrl] = videoInfo

            return videoInfo
        }
        return null
    }
}