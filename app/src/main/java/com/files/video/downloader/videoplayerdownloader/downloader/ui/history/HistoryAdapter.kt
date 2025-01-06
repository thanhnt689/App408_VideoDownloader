package com.files.video.downloader.videoplayerdownloader.downloader.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.SwipeRevealLayout
import com.chauthai.swipereveallayout.SwipeRevealLayout.SimpleSwipeListener
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ItemHistoryBinding

class HistoryAdapter(
    private var context: Context,
    private var isShowShare: Boolean = false,
    private var listItemHistory: List<HistoryItem>,
    private var onClickItemHistory: (HistoryItem, position: Int) -> Unit,
    private var onClickDeleteItemHistory: (HistoryItem, position: Int) -> Unit,
    private var onClickShareItemHistory: (HistoryItem, position: Int) -> Unit
) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    inner class ViewHolder(var binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(historyItem: HistoryItem) {
            binding.tvContentMain.text = historyItem.title
            binding.tvTitleMain.text = historyItem.url

            binding.tvContent.text = historyItem.title
            binding.tvTitle.text = historyItem.url


            Glide.with(binding.icIconMain).load(historyItem.faviconBitmap())
                .placeholder(R.drawable.ic_placeholder_icon).into(binding.icIconMain)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return listItemHistory.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listItemHistory[position])

        if (isShowShare) {
            holder.binding.btnShare.visibility = View.VISIBLE
        } else {
            holder.binding.btnShare.visibility = View.GONE
        }

        holder.binding.layoutMain.setOnClickListener {
            onClickItemHistory.invoke(listItemHistory[position], position)
        }

        holder.binding.btnDelete.setOnClickListener {
            onClickDeleteItemHistory.invoke(listItemHistory[position], position)
        }

        holder.binding.btnShare.setOnClickListener {
            onClickShareItemHistory.invoke(listItemHistory[position], position)
        }

        holder.binding.swipeLayout.setSwipeListener(object : SimpleSwipeListener() {
            override fun onClosed(view: SwipeRevealLayout?) {
                // Khi Swipe đóng, xoay từ 180f về 0f
//                ObjectAnimator.ofFloat(holder.binding.ivNextMain, "rotation", 180f, 0f).start()
            }

            override fun onOpened(view: SwipeRevealLayout?) {
                // Khi Swipe mở, xoay từ 0f về 180f
//                ObjectAnimator.ofFloat(holder.binding.ivNextMain, "rotation", 0f, 180f).start()
            }

            override fun onSlide(view: SwipeRevealLayout?, slideOffset: Float) {
                // Cập nhật độ xoay dựa trên slideOffset (0 đến 1)
                val rotation = 180 * slideOffset
                holder.binding.ivNextMain.rotation = rotation
            }
        })
    }

}
