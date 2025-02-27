package com.files.video.downloader.videoplayerdownloader.downloader.ui.pin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivitySecurityBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogQuestion
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogSetPinSuccessfully
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SecurityActivity : BaseActivity<ActivitySecurityBinding>() {

    private var password = ""

    private var selectQuestion = 1

    @Inject
    lateinit var preferences: PreferenceHelper

    override fun setBinding(layoutInflater: LayoutInflater): ActivitySecurityBinding {
        return ActivitySecurityBinding.inflate(layoutInflater)
    }

    override fun initView() {
        password = intent.getStringExtra("pass").toString()

        updateTextSelectQuestion(selectQuestion)

        loadNativeSecurityQuestion()

        binding.imgMoreQuestion.setOnClickListener {
            showDialogQuestion(selectQuestion)
        }

        binding.imgBack.setOnClickListener {
            finish()
        }

        binding.imgDone.setOnClickListener {
            if (binding.edtSearch.text.toString().trim().isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.string_please_enter_your_answer), Toast.LENGTH_SHORT
                ).show()
            } else {
                if (intent.getStringExtra("status") == "forgot") {
                    if (binding.edtSearch.text.toString()
                            .trim() == preferences.getSecurityAnswer() && selectQuestion == preferences.getNumSecurityQuestion()
                    ) {
                        Toast.makeText(
                            this,
                            getString(R.string.string_correct_security_question), Toast.LENGTH_SHORT
                        ).show()

                        preferences.setIsSetupPinCode(false)

                        preferences.setNumSecurityQuestion(1)

                        preferences.setPinCode("")

                        preferences.setSecurityAnswer("")

                        startActivity(Intent(this, PinActivity::class.java).apply {
                            putExtra("status", "security")
                        })

                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.string_incorrect_security_question),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    showDialogSuccessfully()
                }
            }
        }
    }

    private fun showDialogSuccessfully() {

        binding.frAds.visibility = View.GONE

        preferences.setIsSetupPinCode(true)

        preferences.setNumSecurityQuestion(selectQuestion)

        preferences.setPinCode(password)

        preferences.setSecurityAnswer(binding.edtSearch.text.toString().trim())

        val dialogSuccessfully = DialogSetPinSuccessfully(this) {

            finish()
        }

        dialogSuccessfully.show()

        dialogSuccessfully.setOnDismissListener {
            binding.frAds.visibility = View.VISIBLE
        }
    }

    private fun showDialogQuestion(selectQuestion: Int) {
        binding.frAds.visibility = View.GONE
        val dialogQuestion = DialogQuestion(this, selectQuestion) { it ->

            Log.d("ntt", "showDialogQuestion: it: $it")

            this.selectQuestion = it

            updateTextSelectQuestion(it)
        }

        dialogQuestion.show()

        dialogQuestion.setOnDismissListener {
            binding.frAds.visibility = View.VISIBLE
        }
    }

    private fun updateTextSelectQuestion(selectQuestion: Int) {
        Log.d("ntt", "updateTextSelectQuestion: selectQuestion: $selectQuestion")
        when (selectQuestion) {
            1 -> {
                binding.tvQuestion.text = getString(R.string.string_what_is_your_lucky_number)
            }

            2 -> {
                binding.tvQuestion.text = getString(R.string.string_what_is_your_favourite_pet_name)
            }

            3 -> {
                binding.tvQuestion.text =
                    getString(R.string.string_what_was_your_favorite_food_as_a_child)
            }
        }
    }

    private fun loadNativeSecurityQuestion() {
        if (hasNetworkConnection() && ConsentHelper.getInstance(this)
                .canRequestAds() && AdsConstant.isLoadNativeSecurity && Admob.getInstance().isLoadFullAds
        ) {

            if (AdsConstant.nativeAdsAll != null) {
                val adView = if (Admob.getInstance().isLoadFullAds) {
                    LayoutInflater.from(this@SecurityActivity)
                        .inflate(
                            R.layout.layout_ads_native_update_no_bor,
                            null
                        ) as NativeAdView
                } else {
                    LayoutInflater.from(this@SecurityActivity)
                        .inflate(
                            R.layout.layout_ads_native_update,
                            null
                        ) as NativeAdView
                }
                binding.frAds.removeAllViews()
                binding.frAds.addView(adView)
                Admob.getInstance().pushAdsToViewCustom(AdsConstant.nativeAdsAll, adView)
            } else {
                try {
                    Admob.getInstance().loadNativeAd(
                        this,
                        getString(R.string.native_all),
                        object : NativeCallback() {
                            override fun onNativeAdLoaded(nativeAd: NativeAd?) {
                                val adView = if (Admob.getInstance().isLoadFullAds) {
                                    LayoutInflater.from(this@SecurityActivity)
                                        .inflate(
                                            R.layout.layout_ads_native_update_no_bor,
                                            null
                                        ) as NativeAdView
                                } else {
                                    LayoutInflater.from(this@SecurityActivity)
                                        .inflate(
                                            R.layout.layout_ads_native_update,
                                            null
                                        ) as NativeAdView
                                }
                                binding.frAds.removeAllViews()
                                binding.frAds.addView(adView)
                                Admob.getInstance().pushAdsToViewCustom(nativeAd, adView)
                            }

                            override fun onAdFailedToLoad() {
                                binding.frAds.removeAllViews()
                            }
                        })
                } catch (e: Exception) {
                    binding.frAds.removeAllViews()
                }
            }
        } else {
            binding.frAds.removeAllViews()
        }
    }

}