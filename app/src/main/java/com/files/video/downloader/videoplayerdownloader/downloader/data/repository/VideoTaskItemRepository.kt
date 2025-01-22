package com.files.video.downloader.videoplayerdownloader.downloader.data.repository

import androidx.lifecycle.LiveData
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.HistoryDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.VideoTaskItemDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.di.qualifier.LocalData
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

class VideoTaskItemRepository @Inject constructor(
    private val videoTaskItemDao: VideoTaskItemDao
) {

    fun getAllVideoTaskItem(): LiveData<List<VideoTaskItem>> {
        return videoTaskItemDao.getVideoTaskItem()
    }

    suspend fun insertVideoTaskItem(videoTaskItem: VideoTaskItem) {
        videoTaskItemDao.insertVideoTaskItem(videoTaskItem)
    }

    fun deleteVideoTaskItem(videoTaskItem: VideoTaskItem) {
        videoTaskItemDao.deleteVideoTaskItem(videoTaskItem)
    }

    suspend fun updateIsCheckVerse(id: Int, isSecurity: Boolean) {
        videoTaskItemDao.updateIsSecurity(id, isSecurity)
    }

}