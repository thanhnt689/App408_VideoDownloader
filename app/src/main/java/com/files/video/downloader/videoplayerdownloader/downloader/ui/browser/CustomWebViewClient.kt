package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser

import android.graphics.Bitmap
import android.os.Build
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.IVideoDetector
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.PageTabProvider
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTab
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdBlockerHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.CookieUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.FaviconUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import com.files.video.downloader.videoplayerdownloader.downloader.util.VideoUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.CustomProxyController
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

enum class ContentType {
    M3U8,
    MPD,
    MP4,
    OTHER
}

class CustomWebViewClient(
    private val tabViewModel: WebTabViewModel,
    private val settingsModel: SettingsViewModel,
    private val videoDetectionModel: IVideoDetector,
    private val historyModel: HistoryViewModel,
    private val okHttpProxyClient: OkHttpProxyClient,
//    private val updateTabEvent: SingleLiveEvent<WebTab>,
//    private val pageTabProvider: PageTabProvider,
    private val proxyController: CustomProxyController
) : WebViewClient() {
    var videoAlert: MaterialAlertDialogBuilder? = null
    private var lastSavedHistoryUrl: String = ""
    private var lastSavedTitleHistory: String = ""
    private var lastRegularCheckUrl = ""
    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        val viewTitle = view?.title
        val title = tabViewModel.currentTitle.get()
        val userAgent = view?.settings?.userAgentString ?: tabViewModel.userAgent.get()

        if (url != null && lastSavedHistoryUrl != url) {
            historyModel.viewModelScope.launch(Dispatchers.IO) {
                val icon = try {
                    FaviconUtils.getEncodedFaviconFromUrl(
                        okHttpProxyClient.getProxyOkHttpClient(), url
                    )
                } catch (e: Throwable) {
                    null
                }
                saveUrlToHistory(url, icon, viewTitle ?: title)

                videoDetectionModel.onStartPage(
                    url,
                    userAgent
                        ?: BrowserFragment.MOBILE_USER_AGENT
                )
                tabViewModel.onUpdateVisitedHistory(
                    url,
                    title,
                    userAgent
                )
            }
        }
        super.doUpdateVisitedHistory(view, url, isReload)
    }

    // TODO handle for proxy and others
    override fun onReceivedHttpAuthRequest(
        view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?
    ) {
        val creds = proxyController.getProxyCredentials()
        handler?.proceed(creds.first, creds.second)
    }

    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {
        val isAdBlockerOn = settingsModel.isAdBlocker.get()
        val url = request?.url.toString()

        val isUrlAd: Boolean = isAdBlockerOn

        if (isUrlAd) {
            return AdBlockerHelper.createEmptyResource()
        }

        val isCheckM3u8 = settingsModel.isCheckIfEveryRequestOnM3u8.get()
        val isCheckOnMp4 = settingsModel.getIsCheckEveryRequestOnMp4Video().get()

        if (isCheckOnMp4 || isCheckM3u8) {
            val requestWithCookies = request?.let { resourceRequest ->
                try {
                    CookieUtils.webRequestToHttpWithCookies(
                        resourceRequest
                    )
                } catch (e: Throwable) {
                    null
                }
            }

            val contentType =
                VideoUtils.getContentTypeByUrl(url, requestWithCookies?.headers, okHttpProxyClient)

            when {

                contentType == ContentType.M3U8 || contentType == ContentType.MPD || url.contains(".m3u8") || url.contains(
                    ".mpd"
                ) || (url.contains(".txt") && url.contains("hentaihaven")) -> {
                    if (requestWithCookies != null && isCheckM3u8) {
                        videoDetectionModel.verifyLinkStatus(
                            requestWithCookies, tabViewModel.currentTitle.get(), true
                        )
                    }
                }

                else -> {
                    if (isCheckOnMp4) {
                        val disposable = videoDetectionModel.checkRegularMp4(requestWithCookies)

                        val currentUrl = tabViewModel.getTabTextInput().get() ?: ""
                        if (currentUrl != lastRegularCheckUrl) {
                            regularJobsStorage[lastRegularCheckUrl]?.forEach {
                                it.dispose()
                            }
                            regularJobsStorage.remove(lastRegularCheckUrl)
                            lastRegularCheckUrl = currentUrl
                        }
                        if (disposable != null) {
                            val overall = mutableListOf<Disposable>()
                            overall.addAll(regularJobsStorage[currentUrl]?.toList() ?: emptyList())
                            overall.add(disposable)
                            regularJobsStorage[currentUrl] = overall
                        }
                    }
                }
            }
        }

        return super.shouldInterceptRequest(
            view, request
        )
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

//        videoAlert = null
//        val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())
//        val headers = pageTab.getHeaders() ?: emptyMap()
//        val favi = pageTab.getFavicon() ?: view.favicon ?: favicon
//
//        updateTabEvent.value = WebTab(
//            url,
//            view.title,
//            favi,
//            headers,
//            view,
//            id = pageTab.id
//        )
//        tabViewModel.onStartPage(url, view.title)
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: WebResourceRequest): Boolean {
        val isAdBlockerOn = settingsModel.isAdBlocker.get()
        val isAd = false

        return if (url.url.toString().startsWith("http") && url.isForMainFrame && !isAd) {
            if (!tabViewModel.isTabInputFocused.get()) {
                tabViewModel.setTabTextInput(url.url.toString())
            }
            false
        } else {
            true
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        tabViewModel.finishPage(url)
    }

    override fun onRenderProcessGone(
        view: WebView?, detail: RenderProcessGoneDetail?
    ): Boolean {
//        val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())
//
//        val webView = pageTab.getWebView()
//        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                view == webView && detail?.didCrash() == true
//            } else {
//                view == webView
//            }) {
//            webView?.destroy()
//            return true
//        }

        return super.onRenderProcessGone(view, detail)
    }

    private suspend fun saveUrlToHistory(url: String, favicon: Bitmap?, title: String?) {
        val isTitleEmpty = title?.trim()?.isEmpty() == true

        if (!isTitleEmpty && lastSavedTitleHistory != title && lastSavedHistoryUrl != url && url.isNotEmpty() && !url.contains(
                "about:blank"
            )
        ) {
            lastSavedHistoryUrl = url
            lastSavedTitleHistory = title ?: ""

            val outputFavicon = FaviconUtils.bitmapToBytes(favicon)

            yield()

            historyModel.saveHistory(
                HistoryItem(
                    url = url, favicon = outputFavicon, title = title
                )
            )
        }
    }
}