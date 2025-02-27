package com.files.video.downloader.videoplayerdownloader.downloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.AdBlockHostsRemoteDataSource
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityMainBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogExitApp
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.RatingDialog
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.BrowserFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.DownloadedFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.processing.ProcessingFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.settings.SettingsFragment
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsInitializerHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.UpdateEvent
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.youtubedl_downloader.YoutubeDlDownloaderWorker
import com.google.android.gms.ads.LoadAdError
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.nlbn.ads.banner.BannerPlugin
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    // Color
    private var colorSelected = 0
    private var colorNormal = 0

    @DrawableRes
    private var drawableBrowserNormal: Int = 0

    @DrawableRes
    private var drawableBrowserSelected: Int = 0

    @DrawableRes
    private var drawableProcessingNormal: Int = 0

    @DrawableRes
    private var drawableProcessingSelected: Int = 0

    @DrawableRes
    private var drawableDownloadedNormal: Int = 0

    @DrawableRes
    private var drawableDownloadedSelected: Int = 0

    @DrawableRes
    private var drawableSettingsNormal: Int = 0

    @DrawableRes
    private var drawableSettingsSelected: Int = 0

    private var posSelectedNavigation = 0

    @Inject
    lateinit var adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource

    override fun setBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {

        if (this.hasNetworkConnection() && AdsConstant.isLoadBanner && Admob.getInstance().isLoadFullAds) {
            binding.frBanner.visibility = View.VISIBLE
        } else {
            binding.frBanner.visibility = View.GONE
        }

        val consentHelper = ConsentHelper.getInstance(this)
        if (!consentHelper.canLoadAndShowAds()) {
            consentHelper.reset()
        }
        consentHelper.obtainConsentAndShow(this) {

            loadBanner()
            AdsConstant.loadNativeAll(this)

            EventBus.getDefault().postSticky(UpdateEvent("Ads"))
        }

        AdsInitializerHelper.initializeAdBlocker(
            preferenceHelper,
            lifecycleScope,
            adBlockHostsRemoteDataSource
        )

        drawableBrowserNormal = R.drawable.ic_browser_navigation_normal
        drawableBrowserSelected = R.drawable.ic_browser_navigation_selected
        drawableProcessingNormal = R.drawable.ic_processing_navigation_normal
        drawableProcessingSelected = R.drawable.ic_processing_navigation_selected
        drawableDownloadedNormal = R.drawable.ic_downloaded_navigation_normal
        drawableDownloadedSelected = R.drawable.ic_downloaded_navigation_selected
        drawableSettingsNormal = R.drawable.ic_settings_navigation_normal
        drawableSettingsSelected = R.drawable.ic_settings_navigation_selected


        /* color text */
        colorNormal = Color.parseColor("#BFBFBF")
        colorSelected = Color.parseColor("#A264FF")

        replaceFragment(BrowserFragment(), "BrowserFragment")

        changeImageIconWhenTap(
            drawableBrowserSelected,
            drawableProcessingNormal,
            drawableDownloadedNormal,
            drawableSettingsNormal
        )
        changeTextColorWhenTap(colorSelected, colorNormal, colorNormal, colorNormal)

        initListener()

        if (intent?.getBooleanExtra(
                YoutubeDlDownloaderWorker.IS_FINISHED_DOWNLOAD_ACTION_KEY,
                false
            ) == true
        ) {
            if (intent.getBooleanExtra(
                    YoutubeDlDownloaderWorker.IS_FINISHED_DOWNLOAD_ACTION_ERROR_KEY,
                    false
                )
            ) {
                if (posSelectedNavigation != 1) {
                    posSelectedNavigation = 1
                    changeImageIconWhenTap(
                        drawableBrowserNormal,
                        drawableProcessingSelected,
                        drawableDownloadedNormal,
                        drawableSettingsNormal
                    )
                    changeTextColorWhenTap(colorNormal, colorSelected, colorNormal, colorNormal)

                    replaceFragment(ProcessingFragment(), "ProcessingFragment")
                }
            } else {
                if (posSelectedNavigation != 2) {
                    posSelectedNavigation = 2
                    changeImageIconWhenTap(
                        drawableBrowserNormal,
                        drawableProcessingNormal,
                        drawableDownloadedSelected,
                        drawableSettingsNormal
                    )
                    changeTextColorWhenTap(colorNormal, colorNormal, colorSelected, colorNormal)

                    replaceFragment(DownloadedFragment(), "DownloadedFragment")
                }
            }

            if (intent.hasExtra(YoutubeDlDownloaderWorker.DOWNLOAD_FILENAME_KEY)) {
                val downloadFileName =
                    intent.getStringExtra(YoutubeDlDownloaderWorker.DOWNLOAD_FILENAME_KEY)
                        .toString()

                Log.d("ntt", "initView: $downloadFileName")

                Handler(Looper.getMainLooper()).postDelayed({
                    tabViewModels.openDownloadedVideoEvent.value = downloadFileName
                }, 1000)
            }
        } else {
            if (intent?.hasExtra(YoutubeDlDownloaderWorker.IS_FINISHED_DOWNLOAD_ACTION_KEY) == true) {
                if (posSelectedNavigation != 1) {
                    posSelectedNavigation = 1
                    changeImageIconWhenTap(
                        drawableBrowserNormal,
                        drawableProcessingSelected,
                        drawableDownloadedNormal,
                        drawableSettingsNormal
                    )
                    changeTextColorWhenTap(colorNormal, colorSelected, colorNormal, colorNormal)

                    replaceFragment(ProcessingFragment(), "ProcessingFragment")
                }
            } else {
                if (posSelectedNavigation != 0) {
                    posSelectedNavigation = 0
                    changeImageIconWhenTap(
                        drawableBrowserSelected,
                        drawableProcessingNormal,
                        drawableDownloadedNormal,
                        drawableSettingsNormal
                    )
                    changeTextColorWhenTap(colorSelected, colorNormal, colorNormal, colorNormal)

                    replaceFragment(BrowserFragment(), "BrowserFragment")
                }
            }
        }

    }


    private fun initListener() {
        binding.layoutBrowser.setOnClickListener {
            if (posSelectedNavigation != 0) {
                showInterTabHome()
                posSelectedNavigation = 0
                changeImageIconWhenTap(
                    drawableBrowserSelected,
                    drawableProcessingNormal,
                    drawableDownloadedNormal,
                    drawableSettingsNormal
                )
                changeTextColorWhenTap(colorSelected, colorNormal, colorNormal, colorNormal)

                replaceFragment(BrowserFragment(), "BrowserFragment")
            }
        }

        binding.layoutProcessing.setOnClickListener {
            if (posSelectedNavigation != 1) {
                showInterTabHome()
                posSelectedNavigation = 1
                changeImageIconWhenTap(
                    drawableBrowserNormal,
                    drawableProcessingSelected,
                    drawableDownloadedNormal,
                    drawableSettingsNormal
                )
                changeTextColorWhenTap(colorNormal, colorSelected, colorNormal, colorNormal)

                replaceFragment(ProcessingFragment(), "ProcessingFragment")
            }
        }

        binding.layoutDownloaded.setOnClickListener {
            if (posSelectedNavigation != 2) {
                showInterTabHome()
                posSelectedNavigation = 2
                changeImageIconWhenTap(
                    drawableBrowserNormal,
                    drawableProcessingNormal,
                    drawableDownloadedSelected,
                    drawableSettingsNormal
                )
                changeTextColorWhenTap(colorNormal, colorNormal, colorSelected, colorNormal)

                replaceFragment(DownloadedFragment(), "DownloadedFragment")
            }
        }

        binding.layoutSetting.setOnClickListener {
            if (posSelectedNavigation != 3) {
                showInterTabHome()
                posSelectedNavigation = 3
                changeImageIconWhenTap(
                    drawableBrowserNormal,
                    drawableProcessingNormal,
                    drawableDownloadedNormal,
                    drawableSettingsSelected
                )
                changeTextColorWhenTap(colorNormal, colorNormal, colorNormal, colorSelected)

                replaceFragment(SettingsFragment(), "SettingsFragment")
            }
        }

    }

    private fun changeTextColorWhenTap(
        color1: Int,
        color2: Int,
        color3: Int,
        color4: Int,
    ) {
        binding.tvBrowser.setTextColor(color1)
        binding.tvProcessing.setTextColor(color2)
        binding.tvDownloaded.setTextColor(color3)
        binding.tvSetting.setTextColor(color4)
    }

    /* change image bottom nav */
    private fun changeImageIconWhenTap(
        @DrawableRes image1: Int,
        @DrawableRes image2: Int,
        @DrawableRes image3: Int,
        @DrawableRes image4: Int,
    ) {
        binding.ivBrowser.setImageResource(image1)
        binding.ivProcessing.setImageResource(image2)
        binding.ivDownloaded.setImageResource(image3)
        binding.ivSetting.setImageResource(image4)
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment, tag)
        fragmentTransaction.commit()
    }

    private fun showInterTabHome() {
        if (hasNetworkConnection() && ConsentHelper.getInstance(
                this
            ).canRequestAds() && AdsConstant.isLoadInterTabHome && Admob.getInstance().isLoadFullAds
        ) {
            Admob.getInstance().loadAndShowInter(
                this,
                getString(R.string.inter_tab_home), true,
                object : AdCallback() {
                    override fun onNextAction() {

                    }

                    override fun onAdFailedToLoad(p0: LoadAdError?) {

                    }
                })
        } else {

        }
    }

    private fun loadBanner() {
        if (this.hasNetworkConnection() && ConsentHelper.getInstance(this).canRequestAds() && Admob.getInstance().isLoadFullAds) {

            if (AdsConstant.isLoadBanner) {
                binding.frBanner.visibility = View.VISIBLE
                val config = BannerPlugin.Config()
                config.defaultAdUnitId = getString(R.string.banner)
                config.defaultBannerType = BannerPlugin.BannerType.Adaptive
                val cbFetchInterval =
                    FirebaseRemoteConfig.getInstance().getLong("cb_fetch_interval").toInt()
                config.defaultRefreshRateSec = cbFetchInterval
                config.defaultCBFetchIntervalSec = cbFetchInterval
                Admob.getInstance().loadBannerPlugin(
                    this,
                    findViewById<ViewGroup>(R.id.fr_banner),
                    findViewById<ViewGroup>(R.id.include),
                    config
                )
            } else {
                binding.frBanner.visibility = View.GONE
            }

        } else {
            binding.frBanner.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        val countOpenApp = preferenceHelper.getCountExitApp()

        Log.d("ntt", countOpenApp.toString())

        if (!preferenceHelper.isRate() && (countOpenApp % 2 == 0)) {
            showDialogRate(true)
        } else {

            showDialogExitApp()

        }
    }

    private fun showDialogExitApp() {
        EventBus.getDefault().post(UpdateEvent("hide_ads"))
        val dialogExitApp = DialogExitApp(this@MainActivity) {
            preferenceHelper.increaseCountExitApp()

            finishAffinity()
            exitProcess(1)
        }

        dialogExitApp.show()

        dialogExitApp.setOnDismissListener {
            EventBus.getDefault().post(UpdateEvent("show_ads"))
        }
    }

    private fun showDialogRate(isExit: Boolean) {
        EventBus.getDefault().post(UpdateEvent("hide_ads"))
        val ratingDialog = RatingDialog(this)
        ratingDialog.init(this, object : RatingDialog.OnPress {
            override fun sendThank() {
                preferenceHelper.forceRated()
                ratingDialog.dismiss()

                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.string_thank_for_rate),
                    Toast.LENGTH_SHORT
                ).show()

                if (isExit) {
                    finishAffinity()

                    exitProcess(1)
                }

            }

            override fun rating() {
                val manager = ReviewManagerFactory.create(this@MainActivity)
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(this@MainActivity, reviewInfo)
                        flow.addOnSuccessListener {
                            preferenceHelper.forceRated()
                            ratingDialog.dismiss()

                            if (isExit) {
                                finishAffinity()
                                exitProcess(1)
                            }

                        }
                    } else {
                        preferenceHelper.forceRated()
                        ratingDialog.dismiss()

                        if (isExit) {
                            finishAffinity()
                            exitProcess(1)
                        }
                    }
                }
            }

            override fun later() {
                ratingDialog.dismiss()

                if (isExit) {

                    preferenceHelper.increaseCountExitApp()

                    finishAffinity()
                    exitProcess(1)
                } else {
                    preferenceHelper.increaseCountBackHome()
                }

            }

        })

        ratingDialog.show()

        ratingDialog.setOnDismissListener {
            EventBus.getDefault().post(UpdateEvent("show_ads"))
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }
}