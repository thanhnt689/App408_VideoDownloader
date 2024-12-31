package com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service

import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.VideoRepository
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRemoteDataSource @Inject constructor(
    private val videoService: VideoService
) : VideoRepository {

    override fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        return videoService.getVideoInfo(url, isM3u8OrMpd)?.videoInfo
    }

    override fun saveVideoInfo(videoInfo: VideoInfo) {
    }
}