package com.files.video.downloader.videoplayerdownloader.downloader.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.AdHostDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.HistoryDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.ProgressDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.AdHost
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.DownloadUrlsConverter
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.FormatsConverter
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.ProgressInfo
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo


const val DB_VERSION = 1

@Database(
    entities = [VideoInfo::class, HistoryItem::class, AdHost::class, ProgressInfo::class],
    version = DB_VERSION,
)
@TypeConverters(FormatsConverter::class, DownloadUrlsConverter::class)
abstract class AppDatabase : RoomDatabase() {

//    abstract fun configDao(): ConfigDao
//
//    abstract fun videoDao(): VideoDao
//
//    abstract fun progressDao(): ProgressDao
//
//    abstract fun pageDao(): PageDao

    abstract fun historyDao(): HistoryDao

    abstract fun adHostDao(): AdHostDao

    abstract fun progressDao(): ProgressDao

}