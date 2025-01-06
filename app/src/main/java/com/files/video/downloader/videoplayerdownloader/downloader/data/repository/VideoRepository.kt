package com.files.video.downloader.videoplayerdownloader.downloader.data.repository

import android.util.Log
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
class VideoRepositoryImpl @Inject constructor() {

    private val cachedVideos: MutableMap<String, VideoInfo> = mutableMapOf()

    fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        Log.d("ntt", "getVideoInfo: ${cachedVideos[url.url.toString()]}")
        cachedVideos[url.url.toString()]?.let { return it }

        return getAndCacheRemoteVideo(url, isM3u8OrMpd)
    }

    fun saveVideoInfo(videoInfo: VideoInfo) {
        cachedVideos[videoInfo.originalUrl] = videoInfo
    }

    private fun getAndCacheRemoteVideo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        // Logic lấy video từ nguồn từ xa
        val videoInfo = fetchRemoteVideo(url, isM3u8OrMpd)
        if (videoInfo != null) {
            videoInfo.originalUrl = url.url.toString()
            cachedVideos[videoInfo.originalUrl] = videoInfo
        }
        return videoInfo
    }

    private fun fetchRemoteVideo(url: Request, isM3u8OrMpd: Boolean): VideoInfo {
        // Giả lập fetch video từ remote
        return VideoInfo(originalUrl = url.url.toString())
    }

}