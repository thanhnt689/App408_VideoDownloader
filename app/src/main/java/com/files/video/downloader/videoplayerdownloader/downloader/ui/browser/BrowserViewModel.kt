package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser

import androidx.databinding.*
import androidx.lifecycle.MutableLiveData
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.HOME_TAB_INDEX
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTab
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import javax.inject.Inject

//@OpenForTesting
class BrowserViewModel @Inject constructor() : BaseViewModel() {

    companion object {
        const val SEARCH_URL = "https://www.google.com/search?q=%s"

        var instance: BrowserViewModel? = null
    }

    var settingsModel: SettingsViewModel? = null

    val openPageEvent = SingleLiveEvent<WebTab>()

    val closePageEvent = SingleLiveEvent<WebTab>()

    val selectWebTabEvent = SingleLiveEvent<WebTab>()

    val updateWebTabEvent = SingleLiveEvent<WebTab>()

    val workerM3u8MpdEvent = MutableLiveData<DownloadButtonState>()

    val workerMP4Event = MutableLiveData<DownloadButtonState>()

    val progress = ObservableInt(0)

    val changeSearchFocusEvent = SingleLiveEvent<Boolean>()

    val tabs = ObservableField(listOf(WebTab.HOME_TAB))

    val currentTab = ObservableInt(HOME_TAB_INDEX)

    override fun start() {
        instance = this
    }

    override fun stop() {
        instance = null
    }
}

abstract class DownloadButtonState

class DownloadButtonStateLoading : DownloadButtonState()

class DownloadButtonStateCanDownload(val info: VideoInfo?) : DownloadButtonState()
class DownloadButtonStateCanNotDownload : DownloadButtonState()