package com.mzanak.animedl

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun GenreTag(t: String) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(t, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun DetailHeader(a: Anime, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(300.dp)) {
        AsyncImage(
            model = if (a.banner.isNotBlank()) a.banner else a.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
        Box(
            Modifier.fillMaxWidth().height(220.dp).background(
                Brush.verticalGradient(listOf(Color(0x66000000), MaterialTheme.colorScheme.background))
            )
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(8.dp).clip(CircleShape).background(Color(0x66000000))
        ) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
        }
        Row(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            AsyncImage(
                model = a.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(105.dp).aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF232733))
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(bottom = 6.dp)) {
                Text(a.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(metaLine(a), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                val s = a.score
                if (s != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFFFACC15), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${s / 10.0}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(a: Anime, st: Prefs, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var info by remember { mutableStateOf<Detail?>(null) }
    var files by remember { mutableStateOf<List<DlFile>?>(null) }
    var checked by remember { mutableStateOf(setOf<String>()) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(a.id) {
        info = try { Api.detail(a.id) } catch (e: Exception) { Detail("", emptyList(), emptyList()) }
    }
    LaunchedEffect(a.id) {
        files = try { Api.archive(a.title) } catch (e: Exception) { emptyList() }
    }
    val shown = remember(files, st.dub, st.quality) { Api.filter(files ?: emptyList(), st.dub, st.quality) }
    val picked = shown.filter { it.url in checked }
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (picked.isNotEmpty()) 96.dp else 24.dp)
        ) {
            item { DetailHeader(a, onBack) }
            item {
                val d = info
                Column(Modifier.padding(horizontal = 16.dp)) {
                    if (d == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        if (d.genres.isNotEmpty()) {
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                d.genres.forEach { g -> GenreTag(g) }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        if (d.desc.isNotBlank()) {
                            Text(
                                d.desc,
                                color = subColor,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                maxLines = if (expanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { expanded = !expanded }
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Audio", fontWeight = FontWeight.SemiBold)
                    FilterChip(selected = !st.dub, onClick = { st.setDub(false) }, label = { Text("Sub") })
                    FilterChip(selected = st.dub, onClick = { st.setDub(true) }, label = { Text("Dub") })
                }
            }
            val links = info?.links.orEmpty()
            if (links.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Watch officially", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            links.forEach { l ->
                                OutlinedButton(onClick = { openUrl(ctx, l.url) }, shape = RoundedCornerShape(12.dp)) {
                                    Text(l.site)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text("Free downloads", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Legal files found on the Internet Archive", color = subColor, fontSize = 12.sp)
                }
            }
            when {
                files == null -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() }
                }
                shown.isEmpty() -> item {
                    Text(
                        "No free, legal downloads found for this title with these options. Most anime is licensed, so use Watch officially above, try the other audio option, or paste a direct link in Browse.",
                        Modifier.padding(16.dp), color = subColor, fontSize = 13.sp
                    )
                }
                else -> {
                    item {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                checked = if (picked.size == shown.size) emptySet() else shown.map { it.url }.toSet()
                            }.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = picked.size == shown.size, onCheckedChange = { on: Boolean ->
                                checked = if (on) shown.map { f -> f.url }.toSet() else emptySet()
                            })
                            Text("Select all (${shown.size})")
                        }
                    }
                    items(shown, key = { it.url }) { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                checked = if (f.url in checked) checked - f.url else checked + f.url
                            }.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = f.url in checked, onCheckedChange = { on: Boolean ->
                                checked = if (on) checked + f.url else checked - f.url
                            })
                            Column {
                                Text(f.name.substringAfterLast('/'), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                                Text(Api.fmt(f.size), color = subColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        if (picked.isNotEmpty()) {
            Button(
                onClick = {
                    picked.forEach { Api.enqueue(ctx, it, st.wifiOnly, a.title, a.cover) }
                    Toast.makeText(ctx, "Added ${picked.size} to Downloads", Toast.LENGTH_SHORT).show()
                    checked = emptySet()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).height(56.dp)
            ) {
                Icon(Icons.Filled.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("Download ${picked.size} selected", fontWeight = FontWeight.Bold)
            }
        }
    }
}
