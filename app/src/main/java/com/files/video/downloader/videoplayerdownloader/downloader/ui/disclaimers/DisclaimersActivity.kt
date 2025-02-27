package com.files.video.downloader.videoplayerdownloader.downloader.ui.disclaimers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.files.video.downloader.videoplayerdownloader.downloader.MainActivity
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityDisclaimersBinding
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.ui.permission.PermissionActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.nlbn.ads.banner.BannerPlugin
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper

class DisclaimersActivity : BaseActivity<ActivityDisclaimersBinding>() {

    override fun setBinding(layoutInflater: LayoutInflater): ActivityDisclaimersBinding {
        return ActivityDisclaimersBinding.inflate(layoutInflater)
    }

    override fun initView() {

        loadBanner()

        binding.btnAgree.setOnClickListener {
            val intent =
                Intent(this@DisclaimersActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun loadBanner() {
        if (this.hasNetworkConnection() && ConsentHelper.getInstance(this).canRequestAds()) {

            if (AdsConstant.isLoadBanner) {
                binding.frBanner.visibility = View.VISIBLE
                val config = BannerPlugin.Config()
                config.defaultAdUnitId = getString(R.string.banner)
                config.defaultBannerType = BannerPlugin.BannerType.Adaptive
                val cbFetchInterval =
                    FirebaseRemoteConfig.getInstance().getLong("cb_fetch_interval").toInt()
                config.defaultRefreshRateSec = cbFetchInterval
                config.defaultCBFetchIntervalSec = cbFetchInterval
                Admob.getInstance().loadBannerPlugin(
                    this,
                    findViewById<ViewGroup>(R.id.fr_banner),
                    findViewById<ViewGroup>(R.id.include),
                    config
                )
            } else {
                binding.frBanner.visibility = View.GONE
            }

        } else {
            binding.frBanner.visibility = View.GONE
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, DisclaimersActivity::class.java)
        }
    }
}