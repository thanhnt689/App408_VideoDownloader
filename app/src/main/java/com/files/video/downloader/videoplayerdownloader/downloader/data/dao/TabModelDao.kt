package com.files.video.downloader.videoplayerdownloader.downloader.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import io.reactivex.rxjava3.core.Flowable

@Dao
interface TabModelDao {

    @Query("SELECT * FROM TabModel")
    fun getAllTabModel(): LiveData<List<TabModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTabModel(item: TabModel)

    @Delete
    fun deleteTabModel(item: TabModel)

    @Query("UPDATE TabModel SET _url = :newUrl, _is_selected = :isSelected,_favicon = :favicon WHERE _id = :id")
    suspend fun updateInfoTabModel(
        id: String,
        newUrl: String,
        favicon: ByteArray?,
        isSelected: Boolean
    )

    @Query("SELECT COUNT(*) FROM TabModel")
    suspend fun getItemCount(): Int

    @Query("SELECT * FROM TabModel WHERE _is_selected = 1 LIMIT 1")
    suspend fun getSelectedTabModel(): TabModel

    @Query("UPDATE TabModel SET _is_selected = 0 WHERE _is_selected = 1")
    suspend fun updateAllTabsToUnselected()

    @Transaction
    suspend fun insertAndUnselectOthers(item: TabModel) {
        updateAllTabsToUnselected()
        insertTabModel(item)
    }

    @Transaction
    suspend fun updateAndUnselectOthers(
        id: String,
        newUrl: String,
        favicon: ByteArray?,
        isSelected: Boolean
    ) {
        updateAllTabsToUnselected()
        updateInfoTabModel(id, newUrl, favicon, isSelected)
    }

    @Query("SELECT * FROM TabModel WHERE _id = :id")
    fun findVideoTaskItemByName(id: String): TabModel

    @Query("DELETE FROM TabModel")
    suspend fun clear()

}