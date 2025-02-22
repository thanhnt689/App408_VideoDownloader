package com.files.video.downloader.videoplayerdownloader.downloader.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.AdHostDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.HistoryDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.ProgressDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.TabModelDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.VideoTaskItemDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.AdHost
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.DownloadUrlsConverter
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.FormatsConverter
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.ProgressInfo
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.Video
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem


const val DB_VERSION = 1

@Database(
    entities = [HistoryItem::class, AdHost::class, ProgressInfo::class, VideoTaskItem::class, TabModel::class],
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

    abstract fun tabModelDao(): TabModelDao

    abstract fun adHostDao(): AdHostDao

    abstract fun progressDao(): ProgressDao

    abstract fun videoTaskItemDao(): VideoTaskItemDao

}