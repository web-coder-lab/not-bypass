package com.notbypass.browser

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Streams the engine's own response body (so cookies/auth/blob responses work)
 * into MediaStore: images -> Pictures, videos -> Movies, everything else -> Downloads.
 * On Android 8-9 files go to the app's external files dir (shared via FileProvider).
 */
class Downloader(
    private val app: Application,
    private val onUpdate: (DownloadItem) -> Unit
) {
    private class Canceled : Exception()

    private class Target(val uri: Uri, val out: OutputStream, val legacyFile: File?)

    private val exec = Executors.newFixedThreadPool(3)
    private val cancels = ConcurrentHashMap<String, AtomicBoolean>()
    private val main = Handler(Looper.getMainLooper())

    fun cancel(id: String) {
        cancels[id]?.set(true)
    }

    /** Must be called on the main thread. Returns null if the response has no body. */
    fun start(response: WebResponse, images: Boolean, videos: Boolean): DownloadItem? {
        val body = response.body ?: return null
        val headers = response.headers
        fun header(key: String): String? =
            headers.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value

        var mime = header("content-type")?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        val disposition = header("content-disposition")
        var name = URLUtil.guessFileName(response.uri, disposition, mime.ifBlank { null })
        name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "download" }
        if (mime.isBlank() || mime == "application/octet-stream") {
            val ext = name.substringAfterLast('.', "").lowercase()
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let { mime = it }
        }
        val size = header("content-length")?.toLongOrNull() ?: -1L
        val item = DownloadItem(name = name, url = response.uri, mime = mime, size = size)
        onUpdate(item)
        exec.execute { run(item, body, images, videos) }
        return item
    }

    private fun post(item: DownloadItem) {
        main.post { onUpdate(item) }
    }

    private fun run(start: DownloadItem, body: InputStream, images: Boolean, videos: Boolean) {
        var item = start
        val flag = AtomicBoolean(false)
        cancels[item.id] = flag
        var target: Target? = null
        try {
            val t = createTarget(item.name, item.mime, images, videos)
            target = t
            var received = 0L
            var last = 0L
            val t0 = SystemClock.elapsedRealtime()
            t.out.use { out ->
                body.use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        if (flag.get()) throw Canceled()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        received += n
                        val now = SystemClock.elapsedRealtime()
                        if (now - last > 400) {
                            last = now
                            val speed = received * 1000 / maxOf(1L, now - t0)
                            item = item.copy(received = received, speed = speed)
                            post(item)
                        }
                    }
                }
            }
            finish(t)
            item = item.copy(
                status = "completed", received = received,
                size = if (item.size > 0) item.size else received,
                speed = 0, localUri = t.uri.toString()
            )
            post(item)
        } catch (e: Canceled) {
            target?.let { discard(it) }
            post(item.copy(status = "canceled", speed = 0))
        } catch (e: Exception) {
            target?.let { discard(it) }
            post(item.copy(status = "failed", speed = 0, error = e.message ?: "Download interrupted"))
        } finally {
            runCatching { body.close() }
            cancels.remove(item.id)
        }
    }

    private fun createTarget(name: String, mime: String, images: Boolean, videos: Boolean): Target {
        val type = mime.ifBlank { "application/octet-stream" }
        if (Build.VERSION.SDK_INT >= 29) {
            val (collection, rel) = when {
                images && type.startsWith("image/") ->
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to
                        (Environment.DIRECTORY_PICTURES + "/NotBypass")
                videos && type.startsWith("video/") ->
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY) to
                        (Environment.DIRECTORY_MOVIES + "/NotBypass")
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_DOWNLOADS
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, type)
                put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = app.contentResolver.insert(collection, values) ?: throw IOException("Could not create file")
            val out = app.contentResolver.openOutputStream(uri) ?: throw IOException("Could not open file")
            return Target(uri, out, null)
        }
        val dir = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: app.filesDir
        dir.mkdirs()
        var file = File(dir, name)
        var n = 1
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        while (file.exists()) {
            file = File(dir, if (ext.isEmpty()) "$base ($n)" else "$base ($n).$ext")
            n++
        }
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
        return Target(uri, FileOutputStream(file), file)
    }

    private fun finish(t: Target) {
        if (t.legacyFile == null && Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            app.contentResolver.update(t.uri, cv, null, null)
        }
    }

    private fun discard(t: Target) {
        runCatching { t.out.close() }
        if (t.legacyFile != null) t.legacyFile.delete()
        else runCatching { app.contentResolver.delete(t.uri, null, null) }
    }
}
