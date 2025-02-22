package com.files.video.downloader.videoplayerdownloader.downloader.data.repository

import androidx.lifecycle.LiveData
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.HistoryDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.TabModelDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.di.qualifier.LocalData
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabModel
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

class TabModelRepository @Inject constructor(
    private val tabModelDao: TabModelDao
) {

    fun getAllTabModel(): LiveData<List<TabModel>> {
        return tabModelDao.getAllTabModel()
    }

    suspend fun insertTabModel(item: TabModel) {

        tabModelDao.insertAndUnselectOthers(item)

    }

    fun deleteTabModel(item: TabModel) {
        tabModelDao.deleteTabModel(item)
    }

    suspend fun clearAllTabModel() {
        tabModelDao.clear()
    }

    suspend fun updateInfoTabModel(
        id: String,
        newUrl: String,
        favicon: ByteArray?,
        isSelected: Boolean
    ) {
        tabModelDao.updateAndUnselectOthers(id, newUrl, favicon, isSelected)
    }

    suspend fun getSelectedTabModel(): TabModel {
        return tabModelDao.getSelectedTabModel()
    }

    fun findVideoTaskItemByName(id: String): TabModel {
        return tabModelDao.findVideoTaskItemByName(id)
    }
}