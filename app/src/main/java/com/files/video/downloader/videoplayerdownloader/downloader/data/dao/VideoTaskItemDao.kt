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

    @Query("SELECT * FROM VideoTaskItem WHERE is_security = 0")
    fun getVideoTaskItem(): LiveData<List<VideoTaskItem>>

    @Query("SELECT * FROM VideoTaskItem")
    fun getAllVideoTaskItems(): List<VideoTaskItem>

    @Delete
    fun deleteVideoTaskItems(videoTaskItems: List<VideoTaskItem>)

    @Query("SELECT * FROM VideoTaskItem WHERE is_security = 1")
    fun getVideoSecurityTaskItem(): LiveData<List<VideoTaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVideoTaskItem(videoTaskItem: VideoTaskItem)

    @Query(
        """SELECT * FROM VideoTaskItem 
       WHERE (:isAll = 1 OR mime_type = :typeItem) 
             AND title LIKE :textSearch 
             AND is_security = 0
       ORDER BY  
                CASE WHEN :typeSort = 1 THEN file_name COLLATE NOCASE COLLATE UNICODE END ASC,
                CASE WHEN :typeSort = 2 THEN file_name COLLATE NOCASE COLLATE UNICODE END DESC,
                CASE WHEN :typeSort = 3 THEN file_date END ASC,
                CASE WHEN :typeSort = 4 THEN file_date END DESC,
                CASE WHEN :typeSort = 5 THEN CAST(file_size AS LONG) END ASC,
                CASE WHEN :typeSort = 6 THEN CAST(file_size AS LONG) END DESC
       """
    )
    fun getLiveDataVideoTaskItemByTextSearch(
        isAll: Int,
        typeItem: String?,
        textSearch: String,
        typeSort: Int
    ): LiveData<List<VideoTaskItem>>

    @Query(
        """SELECT * FROM VideoTaskItem 
       WHERE (:isAll = 1 OR mime_type = :typeItem) 
             AND title LIKE :textSearch 
             AND is_security = 1
       ORDER BY  
                CASE WHEN :typeSort = 1 THEN file_name COLLATE NOCASE COLLATE UNICODE END ASC,
                CASE WHEN :typeSort = 2 THEN file_name COLLATE NOCASE COLLATE UNICODE END DESC,
                CASE WHEN :typeSort = 3 THEN file_date END ASC,
                CASE WHEN :typeSort = 4 THEN file_date END DESC,
                CASE WHEN :typeSort = 5 THEN CAST(file_size AS LONG) END ASC,
                CASE WHEN :typeSort = 6 THEN CAST(file_size AS LONG) END DESC
       """
    )
    fun getLiveDataVideoTaskItemSecurityByTextSearch(
        isAll: Boolean?,
        typeItem: String?,
        textSearch: String,
        typeSort: Int
    ): LiveData<List<VideoTaskItem>>

    @Delete
    suspend fun deleteVideoTaskItem(videoTaskItem: VideoTaskItem)

    @Query("UPDATE VideoTaskItem SET is_security = :isSecurity WHERE _id = :id")
    suspend fun updateIsSecurity(id: String, isSecurity: Boolean)

    @Query("UPDATE VideoTaskItem SET file_name = :newName, file_path = :newPath WHERE _id = :id")
    suspend fun updateNameVideoTaskItem(id: String, newName: String, newPath: String)

    @Query("SELECT COUNT(*) FROM VideoTaskItem WHERE file_name = :newName AND mime_type = 'video'")
    suspend fun isFileNameVideoExists(newName: String): Int

    @Query("SELECT COUNT(*) FROM VideoTaskItem WHERE file_name = :newName AND mime_type = 'image'")
    suspend fun isFileNameImageExists(newName: String): Int

    @Query("SELECT * FROM VideoTaskItem WHERE file_name = :name AND is_security = 0")
    fun findVideoTaskItemByName(name: String): VideoTaskItem

    @Query("UPDATE VideoTaskItem SET is_security = 0")
    suspend fun resetSecurityFlag()
}