package com.files.video.downloader.videoplayerdownloader.downloader.util

enum class SortState(var value: Int) {
    NAME(1),
    NAME_DESC(2),
    DATE(3),
    DATE_DESC(4),
    SIZE(5),
    SIZE_DESC(6);

    companion object {
        fun getSortState(value: Int): SortState {
            return when (value) {
                1 -> NAME
                2 -> NAME_DESC
                3 -> DATE
                4 -> DATE_DESC
                5 -> SIZE
                6 -> SIZE_DESC
                else -> NAME
            }
        }
    }
}