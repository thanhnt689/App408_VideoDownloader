package com.files.video.downloader.videoplayerdownloader.downloader.ui.processing

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.databinding.Observable
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseFragment
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.FragmentProcessingBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.guide.GuideActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ProcessingFragment : BaseFragment<FragmentProcessingBinding>(), ProgressListener {

    private val progressViewModel: ProgressViewModel by viewModels()

    private lateinit var progressAdapter: ProcessAdapter

    override fun getViewBinding(): FragmentProcessingBinding {
        return FragmentProcessingBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pbLoading.visibility = View.VISIBLE

        progressViewModel.start()

        progressAdapter = ProcessAdapter(emptyList(), this)

        progressViewModel.progressInfos.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {

                lifecycleScope.launch(Dispatchers.Main) {

                    progressViewModel.progressInfos.get()?.let {
                        progressAdapter.setData(it)
                        binding.pbLoading.visibility = View.GONE
                    }

                    val manager =
                        WrapContentLinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.VERTICAL,
                            false
                        )
                    binding.rcvProcess.layoutManager = manager
                    binding.rcvProcess.adapter = progressAdapter
                }
            }

        })

        binding.imgGuide.setOnClickListener {
            startActivity(GuideActivity.newIntent(requireContext()))
        }

    }

    override fun onCloseClicked(downloadId: Long, isRegular: Boolean) {
        progressViewModel.cancelDownload(downloadId, true)
    }

    override fun onPlayPauseDownloadClicked(
        view: View,
        downloadId: Long,
        isRegular: Boolean,
        isPlay: Boolean
    ) {
        if (isPlay) {
            progressViewModel.pauseDownload(downloadId)
        } else {
            progressViewModel.resumeDownload(downloadId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressViewModel.stop()
    }
}

class WrapContentLinearLayoutManager : LinearLayoutManager {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            AppLogger.e("meet a IOOBE in RecyclerView")
        }
    }
}