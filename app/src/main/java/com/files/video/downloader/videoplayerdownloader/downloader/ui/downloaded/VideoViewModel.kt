package com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.VideoTaskItemRepository
import com.files.video.downloader.videoplayerdownloader.downloader.model.LocalVideo
import com.files.video.downloader.videoplayerdownloader.downloader.util.ContextUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

//@OpenForTesting
@HiltViewModel
class VideoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileUtil: FileUtil,
    private val videoTaskItemRepository: VideoTaskItemRepository,
) : BaseViewModel() {

    companion object {
        const val FILE_EXIST_ERROR_CODE = 1
        const val FILE_INVALID_ERROR_CODE = 2
    }

    var localVideos: ObservableField<MutableList<LocalVideo>> = ObservableField(mutableListOf())

    val renameErrorEvent = SingleLiveEvent<Int>()
    val shareEvent = SingleLiveEvent<Uri>()

    override fun start() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1000)
                val newList = getFilesList().toMutableList()
                newList.sortBy { it.uri }
                localVideos.set(newList)
            }
        }
    }


    override fun stop() {
    }

    private fun getFilesList(): List<LocalVideo> {
        val listVideos: MutableList<LocalVideo> = mutableListOf()
        fileUtil.listFiles.forEach { entry ->
            val fileUri = entry.value.second
            val fileSize = fileUtil.getContentLength(ContextUtils.getApplicationContext(), fileUri)
            val readableSize = FileUtil.getFileSizeReadable(fileSize.toDouble())
            val duration = formatDuration(FileUtil.getVideoDuration(context,fileUri))
            val date = FileUtil.getFileCreationDate(context,fileUri)
            val formatDate = if(date != null) formatDate(date.toLong()) else ""
            val video = LocalVideo(
                entry.value.first,
                fileUri,
                entry.key
            )
            video.size = readableSize
            video.duration = duration
            video.date = formatDate
            listVideos.add(video)
        }

        return listVideos.toList()
    }

    fun formatDuration(durationMillis: Long): String {
        val minutes = (durationMillis / 1000) / 60
        val seconds = (durationMillis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val sdf = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    fun deleteVideo(context: Context, video: LocalVideo) {
        localVideos.get()?.find { it.uri.path == video.uri.path }?.let {
            fileUtil.deleteMedia(context, video.uri)

            val list = localVideos.get()?.toMutableList()
            list?.remove(it)
            localVideos.set(list ?: mutableListOf())
        }
    }

    fun renameVideo(context: Context, uri: Uri, newName: String) {
        if (newName.isNotEmpty()) {
            val exists = fileUtil.isUriExists(context, uri)
            if (exists) {
                val isFileWithNameNotExists =
                    fileUtil.isFileWithNameNotExists(context, uri, newName)
                if (isFileWithNameNotExists) {
                    val newMediaNameUri = fileUtil.renameMedia(context, uri,newName)
                    if (newMediaNameUri != null) {
                        localVideos.get()?.find { it.uri.toString() == uri.toString() }?.let {
                            it.uri = newMediaNameUri.second
                            it.name = newMediaNameUri.first

                            Log.d("ntt", "renameVideo: newMediaNameUri.second ${{newMediaNameUri.second}}")

                            Log.d("ntt", "renameVideo: path:  ${newMediaNameUri.second.path}")

                            localVideos.get().let { list ->
                                list?.set(list.indexOf(it), it)
                            }
                        }
                        return
                    }
                }

                renameErrorEvent.value = FILE_EXIST_ERROR_CODE
                return
            }
        }

        renameErrorEvent.value = FILE_INVALID_ERROR_CODE
    }

    fun findVideoByName(downloadFilename: String?): Observable<LocalVideo> {
        return Observable.create { emitter ->
            val videos = getFilesList()
            val found =
                videos.find { it.name.contains(File(downloadFilename.toString()).name) }
            if (found != null) {
                emitter.onNext(found)
                emitter.onComplete()
            }
        }
    }
}