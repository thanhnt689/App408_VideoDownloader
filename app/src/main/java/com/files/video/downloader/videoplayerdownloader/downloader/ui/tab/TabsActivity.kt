package com.files.video.downloader.videoplayerdownloader.downloader.ui.tab

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityTabsBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTab
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabFactory
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryAdapter

class TabsActivity : BaseActivity<ActivityTabsBinding>() {

    private var listTabs = arrayListOf<WebTab>()

    private lateinit var tabsAdapter: TabsAdapter

    override fun setBinding(layoutInflater: LayoutInflater): ActivityTabsBinding {
        return ActivityTabsBinding.inflate(layoutInflater)
    }

    override fun initView() {
        tabViewModels.listTabWeb.observe(this) {
            binding.tvNumTabs.text = getString(R.string.string_num_tabs, it.size.toString())
            listTabs.clear()
            listTabs.addAll(it)

            binding.rcvTabs.apply {
                setHasFixedSize(true)

                layoutManager =
                    GridLayoutManager(this@TabsActivity, 2)  // List layout

                tabsAdapter = TabsAdapter(
                    this@TabsActivity,
                    listTabs,
                    onClickItemTab = { webTab, position ->
//                        val intent = Intent(this@TabsActivity, WebTabActivity::class.java)
//                        val bundle = Bundle()
//                        bundle.putSerializable("webtab", webTab)
//                        intent.putExtras(bundle)
//                        startActivity(intent)
                        openNewTab(webTab.getUrl())
                        Log.d("ntt", "initView: $webTab")
                    },
                    onClickDeleteItemTab = { webTab, position ->
                        tabViewModels.removeTabAt(position)
                    },
                )

                adapter = tabsAdapter

            }


        }

        tabViewModels.currentPositionTabWeb.observe(this) {
            if (this::tabsAdapter.isInitialized) {
                tabsAdapter.setPositionCurrentTab(it)
                tabsAdapter.notifyDataSetChanged()
            }
        }

        binding.tvCloseAll.setOnClickListener {
            tabViewModels.clearAllTabs()
        }
    }

    private fun openNewTab(input: String) {
        if (input.isNotEmpty()) {
            val webTab = WebTabFactory.createWebTabFromInput(input)

            val intent = Intent(this, WebTabActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("webtab", webTab)
            intent.putExtras(bundle)
            startActivity(intent)

        }
    }
}