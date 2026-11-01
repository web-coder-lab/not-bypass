package com.notbypass.browser

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.geckoview.StorageController
import java.text.DateFormat
import java.util.Date

class PageDef(val id: String, val title: String, val sub: String, val icon: ImageVector)

val settingPages = listOf(
    PageDef("general", "General", "Toolbar position, external links", Icons.Outlined.Settings),
    PageDef("search", "Search engine", "Default and custom engines", Icons.Outlined.Search),
    PageDef("privacy", "Privacy and security", "Tracking protection, cookies, HTTPS-only", Icons.Outlined.Shield),
    PageDef("privacy-dash", "Privacy dashboard", "Live status of protections", Icons.Outlined.Security),
    PageDef("network", "Network and connection", "Status, diagnostics", Icons.Outlined.Wifi),
    PageDef("proxy", "Proxy", "What is and isn\u2019t supported", Icons.Outlined.Public),
    PageDef("downloads", "Downloads", "Where files are saved", Icons.Outlined.Download),
    PageDef("tabs", "Tabs and browsing", "Layout, desktop site", Icons.Outlined.Tab),
    PageDef("permissions", "Website permissions", "Location, camera, microphone", Icons.Outlined.Lock),
    PageDef("appearance", "Appearance", "Theme, text size", Icons.Outlined.Palette),
    PageDef("accessibility", "Accessibility", "Text size", Icons.Outlined.Accessibility),
    PageDef("advanced", "Advanced", "JavaScript, engine features", Icons.Outlined.Tune),
    PageDef("storage", "Storage management", "Clear cookies, cache, site data", Icons.Outlined.Storage),
    PageDef("password", "Passwords and autofill", "Not saved", Icons.Outlined.VpnKey),
    PageDef("media", "Media", "Playback", Icons.Outlined.PlayArrow),
    PageDef("notifications", "Notifications", "Downloads, security, network", Icons.Outlined.Notifications),
    PageDef("homepage", "Homepage", "Shortcuts, bookmarks, custom URL", Icons.Outlined.Home),
    PageDef("data", "Data management", "Clear and reset", Icons.Outlined.Delete),
    PageDef("updates", "Updates and engine", "Versions", Icons.Outlined.Update),
    PageDef("performance", "Performance", "Memory and sessions", Icons.Outlined.Speed),
    PageDef("developer", "Developer tools", "View page source", Icons.Outlined.Code),
    PageDef("backup", "Backup and restore", "Local files only", Icons.Outlined.Backup),
    PageDef("profiles", "Browser Experience", "Interface presets", Icons.Outlined.Language),
    PageDef("network-dash", "Network dashboard", "Connection diagnostics", Icons.Outlined.Wifi),
    PageDef("about", "About NOT BYPASS", "Version, privacy, licenses", Icons.Outlined.Info)
)

@Composable
fun SettingsHome(vm: AppVm) {
    val c = Nb.c
    Column(Modifier.fillMaxSize().background(c.bg)) {
        ScreenBar("Settings", { vm.pop() })
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                NavRow(
                    if (vm.unread > 0) "Notification center (${vm.unread} new)" else "Notification center",
                    "Download, network and security notices", Icons.Outlined.Notifications
                ) { vm.push("notices") }
            }
            items(settingPages) { p -> NavRow(p.title, p.sub, p.icon) { vm.push("settings/${p.id}") } }
        }
    }
}

@Composable
fun SettingsPage(id: String, vm: AppVm) {
    val c = Nb.c
    val page = settingPages.firstOrNull { it.id == id }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        ScreenBar(page?.title ?: "Settings", { vm.pop() })
        when (id) {
            "search" -> SearchEnginePage(vm)
            "profiles" -> ProfilesPage(vm)
            "privacy-dash" -> PrivacyDashboard(vm)
            "network", "network-dash" -> NetworkDashboard(vm)
            "backup" -> BackupPage(vm)
            "about" -> AboutPage()
            else -> GenericPage(id, vm)
        }
    }
}

// ================================================================ generic declarative pages

sealed interface SItem
class SHeader(val text: String) : SItem
class SSwitch(val key: String, val title: String, val sub: String = "") : SItem
class SChoice(val key: String, val title: String, val opts: List<Pair<String, String>>) : SItem
class SInfo(val title: String, val value: String) : SItem
class SAction(val title: String, val sub: String = "", val confirm: String? = null, val danger: Boolean = false, val run: () -> Unit) : SItem
class SField(val key: String, val title: String, val hint: String) : SItem
class SNote(val text: String) : SItem

private val TEXT_SIZES = listOf("80" to "80%", "100" to "100% (default)", "120" to "120%", "150" to "150%", "200" to "200%")
private val ASK_BLOCK = listOf("ask" to "Ask every time", "block" to "Blocked")

private fun pageItems(id: String, vm: AppVm, ctx: android.content.Context): List<SItem> = when (id) {
    "general" -> listOf(
        SChoice("toolbar", "Toolbar position", listOf("top" to "Top", "bottom" to "Bottom")),
        SSwitch("linksNewTab", "Open links from other apps in a new tab"),
        SInfo("Language", "Follows your Android system language")
    )
    "privacy" -> listOf(
        SChoice("tracking", "Tracking protection", listOf("standard" to "Standard (recommended)", "strict" to "Strict (may break some sites)")),
        SSwitch("blockThirdPartyCookies", "Block third-party cookies", "Cross-site cookies are not accepted"),
        SSwitch("httpsOnly", "HTTPS-only mode", "Plain HTTP pages are blocked"),
        SSwitch("hideSwitcher", "Protect private tabs in app switcher", "Also blocks screenshots while a private tab is open"),
        SSwitch("clearOnExit", "Clear site data when the app closes", "Best effort. Runs when you leave with Back"),
        SInfo("Browsing history", "Never stored by NOT BYPASS"),
        SInfo("Passwords", "Never saved"),
        SNote("Private tabs and HTTPS do not make you anonymous. Websites, your network and your internet provider can still see activity metadata.")
    )
    "proxy" -> listOf(
        SInfo("Proxy", "Not available"),
        SNote("GeckoView has no supported API for per-app proxy settings, and Android does not route the engine through a system proxy for apps. Rather than show a switch that does nothing, NOT BYPASS offers none. Use a system-wide VPN or Private DNS if you need that."),
        SNote("Automatic proxy rotation, chains and restriction bypass are intentionally not implemented.")
    )
    "downloads" -> listOf(
        SSwitch("galleryImages", "Save images to Gallery", "Pictures/NotBypass (Android 10+)"),
        SSwitch("galleryVideos", "Save videos to Gallery", "Movies/NotBypass (Android 10+)"),
        SInfo("Other files", "Downloads folder"),
        SSwitch("notifDownloads", "Download notifications"),
        SNote("On Android 8 and 9 files are saved in the app\u2019s own folder and shared through the system chooser."),
        SAction("Clear finished download records", "Files stay on your device", "Remove all finished download records?") { vm.clearFinishedDownloads() }
    )
    "tabs" -> listOf(
        SChoice("tabLayout", "Tab layout", listOf("grid" to "Grid", "list" to "List")),
        SSwitch("desktopDefault", "Request desktop site on new tabs"),
        SInfo("Restore tabs", "Tabs are not restored after the app closes")
    )
    "permissions" -> listOf(
        SChoice("permLocation", "Location", ASK_BLOCK),
        SChoice("permCamera", "Camera", ASK_BLOCK),
        SChoice("permMic", "Microphone", ASK_BLOCK),
        SInfo("Website notifications", "Blocked (Web Push is not supported yet)"),
        SAction("Reset saved site permissions", "", "Forget all permissions saved for websites?", true) {
            vm.clear(StorageController.ClearFlags.PERMISSIONS, "Site permissions")
        }
    )
    "appearance" -> listOf(
        SChoice("theme", "Theme", listOf("system" to "System default", "dark" to "Dark", "amoled" to "AMOLED black", "light" to "Light")),
        SChoice("textScale", "Web text size", TEXT_SIZES)
    )
    "accessibility" -> listOf(
        SChoice("textScale", "Web text size", TEXT_SIZES),
        SNote("App controls use standard Android labels and 48dp touch targets. The app UI follows the system font size.")
    )
    "advanced" -> listOf(
        SSwitch("javascript", "JavaScript", "Turning this off breaks most websites"),
        SInfo("WebAssembly, WebGL, HTTP/2, HTTP/3", "Provided by GeckoView, no switch"),
        SInfo("Engine", "GeckoView ${BuildConfig.GECKO_VERSION}")
    )
    "storage" -> listOf(
        SInfo("Bookmarks", "${vm.bookmarks.size} saved locally"),
        SInfo("Download records", "${vm.downloads.size}"),
        SAction("Clear cookies", "", "Clear all cookies?", true) { vm.clear(StorageController.ClearFlags.COOKIES, "Cookies") },
        SAction("Clear cache", "", "Clear the cache?", true) { vm.clear(StorageController.ClearFlags.ALL_CACHES, "Cache") },
        SAction("Clear website data", "Cookies, cache and storage", "Clear all website data? You will be signed out of sites.", true) {
            vm.clear(StorageController.ClearFlags.ALL, "Website data")
        }
    )
    "password" -> listOf(
        SInfo("Save passwords", "Off. NOT BYPASS does not save passwords"),
        SNote("Android Autofill integration is not implemented yet. Passwords are never exported.")
    )
    "media" -> listOf(
        SNote("Autoplay, picture-in-picture and background playback use GeckoView defaults. No switches are shown until they are verified to work."),
        SInfo("Full-screen video", "Supported")
    )
    "notifications" -> listOf(
        SSwitch("notifications", "Enable notifications"),
        SSwitch("notifDownloads", "Downloads"),
        SSwitch("notifSecurity", "Security"),
        SSwitch("notifNetwork", "Network changes"),
        SSwitch("hidePreviews", "Hide content on lock screen"),
        SInfo("Android permission", if (androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()) "Allowed" else "Blocked in system settings"),
        SAction("Open Android notification settings") {
            ctx.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },
        SAction("Open notification center") { vm.push("notices") },
        SNote("Sounds, vibration and priority are controlled per channel by Android.")
    )
    "homepage" -> listOf(
        SField("homeUrl", "Custom homepage", "Leave empty for the new tab page"),
        SSwitch("showShortcuts", "Show shortcuts"),
        SSwitch("showBookmarks", "Show bookmarks"),
        SAction("Reset shortcuts", "", "Restore default shortcuts?") { vm.resetShortcuts() }
    )
    "data" -> listOf(
        SAction("Clear cookies", "", "Clear all cookies?", true) { vm.clear(StorageController.ClearFlags.COOKIES, "Cookies") },
        SAction("Clear cache", "", "Clear the cache?", true) { vm.clear(StorageController.ClearFlags.ALL_CACHES, "Cache") },
        SAction("Clear website data", "", "Clear all website data?", true) { vm.clear(StorageController.ClearFlags.ALL, "Website data") },
        SAction("Close all tabs", "", "Close every tab?", true) { vm.closeAll(null) },
        SAction("Reset app settings", "Bookmarks are kept", "Reset all settings to defaults?", true) { vm.resetSettings() }
    )
    "updates" -> listOf(
        SInfo("NOT BYPASS", BuildConfig.VERSION_NAME),
        SInfo("GeckoView", BuildConfig.GECKO_VERSION),
        SInfo("Android", "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"),
        SNote("NOT BYPASS never checks for updates in the background and does not claim any update is available. Install new versions yourself.")
    )
    "performance" -> {
        val (avail, total) = vm.memoryInfo()
        listOf(
            SInfo("Device memory", "${formatBytes(avail)} free of ${formatBytes(total)}"),
            SInfo("Open tabs", "${vm.tabs.size}"),
            SInfo("Live engine sessions", "${vm.engine.sessionCount}"),
            SNote("Tabs that were never opened do not create an engine session. Closing a tab frees its session.")
        )
    }
    "developer" -> listOf(
        SAction("View source of current page", "Opens in a new tab") {
            val u = vm.active.url
            if (u.isNotBlank() && !u.startsWith("view-source:")) {
                vm.newTab(vm.active.private, "view-source:$u"); vm.stack.clear()
            } else vm.toast("Open a page first")
        },
        SInfo("Remote debugging", "Disabled"),
        SNote("Nothing in developer tools is sent anywhere.")
    )
    else -> listOf(SNote("Nothing to configure here."))
}

@Composable
fun GenericPage(id: String, vm: AppVm) {
    val ctx = LocalContext.current
    val items = pageItems(id, vm, ctx)
    LazyColumn(Modifier.fillMaxSize()) {
        items(items) { RenderItem(it, vm) }
    }
}

@Composable
fun RenderItem(item: SItem, vm: AppVm) {
    val c = Nb.c
    when (item) {
        is SHeader -> SectionLabel(item.text)
        is SSwitch -> SwitchRow(item.title, item.sub.ifBlank { null }, vm.b(item.key)) { vm.set(item.key, it) }
        is SInfo -> ListRow(item.title, item.value)
        is SNote -> Text(item.text, color = c.sub, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
        is SChoice -> {
            var open by remember { mutableStateOf(false) }
            val cur = vm.s(item.key)
            ListRow(item.title, item.opts.firstOrNull { it.first == cur }?.second ?: cur, onClick = { open = true })
            if (open) {
                AppDialog(item.title, { open = false }, dismissText = "Close") {
                    Column {
                        item.opts.forEach { (k, label) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = k == cur,
                                    onClick = { vm.set(item.key, k); open = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.sub)
                                )
                                Text(label, color = c.text, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
        is SField -> {
            var open by remember { mutableStateOf(false) }
            ListRow(item.title, vm.s(item.key).ifBlank { item.hint }, onClick = { open = true })
            if (open) {
                var text by remember { mutableStateOf(vm.s(item.key)) }
                AppDialog(item.title, { open = false }, confirm = "Save", onConfirm = {
                    vm.set(item.key, text.trim()); open = false
                }) { OutlinedTextField(text, { text = it }, singleLine = true, placeholder = { Text(item.hint) }) }
            }
        }
        is SAction -> {
            var ask by remember { mutableStateOf(false) }
            ListRow(item.title, item.sub.ifBlank { null }, onClick = { if (item.confirm != null) ask = true else item.run() }, danger = item.danger)
            if (ask) {
                AppDialog("Are you sure?", { ask = false }, confirm = "Continue", danger = item.danger, onConfirm = {
                    ask = false; item.run()
                }) { Text(item.confirm ?: "", color = c.text) }
            }
        }
    }
}

// ================================================================ search engines

@Composable
fun SearchEnginePage(vm: AppVm) {
    val c = Nb.c
    var editing by remember { mutableStateOf<SearchEngine?>(null) }
    var adding by remember { mutableStateOf(false) }
    val current = vm.s("engine")
    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionLabel("Default search engine") }
        items(vm.engines, key = { it.id }) { e ->
            ListRow(e.name, e.template, onClick = { vm.set("engine", e.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (e.custom) TextButton(onClick = { editing = e }) { Text("Edit", color = c.accent) }
                    RadioButton(
                        selected = e.id == current, onClick = { vm.set("engine", e.id) },
                        colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.sub)
                    )
                }
            }
        }
        item { ListRow("Add search engine", "Use %s where the search text goes", Icons.Outlined.Edit, onClick = { adding = true }) }
        item { ListRow("Reset to defaults", null, null, onClick = { vm.resetEngines() }) }
        item { ListRow("Search history", "Never saved") }
        item { ListRow("Search suggestions", "Off. No keystrokes are sent to any server") }
    }
    if (adding || editing != null) {
        val e = editing
        var name by remember(e?.id) { mutableStateOf(e?.name ?: "") }
        var tpl by remember(e?.id) { mutableStateOf(e?.template ?: "https://") }
        val close = { adding = false; editing = null }
        AppDialog(if (e == null) "Add search engine" else "Edit search engine", close, confirm = "Save", onConfirm = {
            val ok = if (e == null) vm.addEngine(name, tpl) else vm.updateEngine(e.id, name, tpl)
            if (ok) close() else vm.toast("Name and an http(s) URL containing %s are required")
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(tpl, { tpl = it }, label = { Text("URL with %s") }, singleLine = true)
                if (e != null) TextButton(onClick = { vm.removeEngine(e.id); close() }) { Text("Remove engine", color = c.danger) }
            }
        }
    }
}

// ================================================================ browser experience presets

@Composable
fun ProfilesPage(vm: AppVm) {
    val c = Nb.c
    val cur = vm.s("profile")
    val presets = listOf(
        Triple("chrome", "Chrome-inspired", "Top toolbar, tab grid, standard protection."),
        Triple("firefox", "Firefox", "Bottom toolbar, tab list, standard protection."),
        Triple("ddg", "DuckDuckGo-inspired", "Strict tracking protection, no third-party cookies, minimal home page."),
        Triple("custom", "Custom", "Your own combination. Selecting it changes nothing by itself.")
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "These are interface and privacy presets. The engine is always Mozilla GeckoView. Your search engine stays a separate setting. Names are used only to describe a style, with no affiliation.",
                color = c.sub, fontSize = 14.sp
            )
        }
        items(presets) { (key, name, desc) ->
            Card(onClick = { applyPreset(vm, key) }, borderColor = if (cur == key) c.accent else c.divider) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(desc, color = c.sub, fontSize = 14.sp)
                    }
                    if (cur == key) Icon(Icons.Outlined.Check, "Selected", tint = c.accent)
                }
            }
        }
    }
}

private fun applyPreset(vm: AppVm, key: String) {
    vm.set("profile", key)
    when (key) {
        "chrome" -> {
            vm.set("toolbar", "top"); vm.set("tabLayout", "grid"); vm.set("tracking", "standard")
            vm.set("blockThirdPartyCookies", false); vm.set("showShortcuts", true)
        }
        "firefox" -> {
            vm.set("toolbar", "bottom"); vm.set("tabLayout", "list"); vm.set("tracking", "standard")
            vm.set("blockThirdPartyCookies", false); vm.set("showShortcuts", true)
        }
        "ddg" -> {
            vm.set("toolbar", "bottom"); vm.set("tabLayout", "list"); vm.set("tracking", "strict")
            vm.set("blockThirdPartyCookies", true); vm.set("showShortcuts", false); vm.set("showBookmarks", false)
        }
    }
    vm.toast("Preset applied")
}

// ================================================================ dashboards

@Composable
fun PrivacyDashboard(vm: AppVm) {
    val c = Nb.c
    val t = vm.active
    val rows = listOf(
        Triple("Tracking protection", if (vm.s("tracking") == "strict") "Strict" else "Standard", true),
        Triple("Third-party cookies", if (vm.b("blockThirdPartyCookies")) "Blocked" else "Engine default (trackers blocked)", true),
        Triple("HTTPS-only mode", if (vm.b("httpsOnly")) "On" else "Off", vm.b("httpsOnly")),
        Triple("Browsing history", "Not stored", true),
        Triple("Passwords", "Not saved", true),
        Triple("Private tabs open", "${vm.tabs.count { it.private }}", true),
        Triple("Clear data on exit", if (vm.b("clearOnExit")) "On" else "Off", vm.b("clearOnExit")),
        Triple(
            "Current page connection",
            when {
                t.url.isBlank() -> "No page open"
                t.url.startsWith("http://") -> "Not secure (HTTP)"
                t.secure == true -> "Secure (HTTPS, verified by engine)"
                else -> "Not confirmed"
            },
            t.secure == true && !t.url.startsWith("http://")
        )
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(rows) { (title, value, good) ->
            Card {
                Text(title, color = c.sub, fontSize = 13.sp)
                Text(value, color = if (good) c.ok else c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
        item {
            Text(
                "Green only appears for settings that are verifiably on. NOT BYPASS shows no tracker counts because the engine does not expose verified numbers.",
                color = c.sub, fontSize = 13.sp
            )
        }
    }
}

@Composable
fun NetworkDashboard(vm: AppVm) {
    val c = Nb.c
    val d = vm.diag
    val fmt = remember { DateFormat.getTimeInstance(DateFormat.MEDIUM) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card {
                Text("Status", color = c.sub, fontSize = 13.sp)
                Text(
                    if (!vm.online) "Disconnected" else if (!vm.validated) "Connected, limited connectivity" else "Connected",
                    color = if (vm.online && vm.validated) c.ok else c.danger, fontSize = 20.sp, fontWeight = FontWeight.Medium
                )
                if (!vm.online) Text(BrowserEngine.OFFLINE_MESSAGE, color = c.sub, fontSize = 14.sp)
            }
        }
        item {
            Card {
                Text("Network type", color = c.sub, fontSize = 13.sp)
                Text(if (vm.online) vm.netType else "None", color = c.text, fontSize = 18.sp)
            }
        }
        item {
            Card {
                Text("Last diagnostic", color = c.sub, fontSize = 13.sp)
                if (d == null) {
                    Text("Not run yet", color = c.text, fontSize = 16.sp)
                } else {
                    Text(d.summary, color = if (d.ok) c.ok else c.danger, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (d.dnsMs >= 0) Text("DNS lookup: ${d.dnsMs} ms", color = c.text, fontSize = 14.sp)
                    if (d.connectMs >= 0) Text("TCP connect (port 443): ${d.connectMs} ms", color = c.text, fontSize = 14.sp)
                    Text("at ${fmt.format(Date(d.time))}", color = c.sub, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { vm.runDiagnostics() }, enabled = !vm.diagRunning && vm.online,
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)
                ) { Text(if (vm.diagRunning) "Testing\u2026" else "Run test") }
            }
        }
        item {
            Text(
                "The test resolves example.com and opens one TCP connection to it, only when you press the button. It is a connectivity check, not a security audit. Traffic is never intercepted. DNS settings follow Android (Private DNS in system settings).",
                color = c.sub, fontSize = 13.sp
            )
        }
    }
}

// ================================================================ backup

@Composable
fun BackupPage(vm: AppVm) {
    val ctx = LocalContext.current
    fun writer(text: String): (Uri?) -> Unit = { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                vm.toast("Exported")
            } catch (e: Exception) {
                vm.toast("Export failed")
            }
        }
    }
    val expBm = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { writer(vm.exportBookmarks())(it) }
    val expSet = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { writer(vm.exportSettings())(it) }
    val impBm = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val n = vm.importBookmarks(ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: "")
            vm.toast(if (n >= 0) "Imported $n bookmarks" else "Not a valid bookmarks file")
        } catch (e: Exception) {
            vm.toast("Import failed")
        }
    }
    val impSet = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            val ok = vm.importSettings(ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: "")
            vm.toast(if (ok) "Settings imported" else "Not a valid settings file")
        } catch (e: Exception) {
            vm.toast("Import failed")
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item { ListRow("Export bookmarks", "Save a JSON file", onClick = { expBm.launch("notbypass-bookmarks.json") }) }
        item { ListRow("Import bookmarks", "Merge from a JSON file", onClick = { impBm.launch(arrayOf("*/*")) }) }
        item { ListRow("Export settings", "Contains no passwords or browsing data", onClick = { expSet.launch("notbypass-settings.json") }) }
        item { ListRow("Import settings", null, onClick = { impSet.launch(arrayOf("*/*")) }) }
        item {
            Text(
                "Files are written only where you choose, through Android\u2019s file picker. There is no cloud backup.",
                color = Nb.c.sub, fontSize = 14.sp, modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// ================================================================ about

@Composable
fun AboutPage() {
    val c = Nb.c
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.ic_mark), null, Modifier.size(96.dp))
                Text("NOT BYPASS", color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = 3.sp)
                Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = c.sub, fontSize = 14.sp)
                Text("GeckoView ${BuildConfig.GECKO_VERSION}", color = c.sub, fontSize = 14.sp)
            }
        }
        item {
            Card {
                Text("Privacy", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "No account, no analytics, no ads, no backend. Settings and bookmarks live in app-private files on this device. Browsing history is not stored. Private tabs and HTTPS do not make you anonymous.",
                    color = c.sub, fontSize = 14.sp
                )
            }
        }
        item {
            Card {
                Text("Open-source licenses", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mozilla GeckoView: Mozilla Public License 2.0\nAndroidX, Jetpack Compose, Material, Kotlin: Apache License 2.0",
                    color = c.sub, fontSize = 14.sp
                )
            }
        }
        item { Text("Author: Syed Muhammad Hussain Shah", color = c.sub, fontSize = 13.sp) }
    }
}
