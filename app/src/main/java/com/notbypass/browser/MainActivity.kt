package com.notbypass.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppVm = viewModel()
            var tab by remember { mutableStateOf("home") }
            var setting by remember { mutableStateOf<String?>(null) }
            val browsing = tab == "browser"
            MaterialTheme(colorScheme = NbTheme) {
                Scaffold(
                    containerColor = Nb.bg,
                    bottomBar = {
                        if (!browsing && setting == null) {
                            NavigationBar(containerColor = Nb.bg2) {
                                listOf(
                                    Triple("home", "Home", Icons.Outlined.Home),
                                    Triple("search", "Search", Icons.Outlined.Search),
                                    Triple("tabs", "Tabs", Icons.Outlined.Tab),
                                    Triple("files", "Files", Icons.Outlined.Folder),
                                    Triple("settings", "Settings", Icons.Outlined.Settings)
                                ).forEach { (id, label, icon) ->
                                    NavigationBarItem(
                                        selected = tab == id,
                                        onClick = { tab = id },
                                        icon = { Icon(icon, null) },
                                        label = { Text(label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Nb.accent,
                                            selectedTextColor = Nb.accent,
                                            indicatorColor = Nb.elevated,
                                            unselectedIconColor = Nb.muted,
                                            unselectedTextColor = Nb.muted
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { pad ->
                    Box(Modifier.padding(pad).background(Nb.bg)) {
                        when {
                            setting != null -> SettingDetail(setting!!, vm)
                            tab == "home" || tab == "search" -> HomeScreen(vm, { tab = "browser" }, { tab = "settings" })
                            tab == "browser" -> BrowserScreen(vm, { tab = "home" })
                            tab == "tabs" -> TabsScreen(vm, { tab = "browser" })
                            tab == "files" -> FilesScreen(vm)
                            tab == "settings" -> SettingsHome { setting = it }
                        }
                    }
                }
            }
        }
    }
}
