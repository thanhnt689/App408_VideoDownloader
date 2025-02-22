package com.files.video.downloader.videoplayerdownloader.downloader.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogAddBookmarkBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogConfirmDeleteBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogDownloadSuccessfulBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.DialogExitAppBinding
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient


class DialogAddBookmark(
    context: Context,
    var name: String = "",
    var url: String = "",
    var add: (name: String, url: String) -> Unit
) :
    Dialog(context) {
    private lateinit var binding: DialogAddBookmarkBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogAddBookmarkBinding.inflate(LayoutInflater.from(context))
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

        binding.edtName.setText(this.name)
        binding.edtUrl.setText(this.url)
    }

    private fun initListener() {
        binding.tvAdd.setOnClickListener {

            if (binding.edtName.text.toString().trim().isNotEmpty() && Patterns.WEB_URL.matcher(
                    binding.edtUrl.text.toString().trim()
                ).matches() && binding.edtUrl.text.toString().trim().isNotEmpty()
            ) {
                dismiss()
                add(binding.edtName.text.toString(), binding.edtUrl.text.toString())
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.string_invalid_data), Toast.LENGTH_SHORT
                ).show()
            }

        }

        binding.tvCancel.setOnClickListener {
            dismiss()
        }
    }

}