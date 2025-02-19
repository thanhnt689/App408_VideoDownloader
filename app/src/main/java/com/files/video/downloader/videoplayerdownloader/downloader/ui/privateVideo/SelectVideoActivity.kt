package com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.MainActivity
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivitySelectVideoBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoListener
import com.files.video.downloader.videoplayerdownloader.downloader.ui.processing.WrapContentLinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.ViewUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelectVideoActivity : BaseActivity<ActivitySelectVideoBinding>(), VideoListener {

    private lateinit var videoAdapter: VideoAdapter

    private var listVideoTaskItem = arrayListOf<VideoTaskItem>()

    private val privateVideoViewModel: PrivateVideoViewModel by viewModels()

    private var fileType: String = "video"

    private var isGridLayoutManager: Boolean = true


    @Inject
    lateinit var fileUtil: FileUtil

    override fun setBinding(layoutInflater: LayoutInflater): ActivitySelectVideoBinding {
        return ActivitySelectVideoBinding.inflate(layoutInflater)
    }

    override fun initView() {

        binding.tab.addTab(binding.tab.newTab().setId(0).setText(getString(R.string.string_video)))
        binding.tab.addTab(binding.tab.newTab().setId(1).setText(getString(R.string.string_image)))

        privateVideoViewModel.fileType.postValue(fileType)

        videoAdapter =
            VideoAdapter(
                this@SelectVideoActivity,
                false,
                listVideoTaskItem,
                this@SelectVideoActivity,
                fileUtil
            )

        privateVideoViewModel.fileTabLiveData.observe(this) { tabIndex ->
            binding.tab.selectTab(binding.tab.getTabAt(tabIndex))
            setupCurrentTab(tabIndex)

//            ViewUtils.hideView(true, binding.layoutSelected, 300)
//            ViewUtils.hideView(false, binding.layoutSelectOption, 300)
//            ViewUtils.hideView(false, binding.llSearch, 300)
//            ViewUtils.showView(true, binding.layout, 300)

//            videoAdapter.setIsChecked(false)

            when (tabIndex) {
                0 -> {
                    isGridLayoutManager = false
                }

                1 -> {
                    isGridLayoutManager = true
                }
            }


        }

        lifecycleScope.launch {
            privateVideoViewModel.queryVideoTaskItem()
                .observe(this@SelectVideoActivity) {
                    if (it.isEmpty()) {
                        binding.layoutNoData.visibility = View.VISIBLE
                        binding.tvAddToPrivate.visibility = View.GONE
                    } else {
                        binding.layoutNoData.visibility = View.GONE
                        binding.tvAddToPrivate.visibility = View.VISIBLE
                    }
//
                    listVideoTaskItem.clear()
                    listVideoTaskItem.addAll(it)

                    videoAdapter.setData(listVideoTaskItem)

                    videoAdapter.deSelectAllItem()

                    binding.imgCheck.setImageResource(R.drawable.ic_check_box_normal)

                    videoAdapter.setIsChecked(true)

                    if (fileType == "video") {
                        binding.tvTitle.text =
                            getString(
                                R.string.string_num_video_selected,
                                videoAdapter.getCountSelectFile().toString()
                            )
                    } else {
                        binding.tvTitle.text =
                            getString(
                                R.string.string_num_image_selected,
                                videoAdapter.getCountSelectFile().toString()
                            )
                    }

                    if (isGridLayoutManager) {
                        var layoutManagerGrid = GridLayoutManager(this@SelectVideoActivity, 3)

                        binding.rcvFiles.layoutManager = layoutManagerGrid
                        binding.rcvFiles.adapter = videoAdapter
                    } else {
                        val layoutManagerLiner = LinearLayoutManager(this@SelectVideoActivity)

                        binding.rcvFiles.layoutManager = layoutManagerLiner
                        binding.rcvFiles.adapter = videoAdapter
                    }

                    videoAdapter.setLayoutType(isGridLayoutManager)

//                        videoAdapter.notifyDataSetChanged()

//                        val managerL =
//                            WrapContentLinearLayoutManager(
//                                this@SelectVideoActivity,
//                                LinearLayoutManager.VERTICAL,
//                                false
//                            )
//                        binding.rcvFiles.layoutManager = managerL
//                        binding.rcvFiles.adapter = videoAdapter

                }
        }



        binding.tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    privateVideoViewModel.fileTabLiveData.postValue(tab.id)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })


        binding.imgCheck.setOnClickListener {
            if (videoAdapter.getSelectedFile().size == listVideoTaskItem.size) {
                videoAdapter.deSelectAllItem()
            } else {
                videoAdapter.selectAllItem()
            }
        }

        binding.imgBack.setOnClickListener {
            finish()
        }

        binding.tvAddToPrivate.setOnClickListener {
            for (videoTaskItem in videoAdapter.getSelectedFile()) {
                lifecycleScope.launch {
                    privateVideoViewModel.updateIsCheckSecurity(
                        videoTaskItem.mId,
                        !videoTaskItem.isSecurity
                    )
                }
            }

            binding.imgCheck.setImageResource(R.drawable.ic_check_box_normal)
        }

        binding.tvGoToBrowser.setOnClickListener {
            startActivity(MainActivity.newIntent(this).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun setupCurrentTab(index: Int) {
        when (index) {

            0 -> {
                fileType = "video"
            }

            1 -> {
                fileType = "image"
            }

        }

        privateVideoViewModel.fileType.postValue(fileType)
    }

    override fun setStatusSelectAll() {
        binding.imgCheck.setImageResource(R.drawable.ic_check_box_selected)
    }

    override fun setStatusUnSelectAll() {
        binding.imgCheck.setImageResource(R.drawable.ic_check_box_normal)
    }

    override fun updateStatusNumSelect() {
        val listPhotoSelect = videoAdapter.getCountSelectFile()
        if (fileType == "video") {
            binding.tvTitle.text =
                getString(R.string.string_num_video_selected, listPhotoSelect.toString())
        } else {
            binding.tvTitle.text =
                getString(R.string.string_num_image_selected, listPhotoSelect.toString())
        }

    }

    override fun onItemClicked(videoTaskItem: VideoTaskItem) {
    }

    override fun onMenuClicked(view: View, videoTaskItem: VideoTaskItem) {
    }

    override fun onClickItemChecked(videoTaskItem: VideoTaskItem) {
        var numSelect = videoAdapter.getCountSelectFile()

        if (fileType == "video") {
            binding.tvTitle.text =
                getString(R.string.string_num_video_selected, numSelect.toString())
        } else {
            binding.tvTitle.text =
                getString(R.string.string_num_image_selected, numSelect.toString())
        }
    }

}