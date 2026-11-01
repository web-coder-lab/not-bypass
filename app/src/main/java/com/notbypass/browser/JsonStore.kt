package com.notbypass.browser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/**
 * App-private JSON storage. No database, no cloud, no history archive.
 * Writes are atomic (tmp file + rename) and happen off the main thread.
 * A malformed file is moved aside as *.corrupt and defaults are used.
 */
class JsonStore(context: Context) {
    private val root = File(context.filesDir, "notbypass").apply { mkdirs() }
    private val io = Executors.newSingleThreadExecutor()

    private fun read(name: String): JSONObject? {
        val file = File(root, name)
        if (!file.exists()) return null
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            runCatching { file.renameTo(File(root, "$name.corrupt")) }
            null
        }
    }

    private fun write(name: String, value: JSONObject) {
        val text = value.toString(2)
        io.execute {
            try {
                val tmp = File(root, "$name.tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(File(root, name))) {
                    File(root, name).writeText(text)
                    tmp.delete()
                }
            } catch (_: Exception) {
            }
        }
    }

    // ---- settings ----
    fun settings(): JSONObject {
        val defaults = Defaults.settings()
        val loaded = read("settings.json") ?: return defaults
        val keys = defaults.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (!loaded.has(k)) loaded.put(k, defaults.get(k))
        }
        return loaded
    }

    fun saveSettings(value: JSONObject) = write("settings.json", value)

    // ---- bookmarks ----
    fun bookmarks(): List<Bookmark> = parseBookmarks(read("bookmarks.json"))

    fun saveBookmarks(items: List<Bookmark>) = write("bookmarks.json", bookmarksJson(items))

    fun bookmarksJson(items: List<Bookmark>): JSONObject {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("title", it.title).put("url", it.url)
                    .put("folder", it.folder).put("createdAt", it.createdAt)
            )
        }
        return JSONObject().put("schema", 2).put("items", arr)
    }

    fun parseBookmarks(obj: JSONObject?): List<Bookmark> {
        val arr = obj?.optJSONArray("items") ?: return emptyList()
        val out = ArrayList<Bookmark>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val url = o.optString("url", "")
            if (url.isBlank()) continue
            out.add(
                Bookmark(
                    id = o.optString("id", "").ifBlank { java.util.UUID.randomUUID().toString() },
                    title = o.optString("title", url).ifBlank { url },
                    url = url,
                    folder = o.optString("folder", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        return out
    }

    // ---- shortcuts ----
    fun shortcuts(): List<Shortcut> {
        val obj = read("shortcuts.json") ?: return Defaults.shortcuts
        val arr = obj.optJSONArray("items") ?: return Defaults.shortcuts
        val out = ArrayList<Shortcut>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val url = o.optString("url", "")
            if (url.isBlank()) continue
            out.add(Shortcut(o.optString("id", java.util.UUID.randomUUID().toString()), o.optString("name", url), url))
        }
        return out
    }

    fun saveShortcuts(items: List<Shortcut>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("url", it.url)) }
        write("shortcuts.json", JSONObject().put("schema", 1).put("items", arr))
    }

    // ---- custom search engines ----
    fun engines(): List<SearchEngine> {
        val arr = read("search_engines.json")?.optJSONArray("items") ?: return emptyList()
        val out = ArrayList<SearchEngine>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val t = o.optString("template", "")
            if (!t.contains("%s")) continue
            out.add(SearchEngine(o.optString("id"), o.optString("name", "Custom"), t, custom = true))
        }
        return out
    }

    fun saveEngines(items: List<SearchEngine>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("template", it.template)) }
        write("search_engines.json", JSONObject().put("schema", 1).put("items", arr))
    }

    // ---- download records (metadata only, never page history) ----
    fun downloads(): List<DownloadItem> {
        val arr = read("downloads.json")?.optJSONArray("items") ?: return emptyList()
        val out = ArrayList<DownloadItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                DownloadItem(
                    id = o.optString("id"), name = o.optString("name", "file"), url = o.optString("url"),
                    mime = o.optString("mime"), size = o.optLong("size", -1), received = o.optLong("received", 0),
                    status = o.optString("status", "failed"), error = o.optString("error"),
                    localUri = o.optString("localUri"), createdAt = o.optLong("createdAt", 0)
                )
            )
        }
        return out
    }

    fun saveDownloads(items: List<DownloadItem>) {
        val arr = JSONArray()
        items.filter { it.status != "downloading" }.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("name", it.name).put("url", it.url).put("mime", it.mime)
                    .put("size", it.size).put("received", it.received).put("status", it.status)
                    .put("error", it.error).put("localUri", it.localUri).put("createdAt", it.createdAt)
            )
        }
        write("downloads.json", JSONObject().put("schema", 1).put("items", arr))
    }
}
