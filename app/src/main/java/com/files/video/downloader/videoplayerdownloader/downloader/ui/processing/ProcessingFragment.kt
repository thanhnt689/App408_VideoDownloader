package com.files.video.downloader.videoplayerdownloader.downloader.ui.processing

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.webkit.WebResourceRequest
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.Observable
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseFragment
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.FragmentProcessingBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.ContentType
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.detectedVideos.DetectedVideosTabViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.guide.GuideActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdBlockerHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.AppLogger
import com.files.video.downloader.videoplayerdownloader.downloader.util.CookieUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.VideoUtils
import com.google.logging.type.HttpRequest
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URL

@AndroidEntryPoint
class ProcessingFragment : BaseFragment<FragmentProcessingBinding>(), ProgressListener {

    private val progressViewModel: ProgressViewModel by viewModels()

    private lateinit var progressAdapter: ProcessAdapter

    private val videoDetectionTabViewModel: DetectedVideosTabViewModel by viewModels()

    override fun getViewBinding(): FragmentProcessingBinding {
        return FragmentProcessingBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pbLoading.visibility = View.GONE

        binding.layoutNoData.visibility = View.VISIBLE

        progressViewModel.start()

        progressAdapter = ProcessAdapter(emptyList(), this)

        progressViewModel.progressInfos.addOnPropertyChangedCallback(object :
            Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {

                lifecycleScope.launch(Dispatchers.Main) {

                    progressViewModel.progressInfos.get()?.let {
                        progressAdapter.setData(it)
                    }

                    if (progressViewModel.progressInfos.get().isNullOrEmpty()) {
                        binding.layoutNoData.visibility = View.VISIBLE
                    } else {
                        binding.layoutNoData.visibility = View.GONE
                    }

                    val manager =
                        WrapContentLinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.VERTICAL,
                            false
                        )
                    binding.rcvProcess.layoutManager = manager
                    binding.rcvProcess.adapter = progressAdapter
                }
            }

        })

        binding.imgGuide.setOnClickListener {
            startActivity(GuideActivity.newIntent(requireContext()))
        }

        binding.tvPaste.setOnClickListener {
            val clipboard: ClipboardManager =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.hasMimeType(
                    ClipDescription.MIMETYPE_TEXT_PLAIN
                ) == true
            ) {
                val clipData = clipboard.primaryClip
                val copiedText = clipData?.getItemAt(0)?.text.toString()

                if (copiedText.isNotEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Text from clipboard: $copiedText",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.edtSearch.setText(copiedText)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.string_clipboard_is_empty), Toast.LENGTH_SHORT
                    )
                        .show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.string_no_text_in_clipboard), Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.imgDownload.setOnClickListener {
            val url = binding.edtSearch.text.toString()
            val contentType =
                getContentTypeFromUrl(url)

            val cookies = getCookiesForUrl(url)



        }

    }

    fun getCookiesForUrl(url: String): List<Cookie>? {
        val httpUrl = url.toHttpUrlOrNull() ?: return null

        val cookieJar = object : CookieJar {
            private val cookieStore = mutableMapOf<String, List<Cookie>>()

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                // Lấy cookies đã lưu từ URL
                return cookieStore[url.host] ?: emptyList()
            }

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                // Lưu cookies sau khi gửi yêu cầu
                cookieStore[url.host] = cookies
            }
        }

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()

        val request = Request.Builder()
            .url(httpUrl)
            .build()

        return try {
            // Gửi request
            val response = client.newCall(request).execute()

            // Lấy cookies từ response
            val cookies = cookieJar.loadForRequest(httpUrl)

            // Hoặc lấy từ header "Set-Cookie" nếu có
            response.headers.values("Set-Cookie")
            cookies
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }


    fun getContentTypeFromUrl(url: String): ContentType {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .head()  // Chỉ gửi header request để không tải toàn bộ nội dung
            .build()

        if (url.contains(".js") || url.contains(".css") || url.startsWith("blob")) {
            return ContentType.OTHER
        }

        val response: Response = client.newCall(request).execute()

        val contentTypeStr = response.header("Content-Type")
        var contentType: ContentType = ContentType.OTHER

        when {
            contentTypeStr?.contains("mpegurl") == true -> {
                contentType = ContentType.M3U8
            }

            contentTypeStr?.contains("dash") == true -> {
                contentType = ContentType.MPD
            }

            contentTypeStr?.contains("mp4") == true -> {
                contentType = ContentType.MP4
            }

            contentTypeStr?.contains("application/octet-stream") == true -> {
                val chars = CharArray(7)
                response.body.charStream().read(chars, 0, 7)
                response.body.charStream().close()
                response.body.close()
                val content = chars.toString()
                if (content.startsWith("#EXTM3U")) {
                    contentType = ContentType.M3U8
                } else if (content.contains("<MPD")) {
                    contentType = ContentType.MPD
                }
            }

            else -> {
                contentType = ContentType.OTHER
            }
        }

        return contentType
    }

    override fun onCloseClicked(downloadId: Long, isRegular: Boolean) {
        progressViewModel.cancelDownload(downloadId, true)
    }

    override fun onPlayPauseDownloadClicked(
        view: View,
        downloadId: Long,
        isRegular: Boolean,
        isPlay: Boolean
    ) {
        if (isPlay) {
            progressViewModel.pauseDownload(downloadId)
        } else {
            progressViewModel.resumeDownload(downloadId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressViewModel.stop()
    }
}

class WrapContentLinearLayoutManager : LinearLayoutManager {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            AppLogger.e("meet a IOOBE in RecyclerView")
        }
    }
}