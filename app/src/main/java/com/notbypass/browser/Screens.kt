@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.notbypass.browser

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

// ================================================================ tab switcher

@Composable
fun TabsScreen(vm: AppVm) {
    val c = Nb.c
    var showPrivate by remember { mutableStateOf(vm.active.private) }
    var menu by remember { mutableStateOf(false) }
    val list = vm.tabs.filter { it.private == showPrivate }
    val normalCount = vm.tabs.count { !it.private }
    val privateCount = vm.tabs.count { it.private }
    val grid = vm.s("tabLayout") != "list"

    Column(Modifier.fillMaxSize().background(c.bg)) {
        ScreenBar(if (showPrivate) "Private tabs" else "Tabs", { vm.pop() }) {
            IconBtn(Icons.Outlined.Add, "New tab", { vm.newTab(showPrivate) })
            Box {
                IconBtn(Icons.Outlined.MoreVert, "Menu", { menu = true })
                DropdownMenu(menu, { menu = false }, modifier = Modifier.background(c.surface)) {
                    DropdownMenuItem(
                        text = { Text(if (showPrivate) "Close all private tabs" else "Close all tabs", color = c.text) },
                        onClick = { menu = false; vm.closeAll(showPrivate); vm.pop() }
                    )
                    DropdownMenuItem(
                        text = { Text("Close every tab", color = c.text) },
                        onClick = { menu = false; vm.closeAll(null); vm.pop() }
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("Tabs ($normalCount)", !showPrivate) { showPrivate = false }
            Chip("Private ($privateCount)", showPrivate) { showPrivate = true }
        }
        if (list.isEmpty()) {
            EmptyState(
                if (showPrivate) Icons.Outlined.VisibilityOff else Icons.Outlined.Search,
                if (showPrivate) "No private tabs" else "No tabs",
                if (showPrivate) "Private tabs keep no history and discard cookies when closed." else "Tap + to open a new tab."
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (grid) 2 else 1),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list, key = { it.id }) { t -> TabCard(vm, t, grid) }
            }
        }
    }
}

@Composable
private fun TabCard(vm: AppVm, t: TabState, grid: Boolean) {
    val c = Nb.c
    val active = t.id == vm.activeId
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface)
            .border(if (active) 2.dp else 1.dp, if (active) c.accent else c.divider, shape)
            .clickable { vm.select(t.id) }
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(t.displayTitle, 24)
            Text(
                t.displayTitle, color = c.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconBtn(Icons.Outlined.Close, "Close tab", { vm.closeTab(t.id) }, modifier = Modifier.size(40.dp), tint = c.sub)
        }
        Box(
            Modifier.fillMaxWidth().height(if (grid) 120.dp else 56.dp).background(c.pill),
            contentAlignment = Alignment.Center
        ) {
            if (t.private) {
                Icon(Icons.Outlined.VisibilityOff, null, tint = c.sub, modifier = Modifier.size(32.dp))
            } else {
                Text(
                    t.url.ifBlank { "New tab" }.let { if (t.url.isBlank()) it else displayHost(it) },
                    color = c.sub, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

// ================================================================ bookmarks

@Composable
fun BookmarksScreen(vm: AppVm) {
    val c = Nb.c
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var sortAz by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Bookmark?>(null) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportBookmarks().toByteArray()) }
                vm.toast("Bookmarks exported")
            } catch (e: Exception) {
                vm.toast("Export failed")
            }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: ""
                val n = vm.importBookmarks(text)
                vm.toast(if (n >= 0) "Imported $n bookmarks" else "Not a valid bookmarks file")
            } catch (e: Exception) {
                vm.toast("Import failed")
            }
        }
    }

    val filtered = vm.bookmarks
        .filter { query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true) }
        .let { if (sortAz) it.sortedBy { b -> b.title.lowercase() } else it }
    val grouped = filtered.groupBy { it.folder }.toSortedMap()

    Column(Modifier.fillMaxSize().background(c.bg)) {
        ScreenBar("Bookmarks", { vm.pop() }) {
            Box {
                IconBtn(Icons.Outlined.MoreVert, "Menu", { menu = true })
                DropdownMenu(menu, { menu = false }, modifier = Modifier.background(c.surface)) {
                    DropdownMenuItem(
                        text = { Text(if (sortAz) "Sort by newest" else "Sort A\u2013Z", color = c.text) },
                        onClick = { menu = false; sortAz = !sortAz }
                    )
                    DropdownMenuItem(text = { Text("Export", color = c.text) }, onClick = { menu = false; export.launch("notbypass-bookmarks.json") })
                    DropdownMenuItem(text = { Text("Import", color = c.text) }, onClick = { menu = false; import.launch(arrayOf("*/*")) })
                }
            }
        }
        OutlinedTextField(
            query, { query = it }, singleLine = true, placeholder = { Text("Search bookmarks") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (filtered.isEmpty()) {
            EmptyState(Icons.Outlined.Bookmarks, "No bookmarks", "Tap the star in the menu to save a page. Bookmarks stay on this device.")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                grouped.forEach { (folder, list) ->
                    if (folder.isNotBlank()) item(key = "folder-$folder") { SectionLabel(folder) }
                    items(list, key = { it.id }) { b ->
                        var rowMenu by remember { mutableStateOf(false) }
                        ListRow(b.title, b.url, onClick = { vm.select(vm.activeId); vm.loadUrl(vm.activeId, b.url) }) {
                            Box {
                                IconBtn(Icons.Outlined.MoreVert, "Options", { rowMenu = true }, tint = c.sub)
                                DropdownMenu(rowMenu, { rowMenu = false }, modifier = Modifier.background(c.surface)) {
                                    DropdownMenuItem(
                                        text = { Text("Edit", color = c.text) },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, null, tint = c.sub) },
                                        onClick = { rowMenu = false; editing = b }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open in new tab", color = c.text) },
                                        leadingIcon = { Icon(Icons.Outlined.Add, null, tint = c.sub) },
                                        onClick = { rowMenu = false; vm.newTab(false, b.url) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = c.danger) },
                                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = c.danger) },
                                        onClick = { rowMenu = false; vm.removeBookmark(b.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { b ->
        var title by remember(b.id) { mutableStateOf(b.title) }
        var url by remember(b.id) { mutableStateOf(b.url) }
        var folder by remember(b.id) { mutableStateOf(b.folder) }
        AppDialog("Edit bookmark", { editing = null }, confirm = "Save", onConfirm = {
            vm.editBookmark(b.id, title, url, folder)
            editing = null
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("URL") }, singleLine = true)
                OutlinedTextField(folder, { folder = it }, label = { Text("Folder (optional)") }, singleLine = true)
            }
        }
    }
}

// ================================================================ downloads

private fun iconFor(mime: String, name: String): ImageVector = when {
    mime.startsWith("image/") -> Icons.Outlined.Image
    mime.startsWith("video/") -> Icons.Outlined.Movie
    mime.contains("pdf") || mime.startsWith("text/") || name.endsWith(".txt") -> Icons.Outlined.Description
    else -> Icons.Outlined.InsertDriveFile
}

@Composable
fun DownloadsScreen(vm: AppVm) {
    val c = Nb.c
    val ctx = LocalContext.current
    var filter by remember { mutableStateOf("all") }
    var confirmDelete by remember { mutableStateOf<DownloadItem?>(null) }
    val list = vm.downloads.filter {
        when (filter) {
            "downloading" -> it.status == "downloading"
            "completed" -> it.status == "completed"
            "failed" -> it.status == "failed" || it.status == "canceled"
            else -> true
        }
    }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        ScreenBar("Downloads", { vm.pop() }) {
            if (vm.downloads.any { it.status != "downloading" }) {
                TextButton(onClick = { vm.clearFinishedDownloads() }) { Text("Clear records", color = c.accent) }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("All", filter == "all") { filter = "all" }
            Chip("Downloading", filter == "downloading") { filter = "downloading" }
            Chip("Completed", filter == "completed") { filter = "completed" }
            Chip("Failed", filter == "failed") { filter = "failed" }
        }
        if (list.isEmpty()) {
            EmptyState(Icons.Outlined.Download, "No downloads", "Files you download appear here. Images go to Pictures, videos to Movies, others to Downloads.")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { d ->
                    Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(c.pill),
                                contentAlignment = Alignment.Center
                            ) { Icon(iconFor(d.mime, d.name), null, tint = c.text) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.name, color = c.text, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val status = when (d.status) {
                                    "downloading" -> {
                                        val sp = if (d.speed > 0) " · ${formatBytes(d.speed)}/s" else ""
                                        "${formatBytes(d.received)} of ${if (d.size > 0) formatBytes(d.size) else "?"}$sp"
                                    }
                                    "completed" -> "Completed · ${formatBytes(d.size)}"
                                    "canceled" -> "Canceled"
                                    else -> "Failed" + if (d.error.isNotBlank()) " · ${d.error}" else ""
                                }
                                Text(status, color = if (d.status == "failed") c.danger else c.sub, fontSize = 13.sp)
                            }
                        }
                        if (d.status == "downloading") {
                            Spacer(Modifier.height(10.dp))
                            if (d.progress >= 0) {
                                LinearProgressIndicator(progress = { d.progress / 100f }, modifier = Modifier.fillMaxWidth(), color = c.accent, trackColor = c.pill)
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = c.accent, trackColor = c.pill)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            when (d.status) {
                                "downloading" -> TextButton(onClick = { vm.cancelDownload(d.id) }) { Text("Cancel", color = c.accent) }
                                "completed" -> {
                                    TextButton(onClick = {
                                        if (!ctx.openFile(Uri.parse(d.localUri), d.mime)) vm.toast("No app can open this file")
                                    }) { Text("Open", color = c.accent) }
                                    TextButton(onClick = { ctx.shareFile(Uri.parse(d.localUri), d.mime) }) { Text("Share", color = c.accent) }
                                    TextButton(onClick = { confirmDelete = d }) { Text("Delete", color = c.danger) }
                                }
                                else -> {
                                    TextButton(onClick = { vm.retryDownload(d) }) { Text("Retry", color = c.accent) }
                                    TextButton(onClick = { vm.removeDownloadRecord(d.id) }) { Text("Remove", color = c.sub) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    confirmDelete?.let { d ->
        AppDialog("Delete file?", { confirmDelete = null }, confirm = "Delete", danger = true, onConfirm = {
            vm.deleteDownloadFile(d)
            confirmDelete = null
        }) { Text("\u201c${d.name}\u201d will be deleted from your device.", color = c.text) }
    }
}

// ================================================================ notification center

private fun catLabel(cat: String) = when (cat) {
    "downloads" -> "Downloads"
    "privacy" -> "Privacy & security"
    "network" -> "Network"
    "websites" -> "Websites"
    else -> cat
}

@Composable
fun NoticesScreen(vm: AppVm) {
    val c = Nb.c
    var filter by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val list = vm.notices.filter { filter == null || it.category == filter }
    val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    Column(Modifier.fillMaxSize().background(c.bg)) {
        ScreenBar("Notifications", { vm.pop() }) {
            if (vm.notices.isNotEmpty()) TextButton(onClick = { vm.clearNotices(filter) }) { Text("Clear all", color = c.accent) }
        }
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Chip("All", filter == null) { filter = null } }
            items(listOf("downloads", "privacy", "network", "websites")) { cat ->
                Chip(catLabel(cat), filter == cat) { filter = cat }
            }
        }
        if (list.isEmpty()) {
            EmptyState(Icons.Outlined.Notifications, "No notifications", "Download, network and security notices appear here.")
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(list, key = { it.id }) { n ->
                    val open = expanded == n.id
                    Card(onClick = { vm.markRead(n.id); expanded = if (open) null else n.id }, borderColor = if (n.read) c.divider else c.accent) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(c.pill),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    when (n.category) {
                                        "downloads" -> Icons.Outlined.Download
                                        "network" -> Icons.Outlined.Refresh
                                        else -> Icons.Outlined.Notifications
                                    }, null, tint = c.text
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(n.title, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    if (!n.read) Box(Modifier.size(10.dp).clip(CircleShape).background(c.accent))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(n.body, color = c.sub, fontSize = 15.sp, maxLines = if (open) 20 else 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(8.dp))
                                Text("${catLabel(n.category)} · ${fmt.format(Date(n.time))}", color = c.sub, fontSize = 12.sp)
                                if (open) {
                                    TextButton(onClick = { vm.clearNotice(n.id) }) { Text("Clear", color = c.danger) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
