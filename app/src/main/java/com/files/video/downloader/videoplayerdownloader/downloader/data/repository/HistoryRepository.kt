package com.files.video.downloader.videoplayerdownloader.downloader.data.repository

import androidx.lifecycle.LiveData
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.HistoryDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.di.qualifier.LocalData
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

//interface HistoryRepository {
//    fun getAllHistory(): Flowable<List<HistoryItem>>
//
//    fun saveHistory(history: HistoryItem)
//
//    fun deleteHistory(history: HistoryItem)
//
//    fun deleteAllHistory()
//}

class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) {

    fun getAllHistory(): LiveData<List<HistoryItem>> {
        return historyDao.getHistory()
    }

    suspend fun saveHistory(history: HistoryItem) {
        historyDao.insertHistoryItem(history)
    }

    fun deleteHistory(history: HistoryItem) {
        historyDao.deleteHistoryItem(history)
    }

    fun deleteAllHistory() {
        historyDao.clear()
    }

}