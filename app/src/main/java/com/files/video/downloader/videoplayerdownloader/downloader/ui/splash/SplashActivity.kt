package com.files.video.downloader.videoplayerdownloader.downloader.ui.splash

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.realdrum.simpledrumsrock.drumpadmachine.utils.widget.ICallBackProgress
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivitySplashBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.intro.IntroActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.language.LanguageActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    override fun setBinding(layoutInflater: LayoutInflater): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun initView() {
        Handler(Looper.getMainLooper()).postDelayed({
            startNextAct()
        }, 3000)

        binding.loadFile.onProgress = object : ICallBackProgress {
            override fun onProgress(progress: Int) {
                binding.tvLoading.text = getString(R.string.string_loading, progress.toString())
            }
        }
    }
    private fun startNextAct() {
        if (preferenceHelper.getBoolean(PreferenceHelper.PREF_SHOWED_START_LANGUAGE) == true) {
            startActivity(
                IntroActivity.newIntent(this)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        } else {
            startActivity(
                LanguageActivity.newIntent(this, true)
            )
        }
    }
}