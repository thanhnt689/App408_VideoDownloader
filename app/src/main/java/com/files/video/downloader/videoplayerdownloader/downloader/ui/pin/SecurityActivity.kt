package com.files.video.downloader.videoplayerdownloader.downloader.ui.pin

import android.os.Bundle
import android.view.LayoutInflater
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
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
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

        binding.imgMoreQuestion.setOnClickListener {
            showDialogQuestion(selectQuestion)
        }

        binding.imgDone.setOnClickListener {
            if (binding.edtSearch.text.toString().trim().isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.string_please_enter_your_answer), Toast.LENGTH_SHORT
                ).show()
            } else {

                showDialogSuccessfully()

            }
        }
    }

    private fun showDialogSuccessfully() {

        preferences.setIsSetupPinCode(true)

        preferences.setNumSecurityQuestion(selectQuestion)

        preferences.setPinCode(password)

        preferences.setSecurityAnswer(binding.edtSearch.text.toString().trim())

        val dialogSuccessfully = DialogSetPinSuccessfully(this) {

            finish()
        }

        dialogSuccessfully.show()
    }

    private fun showDialogQuestion(selectQuestion: Int) {
        val dialogQuestion = DialogQuestion(this, selectQuestion) {
            this.selectQuestion = it

            updateTextSelectQuestion(selectQuestion)
        }

        dialogQuestion.show()
    }

    private fun updateTextSelectQuestion(selectQuestion: Int) {
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
}