package com.files.video.downloader.videoplayerdownloader.downloader

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.google.firebase.FirebaseApp
import com.nlbn.ads.util.Adjust
import com.nlbn.ads.util.AdsApplication
import com.nlbn.ads.util.AppOpenManager
import com.files.video.downloader.videoplayerdownloader.downloader.ui.splash.SplashActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.SystemUtil
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class Application : AdsApplication() {

    val globalViewModel by lazy {
        ViewModelProvider(
            ViewModelStore(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        ).get(TabViewModel::class.java)
    }

    override fun onCreate() {
        super.onCreate()

        SystemUtil.setLocale(this)

        FirebaseApp.initializeApp(this)


        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)

    }

    override fun enableAdsResume(): Boolean = true
    override fun getKeyRemoteIntervalShowInterstitial(): String {
        return "interval_show_interstitial"
    }

    override fun getListTestDeviceId(): MutableList<String>? = null

    override fun getResumeAdId(): String = ""

    override fun buildDebug(): Boolean = BuildConfig.DEBUG

    override fun enableAdjustTracking(): Boolean {
        return true
    }

    override fun logRevenueAdjustWithCustomEvent(p0: Double, p1: String?) {
        Adjust.getInstance().logRevenueWithCustomEvent("sothyk", p0, p1)
    }

    override fun getAdjustToken(): String {
        return "thyg2mqmypkw"
    }



}