package com.files.video.downloader.videoplayerdownloader.downloader.ui.intro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.files.video.downloader.videoplayerdownloader.downloader.R

class IntroAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    private val layout =
        arrayOf(
            R.layout.layout_intro1,
            R.layout.layout_intro2,
            R.layout.layout_intro3,
            R.layout.layout_intro4
        )


    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return IntroFragment.newInstance(layout[position])
    }
}