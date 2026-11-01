package com.notbypass.browser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm: AppVm by viewModels()
    private var pendingCb: ((Boolean) -> Unit)? = null

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        pendingCb?.invoke(res.isNotEmpty() && res.values.all { it })
        pendingCb = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.permHook = { perms, cb ->
            val missing = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isEmpty()) cb(true) else {
                pendingCb = cb
                permLauncher.launch(missing.toTypedArray())
            }
        }
        if (savedInstanceState == null) {
            handleIntent(intent)
            if (Build.VERSION.SDK_INT >= 33 && !vm.b("askedNotifications") &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                vm.set("askedNotifications", true)
                pendingCb = null
                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }

        setContent {
            val dark = isSystemInDarkTheme()
            val pal = when {
                vm.active.private -> PalIncognito
                vm.s("theme") == "light" -> PalLight
                vm.s("theme") == "amoled" -> PalAmoled
                vm.s("theme") == "dark" -> PalDark
                dark -> PalDark
                else -> PalLight
            }
            SideEffect {
                val ctl = WindowCompat.getInsetsController(window, window.decorView)
                ctl.isAppearanceLightStatusBars = pal.light
                ctl.isAppearanceLightNavigationBars = pal.light
            }
            LaunchedEffect(vm.active.private, vm.b("hideSwitcher")) {
                if (vm.active.private && vm.b("hideSwitcher")) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            LaunchedEffect(vm.fullscreen) {
                val ctl = WindowCompat.getInsetsController(window, window.decorView)
                if (vm.fullscreen) {
                    ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    ctl.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    ctl.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            BackHandler(enabled = true) {
                if (vm.handleBack()) finish()
            }
            CompositionLocalProvider(LocalPal provides pal) {
                MaterialTheme(colorScheme = materialScheme(pal)) {
                    val pad = if (vm.fullscreen) Modifier else Modifier.statusBarsPadding().navigationBarsPadding()
                    Box(Modifier.fillMaxSize().background(pal.bg).then(pad).imePadding()) {
                        BrowserScreen(vm)
                        val top = vm.stack.lastOrNull()
                        if (top != null) {
                            Box(Modifier.fillMaxSize().background(pal.bg).blockTouches()) {
                                when {
                                    top == "tabs" -> TabsScreen(vm)
                                    top == "bookmarks" -> BookmarksScreen(vm)
                                    top == "downloads" -> DownloadsScreen(vm)
                                    top == "notices" -> NoticesScreen(vm)
                                    top == "settings" -> SettingsHome(vm)
                                    top.startsWith("settings/") -> SettingsPage(top.removePrefix("settings/"), vm)
                                }
                            }
                        }
                        GlobalDialogs(vm)
                        Snack(vm)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val data = i?.data ?: return
        if (i.action == Intent.ACTION_VIEW && (data.scheme == "http" || data.scheme == "https")) {
            vm.openIncoming(data.toString())
        }
    }

    override fun onDestroy() {
        vm.permHook = null
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun GlobalDialogs(vm: AppVm) {
    val c = Nb.c
    val ctx = LocalContext.current
    vm.permQueue.firstOrNull()?.let { req ->
        AppDialog(req.title, { vm.decidePermission(false) }, confirm = "Allow", onConfirm = { vm.decidePermission(true) }, dismissText = "Block") {
            Text(req.text, color = c.text, fontSize = 15.sp)
        }
    }
    vm.ctxMenu?.let { info ->
        AppDialog("Link options", { vm.ctxMenu = null }, dismissText = "Close") {
            Column {
                val link = info.link
                val src = info.src
                if (link != null) {
                    ListRow("Open in new tab", null, onClick = { vm.ctxMenu = null; vm.newTab(vm.active.private, link) })
                    ListRow("Copy link address", null, onClick = { vm.ctxMenu = null; ctx.copyText(link); vm.toast("Link copied") })
                }
                if (src != null) {
                    ListRow("Open image in new tab", null, onClick = { vm.ctxMenu = null; vm.newTab(vm.active.private, src) })
                    ListRow("Copy image address", null, onClick = { vm.ctxMenu = null; ctx.copyText(src); vm.toast("Address copied") })
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Snack(vm: AppVm) {
    val c = Nb.c
    LaunchedEffect(vm.snack) {
        if (vm.snack.isNotEmpty()) {
            delay(2200)
            vm.snack = ""
        }
    }
    if (vm.snack.isNotEmpty()) {
        Box(Modifier.fillMaxSize().padding(PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp)), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(c.text).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(vm.snack, color = c.bg, fontSize = 14.sp)
            }
        }
    }
}
