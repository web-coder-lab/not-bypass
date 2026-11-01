package com.notbypass.browser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notify {
    const val DL = "downloads"
    const val DONE = "downloads_done"
    const val SEC = "security"
    const val NET = "network"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(DL, "Download progress", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(DONE, "Download results", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(SEC, "Privacy and security", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(NET, "Network", NotificationManager.IMPORTANCE_LOW))
    }

    private fun flags() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    fun post(
        ctx: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        progress: Int = -1,
        indeterminate: Boolean = false,
        ongoing: Boolean = false,
        hide: Boolean = true,
        fileUri: Uri? = null,
        mime: String? = null,
        retry: Boolean = false
    ) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags()
        )
        val b = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_mark)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setColor(0xFF8AB4F8.toInt())
        if (progress >= 0) b.setProgress(100, progress, false)
        else if (indeterminate) b.setProgress(0, 0, true)
        if (fileUri != null) {
            val type = mime?.ifBlank { null } ?: "*/*"
            val view = Intent(Intent.ACTION_VIEW).setDataAndType(fileUri, type)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            val send = Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM, fileUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            b.addAction(0, "Open", PendingIntent.getActivity(ctx, id, view, flags()))
            b.addAction(0, "Share", PendingIntent.getActivity(ctx, id + 1, Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags()))
        }
        if (retry) b.addAction(0, "Open downloads", open)
        if (hide) {
            b.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            b.setPublicVersion(
                NotificationCompat.Builder(ctx, channel).setSmallIcon(R.drawable.ic_mark)
                    .setContentTitle("NOT BYPASS").setContentText("Content hidden").build()
            )
        }
        try {
            nm.notify(id, b.build())
        } catch (_: SecurityException) {
        }
    }

    fun cancel(ctx: Context, id: Int) {
        NotificationManagerCompat.from(ctx).cancel(id)
    }
}
