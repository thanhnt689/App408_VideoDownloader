package com.files.video.downloader.videoplayerdownloader.downloader.data.dao

import androidx.room.*
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import io.reactivex.rxjava3.core.Flowable

@Dao
interface HistoryDao {

    @Query("SELECT * FROM HistoryItem")
    fun getHistory(): Flowable<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistoryItem(item: HistoryItem)

    @Delete
    fun deleteHistoryItem(historyItem: HistoryItem)

    @Query("DELETE FROM HistoryItem")
    fun clear()
}