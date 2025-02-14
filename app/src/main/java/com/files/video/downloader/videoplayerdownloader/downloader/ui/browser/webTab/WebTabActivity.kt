package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
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
import android.webkit.WebView.HitTestResult
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideFormatEntityList
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityWebTabBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.LayoutBottomSheetDownloadBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogInformationImage
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogRename
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.BrowserFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.ContentType
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonState
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateCanDownload
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateCanNotDownload
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateLoading
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.SettingsViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.DetectedVideosTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.VideoDetectionAlgVModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo.PrivateVideoViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.processing.ProgressViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabsActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdBlockerHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.CookieUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.FaviconUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileNameCleaner
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent
import com.files.video.downloader.videoplayerdownloader.downloader.util.VideoUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.CustomProxyController
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
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
class WebTabActivity : BaseActivity<ActivityWebTabBinding>(), DownloadTabListener {

    private lateinit var webTab: WebTab

    private val tabViewModel: WebTabViewModel by viewModels()

    private val privateVideoViewModel: PrivateVideoViewModel by viewModels()

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

    @Inject
    lateinit var fileUtil: FileUtil

    private lateinit var downloadBinding: LayoutBottomSheetDownloadBinding

    private lateinit var bottomSheetDialog: BottomSheetDialog

    private val progressViewModel: ProgressViewModel by viewModels()

    var videoAlert: MaterialAlertDialogBuilder? = null
    private var lastSavedHistoryUrl: String = ""
    private var lastSavedTitleHistory: String = ""
    private var lastRegularCheckUrl = ""
    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()

    private lateinit var videoInfoAdapter: VideoInfoAdapter

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

                    videoDetectionTabViewModel.onStartPage(
                        url,
                        userAgent
                            ?: BrowserFragment.MOBILE_USER_AGENT
                    )
                    tabViewModel.onUpdateVisitedHistory(
                        url,
                        title,
                        userAgent
                    )

                    val pageTab =
                        tabViewModels.getTabAt(tabViewModels.currentPositionTabWeb.value!!)

                    pageTab?.setUrl(url)

                    withContext(Dispatchers.Main) {

                        pageTab?.let { tabViewModels.updateCurrentTab(it) }

                    }
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
            val isAdBlockerOn = true
            val url = request?.url.toString()

            val isUrlAd: Boolean = isAdBlockerOn && tabViewModel.isAd(url)

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
                            videoDetectionTabViewModel.verifyLinkStatus(
                                requestWithCookies, tabViewModel.currentTitle.get(), true
                            )

                        }
                    }

                    else -> {
                        if (isCheckOnMp4) {
                            val disposable =
                                videoDetectionTabViewModel.checkRegularMp4(requestWithCookies)

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

            tabViewModels.updateCurrentTab(updateTab)

            tabViewModel.onStartPage(url, view.title)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: WebResourceRequest): Boolean {
//            val isAdBlockerOn = settingsModel.isAdBlocker.get()
            val isAdBlockerOn = true
            val isAd = if (isAdBlockerOn) tabViewModel.isAd(url.url.toString()) else false

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

                val isBlockAds = true

                val isAd = if (isBlockAds) {
                    tabViewModel.isAd(url)
                } else {
                    false
                }

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


            val pageTab = tabViewModels.getTabAt(tabViewModels.currentPositionTabWeb.value!!)


            val headers = pageTab?.getHeaders() ?: emptyMap()
            val updateTab = WebTab(
                pageTab!!.getUrl(),
                pageTab.getTitle(),
                icon ?: pageTab.getFavicon(),
                headers,
                view,
                id = pageTab.id
            )

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


        val swController = ServiceWorkerController.getInstance()
        swController.setServiceWorkerClient(serviceWorkerClient)
        swController.serviceWorkerWebSettings.allowContentAccess = true

        videoDetectionTabViewModel.webTabModel = tabViewModel


        bottomSheetDialog = BottomSheetDialog(this, R.style.CustomAlertBottomSheet)

        videoDetectionTabViewModel.downloadButtonState.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {
//                    browserViewModel.workerM3u8MpdEvent.value =

                    val state = videoDetectionTabViewModel.downloadButtonState.get()

                    when (videoDetectionTabViewModel.downloadButtonState.get()) {
                        is DownloadButtonStateCanNotDownload -> binding.imgDownload.setImageResource(
                            R.drawable.ic_download_disable
                        )

                        is DownloadButtonStateCanDownload -> binding.imgDownload.setImageResource(R.drawable.ic_download_enable)
                        is DownloadButtonStateLoading -> {
                            binding.imgDownload.setImageResource(R.drawable.ic_download_disable)
                        }
                    }

//                    if (state is DownloadButtonStateCanDownload && state.info?.id?.isNotEmpty() == true) {
//                        videoDetectionTabViewModel.pushNewVideoInfoToAll(state.info)
//                        val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
//                        loadings?.remove("m3u8")
//                        videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
//                    }
//                    if (state is DownloadButtonStateLoading) {
//                        val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
//                        loadings?.add("m3u8")
//                        videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
//                        videoDetectionTabViewModel.setButtonState(DownloadButtonStateLoading())
//                    }
//                    if (state is DownloadButtonStateCanNotDownload) {
//                        val loadings = videoDetectionTabViewModel.m3u8LoadingList.get()
//                        loadings?.remove("m3u8")
//                        videoDetectionTabViewModel.m3u8LoadingList.set(loadings?.toMutableSet())
//                        videoDetectionTabViewModel.setButtonState(DownloadButtonStateCanNotDownload())
//                    }
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
            videoDetectionTabViewModel.showVideoInfo()
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
        super.onPause()
        onWebViewPause()
    }

    override fun onResume() {
        super.onResume()
        onWebViewResume()
    }

    private fun configureWebView() {

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


//        binding.webviewContainer.setOnLongClickListener {
//            val result = webView?.hitTestResult
//            if (result?.type == WebView.HitTestResult.IMAGE_TYPE || result?.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
//                val imageUrl = result.extra // URL của ảnh
//                Log.d("ntt", "configureWebView: imageUrl: ${imageUrl}")
//                true
//            } else {
//                false
//            }
//        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        val webView = v as WebView
        val result = webView.hitTestResult

        if (result?.type == WebView.HitTestResult.IMAGE_TYPE || result?.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            val imageUrl = result.extra // URL của ảnh
            Log.d("ntt", "configureWebView: imageUrl: ${imageUrl}")

            if (imageUrl != null) {
                val dialogInformationImage =
                    DialogInformationImage(this@WebTabActivity,
                        imageUrl.toString(),
                        onClickOpenNewTab = {
                            if (imageUrl.startsWith("http")) {
                                webTab.getWebView()?.stopLoading()
                                webTab.getWebView()?.loadUrl(imageUrl)

                            }
                        },
                        onClickShare = {
                            shareUrl(this@WebTabActivity, imageUrl)
                        },
                        onClickCopyLink = {
                            val clipboard =
                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("copied_text", imageUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this, "Đã sao chép vào clipboard", Toast.LENGTH_SHORT)
                                .show()
                        },
                        onClickDownloadImage = {
                            lifecycleScope.launch(Dispatchers.Main) {
                                downloadImage(this@WebTabActivity, imageUrl)
                            }
                        })

                dialogInformationImage.show()
            }
        } else {
            false
        }
    }

    private fun shareUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ liên kết qua:"))
    }

    private suspend fun downloadImage(context: Context, imageUrl: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUrl.startsWith("data:image")) {
                    return@withContext saveBase64Image(context, imageUrl)
                }

                val client = OkHttpClient()
                val request = Request.Builder().url(imageUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("Download", "Download failed: ${response.code}")
                    return@withContext null
                }

                val url = URL(imageUrl)
                var fileName = url.file.substringAfterLast("/")
                if (fileName.isBlank()) fileName = "downloaded_image.jpg"

                val outputFile: File
                val fileSize: Long

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            ?: return@withContext null

                    val fileDescriptor =
                        resolver.openFileDescriptor(uri, "w") ?: return@withContext null
                    fileSize = response.body?.contentLength() ?: 0

                    FileOutputStream(fileDescriptor.fileDescriptor).use { outputStream ->
                        response.body?.byteStream()?.copyTo(outputStream)
                    }

                    fileDescriptor.close()

                    outputFile = File(getRealPathFromURI(context, uri) ?: return@withContext null)
                } else {
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    outputFile = File(downloadsDir, fileName)

                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    fileSize = outputFile.length()
                }

                // Lưu vào Database
                val fileDate = System.currentTimeMillis()

                // 🔥 **Lưu vào Database**
                val videoTaskItem = VideoTaskItem(
                    mId = outputFile.absolutePath.hashCode().toString(),
                    fileName = fileName,
                    filePath = outputFile.absolutePath,
                    fileSize = fileSize,
                    fileDate = fileDate,
                    url = imageUrl,
                    mimeType = "image"
                )
                privateVideoViewModel.insertVideoTaskItem(videoTaskItem)


                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "File saved: ${outputFile.absolutePath}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return@withContext outputFile
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }


    private suspend fun saveBase64Image(
        context: Context,
        base64String: String,
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Tách dữ liệu base64
                val base64Data = base64String.substringAfter("base64,", "")

                if (base64Data.isBlank()) {
                    Log.e("ntt", "Invalid base64 data")
                    return@withContext null
                }

                // Giải mã base64 thành mảng byte
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val fileName = "downloaded_base64_image_${System.currentTimeMillis()}.jpg"

                val outputFile: File?
                val fileSize: Long

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // ✅ Android 10+ (API 29+): Lưu bằng MediaStore
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            ?: return@withContext null

                    val fileDescriptor =
                        resolver.openFileDescriptor(uri, "w") ?: return@withContext null
                    FileOutputStream(fileDescriptor.fileDescriptor).use { it.write(decodedBytes) }
                    fileDescriptor.close()

                    outputFile = File(getRealPathFromURI(context, uri) ?: return@withContext null)
                    fileSize = decodedBytes.size.toLong()
                } else {
                    // ✅ Android 9 trở xuống (API < 29): Lưu vào thư mục Pictures
                    val picturesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    outputFile = File(picturesDir, fileName)

                    FileOutputStream(outputFile).use { it.write(decodedBytes) }
                    fileSize = outputFile.length()
                }

                val fileDate = System.currentTimeMillis()

                // 🔥 **Lưu vào Database**
                val videoTaskItem = VideoTaskItem(
                    mId = outputFile.absolutePath.hashCode().toString(),
                    fileName = fileName,
                    filePath = outputFile.absolutePath,
                    fileSize = fileSize,
                    fileDate = fileDate,
                    url = base64String,
                    mimeType = "image"
                )
                privateVideoViewModel.insertVideoTaskItem(videoTaskItem)

                Log.d("ntt", "File saved at: ${outputFile.absolutePath}")

                return@withContext outputFile
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }


    private fun getRealPathFromURI(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }
        return null
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

                registerForContextMenu(webTab.getWebView())
            }
        }
    }

    private fun handleWorkerEvent() {
//        workerEventProvider.getWorkerM3u8MpdEvent().observe(this) { state ->
//            Log.d("ntt", "handleWorkerEvent: state $state")
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
            Log.d(
                "ntt",
                "handleOpenDetectedVideos: ${videoDetectionTabViewModel.detectedVideosList.get()}"
            )

            val list = videoDetectionTabViewModel.detectedVideosList.get()
            val firstItem = list?.firstOrNull() // Lấy phần tử đầu tiên hoặc null nếu danh sách rỗng

            if (firstItem != null) {
                showBottomSheetDownload(firstItem)
            } else {
                // Xử lý khi danh sách rỗng
                Log.d("ntt", "Danh sách rỗng, không có phần tử đầu tiên")
            }
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

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@WebTabActivity,
                        getString(R.string.string_save_to_bookmarks_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    private fun showBottomSheetDownload(videoInfo: VideoInfo) {

        downloadBinding = LayoutBottomSheetDownloadBinding.inflate(layoutInflater)

        bottomSheetDialog.setContentView(downloadBinding.root)

        bottomSheetDialog.setCanceledOnTouchOutside(true);

        val behavior = bottomSheetDialog.behavior

        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Xử lý sự kiện thay đổi trạng thái của bottom sheet
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Xử lý khi bottom sheet được trượt
            }
        }
        bottomSheetDialog.behavior.addBottomSheetCallback(bottomSheetCallback)


        Glide.with(downloadBinding.icFile).load(videoInfo.thumbnail).into(downloadBinding.icFile)

        val titles =
            videoDetectionTabViewModel.formatsTitles.get()?.toMutableMap() ?: mutableMapOf()
        titles[videoInfo.id] = titles[videoInfo.id] ?: videoInfo.title

        videoDetectionTabViewModel.formatsTitles.set(titles)

        val frmts =
            videoDetectionTabViewModel.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
        val selected = frmts[videoInfo.id]
        val defaultFormat = videoInfo.formats.formats.lastOrNull()?.format ?: "unknown"
        if (selected == null) {
            frmts[videoInfo.id] = defaultFormat
        }

        downloadBinding.nameFile.text = titles[videoInfo.id]


        videoDetectionTabViewModel.selectedFormats.set(frmts)
        if (videoInfo.isRegularDownload) {
            videoDetectionTabViewModel.selectedFormatUrl.set(videoInfo.firstUrlToString)
        } else {
            videoDetectionTabViewModel.selectedFormatUrl.set(videoInfo.formats.formats.lastOrNull()?.url)
        }


        val typeText = if (videoInfo.isM3u8) {
            val isMpd = videoInfo.formats.formats.firstOrNull()?.url?.contains(".mpd") == true
            if (isMpd) "MPD List" else "M3U8 List"
        } else if (videoInfo.isMaster) {
            val isMpd = videoInfo.formats.formats.firstOrNull()?.url?.contains(".mpd") == true
            if (isMpd) "MPD Master List" else "M3U8 Mater List"
        } else if (videoInfo.isRegularDownload) {
            "Regular MP4 Download"
        } else {
            ""
        }

        downloadBinding.txtType.text = typeText

        videoDetectionTabViewModel.selectedFormats.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val curSelected =
                    videoDetectionTabViewModel.selectedFormats.get()?.get(videoInfo?.id)
                val foundFormat =
                    videoInfo?.formats?.formats?.find { it.format == curSelected }
                videoDetectionTabViewModel.selectedFormatUrl.set(foundFormat?.url.toString())
            }
        })

        if (videoInfo.isRegularDownload) {
            val fileSize = videoInfo.formats.formats.firstOrNull()?.fileSize
            if (fileSize != null) {
                val size = FileUtil.getFileSizeReadable(fileSize.toDouble())
                downloadBinding.txtSize.text = "Download Size: $size"
            }
        }


        videoDetectionTabViewModel.detectedVideosList.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {

            }
        })
        videoInfoAdapter = VideoInfoAdapter(
            this@WebTabActivity,
            videoDetectionTabViewModel?.detectedVideosList?.get()?.toList() ?: emptyList(),
            videoDetectionTabViewModel,
            this@WebTabActivity,
            appUtil
        )

        downloadBinding.videoInfoList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        downloadBinding.videoInfoList.adapter = videoInfoAdapter

        downloadBinding.icEdit.setOnClickListener {
            showDialogRename(videoInfo, titles[videoInfo.id].toString())
        }

        downloadBinding.imgClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        downloadBinding.cvImage.setOnClickListener {
//            startActivity(
//                Intent(
//                    this, PlayMediaActivity::class.java
//                ).apply {
//                    val selectedFormatsMap = videoDetectionTabViewModel.selectedFormats.get()
//                    val format = selectedFormatsMap?.get(videoInfo.id)
//
//                    val selectedFormatTitle = videoDetectionTabViewModel.formatsTitles.get()
//                    val title = selectedFormatTitle?.get(videoInfo.id)
//                    // you can add values(if any) to pass to the next class or avoid using `.apply`
//                    val currFormat = videoInfo.formats.formats.filter {
//                        format?.let { it1 ->
//                            it.format?.contains(
//                                it1 as CharSequence
//                            )
//                        } ?: false
//                    }
//
//                    putExtra(PlayMediaActivity.VIDEO_NAME, title)
//
//                    if (currFormat.isNotEmpty()) {
//                        val headers = currFormat.first().httpHeaders?.let {
//                            JSONObject(
//                                currFormat.first().httpHeaders ?: emptyMap<String, String>()
//                            ).toString()
//                        } ?: "{}"
//
//                        putExtra(
//                            PlayMediaActivity.VIDEO_URL, currFormat.first().url
//                        )
//                        val headersFinal = headers
//                        putExtra(
//                            PlayMediaActivity.VIDEO_HEADERS, headersFinal
//                        )
//                    }
//                })
        }


        bottomSheetDialog.show()
    }

    private fun onVideoDownloadPropagate(
        videoInfo: VideoInfo, videoTitle: String, format: String
    ) {
        val info = videoInfo.copy(
            title = FileNameCleaner.cleanFileName(videoTitle),
            formats = VideFormatEntityList(videoInfo.formats.formats.filter {
                it.format?.contains(
                    format
                ) ?: false
            })
        )

        progressViewModel.downloadVideo(info)

        bottomSheetDialog.dismiss()


//        downloadVideo(info)
//
////        downloadVideoEvent.value = info

        Toast.makeText(
            this, getString(R.string.download_started), Toast.LENGTH_SHORT
        ).show()

    }

    private fun showDialogRename(videoInfo: VideoInfo, name: String) {
        val dialogRename = DialogRename(this, name) { it ->
            downloadBinding.nameFile.text = it

            val title = it.toString()
            val titlesF =
                videoDetectionTabViewModel.formatsTitles.get()?.toMutableMap() ?: mutableMapOf()
            titlesF[videoInfo.id] = title
            videoDetectionTabViewModel.formatsTitles.set(titlesF)
        }

        dialogRename.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        tabViewModel.stop()
        videoDetectionModel.stop()
        videoDetectionTabViewModel.stop()
    }

    override fun onCancel() {
        bottomSheetDialog.dismiss()
    }

    @OptIn(UnstableApi::class)
    override fun onPreviewVideo(videoInfo: VideoInfo, format: String, isForce: Boolean) {
        startActivity(
            Intent(
                this, PlayMediaActivity::class.java
            ).apply {
//                val selectedFormatsMap = videoDetectionTabViewModel.selectedFormats.get()
//                val format = selectedFormatsMap?.get(videoInfo.id)

                Log.d("ntt", "onPreviewVideo: format: $format")

                val selectedFormatTitle = videoDetectionTabViewModel.formatsTitles.get()
                val title = selectedFormatTitle?.get(videoInfo.id)
                // you can add values(if any) to pass to the next class or avoid using `.apply`
                val currFormat = videoInfo.formats.formats.filter {
                    format?.let { it1 ->
                        it.format?.contains(
                            it1 as CharSequence
                        )
                    } ?: false
                }

                putExtra(PlayMediaActivity.VIDEO_NAME, title)

                if (currFormat.isNotEmpty()) {
                    val headers = currFormat.first().httpHeaders?.let {
                        JSONObject(
                            currFormat.first().httpHeaders ?: emptyMap<String, String>()
                        ).toString()
                    } ?: "{}"

                    putExtra(
                        PlayMediaActivity.VIDEO_URL, currFormat.first().url
                    )
                    val headersFinal = if (isForce) "{}" else headers
                    putExtra(
                        PlayMediaActivity.VIDEO_HEADERS, headersFinal
                    )
                }
            })
    }

    override fun onDownloadVideo(videoInfo: VideoInfo, format: String, videoTitle: String) {
        onVideoDownloadPropagate(videoInfo, videoTitle, format)
    }

    override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
        val formats =
            videoDetectionTabViewModel.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
        formats[videoInfo.id] = format
        videoDetectionTabViewModel.selectedFormats.set(formats)
    }
}