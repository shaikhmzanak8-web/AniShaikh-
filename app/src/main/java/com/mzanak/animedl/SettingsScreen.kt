package com.mzanak.animedl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            content()
        }
    }
}

@Composable
fun SettingsTab(st: Prefs) {
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        SettingsCard("APPEARANCE") {
            Text("Accent color")
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ACCENTS.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(c).clickable { st.setAccent(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (i == st.accent) {
                            Icon(Icons.Filled.Check, null, tint = if (c.luminance() > 0.5f) Color.Black else Color.White)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pure black background", Modifier.weight(1f))
                Switch(checked = st.amoled, onCheckedChange = { st.setAmoled(it) })
            }
        }
        SettingsCard("DOWNLOADS") {
            Text("Download quality (file size limit)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Data saver", "Standard", "High").forEachIndexed { i, n ->
                    FilterChip(selected = st.quality == i, onClick = { st.setQuality(i) }, label = { Text(n) })
                }
            }
            Text("Data saver: up to 150 MB. Standard: up to 600 MB. High: no limit.", fontSize = 12.sp, color = subColor)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Download on Wi-Fi only", Modifier.weight(1f))
                Switch(checked = st.wifiOnly, onCheckedChange = { st.setWifiOnly(it) })
            }
        }
        SettingsCard("AUDIO") {
            Text("Default audio")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !st.dub, onClick = { st.setDub(false) }, label = { Text("Sub") })
                FilterChip(selected = st.dub, onClick = { st.setDub(true) }, label = { Text("Dub") })
            }
        }
        SettingsCard("ABOUT") {
            Text("Ani Shaikh 2.0")
            Text("Files are saved in Downloads/Anime on your phone.", fontSize = 12.sp, color = subColor)
        }
        Text(
            "Developed by Shaikh Mzanak",
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            textAlign = TextAlign.Center, color = subColor, fontSize = 12.sp
        )
    }
}
