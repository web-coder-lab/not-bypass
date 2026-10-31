package com.notbypass.browser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class JsonStore(context: Context) {
    private val root = File(context.filesDir, "notbypass").apply { mkdirs() }

    fun readObject(name: String, fallback: JSONObject = JSONObject()): JSONObject {
        val file = File(root, name)
        if (!file.exists()) return fallback
        return runCatching { JSONObject(file.readText()) }.getOrElse { fallback }
    }

    fun writeObject(name: String, value: JSONObject) {
        val file = File(root, name)
        val tmp = File(root, "$name.tmp")
        tmp.writeText(value.toString(2))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun settings(): JSONObject = readObject("settings.json", defaultSettings())
    fun saveSettings(value: JSONObject) = writeObject("settings.json", value)

    fun bookmarks(): List<Bookmark> {
        val arr = readObject("bookmarks.json", JSONObject().put("items", JSONArray()))
            .optJSONArray("items") ?: JSONArray()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Bookmark(o.getString("id"), o.getString("title"), o.getString("url"), o.optString("folder", "General"))
        }
    }

    fun saveBookmarks(items: List<Bookmark>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("url", it.url).put("folder", it.folder))
        }
        writeObject("bookmarks.json", JSONObject().put("schema", 1).put("items", arr))
    }

    private fun defaultSettings(): JSONObject = JSONObject()
        .put("schema", 1)
        .put("engine", "ddg")
        .put("profile", "firefox")
        .put("tracking", "standard")
        .put("httpsOnly", true)
        .put("thirdPartyCookies", false)
        .put("savePasswords", false)
        .put("clearOnExit", true)
        .put("hideSwitcher", true)
        .put("history", false)
        .put("searchHistory", false)
        .put("suggestions", false)
        .put("notifications", true)
        .put("downloadNotifications", true)
        .put("networkNotifications", true)
        .put("proxyEnabled", false)
        .put("proxyType", "http")
        .put("proxyHost", "")
        .put("proxyPort", "")
        .put("theme", "amoled")
        .put("animations", true)
        .put("compact", false)
}
