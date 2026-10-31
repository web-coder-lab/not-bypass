package com.notbypass.browser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class NotBypassApp : Application() {
    lateinit var runtime: GeckoRuntime
        private set

    override fun onCreate() {
        super.onCreate()
        val blocking = ContentBlocking.Settings.Builder()
            .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
            .build()
        val settings = GeckoRuntimeSettings.Builder()
            .contentBlocking(blocking)
            .consoleOutput(false)
            .build()
        runtime = GeckoRuntime.create(this, settings)
        createChannels()
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        listOf(
            "downloads" to "Downloads",
            "security" to "Privacy and security",
            "network" to "Network"
        ).forEach { (id, name) ->
            nm.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}
