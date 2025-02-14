package com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseFragment
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.FragmentDownloadedBinding
import com.files.video.downloader.videoplayerdownloader.downloader.model.LocalVideo
import com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded.VideoViewModel.Companion.FILE_EXIST_ERROR_CODE
import com.files.video.downloader.videoplayerdownloader.downloader.ui.processing.WrapContentLinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.IntentUtil
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.LayoutBottomSheetDownloadBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.LayoutBottomSheetSortBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogConfirmDelete
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogRename
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.helper.SharedPreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.VideoInfoAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.VIDEO_NAME
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.VIDEO_URL
import com.files.video.downloader.videoplayerdownloader.downloader.ui.pin.PinActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo.PrivateVideoViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.KeyboardUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.ViewUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import javax.inject.Inject

@AndroidEntryPoint
class DownloadedFragment : BaseFragment<FragmentDownloadedBinding>(), VideoListener {

    private val videoViewModel: VideoViewModel by viewModels()

    private var disposable: Disposable? = null

    private var listVideoTaskItem = arrayListOf<VideoTaskItem>()

    @Inject
    lateinit var intentUtil: IntentUtil

    @Inject
    lateinit var fileUtil: FileUtil

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private lateinit var videoAdapter: VideoAdapter

    private val privateVideoViewModel: PrivateVideoViewModel by viewModels()

    private var fileType: String = "all"

    private lateinit var sortLayout: LayoutBottomSheetSortBinding

    private lateinit var bottomSheetDialog: BottomSheetDialog

    override fun getViewBinding(): FragmentDownloadedBinding {
        return FragmentDownloadedBinding.inflate(layoutInflater)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomAlertBottomSheet)

//        videoViewModel.start()
        binding.tab.addTab(binding.tab.newTab().setId(0).setText(getString(R.string.string_all)))
        binding.tab.addTab(binding.tab.newTab().setId(1).setText(getString(R.string.string_video)))
        binding.tab.addTab(binding.tab.newTab().setId(2).setText(getString(R.string.string_image)))


        videoAdapter =
            VideoAdapter(requireContext(), emptyList(), this@DownloadedFragment, fileUtil)

        handleUIEvents()
        handleIfStartedFromNotification()

        privateVideoViewModel.shareEvent.observe(viewLifecycleOwner) { uri ->
            intentUtil.shareVideo(requireContext(), uri)
        }

//        videoViewModel.localVideos.addOnPropertyChangedCallback(object :
//            Observable.OnPropertyChangedCallback() {
//            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
//
//                lifecycleScope.launch(Dispatchers.Main) {
//
//                    Log.d("ntt", "onPropertyChanged: ${videoViewModel.localVideos.get()}")
//                }
//            }
//
//        })

        privateVideoViewModel.fileTabLiveData.observe(viewLifecycleOwner) { tabIndex ->
            binding.tab.selectTab(binding.tab.getTabAt(tabIndex))
            setupCurrentTab(tabIndex)

            ViewUtils.hideView(true, binding.layoutSelected, 300)
            ViewUtils.hideView(false, binding.layoutSelectOption, 300)
            ViewUtils.hideView(false, binding.llSearch, 300)
            ViewUtils.showView(true, binding.layout, 300)

            videoAdapter.setIsChecked(false)

            lifecycleScope.launch {
                privateVideoViewModel.queryVideoTaskItem(fileType).observe(viewLifecycleOwner) {
                    if (it.isEmpty()) {
                        binding.layoutNoData.visibility = View.VISIBLE
                        binding.imgSearch.visibility = View.GONE
                    } else {
                        binding.layoutNoData.visibility = View.GONE
                        binding.imgSearch.visibility = View.VISIBLE
                    }
//
//                listHistory.clear()
//                listHistory.addAll(it.reversed())

                    listVideoTaskItem.clear()
                    listVideoTaskItem.addAll(it)

                    binding.tvNumFiles.text =
                        getString(R.string.string_num_files, it.size.toString())
                    videoAdapter.setData(it)

                    val managerL =
                        WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    binding.rcvFiles.layoutManager = managerL
                    binding.rcvFiles.adapter = videoAdapter

                }
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



        binding.imgSearch.setOnClickListener {
            ViewUtils.showView(true, binding.llSearch, 300)
            ViewUtils.hideView(true, binding.layout, 300)
            KeyboardUtils.showSoftKeyboard(requireActivity())
            binding.edtSearch.requestFocus()
        }

        binding.imCloseSearch.setOnClickListener {
            KeyboardUtils.hideSoftKeyboard(requireActivity())
            ViewUtils.showView(true, binding.layout, 300)
            ViewUtils.hideView(true, binding.llSearch, 300)
            binding.edtSearch.setText("")
        }

        binding.imCleanSearch.setOnClickListener {
            binding.edtSearch.setText("")
        }


        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                privateVideoViewModel.searchCharObservable.postValue(s.toString())
            }
        })

        binding.imgSecurity.setOnClickListener {
            startActivity(Intent(requireContext(), PinActivity::class.java))
        }

        binding.imgSelected.setOnClickListener {
            ViewUtils.showView(true, binding.layoutSelected, 300)
            ViewUtils.showView(false, binding.layoutSelectOption, 300)
            ViewUtils.hideView(true, binding.layout, 300)

            videoAdapter.setIsChecked(true)

            updateStatusNumSelect()

            Log.d("ntt", "initView: num: ${videoAdapter.getCountSelectFile()}")
        }

        binding.imgClose.setOnClickListener {
            ViewUtils.hideView(true, binding.layoutSelected, 300)
            ViewUtils.hideView(false, binding.layoutSelectOption, 300)
            ViewUtils.showView(true, binding.layout, 300)

            videoAdapter.setIsChecked(false)
        }

        binding.imgCheck.setOnClickListener {
            if (videoAdapter.getSelectedFile().size == listVideoTaskItem.size) {
                videoAdapter.deSelectAllItem()
            } else {
                videoAdapter.selectAllItem()
            }
        }

        binding.llShare.setOnClickListener {
            if (videoAdapter.getSelectedFile().isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.string_please_select_file), Toast.LENGTH_SHORT
                ).show()
            } else {

            }
        }

        binding.llDelete.setOnClickListener {
            if (videoAdapter.getSelectedFile().isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.string_please_select_file), Toast.LENGTH_SHORT
                ).show()
            } else {

                val dialogDelete = DialogConfirmDelete(requireContext()) {
                    for (videoTaskItem in videoAdapter.getSelectedFile()) {
                        val isSuccessfully =
                            fileUtil.deleteMedia(requireContext(), videoTaskItem.filePath)

                        if (isSuccessfully) {

                            lifecycleScope.launch {
                                privateVideoViewModel.deleteVideoTaskItem(videoTaskItem)

                            }
                        }
                    }

                }

                dialogDelete.show()

                ViewUtils.hideView(true, binding.layoutSelected, 300)
                ViewUtils.hideView(false, binding.layoutSelectOption, 300)
                ViewUtils.showView(true, binding.layout, 300)

                videoAdapter.setIsChecked(false)

            }
        }
    }

    private fun setupCurrentTab(index: Int) {
        when (index) {
            0 -> {
                fileType = "all"
            }

            1 -> {
                fileType = "video"
            }

            2 -> {
                fileType = "image"
            }

        }
    }

    private fun showBottomSheetSort(typeSort: Int) {

        sortLayout = LayoutBottomSheetSortBinding.inflate(layoutInflater)

        bottomSheetDialog.setContentView(sortLayout.root)

        bottomSheetDialog.setCanceledOnTouchOutside(true);

        val behavior = bottomSheetDialog.behavior

        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Xử lý sự kiện thay đổi trạng thái của bottom sheet
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Xử lý khi bottom sheet được trượt
            }
        }
        bottomSheetDialog.behavior.addBottomSheetCallback(bottomSheetCallback)

        when (typeSort) {
            1 -> {
                sortLayout.icSelectedAz.setImageResource(R.drawable.ic_selected_sort)
                sortLayout.icSelectedZa.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedNewToOld.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedOldToNew.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedLargeSmall.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedSmallLarge.setImageResource(R.drawable.ic_dot_normal)
            }

            2 -> {
                sortLayout.icSelectedAz.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedZa.setImageResource(R.drawable.ic_selected_sort)
                sortLayout.icSelectedNewToOld.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedOldToNew.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedLargeSmall.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedSmallLarge.setImageResource(R.drawable.ic_dot_normal)
            }

            3 -> {
                sortLayout.icSelectedAz.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedZa.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedNewToOld.setImageResource(R.drawable.ic_selected_sort)
                sortLayout.icSelectedOldToNew.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedLargeSmall.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedSmallLarge.setImageResource(R.drawable.ic_dot_normal)
            }

            4 -> {
                sortLayout.icSelectedAz.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedZa.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedNewToOld.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedOldToNew.setImageResource(R.drawable.ic_selected_sort)
                sortLayout.icSelectedLargeSmall.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedSmallLarge.setImageResource(R.drawable.ic_dot_normal)
            }

            5 -> {
                sortLayout.icSelectedAz.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedZa.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedNewToOld.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedOldToNew.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedLargeSmall.setImageResource(R.drawable.ic_selected_sort)
                sortLayout.icSelectedSmallLarge.setImageResource(R.drawable.ic_dot_normal)
            }

            6 -> {
                sortLayout.icSelectedAz.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedZa.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedNewToOld.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedOldToNew.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedLargeSmall.setImageResource(R.drawable.ic_dot_normal)
                sortLayout.icSelectedSmallLarge.setImageResource(R.drawable.ic_selected_sort)
            }
        }

        sortLayout.llAz.setOnClickListener {

        }

        sortLayout.llZa.setOnClickListener {

        }

        sortLayout.llNewToOld.setOnClickListener {

        }

        sortLayout.llOldToNew.setOnClickListener {

        }

        sortLayout.llLargeSmall.setOnClickListener {

        }

        sortLayout.llSmallLarge.setOnClickListener {

        }

        sortLayout.tvCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        sortLayout.tvOk.setOnClickListener {

        }


        bottomSheetDialog.show()
    }


    private fun updateColorWithTab(tabColor: Int, isLight: Boolean = false) {
        val iconColor: Int
        val normalColor: Int
        val selectedColor: Int
        if (isLight) {
            normalColor = ContextCompat.getColor(requireContext(), R.color.color_tab_light_normal)
            selectedColor =
                ContextCompat.getColor(requireContext(), R.color.color_tab_light_selected)
            iconColor = ContextCompat.getColor(requireContext(), R.color.color_tab_light_selected)

            binding.tab.setSelectedTabIndicatorColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_tab_indicator_light
                )
            )
        } else {
            normalColor = ContextCompat.getColor(requireContext(), R.color.color_tab_normal)
            selectedColor = ContextCompat.getColor(requireContext(), R.color.color_tab_selected)
            iconColor = ContextCompat.getColor(requireContext(), R.color.color_all)

            binding.tab.setSelectedTabIndicatorColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_tab_indicator
                )
            )
        }

        binding.tab.setTabTextColors(normalColor, selectedColor)
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

    private fun handleUIEvents() {
        privateVideoViewModel.apply {
            renameErrorEvent.observe(viewLifecycleOwner, Observer { errorCode ->
                val errorMessage =
                    if (errorCode == FILE_EXIST_ERROR_CODE) R.string.video_rename_exist else R.string.video_rename_invalid
                activity?.runOnUiThread {
                    Toast.makeText(context, context?.getString(errorMessage), Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }

    private fun handleIfStartedFromNotification() {
        tabViewModels.openDownloadedVideoEvent.observe(viewLifecycleOwner) { downloadFilename ->
            disposable?.dispose()
            disposable = null
            disposable =
                privateVideoViewModel.findVideoByName(downloadFilename)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.single()).subscribe { video ->
                        startVideo(video)
                    }

        }
    }

    @OptIn(UnstableApi::class)
    private fun startVideo(videoTaskItem: VideoTaskItem) {
        startActivity(
            Intent(
                requireContext(),
                PlayMediaActivity::class.java
            ).apply {
                putExtra(VIDEO_NAME, videoTaskItem.fileName)
                putExtra(
                    VIDEO_URL,
                    Uri.parse(videoTaskItem.filePath).toString()
                )
            })
    }

    override fun onItemClicked(videoTaskItem: VideoTaskItem) {
        startVideo(videoTaskItem)
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

        imgPrivate.setImageResource(R.drawable.ic_security)
        tvPrivate.text = getString(R.string.string_move_to_private)

        tvPrivate.isSelected = true
        tvRename.isSelected = true
        tvShare.isSelected = true
        tvDelete.isSelected = true

        layoutRename.setOnClickListener {
            val dialogRename = DialogRename(
                requireContext(),
                videoTaskItem.fileName.substringBeforeLast(".")
            ) { it ->

                val newName = it.trim()

                val file = File(videoTaskItem.filePath)
                val fileUri = Uri.fromFile(file)

                lifecycleScope.launch {
                    privateVideoViewModel.renameVideo(
                        requireContext(),
                        videoTaskItem.mId,
                        videoTaskItem.filePath,
                        File(newName).nameWithoutExtension + ".mp4"
                    )

                    withContext(Dispatchers.Main) {
                        balloon.dismiss()
                    }

                }

            }

            dialogRename.show()
        }

        layoutDelete.setOnClickListener {

            val dialogDelete = DialogConfirmDelete(requireContext()) {
                val isSuccessfully = fileUtil.deleteMedia(requireContext(), videoTaskItem.filePath)

                if (isSuccessfully) {

                    lifecycleScope.launch {
                        privateVideoViewModel.deleteVideoTaskItem(videoTaskItem)

                        withContext(Dispatchers.Main) {
                            balloon.dismiss()
                        }
                    }
                }

            }

            dialogDelete.show()

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

    fun canAccessFile(context: Context, file: File): Boolean {
        return file.exists() && file.canRead() && file.canWrite()
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }
}