package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Message
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityWebTabBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.PageTabProvider
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTab
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent

class CustomWebChromeClient(
//    private val tabViewModel: WebTabViewModel,
//    private val settingsViewModel: SettingsViewModel,
//    private val updateTabEvent: SingleLiveEvent<WebTab>,
//    private val pageTabProvider: PageTabProvider,
    private val dataBinding: ActivityWebTabBinding,
    private val appUtil: AppUtil,
    private val webTabActivity: WebTabActivity
) : WebChromeClient() {
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
            val isAd = false

//                if (settingsViewModel.isAdBlocker.get()) {
//                tabViewModel.isAd(url)
//            } else {
//                false
//            }
            if (url.isEmpty() || !url.startsWith("http") || isAd || !isUserGesture) {
                return false
            }

            val transport = resultMsg!!.obj as WebView.WebViewTransport
            transport.webView = WebView(view.context)

//            tabViewModel.openPageEvent.value =
//                WebTab(
//                    webview = transport.webView,
//                    resultMsg = resultMsg,
//                    url = "url",
//                    title = view.title,
//                    iconBytes = null
//                )
            return true
        }
        return false
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
//        val pageTab = pageTabProvider.getPageTab(tabViewModel.thisTabIndex.get())
//
//        val headers = pageTab.getHeaders() ?: emptyMap()
//        val updateTab = WebTab(
//            pageTab.getUrl(),
//            pageTab.getTitle(),
//            icon ?: pageTab.getFavicon(),
//            headers,
//            view,
//            id = pageTab.id
//        )
//        updateTabEvent.value = updateTab
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
//        tabViewModel.setProgress(newProgress)
//        if (newProgress == 100) {
//            tabViewModel.isShowProgress.set(false)
//        } else {
//            tabViewModel.isShowProgress.set(true)
//        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        (webTabActivity).requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        dataBinding.webviewContainer.visibility = View.GONE
//        dataBinding.customView.rootView.findViewById<View>(R.id.bottom_bar).visibility =
//            View.GONE
        (webTabActivity).window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        dataBinding.customView.addView(view)
        appUtil.hideSystemUI(webTabActivity.window, dataBinding.customView)
        dataBinding.customView.visibility = View.VISIBLE
        dataBinding.containerBrowser.visibility =
            View.GONE
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        dataBinding.customView.removeAllViews()
        dataBinding.webviewContainer.visibility = View.VISIBLE
        dataBinding.customView.visibility = View.GONE
        (webTabActivity).window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        dataBinding.customView.rootView.findViewById<View>(R.id.bottom_bar).visibility =
//            View.VISIBLE
        dataBinding.containerBrowser.visibility =
            View.VISIBLE
        webTabActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

//            if (settingsViewModel.isLockPortrait.get()) {
//            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//        } else {
//            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//        }
        appUtil.showSystemUI(webTabActivity.window, dataBinding.customView)
    }
}
