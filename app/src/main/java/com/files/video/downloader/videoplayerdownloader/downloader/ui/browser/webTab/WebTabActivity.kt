package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.AdBlockHostsRepository
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityWebTabBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.BrowserFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.CustomWebChromeClient
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.CustomWebViewClient
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonState
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.SettingsViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.DetectedVideosTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.CustomProxyController
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import com.files.video.downloader.videoplayerdownloader.downloader.util.scheduler.BaseSchedulers
import dagger.hilt.android.AndroidEntryPoint
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

//    private lateinit var tabViewModel: WebTabViewModel
//
//    private lateinit var settingsViewModel: SettingsViewModel
//
//    private lateinit var historyProvider: HistoryProvider
//
//    private lateinit var videoDetectionTabViewModel: DetectedVideosTabViewModel
//
//    private lateinit var tabManagerProvider: TabManagerProvider
//
//    private lateinit var pageTabProvider: PageTabProvider
//
//
//    @Inject
//    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appUtil: AppUtil

    @Inject
    lateinit var proxyController: CustomProxyController

    @Inject
    lateinit var okHttpProxyClient: OkHttpProxyClient

    override fun setBinding(layoutInflater: LayoutInflater): ActivityWebTabBinding {
        return ActivityWebTabBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webTab = intent.extras?.getSerializable("webtab") as WebTab

//        tabViewModel = ViewModelProvider(this, viewModelFactory)[WebTabViewModel::class.java]
//        settingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]
//        historyProvider = tabViewModel.browserServicesProvider!!
//        videoDetectionTabViewModel =
//            ViewModelProvider(this, viewModelFactory)[DetectedVideosTabViewModel::class.java]
//        videoDetectionTabViewModel.settingsModel = settingsViewModel
//        videoDetectionTabViewModel.webTabModel = tabViewModel
//
//        recreateWebView(savedInstanceState)
//
//        val currentWebView = this.webTab.getWebView()
//
//        val webViewClient = CustomWebViewClient(
//            tabViewModel,
//            settingsViewModel,
//            videoDetectionTabViewModel,
//            historyProvider.getHistoryVModel(),
//            okHttpProxyClient,
//            tabManagerProvider.getUpdateTabEvent(),
//            pageTabProvider,
//            proxyController,
//        )
//
//        val chromeClient = CustomWebChromeClient(
//            tabViewModel,
//            settingsViewModel,
//            tabManagerProvider.getUpdateTabEvent(),
//            pageTabProvider,
//            ActivityWebTabBinding.inflate(layoutInflater),
//            appUtil,
//            this
//        )

//        currentWebView?.webChromeClient = chromeClient
//        currentWebView?.webViewClient = webViewClient
//
//
//        val webSettings = webTab.getWebView()?.settings
//        val webView = webTab.getWebView()
//
//        webView?.webViewClient = webViewClient
//        webView?.webChromeClient = chromeClient

//        webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
//        webView?.isScrollbarFadingEnabled = true
//
//        webView?.loadUrl(webTab.getUrl())
//
//        webSettings?.apply {
//            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//            setSupportZoom(true)
//            setSupportMultipleWindows(true)
//            setGeolocationEnabled(false)
//            allowContentAccess = true
//            allowFileAccess = true
//            offscreenPreRaster = false
//            displayZoomControls = false
//            builtInZoomControls = true
//            loadWithOverviewMode = true
//            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
//            useWideViewPort = true
//            domStorageEnabled = true
//            javaScriptEnabled = true
//            databaseEnabled = true
//            cacheMode = WebSettings.LOAD_NO_CACHE
//            javaScriptCanOpenWindowsAutomatically = true
//            mediaPlaybackRequiresUserGesture = false
//            userAgentString = BrowserFragment.MOBILE_USER_AGENT
//        }

        AppLogger.d(webTab.toString())

        binding.webviewContainer.addView(
            webTab.getWebView(),
            LinearLayout.LayoutParams(-1, -1)
        )
    }

    override fun initView() {

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


    }

    private fun recreateWebView(savedInstanceState: Bundle?) {
        if (webTab.getMessage() == null || webTab.getWebView() == null) {
            webTab.setWebView(WebView(this))
        }

        if (savedInstanceState != null) {
            webTab.getWebView()?.restoreState(savedInstanceState)
        }
    }
}