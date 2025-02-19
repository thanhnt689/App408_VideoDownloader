package com.files.video.downloader.videoplayerdownloader.downloader.ui.guide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityGuideBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.disclaimers.DisclaimersActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuideActivity : BaseActivity<ActivityGuideBinding>() {

    override fun setBinding(layoutInflater: LayoutInflater): ActivityGuideBinding {
        return ActivityGuideBinding.inflate(layoutInflater)
    }

    override fun initView() {
        if (intent.getStringExtra("open") == "home") {
            binding.tvStep1.text = HtmlCompat.fromHtml(
                getString(R.string.string_step_1),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.tvStep2.text = HtmlCompat.fromHtml(
                getString(R.string.string_step_2),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.imgStep1.setImageResource(R.drawable.img_step_1)
            binding.imgStep2.setImageResource(R.drawable.img_step_2)

        } else {
            binding.tvStep1.text = HtmlCompat.fromHtml(
                getString(R.string.string_step_1_process),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.tvStep2.text = HtmlCompat.fromHtml(
                getString(R.string.string_step_2_process),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            binding.imgStep1.setImageResource(R.drawable.img_step_1_process)
            binding.imgStep2.setImageResource(R.drawable.img_step_2_process)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GuideActivity::class.java)
        }
    }
}