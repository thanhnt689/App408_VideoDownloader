package com.files.video.downloader.videoplayerdownloader.downloader.util

import android.util.Log
import com.files.video.downloader.videoplayerdownloader.downloader.BuildConfig

class AppLogger {

    companion object {

        private const val TAG = "ntt"

        fun d(message: String) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, message)
            }
        }

        fun i(message: String) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, message)
            }
        }

        fun w(message: String) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, message)
            }
        }

        fun e(message: String) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, message)
            }
        }
    }
}