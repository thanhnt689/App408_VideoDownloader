package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab

import android.webkit.WebView
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.AdBlockHostsRemoteDataSource
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.HistoryRepositoryImpl
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import com.files.video.downloader.videoplayerdownloader.downloader.util.scheduler.BaseSchedulers
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class WebTabViewModel @Inject constructor(
    private val baseSchedulers: BaseSchedulers,
    private val adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource,
) : BaseViewModel() {
    val isTabInputFocused = ObservableBoolean(false)
    val changeTabFocusEvent = SingleLiveEvent<Boolean>()
    val thisTabIndex = ObservableInt(-1)
    val isDownloadDialogShown = ObservableBoolean(false)
    lateinit var tabPublishSubject: PublishSubject<String>
    var listTabSuggestions: ObservableField<MutableList<HistoryItem>> = ObservableField(
        mutableListOf()
    )
    val isShowProgress = ObservableBoolean(true)
    val progress = ObservableInt(0)
    val progressIcon = ObservableInt(R.drawable.ic_reload)

    val currentTitle = ObservableField("")
    var userAgent = ObservableField("")

    // This events from BrowserFragment
    lateinit var openPageEvent: SingleLiveEvent<WebTab>
    lateinit var closePageEvent: SingleLiveEvent<WebTab>

    val loadPageEvent = SingleLiveEvent<WebTab>()

    val tabUrl = ObservableField("")
    private var tabSuggestionJob: Job? = null

    var browserServicesProvider: BrowserServicesProvider? = null

    override fun start() {
        tabPublishSubject = PublishSubject.create()

//        isShowProgress.addOnPropertyChangedCallback(object :
//            Observable.OnPropertyChangedCallback() {
//            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//                when (isShowProgress.get()) {
//                    true -> progressIcon.set(R.drawable.ic_close)
//                    false -> progressIcon.set(R.drawable.ic_reload)
//                }
//            }
//        })
    }

    override fun stop() {
    }

    suspend fun setListHost() {
        adBlockHostsRemoteDataSource.setListHost()
    }

    fun isAd(url: String): Boolean {
        return adBlockHostsRemoteDataSource.isAds(url)
    }

    fun finishPage(url: String) {
        setTabTextInput(url, true)
        isShowProgress.set(false)
    }

    fun onStartPage(url: String, title: String?) {
        setTabTextInput(url)
        isShowProgress.set(true)
        currentTitle.set(title)
        tabUrl.set(url)
    }

    fun onUpdateVisitedHistory(url: String, title: String?, userAgent: String?) {
        if (url.startsWith("http")) {
            setTabTextInput(url)
            isShowProgress.set(true)
            tabUrl.set(url)
        }
    }

//    fun showTabSuggestions() {
//        if (tabSuggestionJob != null && tabSuggestionJob?.isActive == true) {
//            tabSuggestionJob?.cancel()
//        }
//        tabSuggestionJob = viewModelScope.launch(Dispatchers.IO) {
//            try {
//                withContext(this.coroutineContext) {
//                    val list = getListTabSuggestions().blockingFirst().reversed()
//                    if (list.size > 50) {
//                        listTabSuggestions.set(list.subList(0, 50).toMutableList())
//                    } else {
//                        listTabSuggestions.set(list.toMutableList())
//                    }
//                }
//            } catch (e: Throwable) {
//                e.printStackTrace()
//            }
//        }
//    }

//    private fun getListTabSuggestions(): Flowable<List<HistoryItem>> {
//        return Flowable.combineLatest(
//            tabPublishSubject.debounce(300, TimeUnit.MILLISECONDS)
//                .toFlowable(BackpressureStrategy.LATEST), historyRepository.getAllHistory().take(1)
//        ) { input, suggestions ->
//            tabUrl.set(input)
//
//            val listSuggestions = suggestions.filter { historyItem ->
//                historyItem.url.contains(
//                    input
//                )
//            }
//            listSuggestions.toList()
//        }.take(1).observeOn(baseSchedulers.single)
//            .subscribeOn(baseSchedulers.computation) // MAIN_TH
//    }

    fun changeTabFocus(isFocus: Boolean) {
        this.isTabInputFocused.set(isFocus)
        changeTabFocusEvent.value = isFocus
    }


    fun openPage(input: String) {
        if (input.isNotEmpty()) {
            changeTabFocus(false)
            openPageEvent.value = WebTabFactory.createWebTabFromInput(input)
        }
    }

    fun loadPage(input: String) {
        if (input.isNotEmpty()) {
            changeTabFocus(false)
            val tab = WebTabFactory.createWebTabFromInput(input)
            setTabTextInput(tab.getUrl())

            loadPageEvent.value = tab
        }
    }

    fun setTabTextInput(input: String?, isForce: Boolean = false) {
        if (input.isNullOrEmpty()) {
            return
        }

        if (!isTabInputFocused.get() || isForce) {
            tabUrl.set(input)
        }
    }

    fun getTabTextInput(): ObservableField<String> {
        return tabUrl
    }

    fun closeTab(webTab: WebTab) {
        closePageEvent.value = webTab
    }

    fun onPageReload(urlLoader: WebView?) {
        changeTabFocus(false)
        isShowProgress.set(true)

        urlLoader?.reload()
    }

    fun onPageStop(urlLoader: WebView?) {
        changeTabFocus(false)
        isShowProgress.set(false)
        urlLoader?.stopLoading()
    }

    fun onGoBack(webView: WebView) {
        changeTabFocus(false)
        isShowProgress.set(true)
        webView.goBack()
    }

    fun onGoForward(webView: WebView) {
        changeTabFocus(false)
        isShowProgress.set(true)
        webView.goForward()
    }

    fun setProgress(newProgress: Int) {
        progress.set(newProgress)
    }
}
