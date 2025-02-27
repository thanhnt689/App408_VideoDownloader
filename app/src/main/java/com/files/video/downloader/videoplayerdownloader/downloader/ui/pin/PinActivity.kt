package com.files.video.downloader.videoplayerdownloader.downloader.ui.pin

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPinBinding
import com.files.video.downloader.videoplayerdownloader.downloader.extensions.hasNetworkConnection
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo.PrivateVideoActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.nlbn.ads.banner.BannerPlugin
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PinActivity : BaseActivity<ActivityPinBinding>() {

    private var isSetupPinCode = false

    private var isInCorrectPass = false

    private var currentEditTextIndex = 0

    private var passcode = mutableListOf<String>()

    private var isSetPassFirst = false

    private var passCodeFirst = ""
    private var passCodeSecond = ""

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private val editTexts: List<ImageView> by lazy {
        listOf(
            binding.edt1,
            binding.edt2,
            binding.edt3,
            binding.edt4,
        )
    }

    override fun setBinding(layoutInflater: LayoutInflater): ActivityPinBinding {
        return ActivityPinBinding.inflate(layoutInflater)
    }

    override fun initView() {

        isSetupPinCode = preferenceHelper.getIsSetupPinCode()

        loadBanner()

        if (isSetupPinCode && intent.getStringExtra("action") != "changePinCode") {
            binding.tvForgotPassword.visibility = View.VISIBLE
            binding.tvTitlePin.text = getString(R.string.string_enter_your_pin)
            binding.tvDesPin.text = getString(R.string.string_enter_your_pin_to_confirm)
        } else {
            binding.tvForgotPassword.visibility = View.GONE
            binding.tvTitlePin.text = getString(R.string.string_enter_your_new_pin)
            binding.tvDesPin.text = getString(R.string.string_des_pin)
        }

        binding.imgBack.setOnClickListener {

            finish()

        }

        binding.btn0.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.ZERO)
            }
        }

        binding.btn1.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.ONE)
            }
        }

        binding.btn2.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.TWO)
            }
        }

        binding.btn3.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.THREE)
            }
        }

        binding.btn4.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.FOUR)
            }
        }

        binding.btn5.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.FIVE)
            }
        }

        binding.btn6.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.SIX)
            }
        }

        binding.btn7.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.SEVEN)
            }
        }

        binding.btn8.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.EIGHT)
            }
        }

        binding.btn9.setOnClickListener {
            if (passcode.size < 4) {
                setPassword(editTexts[currentEditTextIndex], TextPassword.NINE)
            }
        }

        binding.btnRemove.setOnClickListener {
            if (currentEditTextIndex >= 0) {
                clearEditText(currentEditTextIndex)
                if (currentEditTextIndex > 0) {
                    editTexts[currentEditTextIndex - 1].requestFocus()
                    currentEditTextIndex--
                }

                if (currentEditTextIndex == 0) {
                    binding.btnRemove.setImageResource(R.drawable.ic_remove_disable)
                }
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java).apply {
                putExtra("status", "forgot")
            })

            finish()
        }
    }

    private fun clearEditText(index: Int) {
        if (index <= 0) {
            return
        } else {
            editTexts[index - 1].setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_circle_unselected
                )
            )
            passcode.removeAt(index - 1)

            if (!isSetPassFirst) {
                passCodeFirst = ""
                for (i in passcode.indices) {
                    passCodeFirst += passcode[i]
                }
            } else {
                passCodeSecond = ""
                for (i in passcode.indices) {
                    passCodeSecond += passcode[i]
                }
            }

            Log.d("ntt", "clearEditText: passCodeFirst: $passCodeFirst")

            Log.d("ntt", "clearEditText: passCodeSecond: $passCodeSecond")
        }
    }

    private fun setPassword(currentEdit: ImageView, number: String) {
        passcode.add(currentEditTextIndex, number)

        if (isInCorrectPass) {
            isInCorrectPass = false

            for (i in 0..<4) {
                editTexts[i].setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_circle_unselected
                    )
                )
            }
        }

        binding.btnRemove.setImageResource(R.drawable.ic_remove)

        binding.tvIncorrect.visibility = View.GONE

        currentEdit.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_circle_selected
            )
        )

        if (currentEditTextIndex < editTexts.size) {
            currentEditTextIndex++
        }

        if (isSetupPinCode && intent.getStringExtra("action") != "changePinCode") {
            if (passcode.size == 4) {
                var passCodeSetup = ""
                for (i in passcode.indices) {
                    passCodeSetup += passcode[i]
                }

                if (passCodeSetup == preferenceHelper.getPinCode()) {

                    startActivity(Intent(this, PrivateVideoActivity::class.java))

                    finish()
                } else {

                    binding.tvIncorrect.visibility = View.VISIBLE

                }
            }
        } else {
            if (!isSetPassFirst) {
                passCodeFirst = ""
                for (i in passcode.indices) {
                    passCodeFirst += passcode[i]
                }

                if (passcode.size == 4) {
                    isSetPassFirst = true

                    binding.tvTitlePin.text = getString(R.string.string_re_enter_your_pin)

                    binding.tvDesPin.text = getString(R.string.string_re_enter_your_pin_to_confirm)

                    for (i in 0..<passcode.size) {
                        editTexts[i].setImageDrawable(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.ic_circle_unselected
                            )
                        )
                    }

                    passcode.clear()

                    currentEditTextIndex = 0
                }
            } else {
                passCodeSecond = ""
                for (i in passcode.indices) {
                    passCodeSecond += passcode[i]
                }

                if (passcode.size == 4) {
                    if (passCodeFirst == passCodeSecond) {
                        Toast.makeText(this, "true", Toast.LENGTH_SHORT).show()

                        if (intent.getStringExtra("action") == "changePinCode") {
                            preferenceHelper.setIsSetupPinCode(true)

                            preferenceHelper.setPinCode(passCodeFirst)

                            finish()
                        } else {
                            startActivity(Intent(this, SecurityActivity::class.java).apply {
                                putExtra("pass", passCodeFirst)
                            })

                            finish()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.string_password_does_not_match), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        Log.d("ntt", "setPassword: passCodeFirst: $passCodeFirst")

        Log.d("ntt", "setPassword: passCodeSecond: $passCodeSecond")

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
}

object TextPassword {
    const val ZERO = "0"
    const val ONE = "1"
    const val TWO = "2"
    const val THREE = "3"
    const val FOUR = "4"
    const val FIVE = "5"
    const val SIX = "6"
    const val SEVEN = "7"
    const val EIGHT = "8"
    const val NINE = "9"
    const val EMPTY = ""
}