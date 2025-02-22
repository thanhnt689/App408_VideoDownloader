package com.files.video.downloader.videoplayerdownloader.downloader.ui.tab

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ItemTabsBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTab

class TabsAdapter(
    private var context: Context,
    private var listTabs: ArrayList<TabModel>,
    private var onClickItemTab: ((tabModel: TabModel, position: Int) -> Unit),
    private var onClickDeleteItemTab: ((tabModel: TabModel, position: Int) -> Unit)
) :
    RecyclerView.Adapter<TabsAdapter.ViewHolder>() {

    var currentPosTab = -1

    inner class ViewHolder(var binding: ItemTabsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tabModel: TabModel, position: Int) {
            binding.tvLinkWeb.text = tabModel.url.toString()
            Glide.with(binding.imgIcon).load(tabModel.faviconBitmap()).into(binding.imgIcon)
            Glide.with(binding.imgWallpaper).load(tabModel.faviconBitmap()).into(binding.imgWallpaper)

            binding.tvLinkWeb.isSelected = true

            if (tabModel.isSelected) {
                binding.cardView.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.color_select)
                binding.cv.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.color_bg_select)

            } else {
                binding.cardView.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.color_normal)
                binding.cv.backgroundTintList =
                    ContextCompat.getColorStateList(context, R.color.color_normal)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemTabsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return listTabs.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listTabs[position], position)

        holder.binding.root.setOnClickListener {
            onClickItemTab.invoke(listTabs[position], position)
        }

        holder.binding.imgClose.setOnClickListener {
            onClickDeleteItemTab.invoke(listTabs[position], position)
        }
    }

    fun setPositionCurrentTab(position: Int) {
        currentPosTab = position
    }
}