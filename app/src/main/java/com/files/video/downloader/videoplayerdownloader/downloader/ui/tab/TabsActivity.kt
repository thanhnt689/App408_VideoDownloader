package com.files.video.downloader.videoplayerdownloader.downloader.ui.tab

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityTabsBinding
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTab
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabFactory
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TabsActivity : BaseActivity<ActivityTabsBinding>() {

    private var listTabs = arrayListOf<TabModel>()

    private lateinit var tabsAdapter: TabsAdapter

    private val tabModelViewModel by viewModels<TabModelViewModel>()

    override fun setBinding(layoutInflater: LayoutInflater): ActivityTabsBinding {
        return ActivityTabsBinding.inflate(layoutInflater)
    }

    override fun initView() {

        binding.tvNumTabs.text = getString(R.string.string_num_tabs, listTabs.size.toString())

        loadNativeTabs()

        tabModelViewModel.queryAllTabModel().observe(this) {
            listTabs.clear()
            listTabs.addAll(it)

            if (it.isNullOrEmpty()) {
                binding.frAds.visibility = View.GONE
            } else {
                binding.frAds.visibility = View.VISIBLE
            }

            binding.tvNumTabs.text = getString(R.string.string_num_tabs, listTabs.size.toString())

            binding.rcvTabs.apply {
                setHasFixedSize(true)

                layoutManager =
                    GridLayoutManager(this@TabsActivity, 2)  // List layout

                tabsAdapter = TabsAdapter(
                    this@TabsActivity,
                    listTabs,
                    onClickItemTab = { tabModel, position ->
                        openNewTab(tabModel)
                    },
                    onClickDeleteItemTab = { tabModel, position ->
//                        tabViewModels.removeTabAt(position)

                        tabModelViewModel.deleteTabModel(tabModel)
                    },
                )

                adapter = tabsAdapter

            }
        }

//        tabViewModels.listTabWeb.observe(this) {
//
//
//
//        }

//        tabViewModels.currentPositionTabWeb.observe(this) {
//            if (this::tabsAdapter.isInitialized) {
//                tabsAdapter.setPositionCurrentTab(it)
//                tabsAdapter.notifyDataSetChanged()
//            }
//        }

        binding.tvCloseAll.setOnClickListener {
//            tabViewModels.clearAllTabs()
            lifecycleScope.launch(Dispatchers.IO) {
                tabModelViewModel.clearAllTabModel()
            }
        }

        binding.imgAdd.setOnClickListener {
            newTab("https://www.google.com")
        }
    }

    private fun openNewTab(tabModel: TabModel) {
        if (tabModel.url.isNotEmpty()) {
            val webTab = WebTabFactory.createWebTabFromInput(tabModel.url)

            lifecycleScope.launch(Dispatchers.IO) {
                tabModelViewModel.updateInfoTabModel(
                    tabModel.id,
                    tabModel.url,
                    tabModel.favicon,
                    true
                )

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@TabsActivity, WebTabActivity::class.java)
                    val bundle = Bundle()
                    bundle.putSerializable("webtab", webTab)
                    intent.putExtra("open", "tab")
                    intent.putExtras(bundle)
                    startActivity(intent)
                }
            }

        }
    }

    private fun newTab(input: String) {
        val webTab = WebTabFactory.createWebTabFromInput(input)

        val tabModel = WebTabFactory.createTabModelFromInput(input)

        lifecycleScope.launch(Dispatchers.IO) {
            tabModelViewModel.insertTabModel(tabModel)

            withContext(Dispatchers.Main) {
                val intent = Intent(this@TabsActivity, WebTabActivity::class.java)
                val bundle = Bundle()
                bundle.putSerializable("webtab", webTab)
                intent.putExtra("open", "tab")
                intent.putExtras(bundle)
                startActivity(intent)
            }
        }


//        tabViewModels.addNewTab(webTab)


    }

    private fun loadNativeTabs() {
        if (hasNetworkConnection() && ConsentHelper.getInstance(this)
                .canRequestAds() && AdsConstant.isLoadNativeTab && Admob.getInstance().isLoadFullAds
        ) {

            if (AdsConstant.nativeAdsAll != null) {
                val adView = if (Admob.getInstance().isLoadFullAds) {
                    LayoutInflater.from(this@TabsActivity)
                        .inflate(
                            R.layout.layout_ads_native_update_no_bor,
                            null
                        ) as NativeAdView
                } else {
                    LayoutInflater.from(this@TabsActivity)
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
                                    LayoutInflater.from(this@TabsActivity)
                                        .inflate(
                                            R.layout.layout_ads_native_update_no_bor,
                                            null
                                        ) as NativeAdView
                                } else {
                                    LayoutInflater.from(this@TabsActivity)
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

}