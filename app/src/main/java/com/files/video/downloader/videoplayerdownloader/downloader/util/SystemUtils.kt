package com.files.video.downloader.videoplayerdownloader.downloader.util

import android.content.Context
import android.webkit.CookieManager
import android.widget.Toast
import com.files.video.downloader.videoplayerdownloader.downloader.R
//import com.allVideoDownloaderXmaster.OpenForTesting
import javax.inject.Inject

//@OpenForTesting
class SystemUtils @Inject constructor() {

    fun clearCookies(context: Context?) {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        context?.let {
            Toast.makeText(it, it.getString(R.string.cookies_cleared), Toast.LENGTH_SHORT)
                .show()
        }
    }
}