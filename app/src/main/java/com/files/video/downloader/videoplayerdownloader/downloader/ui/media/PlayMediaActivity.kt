package com.files.video.downloader.videoplayerdownloader.downloader.ui.media

import android.graphics.Color
import android.media.AudioManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
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
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPlayMediaBinding
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
import com.google.android.material.slider.Slider
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.net.CookiePolicy
import java.net.HttpCookie
import java.net.URI
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlayMediaActivity : BaseActivity<ActivityPlayMediaBinding>() {

    companion object {
        var DEFAULT_COOKIE_MANAGER: java.net.CookieManager? = null

        const val VIDEO_URL = "video_url"
        const val VIDEO_HEADERS = "video_headers"
        const val VIDEO_NAME = "video_name"
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

    private var isFill = true

    private var speed = 1.0f

    private val speedValues = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    private var currentIndexSpeed = 0

    private lateinit var audioManager: AudioManager

    override fun setBinding(layoutInflater: LayoutInflater): ActivityPlayMediaBinding {
        return ActivityPlayMediaBinding.inflate(layoutInflater)
    }

    override fun initView() {
        playMediaViewModel.start()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
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

        Log.d("ntt", "initView: VIDEO_NAME: ${intent.getStringExtra(VIDEO_NAME)}")

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

        isFill = preferenceHelper.getIsFillMedia()
        updateFillMedia(isFill)

        playMediaViewModel.videoName.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val name = playMediaViewModel.videoName.get()
                binding.tvName.text = name.toString()
            }
        })


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

        player.setMediaItem(mediaItem)

        player.prepare()

        player.setMediaSource(mediaFactory.createMediaSource(mediaItem))

        player.playWhenReady = false

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
                binding.btnPlayPause.setBackgroundResource(R.drawable.ic_pause)
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

        binding.btnResize.setOnClickListener {
            isFill = !isFill
            updateFillMedia(isFill)

            if (isFill) {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            } else {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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
        if (isLoop) {
            binding.btnLoop.setBackgroundResource(R.drawable.ic_enable_loop)
        } else {
            binding.btnLoop.setBackgroundResource(R.drawable.ic_disable_loop)
        }

        preferenceHelper.setIsLoopMedia(isLoop)
    }

    private fun updateFillMedia(isFill: Boolean) {
        if (isFill) {
            binding.btnResize.setBackgroundResource(R.drawable.ic_fill)
        } else {
            binding.btnResize.setBackgroundResource(R.drawable.ic_fit)
        }

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
        binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play_media)
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

            binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play_media)
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