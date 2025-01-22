package com.files.video.downloader.videoplayerdownloader.downloader.ui.pin

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPinBinding
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PinActivity : BaseActivity<ActivityPinBinding>() {

    private var isSetupPinCode = false

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

        if (isSetupPinCode) {
            binding.tvForgotPassword.visibility = View.VISIBLE
            binding.tvTitlePin.text = getString(R.string.string_enter_your_pin)
            binding.tvDesPin.text = getString(R.string.string_enter_your_pin_to_confirm)
        } else {
            binding.tvForgotPassword.visibility = View.GONE
            binding.tvTitlePin.text = getString(R.string.string_enter_your_new_pin)
            binding.tvDesPin.text = getString(R.string.string_des_pin)
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

        binding.btnRemove.setImageResource(R.drawable.ic_remove)

        currentEdit.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_circle_selected
            )
        )

        if (currentEditTextIndex < editTexts.size) {
            currentEditTextIndex++
        }

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

                    startActivity(Intent(this, SecurityActivity::class.java).apply {
                        putExtra("pass", passCodeFirst)
                    })
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.string_password_does_not_match), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        Log.d("ntt", "setPassword: passCodeFirst: $passCodeFirst")

        Log.d("ntt", "setPassword: passCodeSecond: $passCodeSecond")

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