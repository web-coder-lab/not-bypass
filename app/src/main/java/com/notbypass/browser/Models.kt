package com.notbypass.browser

import java.util.UUID

data class TabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New tab",
    val url: String = "",
    val private: Boolean = false,
    val secure: Boolean? = null,
    val loading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val folder: String = "General"
)

data class Shortcut(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String
)

data class SearchEngine(
    val id: String,
    val name: String,
    val template: String,
    val suggestion: String = "",
    val enabled: Boolean = true
)

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val mime: String = "",
    val size: Long = -1,
    val progress: Int = 0,
    val status: String = "downloading",
    val error: String = "",
    val localUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class AppNotice(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val title: String,
    val body: String,
    val time: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

object Defaults {
    val engines = listOf(
        SearchEngine("ddg", "DuckDuckGo", "https://duckduckgo.com/?q=%s", "https://duckduckgo.com/ac/?q=%s"),
        SearchEngine("google", "Google", "https://www.google.com/search?q=%s"),
        SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
        SearchEngine("wikipedia", "Wikipedia", "https://en.wikipedia.org/w/index.php?search=%s")
    )
    val shortcuts = listOf(
        Shortcut(name = "Wikipedia", url = "https://www.wikipedia.org"),
        Shortcut(name = "DuckDuckGo", url = "https://duckduckgo.com"),
        Shortcut(name = "YouTube", url = "https://m.youtube.com")
    )
}

fun normalizeInput(raw: String, engine: SearchEngine): String {
    val text = raw.trim()
    if (text.isEmpty()) return ""
    val looksUrl = text.contains('.') && !text.contains(' ') ||
        text.startsWith("http://") || text.startsWith("https://") || text.startsWith("about:")
    return if (looksUrl) {
        if (text.startsWith("http://") || text.startsWith("https://") || text.startsWith("about:")) text
        else "https://$text"
    } else {
        engine.template.replace("%s", java.net.URLEncoder.encode(text, "UTF-8"))
    }
}
