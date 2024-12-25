package com.files.video.downloader.videoplayerdownloader.downloader.base

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

abstract class BaseViewModel : ViewModel() {

    abstract fun start()

    abstract fun stop()
}