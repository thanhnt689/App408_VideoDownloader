package com.files.video.downloader.videoplayerdownloader.downloader.data.network.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AdHost")
data class AdHost constructor(
    @PrimaryKey
    var host: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AdHost

        return host == other.host
    }

    override fun hashCode(): Int {
        return host.hashCode()
    }
}
