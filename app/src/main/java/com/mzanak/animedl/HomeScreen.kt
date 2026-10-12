package com.mzanak.animedl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun AppHeader() {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Ani ", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Shaikh", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun HomeTab(
    data: Map<String, List<Anime>>?,
    failed: Boolean,
    state: LazyListState,
    onRetry: () -> Unit,
    onPick: (Anime) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(bottom = 16.dp)) {
        item { AppHeader() }
        if (data == null) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                    if (failed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Couldn't load anime. Check your connection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onRetry) { Text("Retry") }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        } else {
            val hero = data["trending"]?.firstOrNull()
            if (hero != null) {
                item { Hero(hero, onPick) }
            }
            SECTIONS.forEach { (key, title) ->
                val list = data[key].orEmpty()
                if (list.isNotEmpty()) {
                    item(key = "sec_$key") { SectionRow(title, list, onPick) }
                }
            }
        }
    }
}

@Composable
fun Hero(a: Anime, onPick: (Anime) -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp)).clickable { onPick(a) }
    ) {
        AsyncImage(
            model = if (a.banner.isNotBlank()) a.banner else a.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text("TRENDING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                a.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(metaLine(a), fontSize = 12.sp, color = Color(0xCCFFFFFF))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onPick(a) }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Details", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onPick(a) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x99FFFFFF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.Download, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Download")
                }
            }
        }
    }
}

@Composable
fun SectionRow(title: String, list: List<Anime>, onPick: (Anime) -> Unit) {
    Column(Modifier.padding(top = 20.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(list, key = { it.id }) { PosterCard(it, Modifier.width(120.dp), onPick) }
        }
    }
}

@Composable
fun PosterCard(a: Anime, modifier: Modifier, onPick: (Anime) -> Unit) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).clickable { onPick(a) }) {
        Box {
            AsyncImage(
                model = a.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFF232733))
            )
            val s = a.score
            if (s != null) {
                Row(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000)).padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFACC15), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${s / 10.0}", fontSize = 11.sp, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(a.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
