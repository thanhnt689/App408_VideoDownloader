package com.files.video.downloader.videoplayerdownloader.downloader.ui.language

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ItemLanguageBinding
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class LanguageAdapter @Inject constructor() :
    ListAdapter<LanguageModel, LanguageAdapter.LanguageViewHolder>(LanguageDiffCallback()) {

    private var isLoad = true

    private var isSplash = true

    private var onClickListener: IClickItemLanguage? = null

    fun setOnClickListener(onClickListener: IClickItemLanguage) {
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding =
            ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }


    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bindData(getItem(position))
    }

    fun getSelectedLanguage(): LanguageModel? {
        return currentList.firstOrNull { it.active }
    }

    inner class LanguageViewHolder(val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(languageModel: LanguageModel) {
            binding.txtName.text = languageModel.name

            when (languageModel.code) {
                "en" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co01)
                        .into(binding.icLang)
                }

                "zh" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co02)
                        .into(binding.icLang)
                }

                "zh-TW" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co03)
                        .into(binding.icLang)
                }

                "hi" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co04)
                        .into(binding.icLang)
                }

                "es" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co05)
                        .into(binding.icLang)
                }

                "pt-BR" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co06)
                        .into(binding.icLang)
                }

                "pt" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co07)
                        .into(binding.icLang)
                }

                "fr" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co08)
                        .into(binding.icLang)
                }

                "ar" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co09)
                        .into(binding.icLang)
                }

                "bn" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co10)
                        .into(binding.icLang)
                }

                "ru" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co11)
                        .into(binding.icLang)
                }

                "de" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co12)
                        .into(binding.icLang)
                }

                "ja" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co13)
                        .into(binding.icLang)
                }

                "tr" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co14)
                        .into(binding.icLang)
                }

                "ko" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co15)
                        .into(binding.icLang)
                }

                "in" -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co16)
                        .into(binding.icLang)
                }
                else -> {
                    Glide.with(binding.root.context).asBitmap()
                        .load(R.drawable.co01)
                        .into(binding.icLang)
                }
            }


            if (languageModel.active) {
                binding.ivSelect.setImageResource(R.drawable.ic_dot_selected)
                binding.layoutItem.background =
                    ContextCompat.getDrawable(binding.root.context, R.drawable.bg_item_turnon_stroke)
            } else {
                binding.ivSelect.setImageResource(R.drawable.ic_dot_normal)
                binding.layoutItem.background = ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.bg_item_turnon
                )
            }

            if (position == 3 && isLoad && isSplash) {
                binding.lottieViewTab.visibility = View.VISIBLE
            } else {
                binding.lottieViewTab.visibility = View.INVISIBLE
            }

            binding.layoutItem.setOnClickListener {
                isLoad = false
                resetChecked()
                languageModel.active = true
                onClickListener?.onClickItemLanguage()
                notifyDataSetChanged()
            }
        }

        private fun resetChecked() {
            currentList.forEach { it.active = false }
        }

    }

    fun setIsSplash(isSp: Boolean) {
        isSplash = isSp
    }
}



class LanguageDiffCallback : DiffUtil.ItemCallback<LanguageModel>() {
    override fun areItemsTheSame(oldItem: LanguageModel, newItem: LanguageModel): Boolean {
        return oldItem.code == newItem.code
    }

    override fun areContentsTheSame(oldItem: LanguageModel, newItem: LanguageModel): Boolean {
        return oldItem == newItem
    }


}