package com.files.video.downloader.videoplayerdownloader.downloader.ui.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class IntroFragment : Fragment() {
    private var layoutId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            layoutId = it.getInt(LAYOUT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(layoutId ?: 0, container, false)
    }

    companion object {
        const val LAYOUT_ID = "layout_id"

        @JvmStatic
        fun newInstance(layoutId: Int) =
            IntroFragment().apply {
                arguments = Bundle().apply {
                    putInt(LAYOUT_ID, layoutId)
                }
            }
    }
}