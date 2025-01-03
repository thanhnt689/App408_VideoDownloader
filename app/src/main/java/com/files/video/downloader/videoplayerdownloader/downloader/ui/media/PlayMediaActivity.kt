package com.files.video.downloader.videoplayerdownloader.downloader.ui.media

import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPlayMediaBinding
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppUtil
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
    lateinit var appUtil: AppUtil

    private lateinit var player: ExoPlayer

    private var isPlay = false

    private var isPause = false

    private var curPosVideo = 0L

    private val handlerUI = Handler()

    override fun setBinding(layoutInflater: LayoutInflater): ActivityPlayMediaBinding {
        return ActivityPlayMediaBinding.inflate(layoutInflater)
    }

    override fun initView() {
        playMediaViewModel.start()
        intent.getStringExtra(VIDEO_HEADERS)?.let { rawHeaders ->
            val headers =
                Json.parseToJsonElement(rawHeaders).jsonObject.toMap()
                    .map { it.key to it.value.toString() }
                    .toMap()
            playMediaViewModel.videoHeaders.set(headers)
        }
        intent.getStringExtra(VIDEO_NAME)?.let { playMediaViewModel.videoName.set(it) }

        Log.d("ntt", "initView: VIDEO_NAME: ${intent.getStringExtra(VIDEO_NAME)}")

        val iUrl = Uri.parse(intent.getStringExtra(VIDEO_URL))

        if (iUrl != null) {
            playMediaViewModel.videoUrl.set(iUrl)
        }

        val url = playMediaViewModel.videoUrl.get() ?: Uri.EMPTY
        val headers = playMediaViewModel.videoHeaders.get() ?: emptyMap()

        playMediaViewModel.videoName.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val name = playMediaViewModel.videoName
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

        binding.playerView.player = player
        binding.playerView.setShowBuffering(SHOW_BUFFERING_ALWAYS)
        binding.playerView.setFullscreenButtonClickListener {
            if (it) {
                binding.toolbar.visibility = View.GONE
            } else {
                binding.toolbar.visibility = View.VISIBLE
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == PlaybackState.STATE_PLAYING) {
                    binding.pbLoading.visibility = View.GONE
                } else {
                    binding.pbLoading.visibility = View.VISIBLE
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (playMediaViewModel?.videoUrl?.get().toString().startsWith("http")) {
                    AlertDialog.Builder(this@PlayMediaActivity)
                        .setTitle("Download Only")
                        .setMessage("This video supports only download.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                Toast.makeText(this@PlayMediaActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })

        player.setMediaSource(mediaFactory.createMediaSource(mediaItem))
        player.prepare()
        player.playWhenReady = true

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
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }

        binding.sbSoundWave.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                if (fromUser) {

                    val duration = player.duration ?: 0

                    val videoPosition = (progress.toFloat() / 100.toFloat()) * duration
                    player.seekTo(videoPosition.toLong())

                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
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
        binding.btnPlayPause.setImageResource(R.drawable.ic_play_media)
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

            binding.btnPlayPause.setImageResource(R.drawable.ic_play_media)
        }
    }

    private val updateProgressTextRunnable = object : Runnable {
        override fun run() {
            binding.playerView.player?.let {
                val currentPosition = it.currentPosition
                binding.tvTimeRunning.text = formatTime(currentPosition.toLong())

                val duration = player.duration ?: 0

                val progress = (currentPosition.toFloat() / duration.toFloat() * 100).toInt()

                binding.sbSoundWave.progress = progress
            }

            handlerUI.postDelayed(this, 1000)
        }
    }

    private fun startUpdatingUI() {
        handlerUI.post(updateProgressTextRunnable)
    }

    private fun stopUpdatingUI() {
        handlerUI.removeCallbacks(updateProgressTextRunnable)
    }
}