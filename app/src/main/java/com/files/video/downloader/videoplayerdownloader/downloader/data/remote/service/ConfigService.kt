package com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service

import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.SupportedPage
import io.reactivex.rxjava3.core.Flowable
import retrofit2.http.GET

interface ConfigService {

    @GET("supported_pages.json")
    fun getSupportedPages(): Flowable<List<SupportedPage>>
}