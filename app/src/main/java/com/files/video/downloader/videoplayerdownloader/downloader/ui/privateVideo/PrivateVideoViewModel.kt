package com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.HistoryRepositoryImpl
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.VideoTaskItemRepository
import com.files.video.downloader.videoplayerdownloader.downloader.model.LocalVideo
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoViewModel.Companion
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import com.files.video.downloader.videoplayerdownloader.downloader.util.SortState
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.Video
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class PrivateVideoViewModel @Inject constructor(
    private val videoTaskItemRepository: VideoTaskItemRepository,
    private val fileUtil: FileUtil,
) :
    ViewModel() {

    val sortStateObservable: MutableLiveData<SortState> = MutableLiveData(SortState.NAME)

    val fileTabLiveData: MutableLiveData<Int> = MutableLiveData(0)

    companion object {
        const val FILE_EXIST_ERROR_CODE = 1
        const val FILE_INVALID_ERROR_CODE = 2
    }

    val renameErrorEvent = SingleLiveEvent<Int>()
    val shareEvent = SingleLiveEvent<Uri>()

    val searchCharObservable: MutableLiveData<String> = MutableLiveData("")
    val fileType: MutableLiveData<String> = MutableLiveData("")

    suspend fun queryVideoTaskItem(): LiveData<List<VideoTaskItem>> {
        return sortStateObservable.switchMap { sortState ->
            return@switchMap searchCharObservable.switchMap { query ->
                return@switchMap fileType.switchMap { type ->
                    videoTaskItemRepository.queryVideoTaskItem(
                        type == "all",
                        type,
                        query,
                        sortState.value
                    )
                }

            }
        }
    }

    suspend fun queryVideoTaskItemSecurity(fileType: String): LiveData<List<VideoTaskItem>> {
        return sortStateObservable.switchMap { sortState ->
            return@switchMap searchCharObservable.switchMap { query ->
                videoTaskItemRepository.queryVideoTaskItemSecurity(
                    fileType == "all",
                    fileType,
                    query,
                    sortState.value
                )
            }
        }
    }

    suspend fun insertVideoTaskItem(videoTaskItem: VideoTaskItem) {
        videoTaskItemRepository.insertVideoTaskItem(videoTaskItem)
    }


    suspend fun updateIsCheckSecurity(id: String, isSecurity: Boolean) {
        videoTaskItemRepository.updateIsCheckSecurity(id, isSecurity)
    }

    suspend fun renameVideo(context: Context, id: String, filePath: String, newName: String) {
        if (newName.isNotEmpty()) {
//            val exists = fileUtil.isUriExists(context, uri)
//            if (exists) {
//                val isFileWithNameNotExists =
//                    fileUtil.isFileWithNameNotExists(context, uri, newName)
//                if (isFileWithNameNotExists) {
//                    val newMediaNameUri = fileUtil.renameMedia(context, uri, newName)
//                    if (newMediaNameUri != null) {
//                        newMediaNameUri.second.path?.let {
//                            videoTaskItemRepository.updateNameVideoTaskItem(
//                                id, newMediaNameUri.first,
//                                it
//                            )
//                        }
//                        return
//                    }
//                }
//
//                renameErrorEvent.value = VideoViewModel.FILE_EXIST_ERROR_CODE
//                return
//            }

            val newMediaNameUri = fileUtil.renameMedia(context, filePath, newName)
            if (newMediaNameUri != null) {
                newMediaNameUri.second.let {
                    videoTaskItemRepository.updateNameVideoTaskItem(
                        id, newMediaNameUri.first,
                        it
                    )
                }

                return

                renameErrorEvent.value = VideoViewModel.FILE_EXIST_ERROR_CODE

            }
        }

        renameErrorEvent.value = VideoViewModel.FILE_INVALID_ERROR_CODE
    }

    suspend fun renameImage(context: Context, id: String, filePath: String, newName: String) {
        if (newName.isNotEmpty()) {

            val newMediaNameUri = fileUtil.renameImage(context, filePath, newName)
            if (newMediaNameUri != null) {
                newMediaNameUri.second.let {
                    videoTaskItemRepository.updateNameVideoTaskItem(
                        id, newMediaNameUri.first,
                        it
                    )
                }

                return

                renameErrorEvent.value = VideoViewModel.FILE_EXIST_ERROR_CODE

            }
        }

        renameErrorEvent.value = VideoViewModel.FILE_INVALID_ERROR_CODE
    }

//    fun queryVideoTaskItem(): LiveData<List<VideoTaskItem>> {
//        return videoTaskItemRepository.getAllVideoTaskItem()
//    }

    suspend fun deleteVideoTaskItem(videoTaskItem: VideoTaskItem) {

        videoTaskItemRepository.deleteVideoTaskItem(videoTaskItem)

    }


    fun queryVideoSecurityTaskItem(): LiveData<List<VideoTaskItem>> {
        return videoTaskItemRepository.getAllVideoSecurityTaskItem()
    }

    fun findVideoTaskItemByName(name: String): VideoTaskItem {
        return videoTaskItemRepository.findVideoTaskItemByName(name)
    }

    suspend fun resetSecurityFlag() {
        videoTaskItemRepository.resetSecurityFlag()
    }

    fun findVideoByName(downloadFilename: String?): Observable<VideoTaskItem> {
        return Observable.create { emitter ->
            val found =
                downloadFilename?.let { findVideoTaskItemByName(it) }
            if (found != null) {
                emitter.onNext(found)
                emitter.onComplete()
            }
        }
    }
}