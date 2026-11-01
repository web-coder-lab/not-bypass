package com.notbypass.browser

import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID

data class TabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val url: String = "",
    val private: Boolean = false,
    val secure: Boolean? = null,
    val loading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val desktop: Boolean = false,
    val offline: Boolean = false
) {
    val displayTitle: String
        get() = when {
            title.isNotBlank() -> title
            url.isNotBlank() -> displayHost(url)
            private -> "New private tab"
            else -> "New tab"
        }
}

data class Bookmark(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val folder: String = "",
    val createdAt: Long = System.currentTimeMillis()
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
    val custom: Boolean = false
)

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val mime: String = "",
    val size: Long = -1,
    val received: Long = 0,
    val speed: Long = 0,
    val status: String = "downloading", // downloading | completed | failed | canceled
    val error: String = "",
    val localUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val progress: Int
        get() = if (size > 0) ((received * 100) / size).toInt().coerceIn(0, 100) else -1
}

data class AppNotice(
    val id: String = UUID.randomUUID().toString(),
    val category: String, // downloads | privacy | network | websites
    val title: String,
    val body: String,
    val time: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

data class Suggestion(val title: String, val sub: String, val url: String, val kind: String)

object Defaults {
    val engines = listOf(
        SearchEngine("ddg", "DuckDuckGo", "https://duckduckgo.com/?q=%s"),
        SearchEngine("google", "Google", "https://www.google.com/search?q=%s"),
        SearchEngine("bing", "Bing", "https://www.bing.com/search?q=%s"),
        SearchEngine("wikipedia", "Wikipedia", "https://en.wikipedia.org/w/index.php?search=%s")
    )

    val shortcuts = listOf(
        Shortcut(name = "YouTube", url = "https://m.youtube.com"),
        Shortcut(name = "Wikipedia", url = "https://www.wikipedia.org"),
        Shortcut(name = "Reddit", url = "https://www.reddit.com"),
        Shortcut(name = "GitHub", url = "https://github.com")
    )

    fun settings(): JSONObject = JSONObject()
        .put("schema", 2)
        .put("engine", "ddg")
        .put("profile", "chrome")
        .put("theme", "system")
        .put("toolbar", "top")
        .put("linksNewTab", true)
        .put("tracking", "standard")
        .put("blockThirdPartyCookies", false)
        .put("httpsOnly", false)
        .put("hideSwitcher", true)
        .put("clearOnExit", false)
        .put("pendingClear", false)
        .put("javascript", true)
        .put("desktopDefault", false)
        .put("textScale", "100")
        .put("tabLayout", "grid")
        .put("homeUrl", "")
        .put("showShortcuts", true)
        .put("showBookmarks", true)
        .put("galleryImages", true)
        .put("galleryVideos", true)
        .put("notifications", true)
        .put("notifDownloads", true)
        .put("notifNetwork", true)
        .put("notifSecurity", true)
        .put("hidePreviews", true)
        .put("permLocation", "ask")
        .put("permCamera", "ask")
        .put("permMic", "ask")
        .put("askedNotifications", false)
}

private val SCHEMES = listOf("http://", "https://", "about:", "file://", "view-source:", "ftp://")
private val URL_SHAPE = Regex("^[^\\s/?#:]+(:\\d{1,5})?([/?#]\\S*)?$")

fun hostOf(text: String): String =
    text.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')

fun isUrlLike(raw: String): Boolean {
    val t = raw.trim()
    if (t.isEmpty() || t.any { it.isWhitespace() }) return false
    val lower = t.lowercase()
    if (SCHEMES.any { lower.startsWith(it) }) return true
    if (!URL_SHAPE.matches(t)) return false
    val host = hostOf(t)
    if (host.equals("localhost", ignoreCase = true)) return true
    if (!host.contains('.')) return false
    val labels = host.split('.')
    if (labels.any { it.isEmpty() }) return false
    val tld = labels.last()
    if (tld.all { it.isDigit() }) {
        return labels.size == 4 && labels.all { (it.toIntOrNull() ?: 999) <= 255 }
    }
    return true
}

fun normalizeInput(raw: String, engine: SearchEngine): String {
    val t = raw.trim()
    if (t.isEmpty()) return ""
    if (isUrlLike(t)) {
        val lower = t.lowercase()
        if (SCHEMES.any { lower.startsWith(it) }) return t
        val host = hostOf(t)
        val local = host.equals("localhost", ignoreCase = true) ||
            host.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))
        return (if (local) "http://" else "https://") + t
    }
    return engine.template.replace("%s", URLEncoder.encode(t, "UTF-8"))
}

/** "https://www.example.com/a/b" -> "example.com" */
fun displayHost(url: String): String {
    if (url.isBlank()) return ""
    val noScheme = url.substringAfter("://", url)
    val host = noScheme.substringBefore('/').substringBefore('?').substringBefore('#')
    val clean = host.substringAfterLast('@').removePrefix("www.")
    return if (url.startsWith("about:") || url.startsWith("view-source:")) url else clean.ifBlank { url }
}

fun formatBytes(n: Long): String {
    if (n < 0) return "Unknown size"
    if (n < 1024) return "$n B"
    val kb = n / 1024.0
    if (kb < 1024) return String.format("%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
