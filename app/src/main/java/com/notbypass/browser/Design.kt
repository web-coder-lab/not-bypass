package com.notbypass.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Nb {
    val bg = Color(0xFF000000)
    val bg2 = Color(0xFF08090D)
    val glass = Color(0xFF15171D)
    val elevated = Color(0xFF1C1F27)
    val border = Color(0xFF292D36)
    val text = Color(0xFFFFFFFF)
    val muted = Color(0xFF9297A3)
    val accent = Color(0xFFA78BFA)
    val ok = Color(0xFF45D6A0)
    val warn = Color(0xFFFFB454)
    val err = Color(0xFFFF626F)
}

val NbTheme = darkColorScheme(
    primary = Nb.accent,
    background = Nb.bg,
    surface = Nb.glass,
    onPrimary = Color.Black,
    onBackground = Nb.text,
    onSurface = Nb.text
)

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nb.glass)
            .border(1.dp, Nb.border, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp)
    ) { content() }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = Nb.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
fun Muted(text: String) {
    Text(text, color = Nb.muted, fontSize = 14.sp)
}

@Composable
fun SettingRow(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    GlassCard(onClick = onClick) {
        Text(title, color = Nb.text, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Muted(subtitle)
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    GlassCard {
        Text(title, color = Nb.text, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Muted(body)
    }
}
