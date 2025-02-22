package com.files.video.downloader.videoplayerdownloader.downloader.ui.media

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.media.ThumbnailUtils
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPlayMediaBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.LayoutBottomSheetDetailBinding
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.LayoutBottomSheetPermissionBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogRename
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.ui.privateVideo.PrivateVideoViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.FileUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.SystemUtil
import com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models.VideoTaskItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlayMediaActivity : BaseActivity<ActivityPlayMediaBinding>() {

    companion object {
        var DEFAULT_COOKIE_MANAGER: java.net.CookieManager? = null

        const val VIDEO_URL = "video_url"
        const val ITEM_TYPE = "item_type"
        const val VIDEO_HEADERS = "video_headers"
        const val VIDEO_NAME = "video_name"
        const val VIDEO_ITEM = "video_item"
        const val IS_DOWNLOADED = "is_downloaded"
    }

    init {
        DEFAULT_COOKIE_MANAGER = java.net.CookieManager()
        DEFAULT_COOKIE_MANAGER?.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val playMediaViewModel: PlayMediaViewModel by viewModels()

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    @Inject
    lateinit var appUtil: AppUtil

    private lateinit var player: ExoPlayer

    private var isPlay = false

    private var isPause = false

    private var curPosVideo = 0L

    private val handlerUI = Handler()

    private var isLoop = false

    private var isFill = false

    private var speed = 1.0f

    private val speedValues = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    private var currentIndexSpeed = 0

    private lateinit var audioManager: AudioManager

    private val privateVideoViewModel: PrivateVideoViewModel by viewModels()

    private lateinit var detailLayoutBinding: LayoutBottomSheetDetailBinding

    private lateinit var bottomSheetDetailDialog: BottomSheetDialog

    private var videoUri: Uri? = null

    @Inject
    lateinit var fileUtil: FileUtil

    private var isDownloaded = false


    override fun setBinding(layoutInflater: LayoutInflater): ActivityPlayMediaBinding {
        return ActivityPlayMediaBinding.inflate(layoutInflater)
    }

    override fun initView() {
        playMediaViewModel.start()

//        videoUri = intent?.data
//
//        if (videoUri != null) {
//            val fileName = getFileNameFromUri(this, videoUri!!)
//
//            binding.btnMore.visibility = View.GONE
//        } else {
//
//        }

        bottomSheetDetailDialog = BottomSheetDialog(this, R.style.CustomAlertBottomSheet)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        isDownloaded = intent.getBooleanExtra(IS_DOWNLOADED, false)

        if (!isDownloaded) {
            binding.btnMore.visibility = View.GONE
        } else {
            binding.btnMore.visibility = View.VISIBLE
        }

        if (intent.getStringExtra(ITEM_TYPE) == "video") {
            binding.layoutVideo.visibility = View.VISIBLE
            binding.layoutImage.visibility = View.GONE

            isFill = preferenceHelper.getIsFillMedia()

            requestedOrientation = if (isFill) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

        } else {
            binding.layoutVideo.visibility = View.GONE
            binding.layoutImage.visibility = View.VISIBLE

            Glide.with(this)
                .load(intent.getStringExtra(VIDEO_URL))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgDownload)
        }

        intent.getStringExtra(VIDEO_HEADERS)?.let { rawHeaders ->
            val headers =
                Json.parseToJsonElement(rawHeaders).jsonObject.toMap()
                    .map { it.key to it.value.toString() }
                    .toMap()
            playMediaViewModel.videoHeaders.set(headers)
        }

        intent.getStringExtra(VIDEO_NAME)?.let {
            playMediaViewModel.videoName.set(it)
            binding.tvName.text = it
        }

        binding.tvName.isSelected = true

        val iUrl = Uri.parse(intent.getStringExtra(VIDEO_URL))

        if (iUrl != null) {
            playMediaViewModel.videoUrl.set(iUrl)
        }

        val url = playMediaViewModel.videoUrl.get() ?: Uri.EMPTY
        val headers = playMediaViewModel.videoHeaders.get() ?: emptyMap()

        speed = preferenceHelper.getSpeedMedia()
        currentIndexSpeed = setCurrentPositionSpeed(speed)
        binding.tvSpeed.text = "${speed}x"

        isLoop = preferenceHelper.getIsLoopMedia()
        updateStatusLoop(isLoop)



        updateFillMedia(isFill)

        playMediaViewModel.videoName.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val name = playMediaViewModel.videoName.get()
                binding.tvName.text = name.toString()
            }
        })


        if (intent.getStringExtra(ITEM_TYPE) == "video") {
            val mediaItem: MediaItem = MediaItem.fromUri(url)
            val mediaFactory = createMediaFactory(headers, url.toString().startsWith("http"))

            val cookiesStrArr = headers["Cookie"]?.split(";")
            if (!cookiesStrArr.isNullOrEmpty()) {
                for (cookiePair in cookiesStrArr) {
                    val tmp = cookiePair.split("=")
                    val key = tmp.firstOrNull()
                    val value = tmp.lastOrNull()

                    if (key != null && value != null) {
                        DEFAULT_COOKIE_MANAGER?.cookieStore?.add(
                            URI(url.toString()),
                            HttpCookie(key, value)
                        )
                    }
                }
            }

            player = ExoPlayer.Builder(this)
                .setRenderersFactory(createRenderFactory())
                .build()

            binding.playerView.player = player
            binding.playerView.setShowBuffering(SHOW_BUFFERING_ALWAYS)
            player.setPlaybackSpeed(speed)

            if (isFill) {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            } else {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }

            binding.playerView.setFullscreenButtonClickListener {
                if (it) {
                    binding.toolbar.visibility = View.GONE
                } else {
                    binding.toolbar.visibility = View.VISIBLE
                }
            }

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (isLoop) {
                            player.seekTo(0)

                            playAudio()
                        } else {
                            player.seekTo(0)

                            pauseAudio()

                        }
                    } else if (playbackState == Player.STATE_READY) {
                        binding.pbLoading.visibility = View.GONE
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        // Hiển thị ImageView khi video đang tải
                        binding.pbLoading.visibility = View.VISIBLE
                    } else {
                        binding.pbLoading.visibility = View.GONE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (playMediaViewModel.videoUrl.get().toString().startsWith("http")) {
                        AlertDialog.Builder(this@PlayMediaActivity)
                            .setTitle(getString(R.string.string_download_only))
                            .setCancelable(false)
                            .setMessage(getString(R.string.string_this_video_supports_only_download))
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                finish()
                            }
                            .show()
                    }
                    Toast.makeText(this@PlayMediaActivity, error.message, Toast.LENGTH_LONG).show()
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                    updateProgressText()
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    updateProgressText()
                }
            })

            player.setMediaSource(mediaFactory.createMediaSource(mediaItem))
            player.prepare()
            player.playWhenReady = false
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnPlayPause.setOnClickListener {
            if (isPlay) {
                pauseAudio()
            } else {
                if (isPause) {
                    resumeAudio()
                } else {
                    playAudio()
                }
                if (isFill) {
                    binding.btnPlayPause.setBackgroundResource(R.drawable.ic_pause_fill)
                } else {
                    binding.btnPlayPause.setBackgroundResource(R.drawable.ic_pause)
                }
            }
        }

        binding.sbSoundWave.setCustomThumbDrawable(R.drawable.ic_thumb)

        binding.sbSoundWave.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
//                preferenceHelper.setInt(INT_BASS_BOOST, slider.value.toInt() * 10)

                val duration = player.duration ?: 0

                val videoPosition = (slider.value.toInt().toFloat() / 100.toFloat()) * duration
                player.seekTo(videoPosition.toLong())

            }
        })

        binding.btnNext15s.setOnClickListener {
            binding.playerView.player?.let {
                val currentPosition = it.currentPosition + 15000
                it.seekTo(currentPosition.coerceIn(0, it.duration))
            }
        }

        binding.btnPrevious15s.setOnClickListener {
            binding.playerView.player?.let {
                val currentPosition = it.currentPosition - 15000
                it.seekTo(currentPosition.coerceIn(0, it.duration))
            }
        }

        binding.tvSpeed.setOnClickListener {
            currentIndexSpeed = (currentIndexSpeed + 1) % speedValues.size
            binding.tvSpeed.text = "${speedValues[currentIndexSpeed]}x"
            player.setPlaybackSpeed(speedValues[currentIndexSpeed])
            preferenceHelper.setSpeedMedia(speedValues[currentIndexSpeed])
        }

        binding.btnLoop.setOnClickListener {
            isLoop = !isLoop
            updateStatusLoop(isLoop)
        }

        binding.btnMore.setOnClickListener {
            showBottomSheetDetail(intent.getSerializableExtra(VIDEO_ITEM) as VideoTaskItem)
        }

        binding.btnResize.setOnClickListener {
            isFill = !isFill
            updateFillMedia(isFill)
//
//            if (isFill) {
//                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
//            } else {
//                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
//            }

            requestedOrientation =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

        }

        binding.btnVolume.setOnClickListener {

            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            val balloon: Balloon = Balloon.Builder(binding.root.context)
                .setLayout(R.layout.dialog_volume)
                .setArrowSize(0)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setBackgroundColor(Color.TRANSPARENT)
                .build()

            balloon.showAlignTop(binding.btnVolume, 0, 5)

            val tvVolume: TextView = balloon.getContentView().findViewById(R.id.tv_volume)

            val sbVolume: SeekBar = balloon.getContentView().findViewById(R.id.volumeSeekBar)

            tvVolume.text = currentVolume.toString()
            sbVolume.max = maxVolume
            sbVolume.progress = currentVolume

            sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        // Thay đổi âm lượng
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                        tvVolume.text = "$progress"
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // Không cần xử lý
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // Không cần xử lý
                }
            })

        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Khi xoay ngang, mở rộng BottomSheet full màn hình
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            bottomSheetDetailDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetDetailDialog.behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
        }
    }

    private fun showBottomSheetDetail(videoTaskItem: VideoTaskItem) {
        detailLayoutBinding = LayoutBottomSheetDetailBinding.inflate(layoutInflater)

        bottomSheetDetailDialog.setContentView(detailLayoutBinding.root)

        bottomSheetDetailDialog.setCanceledOnTouchOutside(true);

        val behavior = bottomSheetDetailDialog.behavior

        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
//                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Xử lý khi bottom sheet được trượt
            }
        }

        SystemUtil.setLocale(this@PlayMediaActivity)

        bottomSheetDetailDialog.behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels

        bottomSheetDetailDialog.behavior.addBottomSheetCallback(bottomSheetCallback)

        if (videoTaskItem.mimeType == "video") {
            detailLayoutBinding.tvTitleDuration.visibility = View.VISIBLE
            detailLayoutBinding.tvDuration.visibility = View.VISIBLE
        } else {
            detailLayoutBinding.tvTitleDuration.visibility = View.GONE
            detailLayoutBinding.tvDuration.visibility = View.GONE
        }

        val fileSize = formatFileSize(videoTaskItem.fileSize)
        val lastModified = Date(videoTaskItem.fileDate)

        detailLayoutBinding.tvFileName.text = videoTaskItem.fileName
        detailLayoutBinding.tvFileSize.text = fileSize
        detailLayoutBinding.tvDate.text =
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(lastModified)
        detailLayoutBinding.tvDuration.text = getVideoDuration(videoTaskItem.fileDuration)

        detailLayoutBinding.tvPath.text = videoTaskItem.filePath

        bottomSheetDetailDialog.show()
    }

    private fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var fileSize = size.toDouble()
        var index = 0

        while (fileSize > 1024 && index < units.size - 1) {
            fileSize /= 1024
            index++
        }

        return String.format("%.2f %s", fileSize, units[index])
    }

    private fun getVideoDuration(duration: Long): String {
        return try {

            val minutes = duration / 1000 / 60
            val seconds = (duration / 1000) % 60
            String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            "00:00"
        }
    }


    private fun createRenderFactory(): RenderersFactory {
        return DefaultRenderersFactory(this.applicationContext)
            .setExtensionRendererMode(EXTENSION_RENDERER_MODE_PREFER)
            .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                var decoderInfos =
                    MediaCodecSelector.DEFAULT
                        .getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                if (MimeTypes.VIDEO_H264 == mimeType) {
                    // copy the list because MediaCodecSelector.DEFAULT returns an unmodifiable list
                    decoderInfos = ArrayList(decoderInfos)
                    decoderInfos.reverse()
                }
                decoderInfos
            }
    }

    private fun createMediaFactory(
        headers: Map<String, String>,
        isUrl: Boolean
    ): DefaultMediaSourceFactory {
        if (isUrl) {
            if (headers.isEmpty()) {
                val factory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setKeepPostFor302Redirects(true)
                return DefaultMediaSourceFactory(this).setDataSourceFactory(
                    factory
                )
            }
            val fixedHeaders = headers.toMutableMap()

            val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true)
                .setUserAgent(headers["User-Agent"])
                .setDefaultRequestProperties(fixedHeaders)

            return DefaultMediaSourceFactory(this).setDataSourceFactory(
                dataSourceFactory
            )
        } else {
            val dataSourceFactory = DefaultDataSource.Factory(this)

            return DefaultMediaSourceFactory(this).setDataSourceFactory(
                dataSourceFactory
            )
        }

    }

    private fun setCurrentPositionSpeed(speed: Float): Int {
        return when (speed) {
            0.25f -> 1
            0.5f -> 2
            0.75f -> 3
            1.0f -> 4
            1.25f -> 5
            1.5f -> 6
            1.75f -> 7
            else -> 4
        }
    }

    private fun updateStatusLoop(isLoop: Boolean) {

        if (isFill) {
            if (isLoop) {
                binding.btnLoop.setBackgroundResource(R.drawable.ic_enable_loop)
            } else {
                binding.btnLoop.setBackgroundResource(R.drawable.ic_disable_loop_white)
            }
        } else {
            if (isLoop) {
                binding.btnLoop.setBackgroundResource(R.drawable.ic_enable_loop)
            } else {
                binding.btnLoop.setBackgroundResource(R.drawable.ic_disable_loop)
            }
        }

        preferenceHelper.setIsLoopMedia(isLoop)
    }

    private fun updateFillMedia(isFill: Boolean) {
//        if (isFill) {
//            binding.btnResize.setBackgroundResource(R.drawable.ic_fill)
//        } else {
//            binding.btnResize.setBackgroundResource(R.drawable.ic_fit)
//        }

        preferenceHelper.setIsFillMedia(isFill)
    }

    private fun resumeAudio() {
        player.apply {
            play()
            seekTo(curPosVideo)
        }
        isPause = false
        isPlay = true
    }

    private fun playAudio() {
        player.play()

        startUpdatingUI()

        isPlay = true
        isPause = false
    }

    private fun updateProgressText() {
        val player = binding.playerView.player
        val duration = player?.duration ?: 0
        val currentPosition = player?.currentPosition ?: 0

        binding.tvTimeRunning.text = formatTime(currentPosition)
        binding.tvDuration.text = formatTime(duration)

    }

    private fun formatTime(timeMs: Long): String {
        val seconds = (timeMs / 1000).toInt() % 60
        val minutes = (timeMs / (1000 * 60)).toInt() % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun pauseAudio() {
        if (isFill) {
            binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play_media_fill)
        } else {
            binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play_media)
        }
        binding.playerView.onPause()

        isPause = true
        isPlay = false

        player.let {
            it.pause()
            it.currentPosition.let { curPos -> curPosVideo = curPos }
        }
    }

    private fun stopMediaPlayer() {
        binding.playerView.player?.apply {
            stop()
            release()
            stopUpdatingUI()

            isPlay = false
            isPause = false

            if (isFill) {
                binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play_media_fill)
            } else {
                binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play_media)
            }
        }
    }

    private val updateProgressTextRunnable = object : Runnable {
        override fun run() {
            binding.playerView.player?.let {
                val currentPosition = it.currentPosition
                binding.tvTimeRunning.text = formatTime(currentPosition.toLong())

                val duration = player.duration ?: 0

                val progress = (currentPosition.toFloat() / duration.toFloat() * 100).toInt()

                binding.sbSoundWave.value = progress.toFloat()
            }

            handlerUI.postDelayed(this, 500)
        }
    }

    private fun startUpdatingUI() {
        handlerUI.post(updateProgressTextRunnable)
    }

    private fun stopUpdatingUI() {
        handlerUI.removeCallbacks(updateProgressTextRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMediaPlayer()
    }
}