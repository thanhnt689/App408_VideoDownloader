package com.files.video.downloader.videoplayerdownloader.downloader.ui.tab

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTab
import com.files.video.downloader.videoplayerdownloader.downloader.util.SingleLiveEvent


class TabViewModel(application: Application) : AndroidViewModel(application) {

    var listTabWeb = MutableLiveData<MutableList<WebTab>>()

    var currentPositionTabWeb = MutableLiveData<Int>(0)

    val openDownloadedVideoEvent = SingleLiveEvent<String>()

    fun addNewTab(tab: WebTab) {
        val currentList = listTabWeb.value ?: mutableListOf()
        Log.d("ntt", "addNewTab: $currentList")
        currentList.add(tab)
        listTabWeb.value = currentList
        currentPositionTabWeb.value = currentList.lastIndex

        Log.d("ntt", "addNewTab listTabWeb.value: ${listTabWeb.value}")
        Log.d("ntt", "addNewTab  currentPositionTabWeb.value: ${ currentPositionTabWeb.value}")
    }

    fun removeTabAt(position: Int) {
        val currentList = listTabWeb.value ?: return
        if (position in currentList.indices) {
            currentList.removeAt(position)
            listTabWeb.value = currentList
            // Cập nhật vị trí tab hiện tại
//            currentPositionTabWeb.value = if (currentList.isEmpty()) null else currentList.lastIndex
            if (position == currentPositionTabWeb.value) {
                currentPositionTabWeb.value = -1
            }
        }
    }

    fun removeTab(tab: WebTab) {
        val currentList = listTabWeb.value ?: return
        currentList.remove(tab)
        listTabWeb.value = currentList
//        currentPositionTabWeb.value = if (currentList.isEmpty()) null else currentList.lastIndex
    }

    fun updateCurrentTab(tab: WebTab) {
        val currentPosition = currentPositionTabWeb.value ?: return
        val currentList = listTabWeb.value ?: return
        if (currentPosition in currentList.indices) {
            currentList[currentPosition] = tab
            listTabWeb.value = currentList
        }
    }

    fun updateCurrentTab(tab: WebTab,position: Int) {
        var currentPosition = position
        val currentList = listTabWeb.value ?: return
        if (currentPosition in currentList.indices) {
            currentList[currentPosition] = tab
            listTabWeb.value = currentList
        }
    }

    fun clearAllTabs() {
        listTabWeb.value = mutableListOf()
    }

    fun updateCurrentTabPosition(position: Int) {
        currentPositionTabWeb.value = position
    }

    fun getTabAt(position: Int): WebTab? {
        val currentList = listTabWeb.value ?: return null
        return if (position in currentList.indices) currentList[position] else null
    }

}