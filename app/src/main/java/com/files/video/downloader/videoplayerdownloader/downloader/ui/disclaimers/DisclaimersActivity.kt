package com.files.video.downloader.videoplayerdownloader.downloader.ui.disclaimers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.files.video.downloader.videoplayerdownloader.downloader.MainActivity
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityDisclaimersBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.permission.PermissionActivity

class DisclaimersActivity : BaseActivity<ActivityDisclaimersBinding>() {

    override fun setBinding(layoutInflater: LayoutInflater): ActivityDisclaimersBinding {
        return ActivityDisclaimersBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.btnAgree.setOnClickListener {
            val intent =
                Intent(this@DisclaimersActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, DisclaimersActivity::class.java)
        }
    }
}