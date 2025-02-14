package com.files.video.downloader.videoplayerdownloader.downloader.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogConfirmDeleteBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogDownloadSuccessfulBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogExitAppBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogInformationImageBinding


class DialogInformationImage(
    context: Context,
    var link: String,
    var onClickOpenNewTab: (link: String) -> Unit,
    var onClickShare: (link: String) -> Unit,
    var onClickCopyLink: (link: String) -> Unit,
    var onClickDownloadImage: (link: String) -> Unit,
) : Dialog(context) {
    private lateinit var binding: DialogInformationImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogInformationImageBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        window?.apply {
            val windowParams = attributes
            setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            windowParams.dimAmount = 0.8f
            attributes = windowParams
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        setCancelable(true)

        initViews()

        initListener()

    }

    private fun initViews() {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun initListener() {
        binding.tvLink.text = link
        binding.tvOpenNewTab.setOnClickListener {
            onClickOpenNewTab.invoke(link)
            dismiss()
        }

        binding.tvShare.setOnClickListener {
            onClickShare.invoke(link)
            dismiss()
        }

        binding.tvCopyLink.setOnClickListener {
            onClickCopyLink.invoke(link)
            dismiss()
        }

        binding.tvDownloadImage.setOnClickListener {
            onClickDownloadImage.invoke(link)
            dismiss()
        }

    }

}