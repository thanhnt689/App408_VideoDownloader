package com.files.video.downloader.videoplayerdownloader.downloader.ui.intro

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.MainActivity
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityIntroBinding
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.ui.permission.PermissionActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private val introAdapter = IntroAdapter(supportFragmentManager, lifecycle)

    private val introAdList = mutableListOf<String>()

    private var isButtonClicked = false

    private var isLoadingIntro = false

    override fun setBinding(layoutInflater: LayoutInflater): ActivityIntroBinding {
        return ActivityIntroBinding.inflate(layoutInflater)
    }

    override fun initView() {

        introAdList.clear()

        introAdList.add(getString(R.string.native_intro_1))
        introAdList.add(getString(R.string.native_intro_2))
        introAdList.add(getString(R.string.native_intro_3))
        introAdList.add(getString(R.string.native_intro_4))

        initViewPager()
        initPagerIndicator()
        initEvent()
    }

    private fun initViewPager() {
        binding.viewPager.adapter = introAdapter
        binding.viewPager.isUserInputEnabled = false
    }

    private fun initPagerIndicator() {

        binding.dotsIndicator.attachTo(binding.viewPager)

        binding.dotsIndicator.dotsClickable = false

    }

    private fun initEvent() {
        binding.tvStart.setOnClickListener {
            if (binding.viewPager.currentItem < introAdapter.itemCount - 1) {
                binding.viewPager.currentItem = binding.viewPager.currentItem + 1
            } else {
                if (!isButtonClicked) {
                    isButtonClicked = true

                    if (hasNetworkConnection() && AdsConstant.isLoadInterIntro && Admob.getInstance().isLoadFullAds) {
                        Admob.getInstance().loadAndShowInter(
                            this,
                            getString(R.string.inter_intro), true,
                            object : AdCallback() {
                                override fun onNextAction() {
                                    nextScreen()
                                }

                                override fun onAdFailedToLoad(p0: LoadAdError?) {
                                    nextScreen()
                                }
                            })
                    } else {
                        nextScreen()
                    }

                }
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                when (position) {
                    0 -> {
                        if (AdsConstant.isLoadNativeIntro1) {
                            loadNativeIntro(0)
                        } else {
                            binding.frNativeAds.removeAllViews()
                        }
                    }

                    1 -> {

                        if (AdsConstant.isLoadNativeIntro2 && Admob.getInstance().isLoadFullAds) {
                            loadNativeIntro(1)
                        } else {
                            binding.frNativeAds.removeAllViews()
                        }

                    }

                    2 -> {

                        if (AdsConstant.isLoadNativeIntro3) {
                            loadNativeIntro(2)
                        } else {
                            binding.frNativeAds.removeAllViews()
                        }
                    }

                    3 -> {

                        if (ConsentHelper.getInstance(this@IntroActivity).canRequestAds()) {

                            lifecycleScope.launch {

                                AdsConstant.loadNativeHome(this@IntroActivity)

                            }
                        }

                        if (AdsConstant.isLoadNativeIntro4 && Admob.getInstance().isLoadFullAds) {
                            loadNativeIntro(3)
                        } else {
                            binding.frNativeAds.removeAllViews()
                        }
                    }
                }


            }
        })
    }

    private fun nextScreen() {

        if (preferenceHelper.showPermission()) {
            startActivity(PermissionActivity.newIntent(this))
        } else {
            startActivity(MainActivity.newIntent(this))
        }

        finish()

    }

    fun loadNativeIntro(position: Int) {
        if (hasNetworkConnection() && ConsentHelper.getInstance(this).canRequestAds()) {
            if (isLoadingIntro) return
            isLoadingIntro = true

            Admob.getInstance().loadNativeAd(
                this@IntroActivity,
                introAdList[position],
                object : NativeCallback() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                        isLoadingIntro = false

                        val adView = if (Admob.getInstance().isLoadFullAds) {
                            LayoutInflater.from(this@IntroActivity)
                                .inflate(R.layout.ads_native_lang_top_no_bor, null) as NativeAdView
                        } else {
                            LayoutInflater.from(this@IntroActivity)
                                .inflate(R.layout.ads_native_lang, null) as NativeAdView
                        }

                        val nativeAdView = adView as NativeAdView
                        binding.frNativeAds.removeAllViews()
                        binding.frNativeAds.addView(adView)
                        Admob.getInstance().pushAdsToViewCustom(nativeAd, nativeAdView)
                    }

                    override fun onAdFailedToLoad() {
                        isLoadingIntro = false
                        binding.frNativeAds.removeAllViews()
                    }
                }
            )

        } else {
            binding.frNativeAds.removeAllViews()
        }
    }


    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, IntroActivity::class.java)
        }
    }
}