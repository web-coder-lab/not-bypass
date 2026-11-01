@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.notbypass.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoView

@Composable
fun BrowserScreen(vm: AppVm) {
    val tab = vm.active
    val bottom = vm.s("toolbar") == "bottom"
    Column(Modifier.fillMaxSize()) {
        if (!vm.fullscreen && !bottom) BrowserToolbar(vm, tab)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (tab.url.isBlank()) NewTabPage(vm, tab) else GeckoSurface(vm)
            if (tab.offline) OfflineOverlay(vm)
            if (vm.addressEditing) SuggestionsOverlay(vm)
        }
        if (vm.findOpen && !vm.fullscreen) FindBar(vm)
        if (!vm.fullscreen && bottom) BrowserToolbar(vm, tab)
    }
    BrowserDialogs(vm, tab)
}

@Composable
fun GeckoSurface(vm: AppVm) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> GeckoView(ctx) },
        update = { view ->
            val t = vm.tabs.firstOrNull { it.id == vm.activeId } ?: vm.tabs.first()
            vm.engine.attach(view, t)
        },
        onRelease = { view -> view.releaseSession() }
    )
}

// ---------------------------------------------------------------- toolbar

@Composable
fun BrowserToolbar(vm: AppVm, tab: TabState) {
    val c = Nb.c
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (vm.addressEditing) AddressField(vm, tab) else AddressPill(vm, tab)
            }
            if (!vm.addressEditing) {
                TabCountButton(vm)
                Box {
                    IconBtn(Icons.Outlined.MoreVert, "Menu", { menu = true })
                    PageMenu(vm, tab, menu) { menu = false }
                }
            }
        }
        if (tab.loading && !vm.addressEditing) {
            LinearProgressIndicator(
                progress = { tab.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = c.accent,
                trackColor = Color.Transparent
            )
        } else {
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
fun AddressPill(vm: AppVm, tab: TabState) {
    val c = Nb.c
    val blank = tab.url.isBlank()
    val insecure = tab.url.startsWith("http://")
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(c.pill)
            .clickable { vm.addressText = tab.url; vm.addressEditing = true }
            .padding(start = 4.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (blank) {
            Icon(Icons.Outlined.Search, "Search", tint = c.sub, modifier = Modifier.padding(start = 10.dp, end = 4.dp).size(20.dp))
        } else {
            IconBtn(
                if (insecure) Icons.Outlined.Warning else if (tab.secure == true) Icons.Outlined.Lock else Icons.Outlined.Info,
                "Site information",
                { vm.dialog = "siteinfo" },
                modifier = Modifier.size(40.dp),
                tint = if (insecure) c.danger else c.sub
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            if (blank) {
                Text(
                    if (tab.private) "Search privately or type URL" else "Search or type URL",
                    color = c.sub, fontSize = 16.sp, maxLines = 1
                )
            } else {
                Text(displayHost(tab.url), color = c.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (insecure) Text("Not secure", color = c.danger, fontSize = 11.sp)
            }
        }
        if (!blank) {
            IconBtn(
                if (tab.loading) Icons.Outlined.Close else Icons.Outlined.Refresh,
                if (tab.loading) "Stop loading" else "Reload",
                { if (tab.loading) vm.stopLoading() else vm.reload() },
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun AddressField(vm: AppVm, tab: TabState) {
    val c = Nb.c
    var tf by remember { mutableStateOf(TextFieldValue(tab.url, TextRange(0, tab.url.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(tf.text) { vm.addressText = tf.text }
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(22.dp)).background(c.pill)
            .border(1.dp, c.accent, RoundedCornerShape(22.dp)).padding(start = 14.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, null, tint = c.sub, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = tf,
            onValueChange = { tf = it },
            singleLine = true,
            textStyle = TextStyle(color = c.text, fontSize = 16.sp),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.submit(tf.text) }),
            modifier = Modifier.weight(1f).focusRequester(focus).padding(horizontal = 10.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (tf.text.isEmpty()) Text("Search or type URL", color = c.sub, fontSize = 16.sp)
                    inner()
                }
            }
        )
        IconBtn(Icons.Outlined.Close, "Clear", {
            if (tf.text.isEmpty()) vm.addressEditing = false else tf = TextFieldValue("")
        }, modifier = Modifier.size(40.dp))
    }
}

@Composable
fun TabCountButton(vm: AppVm) {
    val c = Nb.c
    val count = vm.tabs.count { it.private == vm.active.private }
    Box(
        Modifier.size(48.dp).clickable { vm.push("tabs") },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(26.dp).border(2.dp, c.text, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (count > 99) ":D" else count.toString(),
                color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------------------------------------------------------- menu

@Composable
fun PageMenu(vm: AppVm, tab: TabState, expanded: Boolean, onDismiss: () -> Unit) {
    val c = Nb.c
    val ctx = LocalContext.current
    val hasPage = tab.url.isNotBlank()

    @Composable
    fun Item(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, check: Boolean = false, run: () -> Unit) {
        DropdownMenuItem(
            text = { Text(text, color = c.text) },
            leadingIcon = { Icon(icon, null, tint = c.sub) },
            trailingIcon = if (check) ({ Icon(Icons.Outlined.Check, null, tint = c.accent) }) else null,
            enabled = enabled,
            onClick = { onDismiss(); run() }
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.background(c.surface)) {
        Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Back", { onDismiss(); vm.back() }, enabled = tab.canGoBack)
            IconBtn(Icons.AutoMirrored.Outlined.ArrowForward, "Forward", { onDismiss(); vm.forward() }, enabled = tab.canGoForward)
            IconBtn(
                if (vm.isBookmarked(tab.url)) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                "Bookmark", { onDismiss(); vm.toggleBookmark(tab) }, enabled = hasPage,
                tint = if (vm.isBookmarked(tab.url)) c.accent else c.text
            )
            IconBtn(Icons.Outlined.Refresh, "Reload", { onDismiss(); vm.reload() }, enabled = hasPage)
        }
        HorizontalDivider(color = c.divider)
        Item("New tab", Icons.Outlined.Add) { vm.newTab(false) }
        Item("New private tab", Icons.Outlined.VisibilityOff) { vm.newTab(true) }
        HorizontalDivider(color = c.divider)
        Item("Bookmarks", Icons.Outlined.Bookmarks) { vm.push("bookmarks") }
        Item("Downloads", Icons.Outlined.Download) { vm.push("downloads") }
        Item(if (vm.unread > 0) "Notifications (${vm.unread})" else "Notifications", Icons.Outlined.Notifications) { vm.push("notices") }
        HorizontalDivider(color = c.divider)
        Item("Share\u2026", Icons.Outlined.Share, hasPage) { ctx.shareText(tab.url) }
        Item("Copy link", Icons.Outlined.ContentCopy, hasPage) { ctx.copyText(tab.url); vm.toast("Link copied") }
        Item("Find in page", Icons.Outlined.FindInPage, hasPage) { vm.findOpen = true }
        Item("Desktop site", Icons.Outlined.DesktopWindows, hasPage, check = tab.desktop) { vm.toggleDesktop() }
        Item("Text size", Icons.Outlined.TextFields) { vm.dialog = "textsize" }
        Item("Duplicate tab", Icons.Outlined.Tab) { vm.duplicateTab() }
        Item("View page source", Icons.Outlined.Code, hasPage && !tab.url.startsWith("view-source:")) { vm.newTab(tab.private, "view-source:" + tab.url) }
        Item("Site information", Icons.Outlined.Info, hasPage) { vm.dialog = "siteinfo" }
        Item("Close tab", Icons.Outlined.Close) { vm.closeTab(tab.id) }
        HorizontalDivider(color = c.divider)
        Item("Settings", Icons.Outlined.Settings) { vm.push("settings") }
    }
}

// ---------------------------------------------------------------- dialogs inside the browser

@Composable
fun BrowserDialogs(vm: AppVm, tab: TabState) {
    val c = Nb.c
    when (vm.dialog) {
        "siteinfo" -> {
            val insecure = tab.url.startsWith("http://")
            val state = when {
                insecure -> "Not secure. This page was loaded over plain HTTP."
                tab.secure == true -> "Connection is secure (HTTPS, certificate verified by the engine)."
                else -> "Security status is not confirmed for this page."
            }
            AppDialog("Site information", { vm.dialog = null }, confirm = "Permissions", onConfirm = {
                vm.dialog = null
                vm.push("settings"); vm.push("settings/permissions")
            }, dismissText = "Close") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tab.url.ifBlank { "No page" }, color = c.text, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(state, color = if (insecure) c.danger else c.sub, fontSize = 14.sp)
                    Text(
                        "HTTPS encrypts page content in transit. It does not hide which sites you visit from your network or provider.",
                        color = c.sub, fontSize = 13.sp
                    )
                    if (tab.private) Text("Private tab: no history or site data is kept after the tab closes.", color = c.sub, fontSize = 13.sp)
                }
            }
        }
        "textsize" -> {
            val pct = vm.s("textScale").ifBlank { "100" }
            AppDialog("Text size", { vm.dialog = null }, dismissText = "Done") {
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { vm.zoom(-1) }) { Text("A\u2212", color = c.accent, fontSize = 22.sp) }
                    Text("$pct%", color = c.text, fontSize = 18.sp)
                    TextButton(onClick = { vm.zoom(1) }) { Text("A+", color = c.accent, fontSize = 22.sp) }
                    TextButton(onClick = { vm.set("textScale", "100") }) { Text("Reset", color = c.accent) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- find bar

@Composable
fun FindBar(vm: AppVm) {
    val c = Nb.c
    var q by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(c.surface).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = q,
            onValueChange = { q = it; vm.find(it, false) },
            singleLine = true,
            textStyle = TextStyle(color = c.text, fontSize = 16.sp),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.find(q, false) }),
            modifier = Modifier.weight(1f).focusRequester(focus).padding(horizontal = 8.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (q.isEmpty()) Text("Find in page", color = c.sub, fontSize = 16.sp)
                    inner()
                }
            }
        )
        IconBtn(Icons.Outlined.KeyboardArrowUp, "Previous match", { vm.find(q, true) })
        IconBtn(Icons.Outlined.KeyboardArrowDown, "Next match", { vm.find(q, false) })
        IconBtn(Icons.Outlined.Close, "Close find", { vm.closeFind() })
    }
}

// ---------------------------------------------------------------- suggestions

@Composable
fun SuggestionsOverlay(vm: AppVm) {
    val c = Nb.c
    val list = vm.suggest(vm.addressText)
    Box(Modifier.fillMaxSize().background(c.bg).blockTouches()) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(list) { s ->
                ListRow(
                    title = s.title,
                    sub = s.sub,
                    icon = when (s.kind) {
                        "search" -> Icons.Outlined.Search
                        "bookmark" -> Icons.Outlined.Star
                        else -> Icons.Outlined.Language
                    },
                    onClick = {
                        vm.addressEditing = false
                        vm.loadUrl(vm.activeId, s.url)
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------- offline overlay

@Composable
fun OfflineOverlay(vm: AppVm) {
    val c = Nb.c
    Box(Modifier.fillMaxSize().background(c.bg).blockTouches(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Outlined.WifiOff, null, tint = c.sub, modifier = Modifier.size(72.dp))
            Text(
                BrowserEngine.OFFLINE_MESSAGE, color = c.text, fontSize = 18.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { vm.retry() },
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Retry") }
            OutlinedButton(onClick = { vm.goHome() }, modifier = Modifier.fillMaxWidth()) {
                Text("Go Home", color = c.accent)
            }
            TextButton(onClick = { vm.push("settings"); vm.push("settings/network") }) {
                Text("Network Settings", color = c.accent)
            }
        }
    }
}

// ---------------------------------------------------------------- new tab page

@Composable
fun NewTabPage(vm: AppVm, tab: TabState) {
    val c = Nb.c
    var addOpen by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<Shortcut?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Image(painterResource(R.drawable.ic_mark), null, Modifier.size(84.dp))
                Spacer(Modifier.height(8.dp))
                Text("NOT BYPASS", color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Medium, letterSpacing = 4.sp)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(c.pill)
                    .clickable { vm.addressText = ""; vm.addressEditing = true }.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, null, tint = c.sub)
                Spacer(Modifier.width(12.dp))
                Text("Search ${vm.currentEngine().name} or type URL", color = c.sub, fontSize = 16.sp, maxLines = 1)
            }
        }
        if (tab.private) {
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VisibilityOff, null, tint = c.accent)
                        Spacer(Modifier.width(12.dp))
                        Text("Private tab", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pages you view here aren\u2019t saved in history, and cookies and site data are discarded when the private tabs close.",
                        color = c.sub, fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Websites, your network and your internet provider can still see your activity. Private tabs are not anonymous.",
                        color = c.sub, fontSize = 14.sp
                    )
                }
            }
        } else {
            if (vm.b("showShortcuts")) {
                item {
                    val cells: List<Shortcut?> = vm.shortcuts.toList() + listOf<Shortcut?>(null)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        cells.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth()) {
                                row.forEach { s ->
                                    Column(
                                        Modifier.weight(1f).combinedClickable(
                                            onClick = {
                                                if (s == null) addOpen = true else vm.loadUrl(vm.activeId, s.url)
                                            },
                                            onLongClick = { if (s != null) removeTarget = s }
                                        ).padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (s == null) {
                                            Box(
                                                Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(c.pill),
                                                contentAlignment = Alignment.Center
                                            ) { Icon(Icons.Outlined.Add, "Add shortcut", tint = c.text) }
                                        } else {
                                            Avatar(s.name, 56)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            s?.name ?: "Add", color = c.text, fontSize = 12.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            if (vm.b("showBookmarks") && vm.bookmarks.isNotEmpty()) {
                item {
                    Text("Bookmarks", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth())
                }
                items(vm.bookmarks.take(5)) { b ->
                    ListRow(b.title, displayHost(b.url), Icons.Outlined.Star, onClick = { vm.loadUrl(vm.activeId, b.url) })
                }
            }
        }
    }

    if (addOpen) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        AppDialog("Add shortcut", { addOpen = false }, confirm = "Add", onConfirm = {
            vm.addShortcut(name, url)
            addOpen = false
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("Address") }, singleLine = true)
            }
        }
    }
    removeTarget?.let { s ->
        AppDialog("Remove shortcut?", { removeTarget = null }, confirm = "Remove", danger = true, onConfirm = {
            vm.removeShortcut(s.id)
            removeTarget = null
        }) { Text(s.name, color = c.text) }
    }
}
