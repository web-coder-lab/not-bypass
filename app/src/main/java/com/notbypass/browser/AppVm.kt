package com.notbypass.browser

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ProgressDelegate

class AppVm(app: Application) : AndroidViewModel(app) {
    private val store = JsonStore(app)
    val engine = BrowserEngine(app as NotBypassApp)
    var settings by mutableStateOf(store.settings())
    val tabs = mutableStateListOf(TabState())
    val bookmarks = mutableStateListOf<Bookmark>().apply { addAll(store.bookmarks()) }
    val shortcuts = mutableStateListOf<Shortcut>().apply { addAll(Defaults.shortcuts) }
    val downloads = mutableStateListOf<DownloadItem>()
    val notices = mutableStateListOf<AppNotice>()
    var activeId by mutableStateOf(tabs.first().id)
    var query by mutableStateOf("")
    var lastError by mutableStateOf("")
    var online by mutableStateOf(true)

    val active: TabState get() = tabs.firstOrNull { it.id == activeId } ?: tabs.first()

    fun engineFor(id: String): SearchEngine {
        val key = settings.optString("engine", "ddg")
        return Defaults.engines.firstOrNull { it.id == key } ?: Defaults.engines.first()
    }

    fun openInput(raw: String, private: Boolean = active.private) {
        val url = normalizeInput(raw, engineFor(active.id))
        if (url.isBlank()) return
        val tab = active.copy(url = url, title = url, private = private)
        replace(tab)
        engine.load(tab, url)
        lastError = ""
    }

    fun newTab(private: Boolean = false) {
        val tab = TabState(private = private)
        tabs.add(tab)
        activeId = tab.id
        engine.open(tab)
    }

    fun closeTab(id: String) {
        engine.close(id)
        tabs.removeAll { it.id == id }
        if (tabs.isEmpty()) tabs.add(TabState())
        if (activeId == id) activeId = tabs.last().id
    }

    fun closeAll() {
        engine.closeAll()
        tabs.clear()
        tabs.add(TabState())
        activeId = tabs.first().id
    }

    fun select(id: String) { activeId = id }

    fun back() = engine.sessions[active.id]?.goBack()
    fun forward() = engine.sessions[active.id]?.goForward()
    fun reload() = engine.sessions[active.id]?.reload()
    fun stop() = engine.sessions[active.id]?.stop()

    fun desktop(on: Boolean) = engine.setDesktop(active, on)

    fun addBookmark(title: String, url: String) {
        if (url.isBlank()) return
        bookmarks.add(0, Bookmark(title = title.ifBlank { url }, url = url))
        store.saveBookmarks(bookmarks)
        notice("privacy", "Bookmark saved", title.ifBlank { url })
    }

    fun removeBookmark(id: String) {
        bookmarks.removeAll { it.id == id }
        store.saveBookmarks(bookmarks)
    }

    fun setSetting(key: String, value: Any) {
        val next = JSONObject(settings.toString())
        when (value) {
            is Boolean -> next.put(key, value)
            is Int -> next.put(key, value)
            else -> next.put(key, value.toString())
        }
        settings = next
        store.saveSettings(next)
    }

    fun refreshNetwork() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        if (!online) {
            lastError = "You internet is not available please cheak and try again"
            notice("network", "Network disconnected", lastError)
        }
    }

    fun startDownload(url: String, name: String) {
        val item = DownloadItem(name = name.ifBlank { "download" }, url = url, status = "downloading")
        downloads.add(0, item)
        runCatching {
            val req = DownloadManager.Request(android.net.Uri.parse(url))
                .setTitle(item.name)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, item.name)
            getApplication<Application>().getSystemService(DownloadManager::class.java).enqueue(req)
            updateDownload(item.id, item.copy(status = "completed", progress = 100))
            notice("downloads", "Download started", item.name)
        }.onFailure {
            updateDownload(item.id, item.copy(status = "failed", error = it.message ?: "Download failed"))
            notice("downloads", "Download failed", it.message ?: item.name)
        }
    }

    private fun updateDownload(id: String, item: DownloadItem) {
        val i = downloads.indexOfFirst { it.id == id }
        if (i >= 0) downloads[i] = item
    }

    fun notice(category: String, title: String, body: String) {
        notices.add(0, AppNotice(category = category, title = title, body = body))
    }

    fun bind(tab: TabState) {
        val session = engine.open(tab)
        session.progressDelegate = object : ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                replace(active.copy(url = url, loading = true, progress = 5))
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                replace(active.copy(loading = false, progress = if (success) 100 else active.progress))
                if (!success && !online) lastError = "You internet is not available please cheak and try again"
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                replace(active.copy(progress = progress, loading = progress < 100))
            }
            override fun onSecurityChange(session: GeckoSession, securityInfo: ProgressDelegate.SecurityInformation) {
                replace(active.copy(secure = securityInfo.isSecure))
            }
        }
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                replace(active.copy(canGoBack = canGoBack))
            }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                replace(active.copy(canGoForward = canGoForward))
            }
        }
    }

    private fun replace(tab: TabState) {
        val i = tabs.indexOfFirst { it.id == tab.id }
        if (i >= 0) tabs[i] = tab
    }
}
