package com.files.video.downloader.videoplayerdownloader.downloader.ui.media

import android.net.Uri
import androidx.databinding.ObservableField
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayMediaViewModel @Inject constructor() : BaseViewModel() {

    val videoName = ObservableField("")
    val videoUrl = ObservableField(Uri.EMPTY)
    val videoHeaders = ObservableField(emptyMap<String, String>())

    val stopPlayerEvent = SingleLiveEvent<Void?>()

    override fun start() {
    }

    override fun stop() {
        stopPlayerEvent.call()
    }
}