package com.notbypass.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Chrome-like palettes. */
data class Pal(
    val light: Boolean,
    val bg: Color,
    val surface: Color,
    val pill: Color,
    val text: Color,
    val sub: Color,
    val accent: Color,
    val onAccent: Color,
    val divider: Color,
    val danger: Color,
    val ok: Color
)

val PalDark = Pal(
    false, Color(0xFF202124), Color(0xFF292A2D), Color(0xFF303134), Color(0xFFE8EAED), Color(0xFF9AA0A6),
    Color(0xFF8AB4F8), Color(0xFF202124), Color(0xFF3C4043), Color(0xFFF28B82), Color(0xFF81C995)
)
val PalAmoled = Pal(
    false, Color(0xFF000000), Color(0xFF111214), Color(0xFF1E1F22), Color(0xFFE8EAED), Color(0xFF9AA0A6),
    Color(0xFF8AB4F8), Color(0xFF000000), Color(0xFF2A2B2E), Color(0xFFF28B82), Color(0xFF81C995)
)
val PalLight = Pal(
    true, Color(0xFFFFFFFF), Color(0xFFF8F9FA), Color(0xFFF1F3F4), Color(0xFF202124), Color(0xFF5F6368),
    Color(0xFF1A73E8), Color(0xFFFFFFFF), Color(0xFFDADCE0), Color(0xFFD93025), Color(0xFF188038)
)
val PalIncognito = Pal(
    false, Color(0xFF28292C), Color(0xFF323336), Color(0xFF3C4043), Color(0xFFE8EAED), Color(0xFFA7ABB0),
    Color(0xFFC58AF9), Color(0xFF202124), Color(0xFF4A4D51), Color(0xFFF28B82), Color(0xFF81C995)
)

val LocalPal = staticCompositionLocalOf { PalDark }

object Nb {
    val c: Pal
        @Composable
        @ReadOnlyComposable
        get() = LocalPal.current
}

fun materialScheme(p: Pal) =
    if (p.light) lightColorScheme(primary = p.accent, background = p.bg, surface = p.surface, onSurface = p.text, onBackground = p.text)
    else darkColorScheme(primary = p.accent, background = p.bg, surface = p.surface, onSurface = p.text, onBackground = p.text)

@Composable
fun IconBtn(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Nb.c.text
) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp), enabled = enabled) {
        Icon(icon, desc, tint = if (enabled) tint else tint.copy(alpha = 0.38f))
    }
}

@Composable
fun ScreenBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    val c = Nb.c
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
        Text(
            title, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
        actions()
    }
}

@Composable
fun ListRow(
    title: String,
    sub: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    val c = Nb.c
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.sub, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) c.danger else c.text, fontSize = 16.sp)
            if (!sub.isNullOrBlank()) Text(sub, color = c.sub, fontSize = 14.sp)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun NavRow(title: String, sub: String?, icon: ImageVector?, onClick: () -> Unit) {
    ListRow(title, sub, icon, onClick) {
        Icon(Icons.Outlined.ChevronRight, null, tint = Nb.c.sub)
    }
}

@Composable
fun SwitchRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Nb.c
    ListRow(title, sub, null, { onChange(!checked) }) {
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                uncheckedThumbColor = c.sub, uncheckedTrackColor = c.pill, uncheckedBorderColor = c.divider
            )
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text, color = Nb.c.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = Nb.c.divider,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Nb.c
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(c.surface)
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun Avatar(label: String, size: Int = 40, modifier: Modifier = Modifier) {
    val c = Nb.c
    Box(
        modifier.size(size.dp).clip(CircleShape).background(c.pill),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = c.text, fontSize = (size * 0.42f).sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Nb.c
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.heightIn(min = 36.dp).clip(shape)
            .background(if (selected) c.accent.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (selected) c.accent else c.divider, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) c.accent else c.text, fontSize = 14.sp)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String) {
    val c = Nb.c
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = c.sub, modifier = Modifier.size(56.dp))
        Text(title, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(body, color = c.sub, fontSize = 14.sp)
    }
}

@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    confirm: String? = null,
    onConfirm: () -> Unit = {},
    dismissText: String = "Cancel",
    danger: Boolean = false,
    content: @Composable () -> Unit
) {
    val c = Nb.c
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        titleContentColor = c.text,
        textContentColor = c.text,
        title = { Text(title, fontSize = 20.sp) },
        text = content,
        confirmButton = {
            if (confirm != null) {
                TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) c.danger else c.accent) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText, color = c.accent) } }
    )
}

/** Stops touches from reaching the GeckoView that sits underneath an overlay. */
fun Modifier.blockTouches(): Modifier = this.pointerInput(Unit) { detectTapGestures { } }
