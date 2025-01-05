package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ItemVideoInfoBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.DetectedVideosTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.google.android.material.bottomsheet.BottomSheetDialog

class VideoInfoAdapter(
    private var videoInfoList: List<VideoInfo>,
    private val model: DetectedVideosTabViewModel,
    private val downloadVideoListener: DownloadTabListener,
    private val appUtil: AppUtil
) : RecyclerView.Adapter<VideoInfoAdapter.ViewHolder>() {
    class ViewHolder(
        var binding: ItemVideoInfoBinding, val model: DetectedVideosTabViewModel,
        private val candidateFormatListener: DownloadTabListener,
        private val appUtil: AppUtil
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(info: VideoInfo) {
            with(binding) {
                val titles = model.formatsTitles.get()?.toMutableMap() ?: mutableMapOf()
                titles[info.id] = titles[info.id] ?: info.title

                model.formatsTitles.set(titles)

                val frmts = model.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
                val selected = frmts[info.id]
                val defaultFormat = info.formats.formats.lastOrNull()?.format ?: "unknown"
                if (selected == null) {
                    frmts[info.id] = defaultFormat
                }

                model.selectedFormats.set(frmts)

                if (info.isRegularDownload) {
                    model.selectedFormatUrl.set(info.firstUrlToString)
                } else {
                    model.selectedFormatUrl.set(info.formats.formats.lastOrNull()?.url)
                }


                videoInfo = info
                val typeText = if (info.isM3u8) {
                    val isMpd = info.formats.formats.firstOrNull()?.url?.contains(".mpd") == true
                    if (isMpd) "MPD List" else "M3U8 List"
                } else if (info.isMaster) {
                    val isMpd = info.formats.formats.firstOrNull()?.url?.contains(".mpd") == true
                    if (isMpd) "MPD Master List" else "M3U8 Mater List"
                } else if (info.isRegularDownload) {
                    "Regular MP4 Download"
                } else {
                    ""
                }
                if (info.isRegularDownload) {
                    val fileSize = info.formats.formats.firstOrNull()?.fileSize
                    if (fileSize != null) {
                        val size = FileUtil.getFileSizeReadable(fileSize.toDouble())
                        binding.txtSize.text = "Download Size: $size"
                    }
                }
                typeTextView.text = typeText
                videoTitleRenameButton.setOnClickListener {
                    videoTitleEdit.requestFocus()
                    this.videoTitleEdit.selectAll()
                    appUtil.showSoftKeyboard(videoTitleEdit)
                }

                this.videoTitleEdit.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        this.videoTitleEdit.clearFocus()
                        appUtil.hideSoftKeyboard(videoTitleEdit)

                        false
                    } else false
                }

                nameFile.setText(titles[info.id])

                viewModel = model

                model.selectedFormats.addOnPropertyChangedCallback(object :
                    OnPropertyChangedCallback() {
                    override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                        val curSelected = model.selectedFormats.get()?.get(videoInfo?.id)
                        val foundFormat =
                            videoInfo?.formats?.formats?.find { it.format == curSelected }
                        model.selectedFormatUrl.set(foundFormat?.url.toString())
                    }
                })

                val layoutManager =
                    LinearLayoutManager(
                        binding.root.context,
                        RecyclerView.HORIZONTAL,
                        false
                    )
                candidatesList.layoutManager = layoutManager
                candidatesList.adapter = CandidatesListRecyclerViewAdapter(
                    info,
                    model.selectedFormats,
                    candidateFormatListener
                )

                dialogListener = object : DownloadTabListener {
                    override fun onCancel() {
                        candidateFormatListener.onCancel()
                    }

                    override fun onPreviewVideo(
                        videoInfo: VideoInfo,
                        format: String,
                        isForce: Boolean
                    ) {
                        candidateFormatListener.onPreviewVideo(videoInfo, format, isForce)
                    }

                    override fun onDownloadVideo(
                        videoInfo: VideoInfo,
                        format: String,
                        videoTitle: String
                    ) {
                        val text = model.formatsTitles.get()?.get(videoInfo.id)
                        if (text != null) {
                            candidateFormatListener.onDownloadVideo(videoInfo, format, text)
                        }
                    }

                    override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
                        candidateFormatListener.onSelectFormat(videoInfo, format)
                    }
                }

                videoTitleEdit.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        val title = p0.toString()
                        val titlesF = model.formatsTitles.get()?.toMutableMap() ?: mutableMapOf()
                        titlesF[info.id] = title
                        model.formatsTitles.set(titlesF)
                    }

                    override fun afterTextChanged(p0: Editable?) {
                    }
                })

                executePendingBindings()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemVideoInfoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), model, downloadVideoListener, appUtil
        )
    }

    override fun getItemCount(): Int {
        return videoInfoList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val videoInfo = videoInfoList[position]
        holder.bind(videoInfo)
    }

    fun setData(localVideos: List<VideoInfo>) {
        this.videoInfoList = localVideos.reversed()
        notifyDataSetChanged()
    }
}

interface DownloadVideoListener {
    fun onPreviewVideo(
        videoInfo: VideoInfo,
        dialog: BottomSheetDialog?,
        format: String,
        isForce: Boolean
    )

    fun onDownloadVideo(
        videoInfo: VideoInfo,
        dialog: BottomSheetDialog?,
        format: String,
        videoTitle: String
    )
}

interface DownloadTabVideoListener {
    fun onPreviewVideo(
        videoInfo: VideoInfo,
        format: String,
        isForce: Boolean
    )

    fun onDownloadVideo(
        videoInfo: VideoInfo,
        format: String,
        videoTitle: String
    )
}

interface DownloadDialogListener : DownloadVideoListener, CandidateFormatListener {
    fun onCancel(dialog: BottomSheetDialog?)
}

interface DownloadTabListener : DownloadTabVideoListener, CandidateFormatListener {
    fun onCancel()
}

interface CandidateFormatListener {
    fun onSelectFormat(videoInfo: VideoInfo, format: String)
}
