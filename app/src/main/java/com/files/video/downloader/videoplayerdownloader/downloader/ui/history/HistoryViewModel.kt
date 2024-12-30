package com.files.video.downloader.videoplayerdownloader.downloader.ui.history

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepositoryImpl,
) :
    ViewModel() {

    val searchCharObservable: MutableLiveData<String> = MutableLiveData("")

//    var searchHistoryItems = ObservableField<List<HistoryItem>>(emptyList())
//
//    val searchQuery = ObservableField("")
//
//    val isLoadingHistory = ObservableField(true)
//
//    val executorSingleHistory = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
//
//    private val historyExecutor = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
//
//    private val additionalExecutor = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

//    override fun start() {
//        fetchAllHistory()
//    }
//
//    override fun stop() {
//    }

//    private fun fetchAllHistory() {
//        isLoadingHistory.set(true)
//
//        viewModelScope.launch(additionalExecutor) {
//            val history = historyRepository.getAllHistory()
//            _historyItem.postValue(history)
//            isLoadingHistory.set(false)
//        }
//    }

    suspend fun queryHistoryFile(): LiveData<List<HistoryItem>> {
        Log.d("ntt", "queryFile: ")
        return searchCharObservable.switchMap { query ->
            historyRepository.queryHistoryItem(
                query
            )
        }
    }


    suspend fun queryBookmarkFile(): LiveData<List<HistoryItem>> {
        Log.d("ntt", "queryFile: ")
        return searchCharObservable.switchMap { query ->
            historyRepository.queryBookmarkItem(
                query
            )
        }
    }

    fun saveHistory(historyItem: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyRepository.saveHistory(historyItem)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun deleteHistory(historyItem: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyRepository.deleteHistory(historyItem)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

//    fun queryHistory(query: String) {
//        if (query.isEmpty()) {
//            searchHistoryItems.set(emptyList())
//        }
//        if (query.isNotEmpty()) {
//            val filtered = historyItems.get()
//                ?.filter { it.url.contains(query) || it.title?.contains(query) ?: false }
//            searchHistoryItems.set(filtered ?: emptyList())
//        }
//    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteAllHistory()
        }
    }
}