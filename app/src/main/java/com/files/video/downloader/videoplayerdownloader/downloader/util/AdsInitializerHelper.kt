package com.files.video.downloader.videoplayerdownloader.downloader.util

import android.widget.Toast
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.AdBlockHostsRepository
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit

class AdsInitializerHelper {
    companion object {
        private const val ADS_LIST_UPDATE_TIME_DAYS = 7

        fun initializeAdBlocker(
            adBlockHostsRepository: AdBlockHostsRepository,
            sharedPrefHelper: PreferenceHelper,
            lifecycleScope: CoroutineScope
        ) {
            var handle: DisposableHandle? = null
            handle = lifecycleScope.launch(Dispatchers.IO) {
                val isAdBlockerOn = sharedPrefHelper.getIsAdBlocker()
                if (isAdBlockerOn) {
                    val cachedCount = adBlockHostsRepository.getCachedCount()
                    if (cachedCount > 0) {
                        return@launch
                    }
                    val lastUpdateTime = sharedPrefHelper.getAdHostsUpdateTime()
                    val currentTime = System.currentTimeMillis()
                    val differenceInMillis = currentTime - lastUpdateTime
                    val daysDifference = TimeUnit.MILLISECONDS.toDays(differenceInMillis)
                    val isOutdated = daysDifference > ADS_LIST_UPDATE_TIME_DAYS

                    var isUpdated = false
                    val isInitialized: Boolean

                    if (isOutdated) {
                        AppLogger.d("HOST LIST OUTDATED, UPDATING...")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                ContextUtils.getApplicationContext(),
                                "AdBlock hosts lists start updating...",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        isInitialized = adBlockHostsRepository.initialize(true)
                        if (isInitialized) {
                            sharedPrefHelper.setIsAdHostsUpdateTime(Date().time)
                            isUpdated = true
                            AppLogger.d("HOST LISTS UPDATED DONE, TIME: ${Date()}")
                        } else {
                            AppLogger.d("HOST LISTS UPDATED FAIL, TIME: ${Date()}")
                            isUpdated = false
                        }
                    } else {
                        isInitialized = adBlockHostsRepository.initialize(false)

                        AppLogger.d("HOST LISTS INITIALIZED DONE, TIME: ${Date()}")
                    }

                    withContext(Dispatchers.Main) {
                        when {
                            // Means initialized and updated
                            isInitialized && !isUpdated -> {
                                Toast.makeText(
                                    ContextUtils.getApplicationContext(),
                                    "AdBlock hosts lists initialized",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            // Initialization Failed
                            !isInitialized -> {
                                Toast.makeText(
                                    ContextUtils.getApplicationContext(),
                                    "AdBlock hosts lists initialized failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            // Means initialized only
                            else -> {
                                Toast.makeText(
                                    ContextUtils.getApplicationContext(),
                                    "AdBlock hosts lists initialized and updated",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }.invokeOnCompletion {
                handle?.dispose()
            }
        }
    }
}