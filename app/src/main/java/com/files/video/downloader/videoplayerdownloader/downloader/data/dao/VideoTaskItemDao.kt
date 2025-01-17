package com.files.video.downloader.videoplayerdownloader.downloader.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import io.reactivex.rxjava3.core.Flowable

@Dao
interface VideoTaskItemDao {

    @Query("SELECT * FROM VideoTaskItem")
    fun getVideoTaskItem(): LiveData<List<VideoTaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideoTaskItem(videoTaskItem: VideoTaskItem)

    @Delete
    fun deleteVideoTaskItem(videoTaskItem: VideoTaskItem)
}