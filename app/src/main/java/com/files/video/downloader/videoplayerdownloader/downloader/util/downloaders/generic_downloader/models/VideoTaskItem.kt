package com.files.video.downloader.videoplayerdownloader.downloader.util.downloaders.generic_downloader.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "VideoTaskItem")
data class VideoTaskItem(
    @ColumnInfo(name = "url")
    var url: String = "",

    @ColumnInfo(name = "cover_url")
    var coverUrl: String = "",

    @ColumnInfo(name = "cover_path")
    var coverPath: String = "",

    @ColumnInfo(name = "title")
    var title: String = "",

    @ColumnInfo(name = "group_name")
    var groupName: String = "",

    @ColumnInfo(name = "download_create_time")
    var downloadCreateTime: Long = 0L,

    @ColumnInfo(name = "task_state")
    var taskState: Int = 0,

    @ColumnInfo(name = "mime_type")
    var mimeType: String = "",

    @ColumnInfo(name = "final_url")
    var finalUrl: String = "",

    @ColumnInfo(name = "error_code")
    var errorCode: Int = 0,

    @ColumnInfo(name = "video_type")
    var videoType: Int = 0,

    @ColumnInfo(name = "total_ts")
    var totalTs: Int = 0,

    @ColumnInfo(name = "cur_ts")
    var curTs: Int = 0,

    @ColumnInfo(name = "speed")
    var speed: Float = 0f,

    @ColumnInfo(name = "percent")
    var percent: Float = 0f,

    @ColumnInfo(name = "download_size")
    var downloadSize: Long = 0L,

    @ColumnInfo(name = "total_size")
    var totalSize: Long = 0L,

    @ColumnInfo(name = "file_hash")
    var fileHash: String = "",

    @ColumnInfo(name = "save_dir")
    var saveDir: String = "",

    @ColumnInfo(name = "is_completed")
    var isCompleted: Boolean = false,

    @ColumnInfo(name = "is_in_database")
    var isInDatabase: Boolean = false,

    @ColumnInfo(name = "last_update_time")
    var lastUpdateTime: Long = 0L,

    @ColumnInfo(name = "file_name")
    var fileName: String = "",

    @ColumnInfo(name = "file_path")
    var filePath: String = "",

    @ColumnInfo(name = "is_paused")
    var isPaused: Boolean = false,

    @ColumnInfo(name = "error_message")
    var errorMessage: String = "",

    @PrimaryKey()
    @ColumnInfo(name = "_id")
    var mId: String = "",

    @ColumnInfo(name = "line_info")
    var lineInfo: String = "",

    @ColumnInfo(name = "is_security")
    var isSecurity: Boolean = false
) {
    fun getPercentFromBytes(): Float =
        if (totalSize == 0L) 0f else (1f * downloadSize / totalSize) * 100f

    fun getPercentFromBytes(downloadSize: Long, totalSize: Long): Float =
        if (totalSize == 0L) 0f else (1f * downloadSize / totalSize) * 100f

    fun reset() {
        downloadCreateTime = 0L
        mimeType = ""
        errorCode = 0
        videoType = 0
        taskState = 0
        speed = 0f
        percent = 0f
        downloadSize = 0L
        totalSize = 0L
        fileName = ""
        filePath = ""
        coverUrl = ""
        coverPath = ""
        title = ""
        groupName = ""
    }

    override fun toString(): String {
        return "VideoTaskItem(url='$url', videoType=$videoType, percent=$percent, " +
                "downloadSize=$downloadSize, taskState=$taskState, filePath='$fileName', " +
                "localFile='$filePath', coverUrl='$coverUrl', coverPath='$coverPath', title='$title')"
    }
}
