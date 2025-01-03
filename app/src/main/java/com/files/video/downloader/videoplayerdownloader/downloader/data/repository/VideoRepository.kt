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

class VideoRepositoryImpl() {

    var cachedVideos: MutableMap<String, VideoInfo> = mutableMapOf()

    fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        return cachedVideos[url.url.toString()] ?: getAndCacheRemoteVideo(url, isM3u8OrMpd)
    }

    fun saveVideoInfo(videoInfo: VideoInfo) {
        cachedVideos[videoInfo.originalUrl] = videoInfo
    }

    private fun getAndCacheRemoteVideo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        // Thực hiện xử lý lấy video từ nguồn bên ngoài
        val videoInfo = fetchRemoteVideoInfo(url, isM3u8OrMpd)
        if (videoInfo != null) {
            videoInfo.originalUrl = url.url.toString()
            cachedVideos[videoInfo.originalUrl] = videoInfo
        }
        return videoInfo
    }

    private fun fetchRemoteVideoInfo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        // Giả lập việc fetch thông tin video từ server hoặc nguồn bên ngoài
        return VideoInfo(url.url.toString()) // Thay bằng logic thực tế
    }
}