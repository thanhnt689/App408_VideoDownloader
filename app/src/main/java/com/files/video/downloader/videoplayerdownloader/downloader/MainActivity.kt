package com.files.video.downloader.videoplayerdownloader.downloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.AdBlockHostsRemoteDataSource
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityMainBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.BrowserFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.DownloadedFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.processing.ProcessingFragment
import com.files.video.downloader.videoplayerdownloader.downloader.ui.settings.SettingsFragment
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsInitializerHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.youtubedl_downloader.YoutubeDlDownloaderWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

        replaceFragment(BrowserFragment(),"BrowserFragment")

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

                    replaceFragment(ProcessingFragment(),"ProcessingFragment")
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

                    replaceFragment(DownloadedFragment(),"DownloadedFragment")
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

                    replaceFragment(ProcessingFragment(),"ProcessingFragment")
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

                    replaceFragment(BrowserFragment(),"BrowserFragment")
                }
            }
        }

    }


    private fun initListener() {
        binding.layoutBrowser.setOnClickListener {

            if (posSelectedNavigation != 0) {
                posSelectedNavigation = 0
                changeImageIconWhenTap(
                    drawableBrowserSelected,
                    drawableProcessingNormal,
                    drawableDownloadedNormal,
                    drawableSettingsNormal
                )
                changeTextColorWhenTap(colorSelected, colorNormal, colorNormal, colorNormal)

                replaceFragment(BrowserFragment(),"BrowserFragment")
            }
        }

        binding.layoutProcessing.setOnClickListener {
            if (posSelectedNavigation != 1) {
                posSelectedNavigation = 1
                changeImageIconWhenTap(
                    drawableBrowserNormal,
                    drawableProcessingSelected,
                    drawableDownloadedNormal,
                    drawableSettingsNormal
                )
                changeTextColorWhenTap(colorNormal, colorSelected, colorNormal, colorNormal)

                replaceFragment(ProcessingFragment(),"ProcessingFragment")
            }
        }

        binding.layoutDownloaded.setOnClickListener {
            if (posSelectedNavigation != 2) {
                posSelectedNavigation = 2
                changeImageIconWhenTap(
                    drawableBrowserNormal,
                    drawableProcessingNormal,
                    drawableDownloadedSelected,
                    drawableSettingsNormal
                )
                changeTextColorWhenTap(colorNormal, colorNormal, colorSelected, colorNormal)

                replaceFragment(DownloadedFragment(),"DownloadedFragment")
            }
        }

        binding.layoutSetting.setOnClickListener {
            if (posSelectedNavigation != 3) {
                posSelectedNavigation = 3
                changeImageIconWhenTap(
                    drawableBrowserNormal,
                    drawableProcessingNormal,
                    drawableDownloadedNormal,
                    drawableSettingsSelected
                )
                changeTextColorWhenTap(colorNormal, colorNormal, colorNormal, colorSelected)

                replaceFragment(SettingsFragment(),"SettingsFragment")
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

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }
}