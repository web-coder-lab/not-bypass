package com.notbypass.browser

import android.app.Application
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File

class NotBypassApp : Application() {

    /** Created only in the main process. Gecko child processes also instantiate Application. */
    val runtime: GeckoRuntime by lazy {
        val blocking = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
            .build()
        val settings = GeckoRuntimeSettings.Builder()
            .contentBlocking(blocking)
            .consoleOutput(false)
            .remoteDebuggingEnabled(false)
            .build()
        GeckoRuntime.create(this, settings)
    }

    override fun onCreate() {
        super.onCreate()
        if (isMainProcess()) {
            Notify.createChannels(this)
            runtime // warm up the engine early
        }
    }

    private fun isMainProcess(): Boolean = try {
        File("/proc/self/cmdline").readText().substringBefore('\u0000').trim() == packageName
    } catch (e: Exception) {
        true
    }
}
