package com.files.video.downloader.videoplayerdownloader.downloader.ui.pin

import android.view.LayoutInflater
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPinBinding

class PinActivity : BaseActivity<ActivityPinBinding>() {

    override fun setBinding(layoutInflater: LayoutInflater): ActivityPinBinding {
        return ActivityPinBinding.inflate(layoutInflater)
    }

    override fun initView() {

    }
}