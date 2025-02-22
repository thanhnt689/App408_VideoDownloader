package com.files.video.downloader.videoplayerdownloader.downloader.ui.tab

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Message
import android.webkit.WebView
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity.HistoryItem
import java.io.Serializable
import java.util.UUID

@Entity(tableName = "TabModel")
data class TabModel(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "_url")
    var url: String,

//    @ColumnInfo(name = "_title")
//    var title: String?,

//    private var iconBytes: Bitmap? = null,
    @ColumnInfo(name = "_is_selected")
    var isSelected: Boolean = true,

    @ColumnInfo(name = "_favicon", typeAffinity = ColumnInfo.BLOB)
    var favicon: ByteArray? = null
) {
    fun faviconBitmap(): Bitmap? {
        if (favicon == null) return null
        return BitmapFactory.decodeByteArray(favicon, 0, favicon?.size ?: 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistoryItem

        if (id != other.id) return false
        if (url != other.url) return false
//        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }
}