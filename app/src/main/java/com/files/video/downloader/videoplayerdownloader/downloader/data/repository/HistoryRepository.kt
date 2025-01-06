package com.files.video.downloader.videoplayerdownloader.downloader.data.repository

import androidx.lifecycle.LiveData
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.HistoryDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.di.qualifier.LocalData
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) {

    fun getAllHistory(): LiveData<List<HistoryItem>> {
        return historyDao.getHistory()
    }

    suspend fun saveHistory(history: HistoryItem) {
        historyDao.insertHistoryItem(history)

        val itemCount = historyDao.getItemCount()
        if (itemCount > 400) {
            val itemsToDelete = itemCount - 400
            historyDao.deleteOldItems(itemsToDelete)
        }
    }

    fun deleteHistory(history: HistoryItem) {
        historyDao.deleteHistoryItem(history)
    }

    fun deleteAllHistory() {
        historyDao.clear()
    }

    fun queryHistoryItem(
        textSearch: String,
    ): LiveData<List<HistoryItem>> {
        return historyDao
            .getLiveDataHistoryByTextSearch("%" + textSearch.trim() + "%")
    }

    fun queryBookmarkItem(
        textSearch: String,
    ): LiveData<List<HistoryItem>> {
        return historyDao
            .getLiveDataBookmarkByTextSearch("%" + textSearch.trim() + "%")
    }

}