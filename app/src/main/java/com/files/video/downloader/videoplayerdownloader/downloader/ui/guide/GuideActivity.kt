package com.files.video.downloader.videoplayerdownloader.downloader.ui.guide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityGuideBinding
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.ui.disclaimers.DisclaimersActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuideActivity : BaseActivity<ActivityGuideBinding>() {

    override fun setBinding(layoutInflater: LayoutInflater): ActivityGuideBinding {
        return ActivityGuideBinding.inflate(layoutInflater)
    }

    override fun initView() {

        loadNativeGuide()

        if (intent.getStringExtra("open") == "home") {
            binding.tvStep1.text = HtmlCompat.fromHtml(
                getString(R.string.string_step_1),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.tvStep2.text = HtmlCompat.fromHtml(
                getString(R.string.string_step_2),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.imgStep1.setImageResource(R.drawable.img_step_1)
            binding.imgStep2.setImageResource(R.drawable.img_step_2)

        } else {
            binding.tvStep1.text = HtmlCompat.fromHtml(
                getString(R.string.string_step_1_process),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.tvStep2.text = HtmlCompat.fromHtml(
                getString(R.string.string_step_2_process),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.imgStep1.setImageResource(R.drawable.img_step_1_process)
            binding.imgStep2.setImageResource(R.drawable.img_step_2_process)
        }

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadNativeGuide() {
        if (hasNetworkConnection() && ConsentHelper.getInstance(this)
                .canRequestAds() && AdsConstant.isLoadNativeGuide
        ) {

            if (AdsConstant.nativeAdsAll != null) {
                val adView = if (Admob.getInstance().isLoadFullAds) {
                    LayoutInflater.from(this@GuideActivity)
                        .inflate(
                            R.layout.layout_ads_native_update_no_bor,
                            null
                        ) as NativeAdView
                } else {
                    LayoutInflater.from(this@GuideActivity)
                        .inflate(
                            R.layout.layout_ads_native_update,
                            null
                        ) as NativeAdView
                }
                binding.frAds.removeAllViews()
                binding.frAds.addView(adView)
                Admob.getInstance().pushAdsToViewCustom(AdsConstant.nativeAdsAll, adView)
            } else {
                try {
                    Admob.getInstance().loadNativeAd(
                        this,
                        getString(R.string.native_all),
                        object : NativeCallback() {
                            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                                val adView = if (Admob.getInstance().isLoadFullAds) {
                                    LayoutInflater.from(this@GuideActivity)
                                        .inflate(
                                            R.layout.layout_ads_native_update_no_bor,
                                            null
                                        ) as NativeAdView
                                } else {
                                    LayoutInflater.from(this@GuideActivity)
                                        .inflate(
                                            R.layout.layout_ads_native_update,
                                            null
                                        ) as NativeAdView
                                }
                                binding.frAds.removeAllViews()
                                binding.frAds.addView(adView)
                                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                            }

                            override fun onAdFailedToLoad() {
                                binding.frAds.removeAllViews()
                            }
                        })
                } catch (e: Exception) {
                    binding.frAds.removeAllViews()
                }
            }
        } else {
            binding.frAds.removeAllViews()
        }
    }

    override fun onBackPressed() {
        if (intent.getStringExtra("open") == "home") {
            if (hasNetworkConnection() && ConsentHelper.getInstance(this)
                    .canRequestAds() && AdsConstant.isLoadInterBack && Admob.getInstance().isLoadFullAds
            ) {
                Admob.getInstance().loadAndShowInter(
                    this,
                    getString(R.string.inter_back), true,
                    object : AdCallback() {
                        override fun onNextAction() {
                            finish()
                        }

                        override fun onAdFailedToLoad(p0: LoadAdError?) {
                            finish()
                        }
                    })
            } else {
                finish()
            }
        } else {
            finish()
        }
    }


    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GuideActivity::class.java)
        }
    }
}