package com.mzanak.animedl

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

val ACCENTS = listOf(
    Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF22D3EE),
    Color(0xFF22C55E), Color(0xFFFB923C), Color(0xFFEC4899),
    Color(0xFFEF4444), Color(0xFFFACC15), Color(0xFF9CA3AF)
)

val GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Mahou Shoujo",
    "Mecha", "Music", "Mystery", "Psychological", "Romance", "Sci-Fi",
    "Slice of Life", "Sports", "Supernatural", "Thriller"
)

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var accent by mutableIntStateOf(p.getInt("accent2", 0)); private set
    var amoled by mutableStateOf(p.getBoolean("amoled", false)); private set
    var quality by mutableIntStateOf(p.getInt("quality", 1)); private set
    var wifiOnly by mutableStateOf(p.getBoolean("wifi", false)); private set
    var dub by mutableStateOf(p.getBoolean("dub", false)); private set
    @JvmName("saveAccent")
    fun setAccent(v: Int) { accent = v; p.edit().putInt("accent2", v).apply() }
    @JvmName("saveAmoled")
    fun setAmoled(v: Boolean) { amoled = v; p.edit().putBoolean("amoled", v).apply() }
    @JvmName("saveQuality")
    fun setQuality(v: Int) { quality = v; p.edit().putInt("quality", v).apply() }
    @JvmName("saveWifiOnly")
    fun setWifiOnly(v: Boolean) { wifiOnly = v; p.edit().putBoolean("wifi", v).apply() }
    @JvmName("saveDub")
    fun setDub(v: Boolean) { dub = v; p.edit().putBoolean("dub", v).apply() }
}

class SearchModel {
    var q by mutableStateOf("")
    var genre by mutableStateOf<String?>(null)
    var res by mutableStateOf<List<Anime>>(emptyList())
    var loading by mutableStateOf(false)
    var searched by mutableStateOf(false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        setContent { Root(prefs) }
    }
}

fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Couldn't open the link", Toast.LENGTH_SHORT).show()
    }
}

fun metaLine(a: Anime): String =
    listOfNotNull(a.status.ifBlank { null }, a.year?.toString(), a.episodes?.let { "$it eps" })
        .joinToString("  |  ")

@Composable
fun Root(st: Prefs) {
    val ctx = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(Unit) {
        val need = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= 28) need += Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= 33) need += Manifest.permission.POST_NOTIFICATIONS
        if (need.isNotEmpty()) perm.launch(need.toTypedArray())
    }
    val acc = ACCENTS[st.accent]
    val bg = if (st.amoled) Color.Black else Color(0xFF0F1115)
    val card = if (st.amoled) Color(0xFF0E0E0E) else Color(0xFF181B22)
    val onAcc = if (acc.luminance() > 0.5f) Color.Black else Color.White
    var home by remember { mutableStateOf<Map<String, List<Anime>>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val homeState = rememberLazyListState()
    val search = remember { SearchModel() }
    LaunchedEffect(reload) {
        val cache = File(ctx.cacheDir, "home.json")
        if (home == null) {
            val c = Api.cachedHome(cache)
            if (c != null) home = c
        }
        failed = false
        try {
            home = Api.home(cache)
        } catch (e: Exception) {
            if (home == null) failed = true
        }
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = acc, onPrimary = onAcc,
            background = bg, onBackground = Color(0xFFECEDEF),
            surface = card, onSurface = Color(0xFFECEDEF),
            surfaceVariant = Color(0xFF232733), onSurfaceVariant = Color(0xFFA7ACB8),
            secondaryContainer = acc.copy(alpha = 0.22f), onSecondaryContainer = Color(0xFFECEDEF),
            outline = Color(0xFF3A3F4D), outlineVariant = Color(0xFF2A2F3B)
        )
    ) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var sel by remember { mutableStateOf<Anime?>(null) }
        BackHandler(sel != null) { sel = null }
        val tabs = listOf("Home", "Browse", "Downloads", "Settings")
        val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Download, Icons.Filled.Settings)
        Scaffold(
            containerColor = bg,
            bottomBar = {
                NavigationBar(containerColor = card) {
                    tabs.forEachIndexed { i, n ->
                        NavigationBarItem(
                            selected = tab == i && sel == null,
                            onClick = { tab = i; sel = null },
                            icon = { Icon(icons[i], n) },
                            label = { Text(n, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = acc,
                                selectedTextColor = acc,
                                indicatorColor = acc.copy(alpha = 0.18f)
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                val cur = sel
                if (cur != null) {
                    DetailScreen(cur, st) { sel = null }
                } else {
                    when (tab) {
                        0 -> HomeTab(home, failed, homeState, { reload++ }) { sel = it }
                        1 -> SearchTab(search, st) { sel = it }
                        2 -> DownloadsTab()
                        else -> SettingsTab(st)
                    }
                }
            }
        }
    }
}
