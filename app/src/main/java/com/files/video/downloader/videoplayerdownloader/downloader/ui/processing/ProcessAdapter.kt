package com.files.video.downloader.videoplayerdownloader.downloader.ui.processing

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.ProgressInfo

class ProcessAdapter(private var progressInfos: List<ProgressInfo>,
                     private var videoListener: ProgressListener) : RecyclerView.Adapter<ProcessAdapter.ViewHolder>(){
    class ViewHolder {

    }
}

interface ProgressListener {
    fun onMenuClicked(view: View, downloadId: Long, isRegular: Boolean)
}