package com.files.video.downloader.videoplayerdownloader.downloader.ui.downloaded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
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
import com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo.SelectVideoActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.Video
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class VideoAdapter(
    private var context: Context,
    private var videoTaskItems: List<VideoTaskItem>,
    private val videoListener: VideoListener,
    private val fileUtil: FileUtil
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = DataBindingUtil.inflate<ItemVideoBinding>(
            LayoutInflater.from(parent.context), R.layout.item_video, parent, false
        )

        return VideoViewHolder(binding, fileUtil)
    }

    override fun getItemCount() = videoTaskItems.size

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) =
        holder.bind(videoTaskItems[position], position)

    inner class VideoViewHolder(var binding: ItemVideoBinding, var fileUtil: FileUtil) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(videoTaskItem: VideoTaskItem, position: Int) {
            val size = getScreenResolution(itemView.context)

            with(binding) {
                var isEdit = false
                tvName.text = videoTaskItem.fileName
                txtDuration.text = getVideoDuration(context, videoTaskItem.filePath)

                var date = ""

                val file = File(videoTaskItem.filePath)
                if (file.exists()) {
                    val fileSize = formatFileSize(file.length())
                    val lastModified = Date(file.lastModified())

                    date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(lastModified)

                    txtSize.text = fileSize
                    txtDate.text = date
                }

                if (date.isEmpty()) {
                    txtDate.visibility = View.GONE
                    view2.visibility = View.GONE
                } else {
                    txtDate.visibility = View.VISIBLE
                    view2.visibility = View.VISIBLE
                }

                Glide.with(this@VideoViewHolder.itemView.context)
                    .load(getVideoThumbnail(videoTaskItem.filePath)).fitCenter()
                    .error(R.drawable.ic_play_download)
                    .placeholder(R.drawable.ic_play_download)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(RequestOptions().override(size.first / 8, size.second / 8))
                    .into(this.ivThumbnail)

                if (videoTaskItem.isChecked) {
                    binding.icSelect.setImageResource(R.drawable.ic_check_box_selected)
                } else {
                    binding.icSelect.setImageResource(R.drawable.ic_check_box_normal)
                }

                if (videoTaskItem.isEditable) {
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
                        videoTaskItem.isChecked = !videoTaskItem.isChecked

                        if (videoTaskItem.isChecked) {
                            binding.icSelect.setImageResource(R.drawable.ic_check_box_selected)
                        } else {
                            binding.icSelect.setImageResource(R.drawable.ic_check_box_normal)
                        }

                        videoListener.onItemClicked(videoTaskItem)

                        checkIfAllFilesDeselected()
                    } else {
                        videoListener.onItemClicked(videoTaskItem)
                    }
                }

                icSelect.setOnClickListener {
                    videoTaskItem.isChecked = !videoTaskItem.isChecked

                    if (videoTaskItem.isChecked) {
                        binding.icSelect.setImageResource(R.drawable.ic_check_box_selected)
                    } else {
                        binding.icSelect.setImageResource(R.drawable.ic_check_box_normal)
                    }

                    videoListener.onItemClicked(videoTaskItem)

                    checkIfAllFilesDeselected()
                }

                icMore.setOnClickListener {
                    videoListener.onMenuClicked(it, videoTaskItem)
                }
            }
        }

        private fun getVideoThumbnail(filePath: String): Bitmap? {
            return try {
                val uri = Uri.parse(filePath)
                uri.path?.let {
                    ThumbnailUtils.createVideoThumbnail(
                        it,
                        MediaStore.Images.Thumbnails.MINI_KIND
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
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


        private fun formatFileSize(size: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB")
            var fileSize = size.toDouble()
            var index = 0

            while (fileSize > 1024 && index < units.size - 1) {
                fileSize /= 1024
                index++
            }

            return String.format("%.2f %s", fileSize, units[index])
        }

        private fun getVideoDuration(context: Context, filePath: String): String {
            return try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.fromFile(File(filePath)))
                val durationMs =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0
                retriever.release()

                val minutes = durationMs / 1000 / 60
                val seconds = (durationMs / 1000) % 60
                String.format("%02d:%02d", minutes, seconds)
            } catch (e: Exception) {
                "00:00"
            }
        }
    }

    fun setData(localVideos: List<VideoTaskItem>) {
        this.videoTaskItems = localVideos
        notifyDataSetChanged()
    }

    fun setIsChecked(check: Boolean) {
        val it = videoTaskItems.iterator()
        while (it.hasNext()) {
            val videoTaskItem: VideoTaskItem = it.next() as VideoTaskItem
            val position: Int =
                videoTaskItems.indexOf(
                    videoTaskItem
                )
            videoTaskItem.isEditable = check
            notifyItemChanged(position)
        }
    }

    fun getSelectedFile(): MutableList<VideoTaskItem> {
        val selectedVideo: MutableList<VideoTaskItem> = mutableListOf()
        val it = videoTaskItems.iterator()
        while (it.hasNext()) {
            val videoTaskItem: VideoTaskItem = it.next() as VideoTaskItem
            if (videoTaskItem.isChecked) {
                selectedVideo.add(videoTaskItem)
            }
        }
        return selectedVideo
    }

    fun getCountSelectFile(): Int {
        val selectedVideo: MutableList<VideoTaskItem> = mutableListOf()
        val it = videoTaskItems.iterator()
        while (it.hasNext()) {
            val videoTaskItem: VideoTaskItem = it.next() as VideoTaskItem
            if (videoTaskItem.isChecked) {
                selectedVideo.add(videoTaskItem)
            }
        }
        return selectedVideo.size
    }

    fun deSelectAllItem() {
        val it = this.videoTaskItems.iterator()
        while (it.hasNext()) {
            val videoTaskItem: VideoTaskItem = it.next() as VideoTaskItem
            val position: Int =
                videoTaskItems.indexOf(
                    videoTaskItem
                )
            videoTaskItem.isChecked = false
            notifyItemChanged(position)

        }

        checkIfAllFilesDeselected()

    }

    fun selectAllItem() {
        val it = this.videoTaskItems.iterator()
        while (it.hasNext()) {
            val videoTaskItem: VideoTaskItem = it.next() as VideoTaskItem
            val position: Int =
                videoTaskItems.indexOf(
                    videoTaskItem
                )
            videoTaskItem.isChecked = true
            notifyItemChanged(position)

        }


        checkIfAllFilesDeselected()
    }

    private fun checkIfAllFilesDeselected() {
        val selectedVideo = getSelectedFile()

        (context as SelectVideoActivity).updateStatusNumSelect()

        when (selectedVideo.size) {
            0 -> {
                (context as SelectVideoActivity).setStatusUnSelectAll()
            }

            this.videoTaskItems.size -> {
                (context as SelectVideoActivity).setStatusSelectAll()
            }

            else -> {
                (context as SelectVideoActivity).setStatusUnSelectAll()
            }
        }
    }
}

interface VideoListener {
    fun onItemClicked(videoTaskItem: VideoTaskItem)
    fun onMenuClicked(view: View, videoTaskItem: VideoTaskItem)
    fun onClickItemChecked(videoTaskItem: VideoTaskItem)
}

@GlideModule
class MyGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(Log.ERROR)
    }
}
