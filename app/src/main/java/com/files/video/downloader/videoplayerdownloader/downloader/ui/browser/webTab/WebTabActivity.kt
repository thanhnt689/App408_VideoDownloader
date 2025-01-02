package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityWebTabBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.BrowserFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.ContentType
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonState
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.SettingsViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.DetectedVideosTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.VideoDetectionAlgVModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabsActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdBlockerHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.CookieUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.FaviconUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import com.files.video.downloader.videoplayerdownloader.downloader.util.VideoUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.CustomProxyController
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

interface BrowserServicesProvider : TabManagerProvider, PageTabProvider, HistoryProvider,
    WorkerEventProvider, CurrentTabIndexProvider

interface TabManagerProvider {
    fun getOpenTabEvent(): SingleLiveEvent<WebTab>

    fun getCloseTabEvent(): SingleLiveEvent<WebTab>

    fun getUpdateTabEvent(): SingleLiveEvent<WebTab>

    fun getTabsListChangeEvent(): ObservableField<List<WebTab>>
}

interface PageTabProvider {
    fun getPageTab(position: Int): WebTab
}

interface HistoryProvider {
    fun getHistoryVModel(): HistoryViewModel
}

interface WorkerEventProvider {
    fun getWorkerM3u8MpdEvent(): MutableLiveData<DownloadButtonState>

    fun getWorkerMP4Event(): MutableLiveData<DownloadButtonState>
}

interface CurrentTabIndexProvider {
    fun getCurrentTabIndex(): ObservableInt
}

interface BrowserListener {
    fun onBrowserMenuClicked()

    fun onBrowserReloadClicked()

    fun onTabCloseClicked()

    fun onBrowserStopClicked()

    fun onBrowserBackClicked()

    fun onBrowserForwardClicked()
}

const val HOME_TAB_INDEX = 0

const val TAB_INDEX_KEY = "TAB_INDEX_KEY"

@AndroidEntryPoint
class WebTabActivity : BaseActivity<ActivityWebTabBinding>() {

    private lateinit var webTab: WebTab

    private val tabViewModel: WebTabViewModel by viewModels()

    private val settingsViewModel: SettingsViewModel by viewModels()

    //
    private val historyViewModel: HistoryViewModel by viewModels()

    //
    private val videoDetectionTabViewModel: DetectedVideosTabViewModel by viewModels()

    private val videoDetectionModel: VideoDetectionAlgVModel by viewModels()


    private lateinit var workerEventProvider: WorkerEventProvider

    private lateinit var historyItemCurrent: HistoryItem

    private var bundle: Bundle? = null

    private var isReload = false

    @Inject
    lateinit var appUtil: AppUtil

    @Inject
    lateinit var proxyController: CustomProxyController

    @Inject
    lateinit var okHttpProxyClient: OkHttpProxyClient

    var videoAlert: MaterialAlertDialogBuilder? = null
    private var lastSavedHistoryUrl: String = ""
    private var lastSavedTitleHistory: String = ""
    private var lastRegularCheckUrl = ""
    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()

    private var webViewClient = object : WebViewClient() {
        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            val viewTitle = view?.title
            val title = tabViewModel.currentTitle.get()
            val userAgent = view?.settings?.userAgentString ?: BrowserFragment.MOBILE_USER_AGENT

            if (url != null && lastSavedHistoryUrl != url) {
                lifecycleScope.launch(Dispatchers.IO) {
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

        override fun onReceivedHttpAuthRequest(
            view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?
        ) {
            val creds = proxyController.getProxyCredentials()
            handler?.proceed(creds.first, creds.second)
        }

        override fun shouldInterceptRequest(
            view: WebView?, request: WebResourceRequest?
        ): WebResourceResponse? {
            val isAdBlockerOn = false
            val url = request?.url.toString()

            val isUrlAd: Boolean = isAdBlockerOn

            if (isUrlAd) {
                return AdBlockerHelper.createEmptyResource()
            }

//            val isCheckM3u8 = settingsModel.isCheckIfEveryRequestOnM3u8.get()
//            val isCheckOnMp4 = settingsModel.getIsCheckEveryRequestOnMp4Video().get()

            var isCheckM3u8 = true
            var isCheckOnMp4 = true

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
                    VideoUtils.getContentTypeByUrl(
                        url,
                        requestWithCookies?.headers,
                        okHttpProxyClient
                    )

                when {

                    contentType == ContentType.M3U8 || contentType == ContentType.MPD || url.contains(
                        ".m3u8"
                    ) || url.contains(
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
                                overall.addAll(
                                    regularJobsStorage[currentUrl]?.toList() ?: emptyList()
                                )
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

            videoAlert = null

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

            val pageTab = tabViewModels.getTabAt(tabViewModels.currentPositionTabWeb.value!!)

            Log.d("ntt", "onPageStarted:")

            Log.d("ntt", "onPageStarted: pageTab: $pageTab")

            val headers = pageTab?.getHeaders() ?: emptyMap()
            val favi = pageTab?.getFavicon() ?: view.favicon ?: favicon

            val updateTab = WebTab(
                url,
                view.title,
                favi,
                headers,
                view,
                id = pageTab!!.id
            )

            Log.d("ntt", "onPageStarted: updateTab: $updateTab")

            tabViewModels.updateCurrentTab(updateTab)

            tabViewModel.onStartPage(url, view.title)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: WebResourceRequest): Boolean {
//            val isAdBlockerOn = settingsModel.isAdBlocker.get()
            val isAdBlockerOn = true
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
            val pageTab = tabViewModels.getTabAt(tabViewModels.currentPositionTabWeb.value!!)

            val webView = pageTab?.getWebView()
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    view == webView && detail?.didCrash() == true
                } else {
                    view == webView
                }
            ) {
                webView?.destroy()
                return true
            }

            return super.onRenderProcessGone(view, detail)
        }
    }

    private var webChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            if (view != null && view.handler != null) {
                val href = view.handler.obtainMessage()
                view.requestFocusNodeHref(href)
                val url = href.data.getString("url") ?: ""
//                val isAd = if (settingsViewModel.isAdBlocker.get()) {
//                    tabViewModel.isAd(url)
//                } else {
//                    false
//                }

                val isAd = false

                AppLogger.d("ON_CREATE_WINDOW::************* $url ${view.url} isAd:: $isAd  $isUserGesture")
                if (url.isEmpty() || !url.startsWith("http") || isAd || !isUserGesture) {
                    return false
                }

                val transport = resultMsg!!.obj as WebView.WebViewTransport
                transport.webView = WebView(view.context)

                tabViewModel.openPageEvent.value =
                    WebTab(
                        webview = transport.webView,
                        resultMsg = resultMsg,
                        url = "url",
                        title = view.title,
                        iconBytes = null
                    )
                return true
            }
            return false
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
//            val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())
//
//            val headers = pageTab.getHeaders() ?: emptyMap()
//            val updateTab = WebTab(
//                pageTab.getUrl(),
//                pageTab.getTitle(),
//                icon ?: pageTab.getFavicon(),
//                headers,
//                view,
//                id = pageTab.id
//            )
//            updateTabEvent.value = updateTab

            Log.d("ntt", "onReceivedIcon: ")

            val pageTab = tabViewModels.getTabAt(tabViewModels.currentPositionTabWeb.value!!)

            Log.d("ntt", "onReceivedIcon: pageTab: $pageTab")

            val headers = pageTab?.getHeaders() ?: emptyMap()
            val updateTab = WebTab(
                pageTab!!.getUrl(),
                pageTab.getTitle(),
                icon ?: pageTab.getFavicon(),
                headers,
                view,
                id = pageTab.id
            )

            Log.d("ntt", "onReceivedIcon: updateTab: $updateTab")
            tabViewModels.updateCurrentTab(updateTab)
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)

            tabViewModel.setProgress(newProgress)
            if (newProgress == 100) {
                tabViewModel.isShowProgress.set(false)
            } else {
                tabViewModel.isShowProgress.set(true)
            }

        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            binding.webviewContainer.visibility = View.GONE

            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            binding.customView.addView(view)
            appUtil.hideSystemUI(window, binding.customView)
            binding.customView.visibility = View.VISIBLE
            binding.containerBrowser.visibility =
                View.GONE
        }

        override fun onHideCustomView() {
            super.onHideCustomView()
            binding.customView.removeAllViews()
            binding.webviewContainer.visibility = View.VISIBLE
            binding.customView.visibility = View.GONE
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            binding.containerBrowser.visibility =
                View.VISIBLE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            if (settingsViewModel.isLockPortrait.get()) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            appUtil.showSystemUI(window, binding.customView)
        }
    }

    private var canGoCounter = 0

    private val serviceWorkerClient = object : ServiceWorkerClient() {
        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()

            val isM3u8Check = true
            val isMp4Check = true

            if (isM3u8Check || isMp4Check) {
                val requestWithCookies = request.let { resourceRequest ->
                    try {
                        CookieUtils.webRequestToHttpWithCookies(
                            resourceRequest
                        )
                    } catch (e: Throwable) {
                        null
                    }
                }

                val contentType =
                    VideoUtils.getContentTypeByUrl(
                        url,
                        requestWithCookies?.headers,
                        okHttpProxyClient
                    )

                if (contentType == ContentType.MPD || contentType == ContentType.M3U8 || url.contains(
                        ".m3u8"
                    ) || url.contains(
                        ".mpd"
                    ) || url.contains(".txt")
                ) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (requestWithCookies != null && isM3u8Check) {
                            videoDetectionModel.verifyLinkStatus(requestWithCookies, "", true)
                        }
                    }
                } else if (contentType == ContentType.MP4 && isMp4Check) {
                    videoDetectionModel.checkRegularMp4(requestWithCookies)
                }
            }

            return super.shouldInterceptRequest(request)
        }
    }

    override fun setBinding(layoutInflater: LayoutInflater): ActivityWebTabBinding {
        return ActivityWebTabBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bundle = savedInstanceState

    }

    override fun initView() {

        videoDetectionTabViewModel.start()

        videoDetectionModel.start()

        tabViewModel.start()

//        tabViewModel = ViewModelProvider(this, viewModelFactory)[WebTabViewModel::class.java]
//        settingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]

//        pageTabProvider = tabViewModel.browserServicesProvider!!
//        historyProvider = tabViewModel.browserServicesProvider!!

//        videoDetectionTabViewModel =
//            ViewModelProvider(this, viewModelFactory)[DetectedVideosTabViewModel::class.java]
//        videoDetectionTabViewModel.settingsModel = settingsViewModel
//        videoDetectionTabViewModel.webTabModel = tabViewModel
//
//        tabViewModel.start()
//        settingsViewModel.start()
//        videoDetectionTabViewModel.start()

        val swController = ServiceWorkerController.getInstance()
        swController.setServiceWorkerClient(serviceWorkerClient)
        swController.serviceWorkerWebSettings.allowContentAccess = true

        videoDetectionModel.downloadButtonState.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {
//                    browserViewModel.workerM3u8MpdEvent.value =
                    videoDetectionModel.downloadButtonState.get()
                    AppLogger.d("download button state::::::::: ${videoDetectionModel.downloadButtonState.get()}")
                }
            }
        })


        webTab = intent.extras?.getSerializable("webtab") as WebTab

        recreateWebView(bundle)

        val message = webTab.getMessage()
        if (message != null) {
            message.sendToTarget()
            webTab.flushMessage()
        } else {
            tabViewModel.loadPage(webTab.getUrl())
        }

        handleLoadPageEvent()

        handleWorkerEvent()

        handleOpenDetectedVideos()

        handleVideoPushed()

        tabViewModel.start()

        videoDetectionTabViewModel.start()

        configureWebView()

        tabViewModels.listTabWeb.observe(this) {
            binding.tvTab.text = it.size.toString()
        }

        tabViewModel.loadPageEvent.observe(this) {
            webTab = it
        }

        tabViewModel.tabUrl.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.edtSearch.setText(tabViewModel.tabUrl.get())
                }
            }

        })

        tabViewModel.progress.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {

                    binding.progressBar.progress = tabViewModel.progress.get()

                    if (tabViewModel.progress.get() == 100) {
                        binding.progressBar.visibility = View.GONE
                    } else {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }

        })

        tabViewModel.isShowProgress.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (tabViewModel.isShowProgress.get()) {
                        binding.imgReload.setImageResource(R.drawable.ic_close)
                        isReload = false
                    } else {
                        binding.imgReload.setImageResource(R.drawable.ic_reload)
                        isReload = true
                    }
                }
            }

        })


        binding.imgBack.setOnClickListener {
            videoDetectionTabViewModel.cancelAllCheckJobs()
            finish()
        }
        binding.imgBookmark.setOnClickListener {
            AppLogger.d("Webview::::::::: ${webTab.getUrl()}")
            saveUrlToHistoryBookmark()
        }

        binding.imgShare.setOnClickListener {
            shareUrlWithDescription(this, webTab.getUrl(), webTab.getTitle(), webTab.getUrl())
        }

        binding.imgArrowBack.setOnClickListener {
            val webView = webTab.getWebView()
            val canGoBack = webView?.canGoBack()
            if (canGoBack == true) {
                webView.goBack()
                tabViewModel.onGoBack(webView)
                videoDetectionTabViewModel.cancelAllCheckJobs()
            }

            if (canGoBack == false) {
                if (canGoCounter >= 1) {
                    canGoCounter = 0
//                    mainViewModel.openNavDrawerEvent.call()
                } else {
                    canGoCounter++
                }
            }
        }

        binding.imgArrowNext.setOnClickListener {
            val webView = webTab.getWebView()
            val canGoForward = webView?.canGoForward()
            if (canGoForward == true) {
                webView.goForward()
                tabViewModel.onGoForward(webView)
                videoDetectionTabViewModel.cancelAllCheckJobs()
            }
        }

        binding.imgReload.setOnClickListener {
            if (isReload) {
                var url = webTab.getWebView()?.url
                var urlWasChange = false

                if (url?.contains("m.facebook") == true) {
                    url = url.replace("m.facebook", "www.facebook")
                    urlWasChange = true
//                val isDesktop = mainActivity.settingsViewModel.isDesktopMode.get()
//                if (!isDesktop) {
//                    mainActivity.settingsViewModel.setIsDesktopMode(true)
//                }
                }

                val userAgent =
                    webTab.getWebView()?.settings?.userAgentString ?: tabViewModel.userAgent.get()
                    ?: BrowserFragment.MOBILE_USER_AGENT
                if (url != null) {
                    videoDetectionTabViewModel.viewModelScope.launch(videoDetectionTabViewModel.executorReload) {
                        videoDetectionTabViewModel.onStartPage(url, userAgent)
                    }

                    if (url.contains("www.facebook") && urlWasChange) {
                        tabViewModel.openPage(url)
                        tabViewModel.closeTab(webTab)
                    } else {
                        tabViewModel.onPageReload(webTab.getWebView())
                    }
                }
            } else {
                tabViewModel.onPageStop(webTab.getWebView())
            }
        }

        binding.imgDownload.setOnClickListener {

        }

        binding.tvTab.setOnClickListener {
            startActivity(Intent(this@WebTabActivity, TabsActivity::class.java))
        }

    }

    fun shareUrlWithDescription(context: Context, url: String, title: String, description: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title) // Tiêu đề
            putExtra(Intent.EXTRA_TEXT, description) // Nội dung và đường dẫn
        }
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
    }

    override fun onPause() {
        AppLogger.d("onPause Webview::::::::: ${webTab.getUrl()}")
        super.onPause()
        onWebViewPause()
    }

    override fun onResume() {
        AppLogger.d("onResume Webview::::::::: ${webTab.getUrl()}")
        super.onResume()
        onWebViewResume()
    }

    private fun configureWebView() {


//        val webViewClient = CustomWebViewClient(
//            tabViewModels,
//            tabViewModel,
//            settingsViewModel,
//            videoDetectionTabViewModel,
//            historyViewModel,
//            okHttpProxyClient,
////            tabManagerProvider.getUpdateTabEvent(),
////            pageTabProvider,
//            proxyController,
//        )

//        val chromeClient = CustomWebChromeClient(
//            tabViewModels,
//            tabViewModel,
//            settingsViewModel,
////            tabManagerProvider.getUpdateTabEvent(),
////            pageTabProvider,
//            ActivityWebTabBinding.inflate(layoutInflater),
//            appUtil,
//            this
//        )

//        currentWebView?.webChromeClient = chromeClient
//        currentWebView?.webViewClient = webViewClient


        val webSettings = webTab.getWebView()?.settings
        val webView = webTab.getWebView()

        webView?.webChromeClient = webChromeClient
        webView?.webViewClient = webViewClient

        webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView?.isScrollbarFadingEnabled = true

        webSettings?.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(true)
            setSupportMultipleWindows(true)
            setGeolocationEnabled(false)
            allowContentAccess = true
            allowFileAccess = true
            offscreenPreRaster = false
            displayZoomControls = false
            builtInZoomControls = true
            loadWithOverviewMode = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            useWideViewPort = true
            domStorageEnabled = true
            javaScriptEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = BrowserFragment.MOBILE_USER_AGENT
        }

        AppLogger.d(webTab.toString())

        binding.webviewContainer.addView(
            webTab.getWebView(),
            LinearLayout.LayoutParams(-1, -1)
        )
    }

    private fun onWebViewPause() {
        webTab.getWebView()?.onPause()
    }

    private fun onWebViewResume() {
        webTab.getWebView()?.onResume()
    }

    private fun handleLoadPageEvent() {
        tabViewModel.loadPageEvent.observe(this) { tab ->
            if (tab.getUrl().startsWith("http")) {
                webTab.getWebView()?.stopLoading()
                webTab.getWebView()?.loadUrl(tab.getUrl())
            }
        }
    }

    private fun handleWorkerEvent() {
//        workerEventProvider.getWorkerM3u8MpdEvent().observe(this) { state ->
//            if (state is DownloadButtonStateCanDownload && state.info?.id?.isNotEmpty() == true) {
//                videoDetectionTabViewModel.pushNewVideoInfoToAll(state.info)
//                val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
//                loadings?.remove("m3u8")
//                videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
//            }
//            if (state is DownloadButtonStateLoading) {
//                val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
//                loadings?.add("m3u8")
//                videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
//                videoDetectionTabViewModel.setButtonState(DownloadButtonStateLoading())
//            }
//            if (state is DownloadButtonStateCanNotDownload) {
//                val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
//                loadings?.remove("m3u8")
//                videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
//                videoDetectionTabViewModel.setButtonState(DownloadButtonStateCanNotDownload())
//            }
//        }
//
//        workerEventProvider.getWorkerMP4Event().observe(this) { state ->
//            if (state is DownloadButtonStateCanDownload && state.info?.id?.isNotEmpty() == true) {
//                AppLogger.d("Worker MP4 event CanDownload: ${state.info}")
//                videoDetectionTabViewModel.pushNewVideoInfoToAll(state.info)
//            } else {
//                AppLogger.d("Worker MP4 event state: $state")
//            }
//        }
    }

    private fun handleOpenDetectedVideos() {
        videoDetectionTabViewModel.showDetectedVideosEvent.observe(this) {
//            navigateToDownloads()
        }
    }

    private fun handleVideoPushed() {
        videoDetectionTabViewModel.videoPushedEvent.observe(this) {
            onVideoPushed()
        }
    }

    private fun onVideoPushed() {
        showToastVideoFound()

        val isDownloadsVisible = true
        val isCond = !tabViewModel.isDownloadDialogShown.get() && !isDownloadsVisible
        if (settingsViewModel.getVideoAlertState()
                .get() && isCond
        ) {
            lifecycleScope.launch(Dispatchers.Main) {
                showAlertVideoFound()
            }
        }
    }

    private fun showAlertVideoFound() {
        if (!tabViewModel.isDownloadDialogShown.get()) {
            tabViewModel.isDownloadDialogShown.set(true)

            videoAlert =
                MaterialAlertDialogBuilder(this).setTitle(R.string.string_video_found)

            videoAlert?.setOnDismissListener {
                videoAlert = null
            }
            videoAlert?.setMessage(R.string.whatshould)?.setPositiveButton(
                R.string.view
            ) { dialog, _ ->
//                navigateToDownloads()
                tabViewModel.isDownloadDialogShown.set(false)
                dialog.dismiss()
            }?.setNeutralButton(R.string.dontshow) { dialog, _ ->
                settingsViewModel.setShowVideoAlertOff()
                tabViewModel.isDownloadDialogShown.set(false)
                dialog.dismiss()
            }?.setNegativeButton(R.string.string_cancel) { dialog, _ ->
                tabViewModel.isDownloadDialogShown.set(false)
                dialog.dismiss()
            }?.show()
        }
    }

    private fun showToastVideoFound() {

        Toast.makeText(
            this, getString(R.string.string_video_found), Toast.LENGTH_SHORT
        ).show()

    }

    private fun recreateWebView(savedInstanceState: Bundle?) {
        if (webTab.getMessage() == null || webTab.getWebView() == null) {
            webTab.setWebView(WebView(this))
        }

        if (savedInstanceState != null) {
            webTab.getWebView()?.restoreState(savedInstanceState)
        }
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

            historyItemCurrent = HistoryItem(
                url = url, favicon = outputFavicon, title = title
            )

            historyViewModel.saveHistory(
                HistoryItem(
                    url = url, favicon = outputFavicon, title = title
                )
            )
        }
    }

    private fun saveUrlToHistoryBookmark() {

        if (this::historyItemCurrent.isInitialized) {

            historyItemCurrent.isBookmark = true
            lifecycleScope.launch(Dispatchers.IO) {
                historyViewModel.saveHistory(
                    historyItemCurrent
                )
            }
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        videoDetectionTabViewModel.stop()
    }
}