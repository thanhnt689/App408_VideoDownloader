package com.files.video.downloader.videoplayerdownloader.downloader.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import kotlinx.coroutines.launch

class AdsConstant {
    companion object {
        var isLoadInterSplash = true
        var isLoadNativeLanguage = true
        var isLoadNativeLanguageSelect = true
        var isLoadNativeLanguageSetting = true
        var isLoadNativeIntro1 = true
        var isLoadNativeIntro2 = true
        var isLoadNativeIntro3 = true
        var isLoadNativeIntro4 = true
        var isLoadInterIntro = true
        var isLoadNativePermission = true
        var isLoadNativePermissionNotice = true
        var isLoadNativePermissionStorage = true
        var isLoadNativePopupPermission = true
        var isLoadNativePopupExit = true
        var isLoadInterBack = true
        var isLoadBanner = true
        var isLoadInterTabHome = true
        var isLoadInterDownloadedItem = true
        var isLoadInterPrivateAdd = true
        var isLoadInterSavePrivate = true
        var isLoadInterTabItem = true
        var isLoadInterSaveQuestion = true
        var isLoadNativeHome = true
        var isLoadNativeNoMediaDownloaded = true
        var isLoadNativeTab = true
        var isLoadNativeHistory = true
        var isLoadNativeBookMark = true
        var isLoadNativeSecurity = true
        var isLoadNativePrivate = true
        var isLoadNativeAdd = true
        var isLoadNativeImageDetail = true
        var isLoadNativeGuide = true
        var isLoadNativeFiles = true
        var isLoadNativeVideo = true

        var nativeAdsAll: NativeAd? = null

        var isDownloadSuccessfully = false

        var nativeAdsLanguageSelect: NativeAd? = null
        var nativeAdsLanguageFirst: NativeAd? = null
        var nativeAdsPopupPermission: NativeAd? = null
        var nativeAdsHome: NativeAd? = null

        var historyInterItem: Long = 0
        const val PAUSE_INTER_TIME: Long = 15


        fun loadNativeLanguageSelect(context: Context) {
            if (AdsConstant.nativeAdsLanguageSelect == null) {
                Admob.getInstance()
                    .loadNativeAd(
                        context,
                        context.getString(R.string.native_language_select),
                        object : NativeCallback() {
                            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                                AdsConstant.nativeAdsLanguageSelect = nativeAd
                            }

                            override fun onAdImpression() {
                                super.onAdImpression()
                                AdsConstant.nativeAdsLanguageSelect = null

                            }
                        })
            }
        }

        fun loadNativeLanguageFirst(context: Context) {
            if (AdsConstant.nativeAdsLanguageFirst == null) {
                Admob.getInstance()
                    .loadNativeAd(
                        context,
                        context.getString(R.string.native_language),
                        object : NativeCallback() {
                            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                                AdsConstant.nativeAdsLanguageFirst = nativeAd
                            }

                            override fun onAdImpression() {
                                super.onAdImpression()
                                AdsConstant.nativeAdsLanguageFirst = null

                            }
                        })
            }
        }

        fun loadNativeHome(context: Context) {
            if (isLoadNativeHome && nativeAdsHome == null && Admob.getInstance().isLoadFullAds) {
                Admob.getInstance()
                    .loadNativeAd(
                        context,
                        context.getString(R.string.native_home),
                        object : NativeCallback() {
                            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                                nativeAdsHome = nativeAd
                            }

                            override fun onAdImpression() {
                                super.onAdImpression()
                                nativeAdsHome = null

                                loadNativeHome(context)

                            }
                        })
            }
        }


        fun loadNativeAll(context: Context) {
            if (AdsConstant.nativeAdsAll == null) {
                Admob.getInstance().loadNativeAd(
                    context,
                    context.getString(R.string.native_all),
                    object : NativeCallback() {
                        override fun onNativeAdLoaded(nativeAd: NativeAd) {
                            AdsConstant.nativeAdsAll = nativeAd
                        }

                        override fun onAdFailedToLoad() {
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            AdsConstant.nativeAdsAll = null

                        }
                    }
                )
            }
        }

    }
}