package com.files.video.downloader.videoplayerdownloader.downloader.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogConfirmDeleteBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogExitAppBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogQuestionBinding


class DialogQuestion(
    context: Context,
    var numQuestion: Int,
    var onSelectQuestion: (numQuestion: Int) -> Unit
) :
    Dialog(context) {
    private lateinit var binding: DialogQuestionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogQuestionBinding.inflate(LayoutInflater.from(context))
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

        binding.tvQuestion1.isSelected = true
        binding.tvQuestion2.isSelected = true
        binding.tvQuestion3.isSelected = true

        when (numQuestion) {
            1 -> {
                binding.tvQuestion1.setTextColor(Color.parseColor("#3785FB"))
                binding.tvQuestion2.setTextColor(Color.parseColor("#BFBFBF"))
                binding.tvQuestion3.setTextColor(Color.parseColor("#BFBFBF"))

                binding.dotQuestion1.setImageResource(R.drawable.ic_dot_selected)
                binding.dotQuestion2.setImageResource(R.drawable.ic_dot_normal)
                binding.dotQuestion3.setImageResource(R.drawable.ic_dot_normal)
            }

            2 -> {
                binding.tvQuestion1.setTextColor(Color.parseColor("#BFBFBF"))
                binding.tvQuestion2.setTextColor(Color.parseColor("#3785FB"))
                binding.tvQuestion3.setTextColor(Color.parseColor("#BFBFBF"))

                binding.dotQuestion1.setImageResource(R.drawable.ic_dot_normal)
                binding.dotQuestion2.setImageResource(R.drawable.ic_dot_selected)
                binding.dotQuestion3.setImageResource(R.drawable.ic_dot_normal)
            }

            3 -> {
                binding.tvQuestion1.setTextColor(Color.parseColor("#BFBFBF"))
                binding.tvQuestion2.setTextColor(Color.parseColor("#BFBFBF"))
                binding.tvQuestion3.setTextColor(Color.parseColor("#3785FB"))

                binding.dotQuestion1.setImageResource(R.drawable.ic_dot_normal)
                binding.dotQuestion2.setImageResource(R.drawable.ic_dot_normal)
                binding.dotQuestion3.setImageResource(R.drawable.ic_dot_selected)
            }
        }
    }

    private fun initListener() {
        binding.layoutQuestion1.setOnClickListener {
            dismiss()
            onSelectQuestion(1)
        }

        binding.layoutQuestion2.setOnClickListener {
            dismiss()
            onSelectQuestion(2)
        }

        binding.layoutQuestion3.setOnClickListener {
            dismiss()
            onSelectQuestion(3)
        }
    }

}