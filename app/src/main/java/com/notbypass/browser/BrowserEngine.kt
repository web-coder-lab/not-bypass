package com.notbypass.browser

import android.net.Uri
import android.text.TextUtils
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

interface EngineHost {
    val online: Boolean
    fun update(id: String, f: (TabState) -> TabState)
    fun onDownload(response: WebResponse)
    fun onFullscreen(id: String, on: Boolean)
    fun onNewWindow(parentId: String, uri: String): GeckoSession
    fun onCloseRequest(id: String)
    fun onCrash(id: String)
    fun onExternal(uri: String)
    fun onContext(link: String?, src: String?)
    fun onContentPermission(perm: GeckoSession.PermissionDelegate.ContentPermission): GeckoResult<Int>
    fun onMedia(
        video: List<GeckoSession.PermissionDelegate.MediaSource>,
        audio: List<GeckoSession.PermissionDelegate.MediaSource>,
        cb: GeckoSession.PermissionDelegate.MediaCallback
    )
    fun onAndroidPermissions(perms: List<String>, cb: GeckoSession.PermissionDelegate.Callback)
}

class BrowserEngine(private val runtime: GeckoRuntime, private val host: EngineHost) {
    private val sessions = HashMap<String, GeckoSession>()

    val sessionCount: Int get() = sessions.size

    fun has(id: String) = sessions.containsKey(id)

    private fun build(tab: TabState): GeckoSession {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(tab.private)
            .userAgentMode(if (tab.desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(if (tab.desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .build()
        val session = GeckoSession(settings)
        wire(tab.id, session)
        return session
    }

    /** Session for a window opened by the page itself (target=_blank / window.open). Must stay unopened. */
    fun adopt(tab: TabState): GeckoSession {
        val s = build(tab)
        sessions[tab.id] = s
        return s
    }

    fun session(tab: TabState): GeckoSession {
        val existing = sessions[tab.id]
        val s = existing ?: build(tab).also { sessions[tab.id] = it }
        if (!s.isOpen) s.open(runtime)
        return s
    }

    fun attach(view: GeckoView, tab: TabState) {
        val s = session(tab)
        if (view.session !== s) {
            view.releaseSession()
            view.setSession(s)
        }
    }

    fun load(tab: TabState, url: String) {
        session(tab).loadUri(url)
    }

    fun reload(id: String) {
        sessions[id]?.reload()
    }

    fun stop(id: String) {
        sessions[id]?.stop()
    }

    fun back(id: String) {
        sessions[id]?.goBack()
    }

    fun forward(id: String) {
        sessions[id]?.goForward()
    }

    fun exitFullScreen(id: String) {
        sessions[id]?.exitFullScreen()
    }

    fun setDesktop(id: String, desktop: Boolean) {
        val s = sessions[id] ?: return
        s.settings.setUserAgentMode(
            if (desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        )
        s.settings.setViewportMode(
            if (desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        )
        s.reload()
    }

    fun find(id: String, text: String, backwards: Boolean) {
        val s = sessions[id] ?: return
        if (text.isEmpty()) {
            s.finder.clear()
        } else {
            s.finder.find(text, if (backwards) GeckoSession.FINDER_FIND_BACKWARDS else 0)
        }
    }

    fun clearFind(id: String) {
        sessions[id]?.finder?.clear()
    }

    fun close(id: String) {
        sessions.remove(id)?.let { s ->
            s.contentDelegate = null
            s.navigationDelegate = null
            s.progressDelegate = null
            s.permissionDelegate = null
            if (s.isOpen) s.close()
        }
    }

    fun closeAll() {
        sessions.keys.toList().forEach { close(it) }
    }

    private fun wire(id: String, s: GeckoSession) {
        s.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                host.update(id) { it.copy(loading = true, progress = 5, offline = false) }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                host.update(id) { it.copy(loading = false, progress = if (success) 100 else it.progress) }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                host.update(id) { it.copy(progress = progress, loading = progress < 100) }
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                host.update(id) { it.copy(secure = securityInfo.isSecure) }
            }
        }

        s.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                val u = url ?: return
                // our own error page and the engine's initial about:blank must not replace the real URL
                if (u.startsWith("data:text/html") || u == "about:blank") return
                host.update(id) { it.copy(url = u) }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                host.update(id) { it.copy(canGoBack = canGoBack) }
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                host.update(id) { it.copy(canGoForward = canGoForward) }
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = request.uri
                val scheme = uri.substringBefore(':', "").lowercase()
                val internal = scheme in setOf(
                    "http", "https", "about", "data", "blob", "view-source", "file", "javascript",
                    "ws", "wss", "ftp", "resource", "moz-extension"
                )
                if (!internal) {
                    if (request.hasUserGesture) host.onExternal(uri)
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                return GeckoResult.fromValue(host.onNewWindow(id, uri))
            }

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: WebRequestError
            ): GeckoResult<String>? {
                val code = error.code
                val offline = !host.online || code == WebRequestError.ERROR_OFFLINE
                val (title, message) = when {
                    offline -> "No internet" to OFFLINE_MESSAGE
                    code == WebRequestError.ERROR_UNKNOWN_HOST ->
                        "Can\u2019t find site" to "The server\u2019s address could not be found (DNS error). Check the address and try again."
                    code == WebRequestError.ERROR_NET_TIMEOUT ->
                        "Connection timed out" to "The site took too long to respond. Try again in a moment."
                    code == WebRequestError.ERROR_CONNECTION_REFUSED ->
                        "Connection refused" to "The server refused the connection. The site may be down."
                    code == WebRequestError.ERROR_SECURITY_BAD_CERT || code == WebRequestError.ERROR_SECURITY_SSL ->
                        "Your connection isn\u2019t private" to
                            "The site\u2019s security certificate could not be verified. NOT BYPASS does not allow bypassing certificate errors."
                    else -> "Page unavailable" to "This page could not be loaded."
                }
                host.update(id) { it.copy(loading = false, offline = offline, secure = false) }
                return GeckoResult.fromValue(errorPage(title, message, uri ?: "", !offline))
            }
        }

        s.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                host.update(id) { it.copy(title = title ?: "") }
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                host.onDownload(response)
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                host.onFullscreen(id, fullScreen)
            }

            override fun onCloseRequest(session: GeckoSession) {
                host.onCloseRequest(id)
            }

            override fun onCrash(session: GeckoSession) {
                host.onCrash(id)
            }

            override fun onKill(session: GeckoSession) {
                host.onCrash(id)
            }

            override fun onContextMenu(
                session: GeckoSession,
                screenX: Int,
                screenY: Int,
                element: GeckoSession.ContentDelegate.ContextElement
            ) {
                if (element.linkUri != null || element.srcUri != null) host.onContext(element.linkUri, element.srcUri)
            }
        }

        s.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onAndroidPermissionsRequest(
                session: GeckoSession,
                permissions: Array<out String>?,
                callback: GeckoSession.PermissionDelegate.Callback
            ) {
                host.onAndroidPermissions(permissions?.toList() ?: emptyList(), callback)
            }

            override fun onContentPermissionRequest(
                session: GeckoSession,
                perm: GeckoSession.PermissionDelegate.ContentPermission
            ): GeckoResult<Int>? = host.onContentPermission(perm)

            override fun onMediaPermissionRequest(
                session: GeckoSession,
                uri: String,
                video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
                callback: GeckoSession.PermissionDelegate.MediaCallback
            ) {
                host.onMedia(video?.toList() ?: emptyList(), audio?.toList() ?: emptyList(), callback)
            }
        }
    }

    companion object {
        const val OFFLINE_MESSAGE = "You internet is not available please cheak and try again"

        fun errorPage(title: String, message: String, url: String, retry: Boolean): String {
            val safeUrl = TextUtils.htmlEncode(url)
            val button = if (retry && url.isNotBlank()) "<a class=b href=\"$safeUrl\">Try again</a>" else ""
            val html = """
                <!doctype html><html><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <style>
                body{margin:0;background:#202124;color:#e8eaed;font-family:sans-serif;padding:32px 24px}
                h1{font-size:22px;font-weight:500;margin:48px 0 12px}
                p{color:#9aa0a6;line-height:1.5;font-size:15px}
                .u{word-break:break-all;font-size:13px;color:#9aa0a6;margin-top:24px}
                .b{display:inline-block;margin-top:24px;padding:12px 24px;border-radius:24px;background:#8ab4f8;color:#202124;text-decoration:none;font-weight:600}
                </style></head><body>
                <h1>${TextUtils.htmlEncode(title)}</h1>
                <p>${TextUtils.htmlEncode(message)}</p>$button
                <div class="u">$safeUrl</div>
                </body></html>
            """.trimIndent()
            return "data:text/html;charset=utf-8," + Uri.encode(html)
        }
    }
}
