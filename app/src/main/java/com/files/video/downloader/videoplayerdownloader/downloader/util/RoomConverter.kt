package com.files.video.downloader.videoplayerdownloader.downloader.util

import androidx.room.TypeConverter
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.google.gson.Gson

class RoomConverter {

    @TypeConverter
    fun convertJsonToVideo(json: String): VideoInfo {
        return Gson().fromJson(json, VideoInfo::class.java)
    }

    @TypeConverter
    fun convertListVideosToJson(video: VideoInfo): String {
        return Gson().toJson(video)
    }
}