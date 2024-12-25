package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab

import android.util.Patterns

class WebTabFactory {
    companion object {

        const val SEARCH_URL = "https://www.google.com/search?q=%s"

        fun createWebTabFromInput(input: String): WebTab {
            if (input.isNotEmpty()) {
                return if (input.startsWith("http://") || input.startsWith("https://")) {
                    WebTab(input, null, null, emptyMap())
                } else if (Patterns.WEB_URL.matcher(input).matches()) {
                    WebTab("https://$input", null, null, emptyMap())
                } else {
                    WebTab(
                        String.format(SEARCH_URL, input),
                        null,
                        null,
                        emptyMap()
                    )
                }
            }

            return WebTab.HOME_TAB
        }
    }
}
