package com.files.video.downloader.videoplayerdownloader.downloader.ui.pin

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivitySecurityBinding

class SecurityActivity : BaseActivity<ActivitySecurityBinding>() {

    override fun setBinding(layoutInflater: LayoutInflater): ActivitySecurityBinding {
        return ActivitySecurityBinding.inflate(layoutInflater)
    }

    override fun initView() {

    }
}