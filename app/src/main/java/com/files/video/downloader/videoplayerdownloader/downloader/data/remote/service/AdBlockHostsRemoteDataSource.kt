package com.files.video.downloader.videoplayerdownloader.downloader.data.remote.service

import android.net.Uri
import android.util.Log
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.data.dao.AdHostDao
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.AdHost
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdBlockerHelper.parseAdsLine
import com.files.video.downloader.videoplayerdownloader.downloader.util.ContextUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.proxy_utils.OkHttpProxyClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import kotlin.math.log

const val AD_HOSTS_URL_LIST_ADAWAY = "https://adaway.org/hosts.txt"
const val AD_HOSTS_URLS_LIST_ADMIRAL = "https://v.firebog.net/hosts/Admiral.txt"
const val TRACKING_BLACK_LIST = "https://v.firebog.net/hosts/Easyprivacy.txt"
const val AD_HOSTS_URLS_AD_GUARD = "https://v.firebog.net/hosts/AdguardDNS.txt"

class AdBlockHostsRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpProxyClient,
    private val sharedPrefHelper: PreferenceHelper,
    private val adHostDao: AdHostDao,
) {

    private val hostsCache = mutableSetOf<AdHost>()

    suspend fun initialize(isUpdate: Boolean): Boolean {
        val isPopulated = sharedPrefHelper.getIsPopulated()

        if (isUpdate) {
            val freshHosts = fetchHosts()

            val freshHostLocal = fetchHostsLocal()

            if (freshHosts.isNotEmpty()) {
                adHostDao.insertAdHosts(freshHosts)
            }

            if (freshHostLocal.isNotEmpty()) {
                adHostDao.insertAdHosts(freshHostLocal)
            }

            val remoteInitialized = freshHosts.isNotEmpty()

            val localInitialized = freshHostLocal.isNotEmpty()

            if (!isPopulated && !remoteInitialized && !localInitialized) {

                initializeLocal(true)

                return false
            }

            return remoteInitialized && localInitialized
        } else {
            return initializeLocal(false)
        }
    }

    private suspend fun initializeLocal(isUpdate: Boolean): Boolean {
        return fetchHostsLocal().isNotEmpty()
    }

    private suspend fun fetchHosts(): Set<AdHost> {
        val tasks = listOf(
            fetchListFromUrl(AD_HOSTS_URLS_AD_GUARD).catch { emit(emptySet()) }.cancellable(),
            fetchListFromUrl(AD_HOSTS_URL_LIST_ADAWAY).catch { emit(emptySet()) }.cancellable(),
            fetchListFromUrl(AD_HOSTS_URLS_LIST_ADMIRAL).catch { emit(emptySet()) }.cancellable(),
            fetchListFromUrl(TRACKING_BLACK_LIST).catch { emit(emptySet()) }.cancellable()
        )
        val result = mutableSetOf<AdHost>()
        merge(tasks[0], tasks[1], tasks[2], tasks[3]).cancellable().onEach { hosts ->
            result.addAll(hosts)
        }.collect()

        return result.toSet()
    }

    private suspend fun fetchHostsLocal(): Set<AdHost> {
        val isPopulated = sharedPrefHelper.getIsPopulated()

        if (isPopulated) {
            hostsCache.addAll(adHostDao.getAdHosts())
        } else {
            var counter = 0

            fetchHostsFromFiles().onEach { adHosts ->
                counter += adHosts.size
                adHostDao.insertAdHosts(adHosts)
            }.onCompletion {
                if (it == null && counter > 80000) {
                    sharedPrefHelper.setIsPopulated(true)
                }
            }.collect()

            hostsCache.addAll(adHostDao.getAdHosts())
        }

        return hostsCache
    }

    suspend fun setListHost() {
        hostsCache.addAll(adHostDao.getAdHosts())
    }

    fun isAds(url: String): Boolean {

//        val hostCacheDb = adHostDao.getAdHosts()

        val uri = try {
            Uri.parse(url)
        } catch (_: Throwable) {
            null
        }
        val host = uri?.host.toString()
            .replace("www.", "")
            .replace("m.", "")
            .trim()
        if (host.isNotEmpty()) {
            return hostsCache.contains(AdHost(host))
        }

        return false
    }

    private suspend fun fetchListFromUrl(url: String): Flow<Set<AdHost>> {
        return flow {
            val response = try {
                okHttpClient.getProxyOkHttpClient().newCall(Request.Builder().url(url).build())
                    .execute()
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            }
            response?.body?.byteStream().use { responseBytesStream ->
                responseBytesStream?.let {
                    emit(readAdServersFromStream(it))
                }
            }
        }
    }

    private suspend fun fetchHostsFromFiles(): Flow<Set<AdHost>> {
        val tasks = listOf(
            fetchHostsFromFileRaw(R.raw.adblockserverlist).catch { emit(emptySet()) },
            fetchHostsFromFileRaw(R.raw.adblockserverlist2).catch { emit(emptySet()) },
            fetchHostsFromFileRaw(R.raw.adblockserverlist3).catch { emit(emptySet()) }
        )

        return merge(tasks[0], tasks[1], tasks[2])
    }

    private suspend fun fetchHostsFromFileRaw(resource: Int): Flow<Set<AdHost>> {
        return flow {
            val inputStream =
                ContextUtils.getApplicationContext().resources.openRawResource(resource)

            emit(readAdServersFromStream(inputStream))
        }
    }

    private suspend fun readAdServersFromStream(inputStream: InputStream): Set<AdHost> {
        val br2 = BufferedReader(InputStreamReader(inputStream))
        val result = mutableSetOf<AdHost>()

        try {
            var line2: String?

            yield()

            while (withContext(Dispatchers.IO) {
                    br2.readLine()
                }.also { line2 = it } != null) {
                if (!line2.toString().startsWith("#")) {
                    val parsedLine = parseAdsLine(line2)
                    if (parsedLine.contains(Regex(".+\\..+"))) {
                        result.add(AdHost(parsedLine))
                    }
                }
            }
            yield()
        } catch (e: IOException) {
            yield()

            e.printStackTrace()
        } finally {
            yield()

            withContext(Dispatchers.IO) {
                br2.close()
            }
        }

        return result
    }

    fun getCachedCount(): Int {
        return hostsCache.size
    }


}
