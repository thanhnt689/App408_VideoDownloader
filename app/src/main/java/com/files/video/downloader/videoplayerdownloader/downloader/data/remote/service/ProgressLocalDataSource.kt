package com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service

import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.ProgressDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.ProgressInfo
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.ProgressRepository
import io.reactivex.rxjava3.core.Flowable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressLocalDataSource @Inject constructor(
    private val progressDao: ProgressDao
) : ProgressRepository {

    override fun getProgressInfos(): Flowable<List<ProgressInfo>> {
        return progressDao.getProgressInfos()
    }

    override fun saveProgressInfo(progressInfo: ProgressInfo) {
        progressDao.insertProgressInfo(progressInfo)
    }

    override fun deleteProgressInfo(progressInfo: ProgressInfo) {
        progressDao.deleteProgressInfo(progressInfo)
    }
}