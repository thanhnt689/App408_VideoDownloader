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
import androidx.databinding.Observable
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
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogRename
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.helper.SharedPreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.VIDEO_NAME
import com.files.video.downloader.videoplayerdownloader.downloader.ui.media.PlayMediaActivity.Companion.VIDEO_URL
import com.files.video.downloader.videoplayerdownloader.downloader.ui.pin.PinActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo.PrivateVideoViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.KeyboardUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.ViewUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import javax.inject.Inject

@AndroidEntryPoint
class DownloadedFragment : BaseFragment<FragmentDownloadedBinding>(), VideoListener {

//    private val videoViewModel: VideoViewModel by viewModels()

    private var disposable: Disposable? = null

    @Inject
    lateinit var intentUtil: IntentUtil

    @Inject
    lateinit var fileUtil: FileUtil

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private lateinit var videoAdapter: VideoAdapter

    private val privateVideoViewModel: PrivateVideoViewModel by viewModels()


    override fun getViewBinding(): FragmentDownloadedBinding {
        return FragmentDownloadedBinding.inflate(layoutInflater)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        videoViewModel.start()

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
//                    videoViewModel.localVideos.get()?.let {
//                        binding.tvNumFiles.text =
//                            getString(R.string.string_num_files, it.size.toString())
//                        videoAdapter.setData(it)
//                    }
//
//                    val managerL =
//                        WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//                    binding.rcvFiles.layoutManager = managerL
//                    binding.rcvFiles.adapter = videoAdapter
//                }
//            }
//
//        })

        lifecycleScope.launch {
            privateVideoViewModel.queryVideoTaskItem().observe(viewLifecycleOwner) {
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

                binding.tvNumFiles.text =
                    getString(R.string.string_num_files, it.size.toString())
                videoAdapter.setData(it)

                val managerL =
                    WrapContentLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rcvFiles.layoutManager = managerL
                binding.rcvFiles.adapter = videoAdapter

            }
        }

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