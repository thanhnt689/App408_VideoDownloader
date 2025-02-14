package com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivitySelectVideoBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoListener
import com.files.video.downloader.videoplayerdownloader.downloader.ui.processing.WrapContentLinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelectVideoActivity : BaseActivity<ActivitySelectVideoBinding>(), VideoListener {

    private lateinit var videoAdapter: VideoAdapter

    private var listVideoTaskItem = arrayListOf<VideoTaskItem>()

    private val privateVideoViewModel: PrivateVideoViewModel by viewModels()

    private var fileType: String = "all"


    @Inject
    lateinit var fileUtil: FileUtil

    override fun setBinding(layoutInflater: LayoutInflater): ActivitySelectVideoBinding {
        return ActivitySelectVideoBinding.inflate(layoutInflater)
    }

    override fun initView() {

        lifecycleScope.launch {
            privateVideoViewModel.queryVideoTaskItem(fileType).observe(this@SelectVideoActivity) {
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

                videoAdapter =
                    VideoAdapter(
                        this@SelectVideoActivity,
                        listVideoTaskItem,
                        this@SelectVideoActivity,
                        fileUtil
                    )

                videoAdapter.setIsChecked(true)

                binding.tvTitle.text =
                    getString(
                        R.string.string_num_video_selected,
                        videoAdapter.getCountSelectFile().toString()
                    )

                val managerL =
                    WrapContentLinearLayoutManager(
                        this@SelectVideoActivity,
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                binding.rcvFiles.layoutManager = managerL
                binding.rcvFiles.adapter = videoAdapter

            }
        }

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
    }

    override fun setStatusSelectAll() {
        binding.imgCheck.setImageResource(R.drawable.ic_check_box_selected)
    }

    override fun setStatusUnSelectAll() {
        binding.imgCheck.setImageResource(R.drawable.ic_check_box_normal)
    }

    override fun updateStatusNumSelect() {
        val listPhotoSelect = videoAdapter.getCountSelectFile()

        binding.tvTitle.text =
            getString(R.string.string_num_video_selected, listPhotoSelect.toString())

    }

    override fun onItemClicked(videoTaskItem: VideoTaskItem) {
    }

    override fun onMenuClicked(view: View, videoTaskItem: VideoTaskItem) {
    }

    override fun onClickItemChecked(videoTaskItem: VideoTaskItem) {
        var numSelect = videoAdapter.getCountSelectFile()

        binding.tvTitle.text = getString(R.string.string_num_video_selected, numSelect.toString())
    }

}