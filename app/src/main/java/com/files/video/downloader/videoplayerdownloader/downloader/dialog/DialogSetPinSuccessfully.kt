package com.files.video.downloader.videoplayerdownloader.downloader.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogQuestionBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogSetPinSuccessfullyBinding


class DialogSetPinSuccessfully(
    context: Context,
    var onSelectOk: () -> Unit
) :
    Dialog(context) {
    private lateinit var binding: DialogSetPinSuccessfullyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSetPinSuccessfullyBinding.inflate(LayoutInflater.from(context))
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

        initViews()

        initListener()

    }

    private fun initViews() {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun initListener() {
        binding.tvOk.setOnClickListener {
            onSelectOk()
            dismiss()
        }

    }

}