package com.files.video.downloader.videoplayerdownloader.downloader.ui.processing

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.databinding.Observable
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseFragment
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.FragmentProcessingBinding
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProcessingFragment : BaseFragment<FragmentProcessingBinding>(), ProgressListener {

    private val progressViewModel: ProgressViewModel by viewModels()

    private lateinit var progressAdapter: ProcessAdapter

    override fun getViewBinding(): FragmentProcessingBinding {
        return FragmentProcessingBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressAdapter = ProcessAdapter(emptyList(), this)

        progressViewModel.progressInfos.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val manager =
                    WrapContentLinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                binding.rcvProcess.layoutManager = manager
                binding.rcvProcess.adapter = progressAdapter
                progressViewModel.progressInfos.get()?.let { progressAdapter.setData(it) }
            }

        })

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