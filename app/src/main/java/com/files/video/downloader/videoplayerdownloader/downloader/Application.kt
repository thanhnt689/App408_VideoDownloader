package com.files.video.downloader.videoplayerdownloader.downloader

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.work.Configuration
import androidx.work.WorkManager
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.google.firebase.FirebaseApp
import com.nlbn.ads.util.Adjust
import com.nlbn.ads.util.AdsApplication
import com.nlbn.ads.util.AppOpenManager
import com.files.video.downloader.videoplayerdownloader.downloader.ui.splash.SplashActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import com.files.video.downloader.videoplayerdownloader.downloader.util.ContextUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SystemUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.DaggerWorkerFactory
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class Application : AdsApplication() {

    @Inject
    lateinit var sharedPrefHelper: PreferenceHelper

//    @Inject
//    lateinit var workerFactory: DaggerWorkerFactory

    @Inject
    lateinit var fileUtil: FileUtil

    val globalViewModel by lazy {
        ViewModelProvider(
            ViewModelStore(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        ).get(TabViewModel::class.java)
    }

    override fun onCreate() {
        super.onCreate()

        ContextUtils.initApplicationContext(applicationContext)

        SystemUtil.setLocale(this)

        FirebaseApp.initializeApp(this)

        initializeFileUtils()

        val file: File = fileUtil.folderDir
        val ctx = applicationContext

//        WorkManager.initialize(
//            ctx,
//            Configuration.Builder()
//                .setWorkerFactory(workerFactory).build()
//        )

        RxJavaPlugins.setErrorHandler { error: Throwable? ->
            AppLogger.e("RxJavaError unhandled $error")
        }

        CoroutineScope(Dispatchers.Default).launch {
            if (!file.exists()) {
                file.mkdirs()
            }

            initializeYoutubeDl()

            updateYoutubeDL()
        }


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

    private fun initializeFileUtils() {
        val isExternal = sharedPrefHelper.getIsExternalUse()
        val isAppDir = sharedPrefHelper.getIsAppDirUse()

        FileUtil.IS_EXTERNAL_STORAGE_USE = isExternal
        FileUtil.IS_APP_DATA_DIR_USE = isAppDir
        FileUtil.INITIIALIZED = true
    }

    private fun initializeYoutubeDl() {
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            AppLogger.e("failed to initialize youtubedl-android $e")
        }
    }

    private fun updateYoutubeDL() {
        try {
            val status = YoutubeDL.getInstance()
                .updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.MASTER)
            AppLogger.d("UPDATE_STATUS MASTER: $status")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }


}