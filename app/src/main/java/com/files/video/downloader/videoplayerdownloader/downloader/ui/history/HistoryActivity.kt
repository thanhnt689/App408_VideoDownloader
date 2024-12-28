package com.files.video.downloader.videoplayerdownloader.downloader.ui.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityHistoryBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.DialogConfirmDelete
import com.files.video.downloader.videoplayerdownloader.downloader.ui.permission.PermissionActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.KeyboardUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.ViewUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryActivity : BaseActivity<ActivityHistoryBinding>() {

    private lateinit var historyAdapter: HistoryAdapter

    private val historyViewModel: HistoryViewModel by viewModels()

    val listHistory = mutableListOf<HistoryItem>()

    private var openAct = "history"

    override fun setBinding(layoutInflater: LayoutInflater): ActivityHistoryBinding {
        return ActivityHistoryBinding.inflate(layoutInflater)
    }

    override fun initView() {

        openAct = intent.getStringExtra("openAct").toString()

        when (openAct) {
            "history" -> {

                binding.tvTitle.text = getString(R.string.string_history)
                binding.tvDes.text = getString(R.string.string_go_see_the_sites)
                binding.fabAdd.visibility = View.GONE

                historyViewModel.historyItem.observe(this@HistoryActivity) {
                    if (it.isEmpty()) {
                        binding.layoutNoData.visibility = View.VISIBLE
                        binding.tvClear.visibility = View.GONE
//                binding.imgSearch.visibility = View.GONE
                    } else {
                        binding.layoutNoData.visibility = View.GONE
                        binding.tvClear.visibility = View.VISIBLE
//                binding.imgSearch.visibility = View.VISIBLE
                    }

                    listHistory.clear()
                    listHistory.addAll(it)
                    historyAdapter.notifyDataSetChanged()
                }

                binding.rcvHistory.apply {
                    setHasFixedSize(true)

                    layoutManager = LinearLayoutManager(this@HistoryActivity)  // List layout

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

            "bookmark" -> {
                binding.tvTitle.text = getString(R.string.string_bookmark)
                binding.tvDes.text = getString(R.string.string_you_have_not_added)
                binding.fabAdd.visibility = View.VISIBLE

                historyViewModel.historyItem.observe(this@HistoryActivity) {
                    if (it.isEmpty()) {
                        binding.layoutNoData.visibility = View.VISIBLE
                        binding.tvClear.visibility = View.GONE
//                binding.imgSearch.visibility = View.GONE
                    } else {
                        binding.layoutNoData.visibility = View.GONE
                        binding.tvClear.visibility = View.VISIBLE
//                binding.imgSearch.visibility = View.VISIBLE
                    }

                    listHistory.clear()
                    listHistory.addAll(it)
                    historyAdapter.notifyDataSetChanged()
                }

                binding.rcvHistory.apply {
                    setHasFixedSize(true)

                    layoutManager = LinearLayoutManager(this@HistoryActivity)  // List layout

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

                        }

                    )

                    adapter = historyAdapter

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

    }

    private fun showDialogConfirmDelete(historyItem: HistoryItem) {
        val dialogDelete = DialogConfirmDelete(this) {
            historyViewModel.deleteHistory(historyItem)

        }

        dialogDelete.show()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, HistoryActivity::class.java)
        }
    }
}