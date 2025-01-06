package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.RecyclerView
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoFormatEntity
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.VideoInfo
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DownloadCandidateItemBinding
import com.google.android.material.color.MaterialColors


class CandidatesListRecyclerViewAdapter(
    private val downloadCandidates: VideoInfo,
    private val selectedFormat: ObservableField<Map<String, String>>,
    private val downloadDialogListener: CandidateFormatListener
) : RecyclerView.Adapter<CandidatesListRecyclerViewAdapter.CandidatesViewHolder>() {

    private var formats: List<VideoFormatEntity> = arrayListOf()

    init {
        val allFormats = downloadCandidates.formats.formats
        formats = getShortenFormats(allFormats)
    }

    class CandidatesViewHolder(val binding: DownloadCandidateItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DownloadCandidateItemBinding.inflate(inflater, parent, false)
        return CandidatesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CandidatesViewHolder, position: Int) {
        with(holder.binding) {
            val candidate = formats[position].format ?: "error"

//            listener = object : CandidateFormatListener {
//                override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
//                    downloadDialogListener.onSelectFormat(videoInfo, format)
//                    notifyDataSetChanged() // Consider using more efficient update methods
//                }
//            }
            val selected = selectedFormat.get()?.get(downloadCandidates.id)

//            val color = MaterialColors.getColor(
//                this.root.context,
//                R.attr.colorSurfaceVariant,
//                Color.YELLOW
//            )
//            this.cardItem.setCardBackgroundColor(color)

//            this.videoInfo = downloadCandidates
//            this.downloadCandidate = candidate
//            this.isCandidateSelected = candidate == selected
            this.tvTitle.text = getShortOfFormat(candidate)

//            this.executePendingBindings()

            this.root.setOnClickListener {
                downloadDialogListener.onSelectFormat(downloadCandidates, candidate)
                notifyDataSetChanged()
            }

            if (candidate == selected) {
                cardItem.setBackgroundResource(R.drawable.bg_item_download_candidate_select)
                tvTitle.setTextColor(Color.parseColor("#A264FF"))
            } else {
                cardItem.setBackgroundResource(R.drawable.bg_item_download_candidate_normal)
                tvTitle.setTextColor(Color.parseColor("#404040"))
            }
        }
    }

    override fun getItemCount(): Int = formats.size

    fun setData(formats: List<VideoFormatEntity>) {
        this.formats = formats
        notifyDataSetChanged()
    }

    private fun makeVideoFormatHumanReadable(input: String): String {
        return input.replace(Regex("-\\w+"), "")
    }

    private fun getShortenFormats(allFormats: List<VideoFormatEntity>): List<VideoFormatEntity> {
        val formatsMap = mutableMapOf<String, VideoFormatEntity>()
        for (format in allFormats) {
            formatsMap[getShortOfFormat(format.format)] = format
        }

        formatsMap.remove("")

        formatsMap.toSortedMap()

        return formatsMap.toSortedMap().values.toList()
    }

    private fun getShortOfFormat(format: String?): String {
        val formattedFormat = makeVideoFormatHumanReadable(format ?: "error")
        if (formattedFormat != "error") {
            return if (formattedFormat.contains("x")) {
                "${formattedFormat.split("x").last().replace(Regex("\\D"), "")}P"
            } else if (!formattedFormat.contains("x") && !formattedFormat.contains("audio only")
                && formattedFormat.contains("-")
            ) {
                val leftSide = formattedFormat.split("-").first()
                if (leftSide.lowercase().contains("hd") || leftSide.contains("sd")) {
                    return leftSide.trim()
                }
                val rightSide = formattedFormat.split("-").last()
                rightSide.replace("p", "P").trim()
            } else if (formattedFormat.contains("audio only")) {
                ""
            } else {
                formattedFormat
            }
        }

        return "Error"
    }
}