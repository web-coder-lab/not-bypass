package com.notbypass.browser

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebResponse
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

class PermReq(val title: String, val text: String, val decide: (Boolean) -> Unit)

class CtxInfo(val link: String?, val src: String?)

class Diag(val ok: Boolean, val summary: String, val dnsMs: Long, val connectMs: Long, val time: Long)

class AppVm(app: Application) : AndroidViewModel(app), EngineHost {
    private val nb = app as NotBypassApp
    private val store = JsonStore(app)
    private val main = Handler(Looper.getMainLooper())
    private val cm = app.getSystemService(ConnectivityManager::class.java)

    var settings by mutableStateOf(store.settings())
        private set
    val tabs = mutableStateListOf(TabState())
    var activeId by mutableStateOf(tabs.first().id)
        private set
    val bookmarks = mutableStateListOf<Bookmark>().apply { addAll(store.bookmarks()) }
    val shortcuts = mutableStateListOf<Shortcut>().apply { addAll(store.shortcuts()) }
    val customEngines = mutableStateListOf<SearchEngine>().apply { addAll(store.engines()) }
    val downloads = mutableStateListOf<DownloadItem>().apply {
        addAll(store.downloads())
    }
    val notices = mutableStateListOf<AppNotice>()

    /** Overlay screens on top of the browser: "tabs", "bookmarks", "downloads", "notices", "settings", "settings/<page>" */
    val stack = mutableStateListOf<String>()
    var addressEditing by mutableStateOf(false)
    var addressText by mutableStateOf("")
    var findOpen by mutableStateOf(false)
    var fullscreen by mutableStateOf(false)
    var snack by mutableStateOf("")
    var dialog by mutableStateOf<String?>(null)
    var ctxMenu by mutableStateOf<CtxInfo?>(null)
    val permQueue = mutableStateListOf<PermReq>()
    var systemDark by mutableStateOf(true)

    override var online by mutableStateOf(true)
        private set
    var netType by mutableStateOf("Unknown")
        private set
    var validated by mutableStateOf(true)
        private set
    var diag by mutableStateOf<Diag?>(null)
        private set
    var diagRunning by mutableStateOf(false)
        private set

    /** Set by MainActivity: asks for Android runtime permissions and reports the result. */
    var permHook: ((List<String>, (Boolean) -> Unit) -> Unit)? = null

    val engine = BrowserEngine(nb.runtime, this)
    private val downloader = Downloader(app) { downloadUpdated(it) }

    val active: TabState get() = tabs.firstOrNull { it.id == activeId } ?: tabs.first()
    val engines: List<SearchEngine> get() = Defaults.engines + customEngines
    val unread: Int get() = notices.count { !it.read }

    // ------------------------------------------------------------ settings

    fun s(key: String): String = settings.optString(key, "")
    fun b(key: String): Boolean = settings.optBoolean(key, false)

    fun set(key: String, value: Any) {
        val next = JSONObject(settings.toString())
        next.put(key, value)
        settings = next
        store.saveSettings(next)
        applyRuntime()
    }

    fun resetSettings() {
        settings = Defaults.settings()
        store.saveSettings(settings)
        applyRuntime()
        toast("Settings reset")
    }

    fun exportSettings(): String = settings.toString(2)

    fun importSettings(text: String): Boolean = try {
        val incoming = JSONObject(text)
        val next = JSONObject(settings.toString())
        val defaults = Defaults.settings()
        val keys = defaults.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k == "pendingClear" || k == "askedNotifications") continue
            if (incoming.has(k) && incoming.get(k)::class == defaults.get(k)::class) next.put(k, incoming.get(k))
        }
        settings = next
        store.saveSettings(next)
        applyRuntime()
        true
    } catch (e: Exception) {
        false
    }

    private fun applyRuntime() {
        val rs = nb.runtime.settings
        runCatching {
            val cb = rs.contentBlocking
            cb.setAntiTracking(
                if (s("tracking") == "strict") ContentBlocking.AntiTracking.STRICT
                else ContentBlocking.AntiTracking.DEFAULT
            )
            cb.setCookieBehavior(
                if (b("blockThirdPartyCookies")) ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY
                else ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS
            )
        }
        runCatching { rs.setJavaScriptEnabled(b("javascript")) }
        runCatching {
            rs.setAllowInsecureConnections(
                if (b("httpsOnly")) GeckoRuntimeSettings.HTTPS_ONLY else GeckoRuntimeSettings.ALLOW_ALL
            )
        }
        runCatching {
            val pct = s("textScale").toIntOrNull() ?: 100
            if (pct == 100) {
                rs.setAutomaticFontSizeAdjustment(true)
            } else {
                rs.setAutomaticFontSizeAdjustment(false)
                rs.setFontSizeFactor(pct / 100f)
            }
        }
    }

    fun zoom(delta: Int) {
        val steps = listOf(80, 100, 120, 150, 200)
        val cur = s("textScale").toIntOrNull() ?: 100
        val idx = steps.indexOf(cur).let { if (it < 0) 1 else it }
        val next = (idx + delta).coerceIn(0, steps.lastIndex)
        set("textScale", steps[next].toString())
    }

    // ------------------------------------------------------------ navigation / tabs

    fun currentEngine(): SearchEngine = engines.firstOrNull { it.id == s("engine") } ?: Defaults.engines.first()

    override fun update(id: String, f: (TabState) -> TabState) {
        val i = tabs.indexOfFirst { it.id == id }
        if (i >= 0) tabs[i] = f(tabs[i])
    }

    fun loadUrl(id: String, url: String) {
        val t = tabs.firstOrNull { it.id == id } ?: return
        update(id) { it.copy(url = url, title = "", offline = false, loading = true, progress = 5, secure = null) }
        engine.load(t, url)
    }

    fun submit(raw: String) {
        val url = normalizeInput(raw, currentEngine())
        if (url.isBlank()) return
        addressEditing = false
        stack.clear()
        loadUrl(activeId, url)
    }

    fun suggest(q: String): List<Suggestion> {
        val t = q.trim()
        if (t.isEmpty()) return emptyList()
        val eng = currentEngine()
        val out = ArrayList<Suggestion>()
        val urlLike = isUrlLike(t)
        if (urlLike) {
            out.add(Suggestion(t, "Go to site", normalizeInput(t, eng), "url"))
            out.add(Suggestion(t, "Search ${eng.name}", eng.template.replace("%s", java.net.URLEncoder.encode(t, "UTF-8")), "search"))
        } else {
            out.add(Suggestion(t, "Search ${eng.name}", normalizeInput(t, eng), "search"))
        }
        val lower = t.lowercase()
        bookmarks.filter { it.title.lowercase().contains(lower) || it.url.lowercase().contains(lower) }
            .take(4).forEach { out.add(Suggestion(it.title, it.url, it.url, "bookmark")) }
        shortcuts.filter { it.name.lowercase().contains(lower) || it.url.lowercase().contains(lower) }
            .take(2).forEach { out.add(Suggestion(it.name, it.url, it.url, "url")) }
        return out.distinctBy { it.url + it.kind }
    }

    fun newTab(private: Boolean = false, url: String? = null) {
        val t = TabState(private = private, desktop = b("desktopDefault"))
        tabs.add(t)
        activeId = t.id
        stack.clear()
        addressEditing = false
        findOpen = false
        val target = url ?: s("homeUrl").takeIf { it.isNotBlank() && !private }?.let { normalizeInput(it, currentEngine()) }
        if (target != null) loadUrl(t.id, target)
    }

    fun select(id: String) {
        activeId = id
        stack.clear()
        addressEditing = false
        findOpen = false
    }

    fun closeTab(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx < 0) return
        engine.close(id)
        tabs.removeAt(idx)
        if (tabs.isEmpty()) {
            val t = TabState()
            tabs.add(t)
            activeId = t.id
        } else if (activeId == id) {
            activeId = tabs[minOf(idx, tabs.size - 1)].id
        }
        findOpen = false
        fullscreen = false
    }

    /** private = true closes private tabs, false closes normal tabs, null closes all. */
    fun closeAll(private: Boolean? = null) {
        tabs.filter { private == null || it.private == private }.map { it.id }.forEach { engine.close(it) }
        tabs.removeAll { private == null || it.private == private }
        if (tabs.isEmpty()) {
            val t = TabState()
            tabs.add(t)
            activeId = t.id
        } else if (tabs.none { it.id == activeId }) {
            activeId = tabs.last().id
        }
        fullscreen = false
    }

    fun duplicateTab() {
        val t = active
        newTab(t.private, t.url.ifBlank { null })
    }

    fun goHome() {
        val id = activeId
        engine.close(id)
        update(id) { TabState(id = it.id, private = it.private, desktop = it.desktop) }
    }

    fun back() = engine.back(activeId)
    fun forward() = engine.forward(activeId)
    fun reload() {
        val t = active
        if (t.offline || !engine.has(t.id)) {
            if (t.url.isNotBlank()) loadUrl(t.id, t.url)
        } else {
            engine.reload(t.id)
        }
    }

    fun stopLoading() = engine.stop(activeId)

    fun retry() {
        val t = active
        if (t.url.isNotBlank()) loadUrl(t.id, t.url)
    }

    fun toggleDesktop() {
        val t = active
        val on = !t.desktop
        update(t.id) { it.copy(desktop = on) }
        engine.setDesktop(t.id, on)
    }

    fun find(text: String, backwards: Boolean) = engine.find(activeId, text, backwards)

    fun closeFind() {
        findOpen = false
        engine.clearFind(activeId)
    }

    fun openIncoming(url: String) {
        if (active.url.isBlank() && !active.private) loadUrl(activeId, url)
        else if (b("linksNewTab")) newTab(false, url)
        else loadUrl(activeId, url)
        stack.clear()
    }

    fun push(route: String) {
        addressEditing = false
        if (stack.lastOrNull() != route) stack.add(route)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    /** Returns true when the app should close. */
    fun handleBack(): Boolean {
        when {
            ctxMenu != null -> ctxMenu = null
            dialog != null -> dialog = null
            fullscreen -> {
                engine.exitFullScreen(activeId)
                fullscreen = false
            }
            addressEditing -> addressEditing = false
            findOpen -> closeFind()
            stack.isNotEmpty() -> pop()
            active.canGoBack -> back()
            tabs.size > 1 -> closeTab(activeId)
            else -> return true
        }
        return false
    }

    // ------------------------------------------------------------ bookmarks / shortcuts / engines

    fun isBookmarked(url: String) = url.isNotBlank() && bookmarks.any { it.url == url }

    fun toggleBookmark(tab: TabState) {
        if (tab.url.isBlank()) return
        val existing = bookmarks.firstOrNull { it.url == tab.url }
        if (existing != null) {
            bookmarks.remove(existing)
            toast("Bookmark removed")
        } else {
            bookmarks.add(0, Bookmark(title = tab.displayTitle, url = tab.url))
            toast("Bookmark added")
        }
        store.saveBookmarks(bookmarks)
    }

    fun editBookmark(id: String, title: String, url: String, folder: String) {
        val i = bookmarks.indexOfFirst { it.id == id }
        if (i < 0 || url.isBlank()) return
        bookmarks[i] = bookmarks[i].copy(title = title.ifBlank { url }, url = url, folder = folder.trim())
        store.saveBookmarks(bookmarks)
    }

    fun removeBookmark(id: String) {
        bookmarks.removeAll { it.id == id }
        store.saveBookmarks(bookmarks)
    }

    fun exportBookmarks(): String = store.bookmarksJson(bookmarks).toString(2)

    fun importBookmarks(text: String): Int = try {
        val list = store.parseBookmarks(JSONObject(text))
        var added = 0
        list.forEach { bm ->
            if (bookmarks.none { it.url == bm.url }) {
                bookmarks.add(bm.copy(id = UUID.randomUUID().toString()))
                added++
            }
        }
        store.saveBookmarks(bookmarks)
        added
    } catch (e: Exception) {
        -1
    }

    fun addShortcut(name: String, rawUrl: String) {
        val url = normalizeInput(rawUrl, currentEngine())
        if (url.isBlank() || !isUrlLike(rawUrl)) {
            toast("Enter a valid address")
            return
        }
        shortcuts.add(Shortcut(name = name.ifBlank { displayHost(url) }, url = url))
        store.saveShortcuts(shortcuts)
    }

    fun removeShortcut(id: String) {
        shortcuts.removeAll { it.id == id }
        store.saveShortcuts(shortcuts)
    }

    fun resetShortcuts() {
        shortcuts.clear()
        shortcuts.addAll(Defaults.shortcuts)
        store.saveShortcuts(shortcuts)
    }

    fun addEngine(name: String, template: String): Boolean {
        if (name.isBlank() || !template.contains("%s") || !template.startsWith("http")) return false
        customEngines.add(SearchEngine(UUID.randomUUID().toString(), name.trim(), template.trim(), custom = true))
        store.saveEngines(customEngines)
        return true
    }

    fun updateEngine(id: String, name: String, template: String): Boolean {
        val i = customEngines.indexOfFirst { it.id == id }
        if (i < 0 || name.isBlank() || !template.contains("%s") || !template.startsWith("http")) return false
        customEngines[i] = customEngines[i].copy(name = name.trim(), template = template.trim())
        store.saveEngines(customEngines)
        return true
    }

    fun removeEngine(id: String) {
        customEngines.removeAll { it.id == id }
        if (s("engine") == id) set("engine", "ddg")
        store.saveEngines(customEngines)
    }

    fun resetEngines() {
        customEngines.clear()
        store.saveEngines(customEngines)
        set("engine", "ddg")
    }

    // ------------------------------------------------------------ downloads

    override fun onDownload(response: WebResponse) {
        val item = downloader.start(response, b("galleryImages"), b("galleryVideos"))
        if (item == null) toast("Download failed: the site sent no data")
    }

    private fun downloadUpdated(item: DownloadItem) {
        val i = downloads.indexOfFirst { it.id == item.id }
        val first = i < 0
        if (first) downloads.add(0, item) else downloads[i] = item
        val nid = item.id.hashCode()
        val ctx = getApplication<Application>()
        val allow = b("notifications") && b("notifDownloads")
        val hide = b("hidePreviews")
        when (item.status) {
            "downloading" -> if (allow) {
                Notify.post(
                    ctx, nid, Notify.DL, "Downloading ${item.name}",
                    if (item.progress >= 0) "${item.progress}% of ${formatBytes(item.size)}" else "${formatBytes(item.received)} downloaded",
                    progress = item.progress, indeterminate = item.progress < 0, ongoing = true, hide = hide
                )
            }
            "completed" -> {
                store.saveDownloads(downloads)
                raise("downloads", "Download complete", "${item.name} · ${formatBytes(item.size)}")
                if (allow) {
                    Notify.post(
                        ctx, nid, Notify.DONE, "Download complete", "${item.name} · ${formatBytes(item.size)}",
                        hide = hide, fileUri = Uri.parse(item.localUri), mime = item.mime
                    )
                }
            }
            "failed" -> {
                store.saveDownloads(downloads)
                raise("downloads", "Download failed", "${item.name}: ${item.error}")
                if (allow) {
                    Notify.post(ctx, nid, Notify.DONE, "Download failed", "${item.name}: ${item.error}", hide = hide, retry = true)
                }
            }
            "canceled" -> {
                store.saveDownloads(downloads)
                Notify.cancel(ctx, nid)
            }
        }
    }

    fun cancelDownload(id: String) = downloader.cancel(id)

    fun retryDownload(item: DownloadItem) {
        downloads.removeAll { it.id == item.id }
        store.saveDownloads(downloads)
        engine.session(active).loadUri(item.url)
        toast("Retrying ${item.name}")
    }

    fun removeDownloadRecord(id: String) {
        downloads.removeAll { it.id == id }
        store.saveDownloads(downloads)
    }

    fun deleteDownloadFile(item: DownloadItem) {
        runCatching { getApplication<Application>().contentResolver.delete(Uri.parse(item.localUri), null, null) }
        removeDownloadRecord(item.id)
        toast("File deleted")
    }

    fun clearFinishedDownloads() {
        downloads.removeAll { it.status != "downloading" }
        store.saveDownloads(downloads)
    }

    // ------------------------------------------------------------ notifications

    fun raise(category: String, title: String, body: String) {
        notices.add(0, AppNotice(category = category, title = title, body = body))
    }

    fun markRead(id: String) {
        val i = notices.indexOfFirst { it.id == id }
        if (i >= 0 && !notices[i].read) notices[i] = notices[i].copy(read = true)
    }

    fun clearNotice(id: String) {
        notices.removeAll { it.id == id }
    }

    fun clearNotices(category: String?) {
        notices.removeAll { category == null || it.category == category }
    }

    private fun systemNotify(channel: String, id: Int, title: String, body: String) {
        Notify.post(getApplication(), id, channel, title, body, hide = b("hidePreviews"))
    }

    // ------------------------------------------------------------ network

    private fun typeOf(c: NetworkCapabilities?): String = when {
        c == null -> "None"
        c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
        c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "Other"
    }

    private val netCb = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            main.post { updateOnline(true) }
        }

        override fun onLost(network: Network) {
            main.post { updateOnline(false) }
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            main.post {
                netType = typeOf(caps)
                validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
    }

    private fun registerNetwork() {
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        netType = typeOf(caps)
        validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        runCatching { cm?.registerDefaultNetworkCallback(netCb) }
    }

    private fun updateOnline(on: Boolean) {
        if (on == online) return
        online = on
        if (on) {
            raise("network", "Network restored", "Your connection is back.")
            if (b("notifications") && b("notifNetwork")) systemNotify(Notify.NET, 9001, "Network restored", "Your connection is back.")
        } else {
            raise("network", "Network disconnected", BrowserEngine.OFFLINE_MESSAGE)
            if (b("notifications") && b("notifNetwork")) systemNotify(Notify.NET, 9001, "Network disconnected", BrowserEngine.OFFLINE_MESSAGE)
        }
    }

    fun runDiagnostics() {
        if (diagRunning) return
        diagRunning = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val host = "example.com"
                val t0 = System.currentTimeMillis()
                try {
                    val addr = InetAddress.getByName(host)
                    val dns = System.currentTimeMillis() - t0
                    val t1 = System.currentTimeMillis()
                    try {
                        Socket().use { it.connect(InetSocketAddress(addr, 443), 5000) }
                        Diag(true, "Reached $host", dns, System.currentTimeMillis() - t1, System.currentTimeMillis())
                    } catch (e: java.net.SocketTimeoutException) {
                        Diag(false, "Timeout connecting to $host", dns, -1, System.currentTimeMillis())
                    } catch (e: Exception) {
                        Diag(false, "Connection failed: ${e.message ?: "unknown"}", dns, -1, System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    Diag(false, "DNS resolution failed", -1, -1, System.currentTimeMillis())
                }
            }
            diag = result
            diagRunning = false
            if (!result.ok) raise("network", "Diagnostic failed", result.summary)
        }
    }

    fun memoryInfo(): Pair<Long, Long> {
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.availMem to mi.totalMem
    }

    // ------------------------------------------------------------ data clearing

    fun clear(flags: Long, label: String) {
        runCatching { nb.runtime.storageController.clearData(flags) }
            .onSuccess {
                toast("$label cleared")
                raise("privacy", "$label cleared", "Done on this device only.")
            }
            .onFailure { toast("Could not clear $label") }
    }

    // ------------------------------------------------------------ EngineHost

    override fun onFullscreen(id: String, on: Boolean) {
        if (id == activeId) fullscreen = on
    }

    override fun onNewWindow(parentId: String, uri: String): GeckoSession {
        val parent = tabs.firstOrNull { it.id == parentId }
        val t = TabState(url = uri, private = parent?.private == true, desktop = parent?.desktop == true, loading = true, progress = 5)
        tabs.add(t)
        activeId = t.id
        stack.clear()
        return engine.adopt(t)
    }

    override fun onCloseRequest(id: String) = closeTab(id)

    override fun onCrash(id: String) {
        val t = tabs.firstOrNull { it.id == id } ?: return
        engine.close(id)
        toast("Tab crashed, reloading")
        raise("privacy", "Tab crashed", "${displayHost(t.url)} was reloaded.")
        if (t.url.isNotBlank()) loadUrl(id, t.url)
    }

    override fun onExternal(uri: String) {
        val ctx = getApplication<Application>()
        try {
            val i = if (uri.startsWith("intent:")) Intent.parseUri(uri, Intent.URI_INTENT_SCHEME) else Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            i.addCategory(Intent.CATEGORY_BROWSABLE)
            i.component = null
            i.selector = null
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (e: Exception) {
            toast("No app can open this link")
        }
    }

    override fun onContext(link: String?, src: String?) {
        ctxMenu = CtxInfo(link, src)
    }

    private fun ask(policyKey: String, title: String, text: String, r: GeckoResult<Int>) {
        val allow = GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
        val deny = GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
        if (s(policyKey) == "block") {
            r.complete(deny)
            return
        }
        permQueue.add(PermReq(title, text) { ok -> r.complete(if (ok) allow else deny) })
    }

    override fun onContentPermission(perm: GeckoSession.PermissionDelegate.ContentPermission): GeckoResult<Int> {
        val r = GeckoResult<Int>()
        val host = displayHost(perm.uri)
        val allow = GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
        val deny = GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
        when (perm.permission) {
            GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION ->
                ask("permLocation", "Use your location?", "$host wants to know your location.", r)
            GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> {
                raise("websites", "Notification request blocked", "$host asked to send notifications. Web Push is not supported yet.")
                r.complete(deny)
            }
            GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE,
            GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> r.complete(allow)
            else -> r.complete(deny)
        }
        return r
    }

    override fun onMedia(
        video: List<GeckoSession.PermissionDelegate.MediaSource>,
        audio: List<GeckoSession.PermissionDelegate.MediaSource>,
        cb: GeckoSession.PermissionDelegate.MediaCallback
    ) {
        val cam = video.isNotEmpty() && s("permCamera") != "block"
        val mic = audio.isNotEmpty() && s("permMic") != "block"
        if (!cam && !mic) {
            cb.reject()
            return
        }
        val what = listOfNotNull(if (cam) "camera" else null, if (mic) "microphone" else null).joinToString(" and ")
        val host = displayHost(active.url)
        permQueue.add(PermReq("Use your $what?", "${host.ifBlank { "This site" }} wants to use your $what.") { ok ->
            if (ok) cb.grant(if (cam) video.firstOrNull() else null, if (mic) audio.firstOrNull() else null)
            else cb.reject()
        })
    }

    override fun onAndroidPermissions(perms: List<String>, cb: GeckoSession.PermissionDelegate.Callback) {
        val hook = permHook
        if (hook == null || perms.isEmpty()) {
            cb.reject()
            return
        }
        hook(perms) { ok -> if (ok) cb.grant() else cb.reject() }
    }

    fun decidePermission(ok: Boolean) {
        val req = permQueue.firstOrNull() ?: return
        permQueue.removeAt(0)
        req.decide(ok)
    }

    // ------------------------------------------------------------ misc

    fun toast(msg: String) {
        snack = msg
    }

    override fun onCleared() {
        runCatching { cm?.unregisterNetworkCallback(netCb) }
        if (b("clearOnExit")) {
            set("pendingClear", true)
            runCatching { nb.runtime.storageController.clearData(StorageController.ClearFlags.ALL) }
        }
        engine.closeAll()
        super.onCleared()
    }

    private fun start() {
        // downloads that were running when the app died cannot continue
        for (i in downloads.indices) {
            if (downloads[i].status == "downloading") {
                downloads[i] = downloads[i].copy(status = "failed", error = "Interrupted")
            }
        }
        applyRuntime()
        if (b("pendingClear")) {
            clear(StorageController.ClearFlags.ALL, "Site data")
            set("pendingClear", false)
        }
        registerNetwork()
        val home = s("homeUrl")
        if (home.isNotBlank()) loadUrl(tabs.first().id, normalizeInput(home, currentEngine()))
    }

    init {
        start()
    }
}
