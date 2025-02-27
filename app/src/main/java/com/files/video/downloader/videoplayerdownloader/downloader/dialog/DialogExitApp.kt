package com.files.video.downloader.videoplayerdownloader.downloader.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogExitAppBinding
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper


class DialogExitApp(private var mContext: Context, var exit: () -> Unit) : Dialog(mContext) {
    private lateinit var binding: DialogExitAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogExitAppBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        window?.apply {
            val windowParams = attributes
            setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            windowParams.dimAmount = 0.8f
            attributes = windowParams
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        initViews()

        initListener()

        showAdsNativePopupExit()

    }

    private fun showAdsNativePopupExit() {
        if (AdsConstant.isLoadNativePopupPermission && mContext.hasNetworkConnection() && ConsentHelper.getInstance(
                mContext
            ).canRequestAds() && Admob.getInstance().isLoadFullAds
        ) {

            Admob.getInstance().loadNativeAd(
                mContext,
                mContext.getString(R.string.native_popup_permission),
                object : NativeCallback() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        val adView = if (Admob.getInstance().isLoadFullAds) {
                            LayoutInflater.from(mContext)
                                .inflate(
                                    R.layout.layout_ads_native_update_no_bor,
                                    null
                                ) as NativeAdView
                        } else {
                            LayoutInflater.from(mContext)
                                .inflate(
                                    R.layout.layout_ads_native_update,
                                    null
                                ) as NativeAdView
                        }
                        val nativeAdView = adView as NativeAdView
                        binding.frAds.removeAllViews()
                        binding.frAds.addView(adView)

                        Admob.getInstance().pushAdsToViewCustom(nativeAd, nativeAdView)

                    }

                    override fun onAdFailedToLoad() {
                        binding.frAds.removeAllViews()
                    }

                }
            )

        } else {
            binding.frAds.removeAllViews()
        }
    }


    private fun initViews() {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun initListener() {
        binding.tvYes.setOnClickListener {
            dismiss()
            exit()
        }

        binding.tvNo.setOnClickListener {
            dismiss()
        }
    }

}