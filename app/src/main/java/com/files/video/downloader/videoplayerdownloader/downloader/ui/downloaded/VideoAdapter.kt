package com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded

import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ItemVideoBinding
import com.files.video.downloader.videoplayerdownloader.downloader.model.LocalVideo
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil

class VideoAdapter(
    private var localVideos: List<LocalVideo>,
    private val videoListener: VideoListener,
    private val fileUtil: FileUtil
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = DataBindingUtil.inflate<ItemVideoBinding>(
            LayoutInflater.from(parent.context), R.layout.item_video, parent, false
        )

        return VideoViewHolder(binding, fileUtil)
    }

    override fun getItemCount() = localVideos.size

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) =
        holder.bind(localVideos[position], position)

    inner class VideoViewHolder(var binding: ItemVideoBinding, var fileUtil: FileUtil) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(localVideo: LocalVideo, position: Int) {
            val size = getScreenResolution(itemView.context)

            with(binding) {
                var isEdit = false
                tvName.text = localVideo.name
                txtDuration.text = localVideo.duration
                txtSize.text = localVideo.size
                txtDate.text = localVideo.date

                if (localVideo.date.isEmpty()) {
                    txtDate.visibility = View.GONE
                    view2.visibility = View.GONE
                } else {
                    txtDate.visibility = View.VISIBLE
                    view2.visibility = View.VISIBLE
                }

                Glide.with(this@VideoViewHolder.itemView.context).load(localVideo.uri).fitCenter()
                    .error(R.drawable.ic_play_download)
                    .placeholder(R.drawable.ic_play_download)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(RequestOptions().override(size.first / 8, size.second / 8))
                    .into(this.ivThumbnail)

                if (localVideo.isEditable) {
                    isEdit = true
                    icMore.visibility = View.GONE
                    icSelect.visibility = View.VISIBLE
                } else {
                    isEdit = false
                    icMore.visibility = View.VISIBLE
                    icSelect.visibility = View.GONE
                }

                root.setOnClickListener {
                    if (isEdit) {

                    } else {
                        videoListener.onItemClicked(localVideo)
                    }
                }

                icMore.setOnClickListener {
                    videoListener.onMenuClicked(it, localVideo)
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

    fun setData(localVideos: List<LocalVideo>) {
        this.localVideos = localVideos
        notifyDataSetChanged()
    }
}

interface VideoListener {
    fun onItemClicked(localVideo: LocalVideo)
    fun onMenuClicked(view: View, localVideo: LocalVideo)
}

@GlideModule
class MyGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(Log.ERROR)
    }
}
