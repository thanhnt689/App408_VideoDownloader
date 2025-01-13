package com.files.video.downloader.videoplayerdownloader.downloader.ui.processing

import androidx.annotation.VisibleForTesting
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.ProgressInfo
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.ProgressRepository
import com.files.video.downloader.videoplayerdownloader.downloader.util.ContextUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.custom_downloader.CustomRegularDownloader
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskState
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.youtubedl_downloader.YoutubeDlDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

//@OpenForTesting
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val fileUtil: FileUtil,
    private val progressRepository: ProgressRepository,
) : BaseViewModel() {
    @VisibleForTesting
    internal val compositeDisposable: CompositeDisposable = CompositeDisposable()

    var progressInfos: ObservableField<List<ProgressInfo>> = ObservableField(emptyList())
    private val executor = Executors.newFixedThreadPool(3).asCoroutineDispatcher()
    private val executor2 = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

    override fun start() {
        downloadProgressStartListen()
    }

    override fun stop() {
        compositeDisposable.clear()
    }

    // TODO: strange, should fix
    fun stopAndSaveDownload(id: Long) {
        val inf = progressInfos.get()?.find { it.downloadId == id }

        if (inf?.videoInfo?.isRegularDownload == false) {
            inf.let {
                YoutubeDlDownloader.stopAndSaveDownload(
                    ContextUtils.getApplicationContext(), it
                )
            }
        } else {
            // TODO For Regular
        }
    }

    fun cancelDownload(id: Long, removeFile: Boolean) {
        val inf = progressInfos.get()?.find { it.downloadId == id }
        inf?.let { progressInfo ->
            deleteProgressInfo(progressInfo) { info ->
                if (info.videoInfo.isRegularDownload) {
                    CustomRegularDownloader.cancelDownload(
                        ContextUtils.getApplicationContext(),
                        inf,
                        removeFile
                    )
                } else {
                    info.let {
                        YoutubeDlDownloader.cancelDownload(
                            ContextUtils.getApplicationContext(), it, removeFile
                        )
                    }
                }
                val newList = progressInfos.get()?.filter { it.id != info.id }
                progressInfos.set(newList?.sortedBy { it.id })
            }
        }
    }

    fun pauseDownload(id: Long) {
        val inf = progressInfos.get()?.find { it.downloadId == id }

        if (inf?.videoInfo?.isRegularDownload == true) {
            CustomRegularDownloader.pauseDownload(ContextUtils.getApplicationContext(), inf)
        } else {
            val updated = inf?.copy(downloadStatus = VideoTaskState.PAUSE)
            if (updated != null) {
                saveProgressInfo(updated) { info ->
                    YoutubeDlDownloader.pauseDownload(ContextUtils.getApplicationContext(), info)
                }
            }
        }
    }

    fun resumeDownload(id: Long) {
        val inf = progressInfos.get()?.find { it.downloadId == id }

        if (inf?.videoInfo?.isRegularDownload == true) {
            CustomRegularDownloader.resumeDownload(ContextUtils.getApplicationContext(), inf)
        } else {
            inf?.let {
                val updated = inf.copy(downloadStatus = VideoTaskState.PREPARE)

                saveProgressInfo(updated) { info ->
                    YoutubeDlDownloader.resumeDownload(
                        ContextUtils.getApplicationContext(),
                        info
                    )
                }
            }
        }
    }

    fun downloadVideo(videoInfo: VideoInfo?) {
        val context = ContextUtils.getApplicationContext()

        videoInfo?.let {
            if (!fileUtil.folderDir.exists() && !fileUtil.folderDir.mkdirs()) {
                return
            }

            val downloadId = videoInfo.id.hashCode().toLong()
            val progressInfo = ProgressInfo(
                id = videoInfo.id,
                downloadId = downloadId,
                videoInfo = videoInfo,
                isM3u8 = videoInfo.isM3u8
            )

            saveProgressInfo(progressInfo) { info ->
                if (info.videoInfo.isRegularDownload) {
                    CustomRegularDownloader.addDownload(context, info.videoInfo)
                } else {
                    YoutubeDlDownloader.startDownload(context, info.videoInfo)
                }
            }
        }
    }

    private fun saveProgressInfo(
        progressInfo: ProgressInfo,
        onSuccess: (ProgressInfo) -> Unit = {}
    ) {
        viewModelScope.launch(executor2) {
            progressRepository.saveProgressInfo(progressInfo)
            onSuccess(progressInfo)
        }
    }

    private fun deleteProgressInfo(
        progressInfo: ProgressInfo,
        onSuccess: (ProgressInfo) -> Unit = {}
    ) {
        viewModelScope.launch(executor2) {
            progressRepository.deleteProgressInfo(progressInfo)
            onSuccess(progressInfo)
        }
    }

    @VisibleForTesting
    internal fun downloadProgressStartListen() {
        viewModelScope.launch(executor) {
            progressObservable().doOnError {
                it.printStackTrace()
            }.blockingForEach { progressInfoList ->
                progressInfos.set(progressInfoList.sortedBy { it.id })
            }
        }
    }

    private fun progressObservable(): Observable<List<ProgressInfo>> {
        val youtubeDlDownloads = Observable.interval(1000, TimeUnit.MILLISECONDS).flatMap {
            progressRepository.getProgressInfos().take(1).flatMap {
                val filtered = it.filter { info -> info.downloadStatus != VideoTaskState.SUCCESS }

                // Don't TOUCH(если убрать это возникнет конфликт ID-ков и не будет показываться прогресс для обычных загрузок)
                //////////////////////////////
                val successed = it.filter { info -> info.downloadStatus == VideoTaskState.SUCCESS }
                for (task in successed) {
                    progressRepository.deleteProgressInfo(task)
                }
                /////////////////////////////
                Observable.just(filtered).toFlowable(BackpressureStrategy.LATEST).take(1)
            }.toObservable().doOnError { error ->
                error.printStackTrace()
            }
        }

        return youtubeDlDownloads
    }
}
