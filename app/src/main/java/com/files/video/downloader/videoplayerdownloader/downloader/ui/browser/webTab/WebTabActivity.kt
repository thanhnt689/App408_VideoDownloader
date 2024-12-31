package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.Observable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.AdBlockHostsRepository
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityWebTabBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.BrowserFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.ContentType
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.CustomWebChromeClient
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.CustomWebViewClient
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonState
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateCanDownload
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateCanNotDownload
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateLoading
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.SettingsViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.DetectedVideosTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.VideoDetectionAlgVModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabsActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.CookieUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import com.files.video.downloader.videoplayerdownloader.downloader.util.VideoUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.CustomProxyController
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import com.files.video.downloader.videoplayerdownloader.downloader.util.scheduler.BaseSchedulers
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
//
//    private lateinit var tabManagerProvider: TabManagerProvider
//
//    private lateinit var pageTabProvider: PageTabProvider
//
//
//    @Inject
//    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var workerEventProvider: WorkerEventProvider

    private var bundle: Bundle? = null

    @Inject
    lateinit var appUtil: AppUtil

    @Inject
    lateinit var proxyController: CustomProxyController

    @Inject
    lateinit var okHttpProxyClient: OkHttpProxyClient

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

        binding.imgBack.setOnClickListener {
            finish()
        }
        binding.imgBookmark.setOnClickListener {
            AppLogger.d("Webview::::::::: ${webTab.getUrl()}")
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

        val webViewClient = CustomWebViewClient(
            tabViewModels,
            tabViewModel,
            settingsViewModel,
            videoDetectionTabViewModel,
            historyViewModel,
            okHttpProxyClient,
//            tabManagerProvider.getUpdateTabEvent(),
//            pageTabProvider,
            proxyController,
        )

        val chromeClient = CustomWebChromeClient(
            tabViewModels,
            tabViewModel,
            settingsViewModel,
//            tabManagerProvider.getUpdateTabEvent(),
//            pageTabProvider,
            ActivityWebTabBinding.inflate(layoutInflater),
            appUtil,
            this
        )

//        currentWebView?.webChromeClient = chromeClient
//        currentWebView?.webViewClient = webViewClient


        val webSettings = webTab.getWebView()?.settings
        val webView = webTab.getWebView()

        webView?.webChromeClient = chromeClient
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
            val client = getWebViewClientCompat(webTab.getWebView())

            client?.videoAlert =
                MaterialAlertDialogBuilder(this).setTitle(R.string.string_video_found)
            client?.videoAlert?.setOnDismissListener {
                client.videoAlert = null
            }
            client?.videoAlert?.setMessage(R.string.whatshould)?.setPositiveButton(
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

    private fun getWebViewClientCompat(webView: WebView?): CustomWebViewClient? {
        return try {
            val getWebViewClientMethod = WebView::class.java.getMethod("getWebViewClient")
            val client = getWebViewClientMethod.invoke(webView) as? CustomWebViewClient
            client
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

    override fun onDestroy() {
        super.onDestroy()
        videoDetectionTabViewModel.stop()
    }
}