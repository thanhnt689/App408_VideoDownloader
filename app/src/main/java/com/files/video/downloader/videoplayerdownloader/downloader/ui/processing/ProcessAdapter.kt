package com.files.video.downloader.videoplayerdownloader.downloader.ui.processing

import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.ProgressInfo
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ItemProgressBinding
import com.google.android.material.color.MaterialColors

class ProcessAdapter(
    private var progressInfos: List<ProgressInfo>,
    private var videoListener: ProgressListener
) : RecyclerView.Adapter<ProcessAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemProgressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(progressInfo: ProgressInfo, position: Int) {
            val thumbnail = progressInfo.videoInfo.thumbnail
            val placeholder = R.drawable.ic_play_download
            val size = getScreenResolution(itemView.context)

            with(binding)
            {

                var isPlay = true

                val downloadId = progressInfo.downloadId
                val isRegular = progressInfo.videoInfo.isRegularDownload

                Glide.with(this@ViewHolder.itemView.context).load(thumbnail).fitCenter()
                    .error(placeholder)
                    .placeholder(placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(RequestOptions().override(size.first / 8, size.second / 8))
                    .into(this.ivThumbnail)

                tvTitle.text = progressInfo.videoInfo.name
                progressBar.progress = progressInfo.progress.toFloat()
                tvProgress.text = progressInfo.progressSize
                infoLine.text = progressInfo.infoLine
                imgClose.setOnClickListener {
                    videoListener.onCloseClicked(downloadId, isRegular)
                }

                if (progressInfo.infoLine.isEmpty()) {
                    isPlay = false
                    imgPlayPause.setImageResource(R.drawable.ic_play_download)
                } else {
                    isPlay = true
                    imgPlayPause.setImageResource(R.drawable.ic_pause_download)
                }

                imgPlayPause.setOnClickListener {
                    videoListener.onPlayPauseDownloadClicked(
                        this@ViewHolder.itemView,
                        downloadId,
                        isRegular,
                        isPlay
                    )
                }

            }

        }

        private fun getScreenResolution(context: Context): Pair<Int, Int> {
            val displayMetrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            windowManager.defaultDisplay.getMetrics(displayMetrics)

            val widthPixels = displayMetrics.widthPixels
            val heightPixels = displayMetrics.heightPixels

            return Pair(widthPixels, heightPixels)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProgressBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return progressInfos.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(progressInfos[position], position)
    }

    fun setData(progressInfos: List<ProgressInfo>) {
        this.progressInfos = progressInfos
        notifyDataSetChanged()
    }
}

interface ProgressListener {
    fun onCloseClicked(downloadId: Long, isRegular: Boolean)
    fun onPlayPauseDownloadClicked(
        view: View,
        downloadId: Long,
        isRegular: Boolean,
        isPlay: Boolean
    )
}