package com.notbypass.browser

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse

class BrowserEngine(private val app: NotBypassApp) {
    val sessions = linkedMapOf<String, GeckoSession>()

    fun open(tab: TabState): GeckoSession {
        sessions[tab.id]?.let { return it }
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(tab.private)
            .build()
        val session = GeckoSession(settings)
        session.contentDelegate = object : GeckoSession.ContentDelegate {}
        session.open(app.runtime)
        sessions[tab.id] = session
        return session
    }

    fun close(id: String) {
        sessions.remove(id)?.close()
    }

    fun closeAll() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }

    fun load(tab: TabState, url: String) {
        open(tab).loadUri(url)
    }

    fun setDesktop(tab: TabState, desktop: Boolean) {
        val session = open(tab)
        session.settings.userAgentMode = if (desktop) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        session.reload()
    }

    fun enqueueDownload(context: Context, response: WebResponse): Long {
        val name = response.uri?.substringAfterLast('/')?.substringBefore('?')?.ifBlank { "download" } ?: "download"
        val request = DownloadManager.Request(Uri.parse(response.uri))
            .setTitle(name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        val dm = context.getSystemService(DownloadManager::class.java)
        return dm.enqueue(request)
    }
}
