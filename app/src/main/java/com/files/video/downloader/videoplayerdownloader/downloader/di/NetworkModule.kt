package com.files.video.downloader.videoplayerdownloader.downloader.di

import android.app.Application
import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.ConfigService
import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.VideoService
import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.VideoServiceLocal
import com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service.YoutubedlHelper
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.Memory
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.CustomProxyController
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DATA_URL = "https://some-url.com/youtube-dl/"

    private fun buildOkHttpClient(application: Application): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10L, TimeUnit.SECONDS)
            .writeTimeout(10L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .cache(
                Cache(
                    File(application.cacheDir, "YoutubeDLCache"),
                    Memory.calcCacheSize(application, .25f)
                )
            )
            .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(application: Application): OkHttpClient = buildOkHttpClient(application)

    @Provides
    @Singleton
    fun provideConfigService(okHttpClient: OkHttpClient): ConfigService = Retrofit.Builder()
        .baseUrl(DATA_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .build()
        .create(ConfigService::class.java)

    @Provides
    @Singleton
    fun provideVideoService(
        proxyController: CustomProxyController,
        youtubedlHelper: YoutubedlHelper
    ): VideoService = VideoServiceLocal(
        proxyController,
        youtubedlHelper
    )

    @Provides
    @Singleton
    fun provideYoutubeHelper(
        okHttpProxyClient: OkHttpProxyClient,
        sharedPrefHelper: PreferenceHelper
    ): YoutubedlHelper =
        YoutubedlHelper(okHttpProxyClient, sharedPrefHelper)
}