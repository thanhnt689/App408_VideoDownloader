package com.files.video.downloader.videoplayerdownloader.downloader.extensions

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import java.util.Locale
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local

fun Context.hasNetworkConnection(): Boolean {
    var haveConnectedWifi = false
    var haveConnectedMobile = false
    val cm =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val netInfo = cm.allNetworkInfo
    for (ni in netInfo) {
        if (ni.typeName.equals("WIFI", ignoreCase = true))
            if (ni.isConnected) haveConnectedWifi = true
        if (ni.typeName.equals("MOBILE", ignoreCase = true))
            if (ni.isConnected) haveConnectedMobile = true
    }
    return haveConnectedWifi || haveConnectedMobile
}

fun Context.setLocale(language: String?) {
    val configuration = resources.configuration
    val locale = if (language.isNullOrEmpty()) {
        Locale.getDefault()
    } else {
        Locale(language)
    }
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    createConfigurationContext(configuration)
}