package com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPrivateVideoBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogRename
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.model.LocalVideo
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoListener
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.IS_DOWNLOADED
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.ITEM_TYPE
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.VIDEO_ITEM
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.VIDEO_NAME
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.VIDEO_URL
import com.files.video.downloader.videoplayerdownloader.downloader.ui.pin.PinActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.pin.SecurityActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.processing.WrapContentLinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.ViewUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.AdCallback
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PrivateVideoActivity : BaseActivity<ActivityPrivateVideoBinding>(), VideoListener {

    private val privateVideoViewModel: PrivateVideoViewModel by viewModels()

    private lateinit var videoAdapter: VideoAdapter

    private var listVideoTaskItem = arrayListOf<VideoTaskItem>()

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    @Inject
    lateinit var fileUtil: FileUtil

    override fun setBinding(layoutInflater: LayoutInflater): ActivityPrivateVideoBinding {
        return ActivityPrivateVideoBinding.inflate(layoutInflater)
    }

    override fun initView() {

        loadNativePrivateVideo()

        videoAdapter = VideoAdapter(this, false, emptyList(), this@PrivateVideoActivity, fileUtil)

        privateVideoViewModel.queryVideoSecurityTaskItem().observe(this) {
            if (it.isEmpty()) {
                binding.layoutNoData.visibility = View.VISIBLE

                binding.frAds.visibility = View.GONE
            } else {
                binding.layoutNoData.visibility = View.GONE

                binding.frAds.visibility = View.VISIBLE
            }
//
//                listHistory.clear()
//                listHistory.addAll(it.reversed())

            listVideoTaskItem.clear()
            listVideoTaskItem.addAll(it)

            binding.tvNumFiles.text =
                getString(R.string.string_num_files, it.size.toString())
            videoAdapter.setData(listVideoTaskItem)

            val managerL =
                WrapContentLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            binding.rcvFiles.layoutManager = managerL
            binding.rcvFiles.adapter = videoAdapter
        }

        binding.imgBack.setOnClickListener {
            finish()
        }

        binding.imgOption.setOnClickListener {
            showDialogOption()
        }

        binding.fabAdd.setOnClickListener {

            if (hasNetworkConnection() && ConsentHelper.getInstance(this)
                    .canRequestAds() && AdsConstant.isLoadInterPrivateAdd && Admob.getInstance().isLoadFullAds
            ) {
                Admob.getInstance().loadAndShowInter(
                    this,
                    getString(R.string.inter_private_add), true,
                    object : AdCallback() {
                        override fun onNextAction() {
                            startActivity(
                                Intent(
                                    this@PrivateVideoActivity,
                                    SelectVideoActivity::class.java
                                )
                            )
                        }

                        override fun onAdFailedToLoad(p0: LoadAdError?) {
                            startActivity(
                                Intent(
                                    this@PrivateVideoActivity,
                                    SelectVideoActivity::class.java
                                )
                            )
                        }
                    })
            } else {
                startActivity(Intent(this, SelectVideoActivity::class.java))
            }

        }

        binding.imgSelected.setOnClickListener {
            ViewUtils.showView(true, binding.layoutSelected, 300)
            ViewUtils.showView(false, binding.layoutSelectOption, 300)
            ViewUtils.hideView(true, binding.layout, 300)
            ViewUtils.hideView(false, binding.fabAdd, 300)

            videoAdapter.setIsChecked(true)

            updateStatusNumSelect()

            Log.d("ntt", "initView: num: ${videoAdapter.getCountSelectFile()}")
        }

        binding.imgClose.setOnClickListener {
            ViewUtils.hideView(true, binding.layoutSelected, 300)
            ViewUtils.hideView(false, binding.layoutSelectOption, 300)
            ViewUtils.showView(true, binding.layout, 300)
            ViewUtils.showView(false, binding.fabAdd, 300)

            videoAdapter.setIsChecked(false)
        }

        binding.imgCheck.setOnClickListener {
            if (videoAdapter.getSelectedFile().size == listVideoTaskItem.size) {
                videoAdapter.deSelectAllItem()
            } else {
                videoAdapter.selectAllItem()
            }
        }

        binding.llDelete.setOnClickListener {

            if (videoAdapter.getSelectedFile().isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.string_please_select_file), Toast.LENGTH_SHORT
                ).show()
            } else {
                for (videoTaskItem in videoAdapter.getSelectedFile()) {
                    lifecycleScope.launch {
                        privateVideoViewModel.updateIsCheckSecurity(
                            videoTaskItem.mId,
                            !videoTaskItem.isSecurity
                        )
                    }
                }

                ViewUtils.hideView(true, binding.layoutSelected, 300)
                ViewUtils.hideView(false, binding.layoutSelectOption, 300)
                ViewUtils.showView(true, binding.layout, 300)
                ViewUtils.showView(false, binding.fabAdd, 300)

                videoAdapter.setIsChecked(false)
            }
        }

        binding.llShare.setOnClickListener {
            if (videoAdapter.getSelectedFile().isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.string_please_select_file), Toast.LENGTH_SHORT
                ).show()
            } else {

            }
        }

    }

    override fun onItemClicked(videoTaskItem: VideoTaskItem) {
        startVideo(videoTaskItem)
    }

    private fun showDialogOption() {
        val balloon: Balloon = Balloon.Builder(binding.root.context)
            .setLayout(R.layout.dialog_private)
            .setArrowSize(0)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setBackgroundColor(Color.TRANSPARENT)
            .build()

        balloon.showAlignBottom(binding.imgOption, -20, 20)

        val layoutChangePinCode: LinearLayout =
            balloon.getContentView().findViewById(R.id.layout_change_pin_code)
        val layoutChangeSecurityQuestion: LinearLayout =
            balloon.getContentView().findViewById(R.id.layout_change_security_question)
        val layoutDelete: LinearLayout = balloon.getContentView().findViewById(R.id.layout_delete)

        val tvChangePinCode: TextView =
            balloon.getContentView().findViewById(R.id.tv_change_pin_code)
        val tvChangeSecurityQuestion: TextView =
            balloon.getContentView().findViewById(R.id.tv_change_security_question)
        val tvDelete: TextView = balloon.getContentView().findViewById(R.id.tv_delete)

        tvChangePinCode.isSelected = true
        tvChangeSecurityQuestion.isSelected = true
        tvDelete.isSelected = true

        layoutChangePinCode.setOnClickListener {
            startActivity(Intent(this, PinActivity::class.java).apply {
                putExtra("action", "changePinCode")
            })
        }

        layoutChangeSecurityQuestion.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java).apply {
                putExtra("pass", preferenceHelper.getPinCode())
                putExtra("action", "changQuestion")
            })
        }

        layoutDelete.setOnClickListener {
            lifecycleScope.launch {
                privateVideoViewModel.resetSecurityFlag()

                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }

    }

    override fun onMenuClicked(view: View, videoTaskItem: VideoTaskItem) {
        val balloon: Balloon = Balloon.Builder(binding.root.context)
            .setLayout(R.layout.dialog_video)
            .setArrowSize(0)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setBackgroundColor(Color.TRANSPARENT)
            .build()

        balloon.showAlignBottom(view, -20, 5)

        val layoutRename: LinearLayout = balloon.getContentView().findViewById(R.id.layout_rename)
        val layoutDelete: LinearLayout = balloon.getContentView().findViewById(R.id.layout_delete)
        val layoutShare: LinearLayout = balloon.getContentView().findViewById(R.id.layout_share)
        val layoutPrivate: LinearLayout = balloon.getContentView().findViewById(R.id.layout_private)
        val imgPrivate: ImageView = balloon.getContentView().findViewById(R.id.img_private)
        val tvPrivate: TextView = balloon.getContentView().findViewById(R.id.tv_private)
        val tvRename: TextView = balloon.getContentView().findViewById(R.id.tv_rename)
        val tvShare: TextView = balloon.getContentView().findViewById(R.id.tv_share)
        val tvDelete: TextView = balloon.getContentView().findViewById(R.id.tv_delete)

        imgPrivate.setImageResource(R.drawable.ic_remove_private)
        tvPrivate.text = getString(R.string.string_remove_from_private)

        tvPrivate.isSelected = true
        tvRename.isSelected = true
        tvShare.isSelected = true
        tvDelete.isSelected = true

        layoutRename.setOnClickListener {
            binding.frAds.visibility = View.GONE
            val dialogRename = DialogRename(
                this,
                videoTaskItem.fileName.substringBeforeLast(".")
            ) { it ->

                val newName = it.trim()

                val file = File(videoTaskItem.filePath)
                val fileUri = Uri.fromFile(file)


                if (videoTaskItem.mimeType == "image") {
                    lifecycleScope.launch {
                        privateVideoViewModel.renameImage(
                            this@PrivateVideoActivity,
                            videoTaskItem.mId,
                            videoTaskItem.filePath,
                            File(newName).nameWithoutExtension + ".jpg"
                        )

                        withContext(Dispatchers.Main) {
                            balloon.dismiss()
                        }

                    }
                } else {
                    lifecycleScope.launch {
                        privateVideoViewModel.renameVideo(
                            this@PrivateVideoActivity,
                            videoTaskItem.mId,
                            videoTaskItem.filePath,
                            File(newName).nameWithoutExtension + ".mp4"
                        )

                        withContext(Dispatchers.Main) {
                            balloon.dismiss()
                        }

                    }
                }

            }

            dialogRename.show()

            dialogRename.setOnDismissListener {
                binding.frAds.visibility = View.VISIBLE
            }
        }

        layoutDelete.setOnClickListener {

            val isSuccessfully = fileUtil.deleteMedia(this, videoTaskItem.filePath)

            if (isSuccessfully) {

                lifecycleScope.launch {
                    privateVideoViewModel.deleteVideoTaskItem(videoTaskItem)

                    withContext(Dispatchers.Main) {
                        balloon.dismiss()
                    }
                }
            }


        }

        layoutShare.setOnClickListener {
            privateVideoViewModel.shareEvent.value = Uri.parse(videoTaskItem.filePath)

            balloon.dismiss()
        }

        layoutPrivate.setOnClickListener {
            lifecycleScope.launch {
                privateVideoViewModel.updateIsCheckSecurity(
                    videoTaskItem.mId,
                    !videoTaskItem.isSecurity
                )

                withContext(Dispatchers.Main) {
                    balloon.dismiss()
                }
            }
        }

    }

    override fun onClickItemChecked(videoTaskItem: VideoTaskItem) {

    }

    override fun setStatusSelectAll() {
        binding.imgCheck.setImageResource(R.drawable.ic_check_box_selected)
    }

    override fun setStatusUnSelectAll() {
        binding.imgCheck.setImageResource(R.drawable.ic_check_box_normal)
    }

    override fun updateStatusNumSelect() {
        val listPhotoSelect = videoAdapter.getCountSelectFile()

        binding.tvTitleSelected.text =
            getString(R.string.string_num_video_selected, listPhotoSelect.toString())

    }

    private fun loadNativePrivateVideo() {
        if (hasNetworkConnection() && ConsentHelper.getInstance(this)
                .canRequestAds() && AdsConstant.isLoadNativePrivate
        ) {

            if (AdsConstant.nativeAdsAll != null) {
                val adView = if (Admob.getInstance().isLoadFullAds) {
                    LayoutInflater.from(this@PrivateVideoActivity)
                        .inflate(
                            R.layout.layout_ads_native_update_no_bor,
                            null
                        ) as NativeAdView
                } else {
                    LayoutInflater.from(this@PrivateVideoActivity)
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
                                    LayoutInflater.from(this@PrivateVideoActivity)
                                        .inflate(
                                            R.layout.layout_ads_native_update_no_bor,
                                            null
                                        ) as NativeAdView
                                } else {
                                    LayoutInflater.from(this@PrivateVideoActivity)
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


    @OptIn(UnstableApi::class)
    private fun startVideo(videoTaskItem: VideoTaskItem) {
        startActivity(
            Intent(
                this,
                PlayMediaActivity::class.java
            ).apply {
                val bundle = Bundle()
                bundle.putSerializable(VIDEO_ITEM, videoTaskItem)
                putExtra(VIDEO_NAME, videoTaskItem.fileName)
                putExtra(ITEM_TYPE, videoTaskItem.mimeType)
                putExtra(IS_DOWNLOADED, true)
                putExtras(bundle)
                putExtra(
                    VIDEO_URL,
                    Uri.parse(videoTaskItem.filePath).toString()
                )
            })
    }

}