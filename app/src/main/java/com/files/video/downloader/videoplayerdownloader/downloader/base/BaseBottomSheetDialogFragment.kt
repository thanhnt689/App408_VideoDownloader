package com.files.video.downloader.videoplayerdownloader.downloader.base

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import javax.inject.Inject

/**
 * [BaseDialogFragment]
 */
abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }
}