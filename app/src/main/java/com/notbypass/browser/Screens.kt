package com.notbypass.browser

import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoView

@Composable
fun HomeScreen(vm: AppVm, onOpen: () -> Unit, onSettings: () -> Unit) {
    LaunchedEffect(Unit) { vm.refreshNetwork() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("NOT BYPASS") }
        item { Muted(if (vm.online) "Network available" else "You internet is not available please cheak and try again") }
        item {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search or enter address") },
                singleLine = true
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.openInput(vm.query); onOpen() }, colors = ButtonDefaults.buttonColors(containerColor = Nb.accent)) {
                    Text("Open", color = Color.Black)
                }
                Button(onClick = { vm.newTab(true); onOpen() }, colors = ButtonDefaults.buttonColors(containerColor = Nb.elevated)) {
                    Text("Private tab")
                }
                Button(onClick = onSettings, colors = ButtonDefaults.buttonColors(containerColor = Nb.elevated)) {
                    Text("Settings")
                }
            }
        }
        item { SectionTitle("Shortcuts") }
        items(vm.shortcuts) { s ->
            SettingRow(s.name, s.url) { vm.openInput(s.url); onOpen() }
        }
        item { SectionTitle("Bookmarks") }
        if (vm.bookmarks.isEmpty()) item { EmptyState("No bookmarks", "Bookmarks stay on this device. History is off by default.") }
        items(vm.bookmarks) { b ->
            SettingRow(b.title, b.url) { vm.openInput(b.url); onOpen() }
        }
        item {
            GlassCard {
                Text("Privacy", color = Nb.text)
                Muted("History is disabled by default. Private browsing does not hide activity from websites or your network.")
            }
        }
    }
}

@Composable
fun BrowserScreen(vm: AppVm, onHome: () -> Unit) {
    val tab = vm.active
    LaunchedEffect(tab.id) { vm.bind(tab) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { vm.back() }, enabled = tab.canGoBack) { Icon(Icons.Outlined.ArrowBack, null, tint = Nb.text) }
            IconButton(onClick = { vm.forward() }, enabled = tab.canGoForward) { Icon(Icons.Outlined.ArrowForward, null, tint = Nb.text) }
            IconButton(onClick = { if (tab.loading) vm.stop() else vm.reload() }) { Icon(Icons.Outlined.Refresh, null, tint = Nb.text) }
            IconButton(onClick = onHome) { Icon(Icons.Outlined.Home, null, tint = Nb.text) }
            Text(if (tab.secure == true) "HTTPS" else if (tab.url.startsWith("http://")) "HTTP" else "—", color = Nb.muted, modifier = Modifier.padding(top = 14.dp))
        }
        if (!vm.online && tab.url.isNotBlank()) {
            GlassCard(Modifier.padding(12.dp)) {
                Text("You internet is not available please cheak and try again", color = Nb.err)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.refreshNetwork(); vm.reload() }) { Text("Retry") }
                Button(onClick = onHome) { Text("Go Home") }
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                GeckoView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    val session = vm.engine.open(tab)
                    setSession(session)
                }
            },
            update = { view ->
                val session = vm.engine.open(tab)
                if (view.session !== session) {
                    view.releaseSession()
                    view.setSession(session)
                }
            }
        )
    }
}

@Composable
fun TabsScreen(vm: AppVm, onOpen: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("Tabs") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.newTab(false); onOpen() }) { Text("New") }
                Button(onClick = { vm.newTab(true); onOpen() }) { Text("Private") }
                Button(onClick = { vm.closeAll() }) { Text("Close all") }
            }
        }
        items(vm.tabs) { t ->
            GlassCard(onClick = { vm.select(t.id); onOpen() }) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(if (t.private) "Private · ${t.title}" else t.title, color = Nb.text)
                        Muted(t.url.ifBlank { "Empty tab" })
                    }
                    IconButton(onClick = { vm.closeTab(t.id) }) { Icon(Icons.Outlined.Close, null, tint = Nb.muted) }
                }
            }
        }
    }
}

@Composable
fun FilesScreen(vm: AppVm) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("Files") }
        item { Muted("Downloads use Android DownloadManager. Images and videos are not forced into Gallery unless the system places them there.") }
        if (vm.downloads.isEmpty()) item { EmptyState("No downloads", "Start a download from a page URL.") }
        items(vm.downloads) { d ->
            GlassCard {
                Text(d.name, color = Nb.text)
                Muted("${d.status} · ${d.url}")
                if (d.error.isNotBlank()) Text(d.error, color = Nb.err)
            }
        }
    }
}

@Composable
fun SettingsHome(onOpen: (String) -> Unit) {
    val cats = listOf(
        "general" to "General",
        "search" to "Search engine",
        "privacy" to "Privacy and security",
        "network" to "Network and connection",
        "proxy" to "Proxy",
        "downloads" to "Downloads",
        "tabs" to "Tabs and browsing",
        "permissions" to "Website permissions",
        "appearance" to "Appearance",
        "accessibility" to "Accessibility",
        "advanced" to "Advanced",
        "storage" to "Storage management",
        "password" to "Password and autofill",
        "media" to "Media",
        "notifications" to "Notifications",
        "homepage" to "Homepage",
        "data" to "Data management",
        "updates" to "Updates and engine",
        "performance" to "Performance",
        "developer" to "Developer tools",
        "backup" to "Backup and restore",
        "about" to "About application",
        "profiles" to "Browser Experience",
        "privacy-dash" to "Privacy dashboard",
        "network-dash" to "Network dashboard",
        "notices" to "Notification center"
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("Settings") }
        items(cats) { (id, name) -> SettingRow(name, "Open $name") { onOpen(id) } }
    }
}

@Composable
fun SettingDetail(id: String, vm: AppVm) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle(id.replace('-', ' ').replaceFirstChar { it.uppercase() }) }
        when (id) {
            "search" -> {
                items(Defaults.engines) { e ->
                    SettingRow(e.name, e.template) { vm.setSetting("engine", e.id) }
                }
                item { Muted("Current: ${vm.settings.optString("engine")}. Search history is off.") }
            }
            "privacy", "privacy-dash" -> {
                item { Toggle("HTTPS-only preference", "httpsOnly", vm) }
                item { Toggle("Block third-party cookies", "thirdPartyCookies", vm) }
                item { Toggle("Clear temporary data on exit", "clearOnExit", vm) }
                item { Toggle("Hide app-switcher preview", "hideSwitcher", vm) }
                item { Muted("Tracking protection is Standard via GeckoView ContentBlocking.AntiTracking.DEFAULT. Counts are not invented.") }
            }
            "network", "network-dash" -> {
                item { Muted(if (vm.online) "Connected" else "Disconnected") }
                item { Button(onClick = { vm.refreshNetwork() }) { Text("Retry") } }
                item { Muted("This is a connectivity check. It does not intercept HTTPS or audit the network.") }
            }
            "proxy" -> {
                item { Toggle("Proxy enabled", "proxyEnabled", vm) }
                item { Muted("Proxy host/port are stored locally. Android and GeckoView do not support every proxy type. Automatic rotation is not implemented.") }
            }
            "password" -> item { Toggle("Save passwords", "savePasswords", vm) }
            "notifications" -> {
                item { Toggle("Notifications", "notifications", vm) }
                item { Toggle("Download notifications", "downloadNotifications", vm) }
                item { Toggle("Network notifications", "networkNotifications", vm) }
            }
            "profiles" -> {
                listOf("chrome" to "Chrome-inspired", "firefox" to "Firefox", "ddg" to "DuckDuckGo-inspired", "custom" to "Custom").forEach { (k, n) ->
                    item { SettingRow(n, "Interface preset. Engine stays GeckoView.") { vm.setSetting("profile", k) } }
                }
            }
            "about", "updates" -> {
                item { Muted("NOT BYPASS 1.0.0") }
                item { Muted("GeckoView ${BuildConfig.GECKO_VERSION}") }
                item { Muted("Engine remains Mozilla GeckoView. Profiles do not switch to Chromium.") }
            }
            "notices" -> {
                if (vm.notices.isEmpty()) item { EmptyState("No notifications", "Download, network, and security notices appear here.") }
                items(vm.notices) { n -> GlassCard { Text(n.title, color = Nb.text); Muted(n.body) } }
            }
            else -> item { Muted("This page stores local preferences. Unsupported engine toggles are not shown as working features.") }
        }
    }
}

@Composable
private fun Toggle(label: String, key: String, vm: AppVm) {
    GlassCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Nb.text, modifier = Modifier.weight(1f))
            Switch(checked = vm.settings.optBoolean(key), onCheckedChange = { vm.setSetting(key, it) })
        }
    }
}
