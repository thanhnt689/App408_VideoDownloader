package com.files.video.downloader.videoplayerdownloader.downloader.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import io.reactivex.rxjava3.core.Flowable

@Dao
interface HistoryDao {

    @Query("SELECT * FROM HistoryItem")
    fun getHistory(): LiveData<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistoryItem(item: HistoryItem)

    @Delete
    fun deleteHistoryItem(historyItem: HistoryItem)

    @Query("DELETE FROM HistoryItem WHERE isBookmark = 0")
    fun clear()

    @Query(
        """SELECT * FROM HistoryItem 
       WHERE title LIKE :textSearch  AND isBookmark = 0
       """
    )
    fun getLiveDataHistoryByTextSearch(textSearch: String?): LiveData<List<HistoryItem>>

    @Query(
        """SELECT * FROM HistoryItem 
       WHERE title LIKE :textSearch  AND isBookmark = 1
       """
    )
    fun getLiveDataBookmarkByTextSearch(textSearch: String?): LiveData<List<HistoryItem>>

    @Query("DELETE FROM HistoryItem WHERE id IN (SELECT id FROM historyitem ORDER BY id ASC LIMIT :count)")
    suspend fun deleteOldItems(count: Int)

    @Query("SELECT COUNT(*) FROM HistoryItem")
    suspend fun getItemCount(): Int
}