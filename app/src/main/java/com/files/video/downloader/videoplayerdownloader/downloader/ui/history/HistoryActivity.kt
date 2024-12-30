package com.files.video.downloader.videoplayerdownloader.downloader.ui.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityHistoryBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogAddBookmark
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogConfirmDelete
import com.files.video.downloader.videoplayerdownloader.downloader.ui.language.LanguageActivity.Companion.FROM_SPLASH
import com.files.video.downloader.videoplayerdownloader.downloader.ui.permission.PermissionActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.FaviconUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.KeyboardUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.ViewUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HistoryActivity : BaseActivity<ActivityHistoryBinding>() {

    private lateinit var historyAdapter: HistoryAdapter

    private val historyViewModel: HistoryViewModel by viewModels()

    val listHistory = mutableListOf<HistoryItem>()

    @Inject
    lateinit var okHttpProxyClient: OkHttpProxyClient

    private var openAct = "history"

    override fun setBinding(layoutInflater: LayoutInflater): ActivityHistoryBinding {
        return ActivityHistoryBinding.inflate(layoutInflater)
    }

    override fun initView() {

        openAct = intent.getStringExtra("open").toString()

        when (openAct) {
            "history" -> {

                binding.tvTitle.text = getString(R.string.string_history)
                binding.tvDes.text = getString(R.string.string_go_see_the_sites)
                binding.fabAdd.visibility = View.GONE

                lifecycleScope.launch {
                    historyViewModel.queryHistoryFile().observe(this@HistoryActivity) {
                        if (it.isEmpty()) {
                            binding.layoutNoData.visibility = View.VISIBLE
                            binding.tvClear.visibility = View.GONE
                            binding.imgSearch.visibility = View.GONE
                        } else {
                            binding.layoutNoData.visibility = View.GONE
                            binding.tvClear.visibility = View.VISIBLE
                            binding.imgSearch.visibility = View.VISIBLE
                        }

                        listHistory.clear()
                        listHistory.addAll(it)

                        binding.rcvHistory.apply {
                            setHasFixedSize(true)

                            layoutManager =
                                LinearLayoutManager(this@HistoryActivity)  // List layout

                            historyAdapter = HistoryAdapter(
                                this@HistoryActivity,
                                false,
                                listHistory,
                                onClickItemHistory = { historyItem, position ->

                                },
                                onClickDeleteItemHistory = { historyItem, position ->
                                    showDialogConfirmDelete(historyItem)
                                },
                                onClickShareItemHistory = { historyItem, position ->

                                }

                            )

                            adapter = historyAdapter

                        }
                    }
                }


            }

            "bookmark" -> {
                binding.tvTitle.text = getString(R.string.string_bookmark)
                binding.tvDes.text = getString(R.string.string_you_have_not_added)
                binding.fabAdd.visibility = View.VISIBLE
                binding.tvClear.visibility = View.GONE

                lifecycleScope.launch {
                    historyViewModel.queryBookmarkFile().observe(this@HistoryActivity) {
                        if (it.isEmpty()) {
                            binding.layoutNoData.visibility = View.VISIBLE
                            binding.imgSearch.visibility = View.GONE
                        } else {
                            binding.layoutNoData.visibility = View.GONE
                            binding.imgSearch.visibility = View.VISIBLE
                        }

                        listHistory.clear()
                        listHistory.addAll(it)

                        binding.rcvHistory.apply {
                            setHasFixedSize(true)

                            layoutManager =
                                LinearLayoutManager(this@HistoryActivity)  // List layout

                            historyAdapter = HistoryAdapter(
                                this@HistoryActivity,
                                true,
                                listHistory,
                                onClickItemHistory = { historyItem, position ->

                                },
                                onClickDeleteItemHistory = { historyItem, position ->
                                    showDialogConfirmDelete(historyItem)
                                },
                                onClickShareItemHistory = { historyItem, position ->
                                    shareUrlWithDescription(
                                        this@HistoryActivity,
                                        historyItem.url,
                                        historyItem.title!!,
                                        historyItem.url
                                    )
                                }

                            )

                            adapter = historyAdapter

                        }
                    }
                }
            }
        }



        binding.tvClear.setOnClickListener {
            historyViewModel.clearHistory()
        }

        binding.imgSearch.setOnClickListener {
            ViewUtils.showView(true, binding.llSearch, 300)
            ViewUtils.hideView(true, binding.layout, 300)
            KeyboardUtils.showSoftKeyboard(this)
            binding.edtSearch.requestFocus()
        }

        binding.imCloseSearch.setOnClickListener {
            KeyboardUtils.hideSoftKeyboard(this)
            ViewUtils.showView(true, binding.layout, 300)
            ViewUtils.hideView(true, binding.llSearch, 300)
            binding.edtSearch.setText("")
        }

        binding.imCleanSearch.setOnClickListener {
            binding.edtSearch.setText("")
        }

        binding.imgBack.setOnClickListener {
            finish()
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {

                historyViewModel.searchCharObservable.postValue(s.toString())
            }
        })

        binding.fabAdd.setOnClickListener {
            showDialogAddBookmark()
        }

    }

    fun shareUrlWithDescription(context: Context, url: String, title: String, description: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title) // Tiêu đề
            putExtra(Intent.EXTRA_TEXT, description) // Nội dung và đường dẫn
        }
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
    }

    private fun showDialogAddBookmark() {
        val dialogAddBookmark = DialogAddBookmark(
            this@HistoryActivity,
        ) { name: String, url: String ->

            lifecycleScope.launch(Dispatchers.IO) {
                val icon = try {
                    FaviconUtils.getEncodedFaviconFromUrl(
                        okHttpProxyClient.getProxyOkHttpClient(), url
                    )
                } catch (e: Throwable) {
                    null
                }

                val outputFavicon = FaviconUtils.bitmapToBytes(icon)

                historyViewModel.saveHistory(
                    HistoryItem(
                        url = url,
                        favicon = outputFavicon,
                        title = name,
                        isBookmark = true
                    )
                )

                withContext(Dispatchers.Main) {

                }
            }

        }

        dialogAddBookmark.show()
    }

    private fun showDialogConfirmDelete(historyItem: HistoryItem) {
        val dialogDelete = DialogConfirmDelete(this) {
            historyViewModel.deleteHistory(historyItem)

        }

        dialogDelete.show()
    }

    companion object {
        fun newIntent(context: Context, open: String = ""): Intent {
            val intent = Intent(context, HistoryActivity::class.java)
            intent.putExtra("open", open)
            return intent
        }
    }
}