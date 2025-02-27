package com.files.video.downloader.videoplayerdownloader.downloader.ui.splash

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import androidx.work.WorkManager
import com.files.video.downloader.videoplayerdownloader.downloader.BuildConfig
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.realdrum.simpledrumsrock.drumpadmachine.utils.widget.ICallBackProgress
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivitySplashBinding
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.ui.intro.IntroActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.language.LanguageActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo.PrivateVideoViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.files.video.downloader.videoplayerdownloader.downloader.util.InAppUpdate
import com.files.video.downloader.videoplayerdownloader.downloader.util.InstallUpdatedListener
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.DaggerWorkerFactory
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private val privateVideoViewModel: PrivateVideoViewModel by viewModels()

    private var interCallback: AdCallback? = null

    private lateinit var inAppUpdate: InAppUpdate

    override fun setBinding(layoutInflater: LayoutInflater): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun initView() {

        getRemoteConfig()

        lifecycleScope.launch {
            privateVideoViewModel.matchAndRemoveDeletedFiles()
        }

        binding.loadFile.onProgress = object : ICallBackProgress {
            override fun onProgress(progress: Int) {
                binding.tvLoading.text = getString(R.string.string_loading, progress.toString())
            }
        }

        val consentHelper = ConsentHelper.getInstance(this)
        if (!consentHelper.canLoadAndShowAds()) {
            consentHelper.reset()
        }

        val remoteConfig = FirebaseRemoteConfig.getInstance()

        inAppUpdate = InAppUpdate(this, remoteConfig.getBoolean("force_update"), object :
            InstallUpdatedListener {
            override fun onUpdateNextAction() {

                if (hasNetworkConnection()) {
                    consentHelper.obtainConsentAndShow(this@SplashActivity) {

                        if (AdsConstant.isLoadInterSplash) {
                            loadInterAdsSplash()
                        } else {
                            Handler(Looper.getMainLooper()).postDelayed({
                                startNextAct()
                            }, 3000)
                        }

                        if (AdsConstant.isLoadNativeLanguageSelect) {
                            AdsConstant.loadNativeLanguageSelect(this@SplashActivity)
                        }

                    }
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startNextAct()
                    }, 3000)
                }

            }

            override fun onUpdateCancel() {
                finish()
            }

        })


    }

    private fun loadInterAdsSplash() {

        Log.d("ntt", "loadInterAdsSplash: ")

        interCallback = object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                startNextAct()
            }
        }

        Admob.getInstance()
            .loadSplashInterAds2(this, getString(R.string.inter_splash), 3000, interCallback)

    }

    override fun onResume() {
        super.onResume()
        if (this::inAppUpdate.isInitialized) {
            inAppUpdate.onResume()
        }
        Admob.getInstance().onCheckShowSplashWhenFail(this, interCallback, 3000)
    }

    override fun onStop() {
        super.onStop()
        Admob.getInstance().dismissLoadingDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::inAppUpdate.isInitialized) {
            inAppUpdate.onDestroy()
        }
        Admob.getInstance().dismissLoadingDialog()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (this::inAppUpdate.isInitialized) {
            inAppUpdate.onActivityResult(requestCode, resultCode, data)
        }
    }


    private fun startNextAct() {
        if (preferenceHelper.getBoolean(PreferenceHelper.PREF_SHOWED_START_LANGUAGE) == true) {
            startActivity(
                IntroActivity.newIntent(this)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        } else {
            startActivity(
                LanguageActivity.newIntent(this, true)
            )
        }
    }

    private fun getRemoteConfig() {

        var index = BuildConfig.Minimum_Fetch
        Log.e("ntt", "initRemoteConfig splash: " + index)
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(index)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        mFirebaseRemoteConfig.fetchAndActivate()

        Log.d("ntt", "getRemoteConfig: 1")

        mFirebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    AdsConstant.isLoadInterSplash =
                        mFirebaseRemoteConfig.getBoolean("is_load_inter_splash")
                    AdsConstant.isLoadNativeLanguage =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_language1")
                    AdsConstant.isLoadNativeLanguageSelect =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_language_select")
                    AdsConstant.isLoadNativeLanguageSetting =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_language_setting")
                    AdsConstant.isLoadNativeIntro1 =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_intro_1")
                    AdsConstant.isLoadNativeIntro2 =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_intro_2")
                    AdsConstant.isLoadNativeIntro3 =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_intro_3")
                    AdsConstant.isLoadNativeIntro4 =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_intro_4")
                    AdsConstant.isLoadInterIntro =
                        mFirebaseRemoteConfig.getBoolean("is_load_inter_intro")
                    AdsConstant.isLoadNativePermission =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_permission")
                    AdsConstant.isLoadNativePermissionNotice =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_permission_notice")
                    AdsConstant.isLoadNativePermissionStorage =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_permission_storage")
                    AdsConstant.isLoadNativePopupPermission =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_popup_permission")
                    AdsConstant.isLoadNativePopupExit =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_popup_exit")
                    AdsConstant.isLoadInterBack =
                        mFirebaseRemoteConfig.getBoolean("is_load_inter_back")
                    AdsConstant.isLoadBanner =
                        mFirebaseRemoteConfig.getBoolean("is_load_banner")
                    AdsConstant.isLoadInterTabHome =
                        mFirebaseRemoteConfig.getBoolean("is_load_inter_tab_home")
                    AdsConstant.isLoadInterDownloadedItem =
                        mFirebaseRemoteConfig.getBoolean("is_load_inter_downloaded_item")
                    AdsConstant.isLoadInterPrivateAdd =
                        mFirebaseRemoteConfig.getBoolean("is_load_inter_private_add")
                    AdsConstant.isLoadNativeHome =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_home")
                    AdsConstant.isLoadNativeNoMediaDownloaded =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_nomedia_downloaded")
                    AdsConstant.isLoadNativeTab =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_tabs")
                    AdsConstant.isLoadNativeHistory =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_history")
                    AdsConstant.isLoadNativeBookMark =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_bookmark")
                    AdsConstant.isLoadNativeSecurity =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_security")
                    AdsConstant.isLoadNativeAdd =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_add")
                    AdsConstant.isLoadNativeAdd =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_private")
                    AdsConstant.isLoadNativeImageDetail =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_image_detail")
                    AdsConstant.isLoadNativeGuide =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_guide")
                    AdsConstant.isLoadNativeFiles =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_files")
                    AdsConstant.isLoadNativeVideo =
                        mFirebaseRemoteConfig.getBoolean("is_load_native_video")

                } else {
                    Log.d("ntt", "Fetch failed: ${task.exception}")
                }
            }

    }

}