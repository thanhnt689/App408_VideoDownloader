package com.files.video.downloader.videoplayerdownloader.downloader.ui.tab

import android.util.Log
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.HistoryRepositoryImpl
import com.files.video.downloader.videoplayerdownloader.downloader.data.repository.TabModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class TabModelViewModel @Inject constructor(
    private val tabModelRepository: TabModelRepository,
) :
    ViewModel() {

    fun queryAllTabModel(): LiveData<List<TabModel>> {
        return tabModelRepository.getAllTabModel()
    }


    suspend fun insertTabModel(item: TabModel) {
        tabModelRepository.insertTabModel(item)
    }


    fun deleteTabModel(item: TabModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tabModelRepository.deleteTabModel(item)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateInfoTabModel(
        id: String,
        newUrl: String,
        favicon: ByteArray?,
        isSelected: Boolean
    ) {
        tabModelRepository.updateInfoTabModel(id, newUrl, favicon, isSelected)
    }

    suspend fun getSelectedTabModel(): TabModel {
        return tabModelRepository.getSelectedTabModel()
    }

    suspend fun clearAllTabModel() {
        tabModelRepository.clearAllTabModel()
    }

}