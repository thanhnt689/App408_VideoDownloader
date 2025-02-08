package com.files.video.downloader.videoplayerdownloader.downloader.ui.splash

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.realdrum.simpledrumsrock.drumpadmachine.utils.widget.ICallBackProgress
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivitySplashBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.intro.IntroActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.language.LanguageActivity
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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

        val path =
            "/storage/emulated/0/Android/data/com.files.video.downloader.videoplayerdownloader.downloader/files/SuperVideoDownloader/tiktok_com.mp4"
        val result = extractFileInfo(path)

        Log.d("ntt", "Tên file đầy đủ: ${result.first}")
        Log.d("ntt", "Tên file không có đuôi: ${result.second}") // tiktok_com_copy_copy_copy_copy
        Log.d("ntt", "Thư mục chứa file: ${result.third}") // .../files/SuperVideoDownloader/

//        getFileInfo(path)
//
//        val duration = getVideoDuration(this, path)
//        Log.d("ntt", "Thời lượng video: ${duration / 1000} giây")
    }

    fun extractFileInfo(path: String): Triple<String, String, String> {
        val fullFileName = path.substringAfterLast("/")  // Tên file có đuôi
        val fileNameWithoutExt = fullFileName.substringBeforeLast(".") // Tên file không có đuôi
        val parentFolder = path.substringBeforeLast("/") // Thư mục chứa file

        return Triple(fullFileName, fileNameWithoutExt, parentFolder)
    }

    fun getFileInfo(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            val fileSize = file.length() // Kích thước file (bytes)
            val lastModified = Date(file.lastModified()) // Thời gian chỉnh sửa cuối

            Log.d("ntt", "Kích thước: ${fileSize / 1024} KB")
            Log.d(
                "ntt",
                "Ngày tạo: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(lastModified)}"
            )
        } else {
            Log.d("ntt", "File không tồn tại!")
        }
    }

    fun getVideoDuration(context: Context, filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(File(filePath)))
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return duration?.toLongOrNull() ?: 0
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