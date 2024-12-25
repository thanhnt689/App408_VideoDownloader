package com.files.video.downloader.videoplayerdownloader.downloader.ui.permission

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPermissionBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.disclaimers.DisclaimersActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {
    override fun setBinding(layoutInflater: LayoutInflater): ActivityPermissionBinding {
        return ActivityPermissionBinding.inflate(layoutInflater)
    }

    override fun initView() {
        binding.btnSkip.setOnClickListener {
            startActivity(DisclaimersActivity.newIntent(this))
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, PermissionActivity::class.java)
        }
    }

}