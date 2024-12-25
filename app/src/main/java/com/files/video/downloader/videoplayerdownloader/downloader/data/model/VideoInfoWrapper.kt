package com.files.video.downloader.videoplayerdownloader.downloader.data.model

import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class VideoInfoWrapper(
    @SerializedName("info")
    @Expose
    var videoInfo: VideoInfo?
)