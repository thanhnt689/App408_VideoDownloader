package com.files.video.downloader.videoplayerdownloader.downloader.model

import android.net.Uri

data class LocalVideo(
    var id: Long,
    var uri: Uri,
    var name: String
) {

    var size: String = ""

    val thumbnailPath: Uri
        get() = uri

}