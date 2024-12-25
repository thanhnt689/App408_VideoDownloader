package com.files.video.downloader.videoplayerdownloader.downloader.data

import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.HistoryDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.HistoryRepository
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryLocalDataSource @Inject constructor(private val historyDao: HistoryDao) :
    HistoryRepository {
    override fun getAllHistory(): Flowable<List<HistoryItem>> {
        return  historyDao.getHistory()
    }

    override fun saveHistory(history: HistoryItem) {
        historyDao.insertHistoryItem(history)
    }

    override fun deleteHistory(history: HistoryItem) {
        historyDao.deleteHistoryItem(history)
    }

    override fun deleteAllHistory() {
        historyDao.clear()
    }
}