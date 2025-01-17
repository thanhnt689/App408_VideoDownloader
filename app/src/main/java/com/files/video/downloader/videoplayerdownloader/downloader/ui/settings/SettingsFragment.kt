package com.files.video.downloader.videoplayerdownloader.downloader.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.BuildConfig
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseFragment
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.FragmentSettingsBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.RatingDialog
import com.files.video.downloader.videoplayerdownloader.downloader.ui.language.LanguageActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {
    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private var isShareDialogOpen = false

    @Inject
    lateinit var fileUtil: FileUtil

    override fun getViewBinding(): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (preferenceHelper.isRate()) {
            binding.layoutRate.visibility = View.GONE
        } else {
            binding.layoutRate.visibility = View.VISIBLE
        }

        binding.layoutLanguage.setOnClickListener {
            startActivity(LanguageActivity.newIntent(requireContext(), false))
        }

        binding.layoutRate.setOnClickListener {
            showDialogRate()
        }

        binding.layoutShare.setOnClickListener {
            onClickLayoutShare()
        }

        binding.layoutPolicy.setOnClickListener {
            onClickLayoutPrivatePolicy()
        }

        binding.tvDesDownloadLocation.text = fileUtil.folderDir.path.toString()
    }

    private fun showDialogRate() {
        val ratingDialog = RatingDialog(requireContext())
        ratingDialog.init(requireContext(), object : RatingDialog.OnPress {
            override fun sendThank() {
                preferenceHelper.forceRated()
                binding.layoutRate.visibility = View.GONE
                ratingDialog.dismiss()

                Toast.makeText(
                    requireContext(),
                    getString(R.string.string_thank_for_rate),
                    Toast.LENGTH_SHORT
                ).show()

            }

            override fun rating() {
                binding.layoutRate.visibility = View.GONE
                val manager = ReviewManagerFactory.create(requireContext())
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                        flow.addOnSuccessListener {
                            preferenceHelper.forceRated()
                            ratingDialog.dismiss()
                        }
                    } else {
                        preferenceHelper.forceRated()
                        ratingDialog.dismiss()
                    }
                }

            }

            override fun later() {
                ratingDialog.dismiss()
            }
        })

        try {
            ratingDialog.show()
        } catch (e: WindowManager.BadTokenException) {
            e.printStackTrace()
        }

    }

    private fun onClickLayoutShare() {
        try {
            if (!isShareDialogOpen) {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                var shareMessage =
                    "${getString(R.string.app_name)} ${getString(R.string.string_let_me_recommend_you_this_application)} ".trimIndent()
                shareMessage =
                    "${shareMessage}https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}".trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)

                activity?.window!!.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

                startActivityForResult(
                    Intent.createChooser(
                        shareIntent,
                        getString(R.string.string_choose_one)
                    ), 1900
                )

                isShareDialogOpen = true
            }
        } catch (e: Exception) {
            Log.d("ntt", "onClickLayoutShare: ${e.printStackTrace()}")
        }
    }

    private fun onClickLayoutPrivatePolicy() {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(""))
        startActivity(browserIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1900) {
            isShareDialogOpen = false
        }
    }

}