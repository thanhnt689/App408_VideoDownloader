package com.files.video.downloader.videoplayerdownloader.downloader.ui.processing

import android.Manifest
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.webkit.WebViewCompat.setAudioMuted
import com.bumptech.glide.Glide
import com.files.video.downloader.videoplayerdownloader.downloader.MainActivity
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseFragment
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideFormatEntityList
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.FragmentProcessingBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.LayoutBottomSheetDownloadBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.LayoutBottomSheetPermissionBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogRename
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.BrowserFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.ContentType
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateCanDownload
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateCanNotDownload
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.DownloadButtonStateLoading
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.DetectedVideosTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.VideoDetectionAlgVModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.DownloadTabListener
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.VideoInfoAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTab
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabFactory
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.guide.GuideActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdBlockerHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.CookieUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileNameCleaner
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SystemUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.VideoUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.CustomProxyController
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.util.AppOpenManager
import javax.inject.Inject

@AndroidEntryPoint
class ProcessingFragment : BaseFragment<FragmentProcessingBinding>(), ProgressListener,
    DownloadTabListener {

    private val progressViewModel: ProgressViewModel by viewModels()

    private lateinit var progressAdapter: ProcessAdapter

    private val videoDetectionTabViewModel: DetectedVideosTabViewModel by viewModels()

    private lateinit var downloadBinding: LayoutBottomSheetDownloadBinding

    private lateinit var bottomSheetDialog: BottomSheetDialog

    private lateinit var permissionLayoutBinding: LayoutBottomSheetPermissionBinding

    private lateinit var bottomSheetPermissionDialog: BottomSheetDialog

    @Inject
    lateinit var appUtil: AppUtil

    @Inject
    lateinit var okHttpProxyClient: OkHttpProxyClient

    private lateinit var videoInfoAdapter: VideoInfoAdapter

    private lateinit var webTab: WebTab

    private val tabViewModel: WebTabViewModel by viewModels()

    @Inject
    lateinit var fileUtil: FileUtil

    private var currentUrl = ""

    @Inject
    lateinit var proxyController: CustomProxyController

    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()

    private var lastRegularCheckUrl = ""

    private var lastSavedHistoryUrl: String = ""

    private val videoDetectionModel: VideoDetectionAlgVModel by viewModels()

    private lateinit var animation: AlphaAnimation

    private lateinit var handler: Handler

    private lateinit var runnable: Runnable

    private var nativePopupPermission: com.google.android.gms.ads.nativead.NativeAd? = null

    private var webViewClient = object : WebViewClient() {
        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            val viewTitle = view?.title
            val title = tabViewModel.currentTitle.get()
            val userAgent = view?.settings?.userAgentString ?: BrowserFragment.MOBILE_USER_AGENT

            if (url != null && lastSavedHistoryUrl != url) {
                lifecycleScope.launch(Dispatchers.IO) {

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

                }
            }
            super.doUpdateVisitedHistory(view, url, isReload)
        }

        override fun onReceivedHttpAuthRequest(
            view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?
        ) {
            Log.d("ntt", "onReceivedHttpAuthRequest: ")
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

            tabViewModel.onStartPage(url, view.title)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: WebResourceRequest): Boolean {
            Log.d("ntt", "shouldOverrideUrlLoading: ")
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
        }

        override fun onHideCustomView() {
            super.onHideCustomView()
        }
    }

    private fun recreateWebView(savedInstanceState: Bundle?) {
        if (webTab.getMessage() == null || webTab.getWebView() == null) {
            webTab.setWebView(WebView(requireContext()))
        }

        if (savedInstanceState != null) {
            webTab.getWebView()?.restoreState(savedInstanceState)
        }
    }

    override fun getViewBinding(): FragmentProcessingBinding {
        return FragmentProcessingBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webTab = WebTabFactory.createWebTabFromInput(currentUrl)

        videoDetectionTabViewModel.start()

        videoDetectionModel.start()

        videoDetectionTabViewModel.webTabModel = tabViewModel

        val message = webTab.getMessage()
        if (message != null) {
            message.sendToTarget()
            webTab.flushMessage()
        } else {
            tabViewModel.loadPage(webTab.getUrl())
        }

        handler = Handler(Looper.getMainLooper())

        animation =
            AlphaAnimation(1f, 0.2f)

        animation.duration = 700

        animation.interpolator = LinearInterpolator()
        animation.repeatCount = Animation.INFINITE

        animation.repeatMode = Animation.REVERSE

        recreateWebView(savedInstanceState)

        handleLoadPageEvent()

        configureWebView()

        tabViewModel.loadPageEvent.observe(viewLifecycleOwner) {
            Log.d("ntt", "onViewCreated: webTab: $webTab")
            webTab = it
        }


        bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomAlertBottomSheet)

        bottomSheetPermissionDialog =
            BottomSheetDialog(requireContext(), R.style.CustomAlertBottomSheet)


        binding.pbLoading.visibility = View.GONE

        binding.layoutNoData.visibility = View.VISIBLE

        progressViewModel.start()

        progressAdapter = ProcessAdapter(emptyList(), this)

        videoDetectionTabViewModel.downloadButtonState.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {

                    when (videoDetectionTabViewModel.downloadButtonState.get()) {
                        is DownloadButtonStateCanNotDownload -> {

                            Glide.with(binding.imgDownload)
                                .load(R.drawable.ic_download_disable_update)
                                .into(binding.imgDownload)

                        }

                        is DownloadButtonStateCanDownload -> {

                            Glide.with(binding.imgDownload)
                                .load(R.drawable.ic_download_enable_update)
                                .into(binding.imgDownload)
                        }

                        is DownloadButtonStateLoading -> {

                            binding.imgDownload.setBackgroundResource(
                                R.drawable.bg_download_disable
                            )

                            Glide.with(binding.imgDownload).load(R.drawable.loading_video)
                                .into(binding.imgDownload)
                        }
                    }

                }
            }
        })

        videoDetectionTabViewModel.showDetectedVideosEvent.observe(viewLifecycleOwner) {

            val list = videoDetectionTabViewModel.detectedVideosList.get()
            val firstItem = list?.firstOrNull()

            if (firstItem != null) {
                if (!checkStoragePermission() || !checkNotificationPermission()) {
                    showBottomSheetPermission()
                } else {
                    showBottomSheetDownload(firstItem)
                }
            } else {
                Log.d("ntt", "Danh sách rỗng, không có phần tử đầu tiên")
            }
        }
        videoDetectionTabViewModel.videoPushedEvent.observe(viewLifecycleOwner) {
            onVideoPushed()
        }


        progressViewModel.progressInfos.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {

                lifecycleScope.launch(Dispatchers.Main) {

                    progressViewModel.progressInfos.get()?.let {
                        progressAdapter.setData(it)
                    }

                    if (progressViewModel.progressInfos.get().isNullOrEmpty()) {
                        binding.layoutNoData.visibility = View.VISIBLE
                    } else {
                        binding.layoutNoData.visibility = View.GONE
                    }

                    val manager =
                        WrapContentLinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.VERTICAL,
                            false
                        )
                    binding.rcvProcess.layoutManager = manager
                    binding.rcvProcess.adapter = progressAdapter
                }
            }

        })

        binding.imgGuide.setOnClickListener {
            startActivity(GuideActivity.newIntent(requireContext()).apply {
                putExtra("open", "process")
            })
        }

        binding.tvPaste.setOnClickListener {
            val clipboard: ClipboardManager =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(
                    ClipDescription.MIMETYPE_TEXT_PLAIN
                ) == true
            ) {
                val clipData = clipboard.primaryClip
                val copiedText = clipData?.getItemAt(0)?.text.toString()

                if (copiedText.isNotEmpty()) {
                    binding.edtSearch.setText(copiedText)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.string_clipboard_is_empty), Toast.LENGTH_SHORT
                    )
                        .show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.string_no_text_in_clipboard), Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                currentUrl = binding.edtSearch.text.toString()

                if (currentUrl.startsWith("http")) {
                    webTab.setUrl(currentUrl)
                    tabViewModel.loadPage(webTab.getUrl())
                }

            }
        })

        binding.imgDownload.setOnClickListener {

            videoDetectionTabViewModel.showVideoInfo()

        }

    }

    private fun showBottomSheetPermission() {

        permissionLayoutBinding = LayoutBottomSheetPermissionBinding.inflate(layoutInflater)

        bottomSheetPermissionDialog.setContentView(permissionLayoutBinding.root)

        bottomSheetPermissionDialog.setCanceledOnTouchOutside(true);

        val behavior = bottomSheetPermissionDialog.behavior

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

        SystemUtil.setLocale(requireContext())

        bottomSheetPermissionDialog.behavior.addBottomSheetCallback(bottomSheetCallback)

        showAdsNativePopupPermission(permissionLayoutBinding.frAds)

        runnable = Runnable {
            if (permissionLayoutBinding.btnStorage.isEnabled) {
                permissionLayoutBinding.btnStorage.startAnimation(animation)
            } else if (permissionLayoutBinding.btnNotification.isEnabled) {
                permissionLayoutBinding.btnNotification.startAnimation(animation)
            }
        }

        handler.postDelayed(runnable, 5000L)

        if (checkStoragePermission()) {

            permissionLayoutBinding.btnStorage.clearAnimation()

            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 5000L)

            permissionLayoutBinding.btnStorage.isEnabled = false
            permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_exit)

            permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#808080"))

            permissionLayoutBinding.tvDes.text = getString(R.string.string_notification)
            permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_notification)

            permissionLayoutBinding.btnNotification.isEnabled = true
            permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_skip_permission)
            permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#FFFFFF"))

        } else {

            permissionLayoutBinding.btnNotification.clearAnimation()

            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 5000L)

            permissionLayoutBinding.btnStorage.isEnabled = true
            permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_skip_permission)
            permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#FFFFFF"))

            permissionLayoutBinding.tvDes.text = getString(R.string.string_storage)
            permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_storage)


            permissionLayoutBinding.btnNotification.isEnabled = false
            permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_exit)

            permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#808080"))

        }

        permissionLayoutBinding.btnClose.setOnClickListener {
            bottomSheetPermissionDialog.dismiss()
        }

        permissionLayoutBinding.btnNotification.setOnClickListener {
            permissionLayoutBinding.btnNotification.clearAnimation()

            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 5000L)

            permissionLayoutBinding.frAds.visibility = View.GONE

            requestNotificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        permissionLayoutBinding.btnStorage.setOnClickListener {
            requestPermissionStorage()
        }

        bottomSheetPermissionDialog.show()
    }

    private fun requestPermissionStorage() {

        permissionLayoutBinding.btnStorage.clearAnimation()

        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 5000L)

        permissionLayoutBinding.frAds.visibility = View.GONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storageImageActivityResultLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            //Android is below 13(R)
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                99
            )
        }
    }

    private val storageImageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            storageActivityResultLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        }

    private val storageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            permissionLayoutBinding.frAds.visibility = View.VISIBLE

            if (isGranted) {
                if (checkNotificationPermission()) {

                    bottomSheetPermissionDialog.dismiss()
                } else {
                    permissionLayoutBinding.btnNotification.clearAnimation()
                    handler.removeCallbacks(runnable)
                    handler.postDelayed(runnable, 5000L)
                    permissionLayoutBinding.btnStorage.isEnabled = false
                    permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_exit)

                    permissionLayoutBinding.tvDes.text = getString(R.string.string_notification)
                    permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_notification)

                    permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#808080"))


                    permissionLayoutBinding.btnNotification.isEnabled = true
                    permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_skip_permission)

                    permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#FFFFFF"))
                }
            } else {
                showSettingsDialog()
            }

        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionLayoutBinding.frAds.visibility = View.VISIBLE

        if (requestCode == 99) {
            if (grantResults.isNotEmpty()) {
                val read = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED

                if (read && write) {
                    if (checkNotificationPermission()) {

                        bottomSheetPermissionDialog.dismiss()
                    } else {
                        permissionLayoutBinding.btnNotification.clearAnimation()
                        handler.removeCallbacks(runnable)
                        handler.postDelayed(runnable, 5000L)
                        permissionLayoutBinding.btnStorage.isEnabled = false
                        permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_exit)

                        permissionLayoutBinding.tvDes.text = getString(R.string.string_notification)
                        permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_notification)

                        permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#808080"))


                        permissionLayoutBinding.btnNotification.isEnabled = true
                        permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_skip_permission)

                        permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#FFFFFF"))
                    }
                } else {
                    showSettingsDialog()
                }
            }
        }

    }


    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

        permissionLayoutBinding.frAds.visibility = View.VISIBLE

        if (isGranted) {
            if (checkStoragePermission()) {

                bottomSheetPermissionDialog.dismiss()
            } else {
                permissionLayoutBinding.btnNotification.clearAnimation()
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, 5000L)
                permissionLayoutBinding.btnNotification.isEnabled = false
                permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_exit)

                permissionLayoutBinding.tvDes.text = getString(R.string.string_storage)
                permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_storage)

                permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#808080"))

                permissionLayoutBinding.btnStorage.isEnabled = true
                permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_skip_permission)

                permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#FFFFFF"))
            }
        } else {
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
//        frAds.visibility = View.GONE
        permissionLayoutBinding.frAds.visibility = View.GONE
        var builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.string_permission))
            .setMessage(getString(R.string.permission_setting))
            .setPositiveButton(getString(R.string.string_ok)) { _: DialogInterface, _: Int ->
                openAppSettings()
            }
            .setCancelable(false)

        var dialog = builder.create()
        builder.setOnDismissListener {
//            frAds.visibility = View.VISIBLE
            permissionLayoutBinding.frAds.visibility = View.VISIBLE
        }
        dialog.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)
        startSettingResult.launch(intent)
    }

    val startSettingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (checkStoragePermission() && checkNotificationPermission()) {
            bottomSheetDialog.dismiss()
        } else {

            if (checkStoragePermission()) {

                permissionLayoutBinding.btnStorage.clearAnimation()

                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, 5000L)

                permissionLayoutBinding.btnStorage.isEnabled = false
                permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_exit)

                permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#808080"))

                permissionLayoutBinding.tvDes.text = getString(R.string.string_notification)
                permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_notification)

                permissionLayoutBinding.btnNotification.isEnabled = true
                permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_skip_permission)
                permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#FFFFFF"))

            } else {

                permissionLayoutBinding.btnNotification.clearAnimation()

                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, 5000L)

                permissionLayoutBinding.btnStorage.isEnabled = true
                permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_skip_permission)
                permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#FFFFFF"))

                permissionLayoutBinding.tvDes.text = getString(R.string.string_storage)
                permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_storage)

                permissionLayoutBinding.btnNotification.isEnabled = false
                permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_exit)

                permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#808080"))

            }
        }

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

        webView?.let { setAudioMuted(it, true) }

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
            mediaPlaybackRequiresUserGesture = true // Chặn autoplay
        }

        AppLogger.d(webTab.toString())

        if (webTab.getWebView() != null) {
            binding.webviewContainer.addView(
                webTab.getWebView(),
                LinearLayout.LayoutParams(-1, -1)
            )
        }
    }

    private fun onWebViewPause() {
        webTab.getWebView()?.onPause()
    }

    private fun onWebViewResume() {
        webTab.getWebView()?.onResume()
    }

    private fun handleLoadPageEvent() {
        tabViewModel.loadPageEvent.observe(viewLifecycleOwner) { tab ->
            if (tab.getUrl().startsWith("http")) {
                webTab.getWebView()?.stopLoading()
                webTab.getWebView()?.loadUrl(tab.getUrl())


            }
        }
    }

    private fun onVideoPushed() {
        Toast.makeText(
            requireContext(), getString(R.string.string_video_found), Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }

    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
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

//                if (this@WebTabActivity::videoInfoAdapter.isInitialized) {
//                    videoInfoAdapter.notifyDataSetChanged()
//                }

                lifecycleScope.launch(Dispatchers.IO) {

                    videoInfoAdapter = VideoInfoAdapter(
                        requireContext(),
                        videoDetectionTabViewModel?.detectedVideosList?.get()?.toList()
                            ?: emptyList(),
                        videoDetectionTabViewModel,
                        this@ProcessingFragment,
                        appUtil
                    )

                    withContext(Dispatchers.Main) {

                        downloadBinding.videoInfoList.layoutManager =
                            LinearLayoutManager(
                                requireContext(),
                                LinearLayoutManager.VERTICAL,
                                false
                            )

                        downloadBinding.videoInfoList.adapter = videoInfoAdapter
                    }

                }

            }
        })

        videoInfoAdapter = VideoInfoAdapter(
            requireContext(),
            videoDetectionTabViewModel?.detectedVideosList?.get()?.toList() ?: emptyList(),
            videoDetectionTabViewModel,
            this,
            appUtil
        )

        downloadBinding.videoInfoList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        downloadBinding.videoInfoList.adapter = videoInfoAdapter

        downloadBinding.icEdit.setOnClickListener {
            showDialogRename(videoInfo, titles[videoInfo.id].toString())
        }

        downloadBinding.imgClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        downloadBinding.cvImage.setOnClickListener {

        }


        bottomSheetDialog.show()
    }

    private fun showDialogRename(videoInfo: VideoInfo, name: String) {
        val dialogRename = DialogRename(requireContext(), name) { it ->
            downloadBinding.nameFile.text = it

            val title = it.toString()
            val titlesF =
                videoDetectionTabViewModel.formatsTitles.get()?.toMutableMap() ?: mutableMapOf()
            titlesF[videoInfo.id] = title
            videoDetectionTabViewModel.formatsTitles.set(titlesF)
        }

        dialogRename.show()
    }


    override fun onCloseClicked(downloadId: Long, isRegular: Boolean) {
        progressViewModel.cancelDownload(downloadId, true)
    }

    override fun onPlayPauseDownloadClicked(
        view: View,
        downloadId: Long,
        isRegular: Boolean,
        isPlay: Boolean
    ) {
        if (isPlay) {
            progressViewModel.pauseDownload(downloadId)
        } else {
            progressViewModel.resumeDownload(downloadId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressViewModel.stop()

        tabViewModel.stop()
        videoDetectionModel.stop()
        videoDetectionTabViewModel.stop()
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

        Toast.makeText(
            requireContext(), getString(R.string.download_started), Toast.LENGTH_SHORT
        ).show()

    }

    override fun onCancel() {
    }

    @OptIn(UnstableApi::class)
    override fun onPreviewVideo(videoInfo: VideoInfo, format: String, isForce: Boolean) {
        startActivity(
            Intent(
                requireContext(), PlayMediaActivity::class.java
            ).apply {

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
                    putExtra(
                        PlayMediaActivity.ITEM_TYPE, "video"
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

    private fun showAdsNativePopupPermission(frAds: FrameLayout) {
        if (AdsConstant.isLoadNativePopupPermission && requireContext().hasNetworkConnection() && ConsentHelper.getInstance(
                requireContext()
            ).canRequestAds()
        ) {
            if (nativePopupPermission != null) {
                val adView = if (Admob.getInstance().isLoadFullAds) {
                    LayoutInflater.from(requireContext())
                        .inflate(
                            R.layout.layout_ads_native_update_no_bor,
                            null
                        ) as NativeAdView
                } else {
                    LayoutInflater.from(requireContext())
                        .inflate(R.layout.layout_ads_native_update, null) as NativeAdView
                }
                val nativeAdView = adView as NativeAdView
                frAds.removeAllViews()
                frAds.addView(adView)

                Admob.getInstance().pushAdsToViewCustom(nativePopupPermission, nativeAdView)
            } else {
                Admob.getInstance().loadNativeAd(
                    requireContext(),
                    this.getString(R.string.native_popup_permission),
                    object : NativeCallback() {
                        override fun onNativeAdLoaded(nativeAd: com.google.android.gms.ads.nativead.NativeAd) {
                            nativePopupPermission = nativeAd
                            val adView = if (Admob.getInstance().isLoadFullAds) {
                                LayoutInflater.from(requireContext())
                                    .inflate(
                                        R.layout.layout_ads_native_update_no_bor,
                                        null
                                    ) as NativeAdView
                            } else {
                                LayoutInflater.from(requireContext())
                                    .inflate(
                                        R.layout.layout_ads_native_update,
                                        null
                                    ) as NativeAdView
                            }
                            val nativeAdView = adView as NativeAdView
                            frAds.removeAllViews()
                            frAds.addView(adView)

                            Admob.getInstance().pushAdsToViewCustom(nativeAd, nativeAdView)

                        }

                        override fun onAdFailedToLoad() {
                            nativePopupPermission = null
                            frAds.removeAllViews()
                        }

                    }
                )
            }

        } else {
            frAds.removeAllViews()
        }
    }

}

class WrapContentLinearLayoutManager : LinearLayoutManager {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            AppLogger.e("meet a IOOBE in RecyclerView")
        }
    }
}
