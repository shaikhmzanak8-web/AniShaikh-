package com.mzanak.animedl

import android.app.DownloadManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun DownloadRow(r: DlRecord, s: DlState?, speed: Long, onOpen: () -> Unit, onDelete: () -> Unit) {
    val active = dlActive(s)
    val done = dlDone(s)
    val total = s?.total ?: -1L
    val got = s?.done ?: 0L
    val frac = if (total > 0L) (got.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val label = when {
        done -> "Completed"
        s == null -> "Cancelled"
        s.status == DownloadManager.STATUS_PAUSED -> "Waiting for network"
        s.status == DownloadManager.STATUS_PENDING -> "Queued"
        s.status == DownloadManager.STATUS_RUNNING -> "Downloading"
        else -> "Failed"
    }
    val line = when {
        active && total > 0L -> {
            val sp = if (speed > 0L) "  |  ${sizeText(speed)}/s" else ""
            "$label  |  ${sizeText(got)} of ${sizeText(total)}  |  ${(frac * 100).toInt()}%$sp"
        }
        done -> "Completed  |  ${sizeText(total)}"
        else -> label
    }
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = r.cover.ifBlank { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(54.dp, 78.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF232733))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.anime.ifBlank { r.name }, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(r.name, fontSize = 12.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                if (active) {
                    if (total > 0L) {
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Text(line, fontSize = 11.sp, color = subColor)
            }
            if (done) {
                IconButton(onClick = onOpen) {
                    Icon(Icons.Filled.PlayArrow, "Play", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(if (active) Icons.Filled.Close else Icons.Filled.Delete, if (active) "Cancel" else "Delete")
            }
        }
    }
}

@Composable
fun SectionLabel(t: String) {
    Text(
        t, fontWeight = FontWeight.Bold, fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
}

@Composable
fun DownloadsTab() {
    val ctx = LocalContext.current
    var records by remember { mutableStateOf<List<DlRecord>>(emptyList()) }
    var states by remember { mutableStateOf<Map<Long, DlState>>(emptyMap()) }
    var speeds by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    LaunchedEffect(Unit) {
        var last = emptyMap<Long, Long>()
        while (true) {
            val recs = DlStore.load(ctx)
            val snap = withContext(Dispatchers.IO) { DlStore.states(ctx, recs.map { it.id }) }
            speeds = snap.mapValues { (id, s) -> (s.done - (last[id] ?: s.done)).coerceAtLeast(0L) }
            last = snap.mapValues { it.value.done }
            records = recs
            states = snap
            delay(1000)
        }
    }
    val active = records.filter { dlActive(states[it.id]) }
    val done = records.filter { dlDone(states[it.id]) }
    val other = records.filter { !dlActive(states[it.id]) && !dlDone(states[it.id]) }
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        item {
            Column(Modifier.padding(top = 14.dp, bottom = 4.dp)) {
                Text("Downloads", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Saved in Downloads/Anime on your phone", fontSize = 12.sp, color = subColor)
            }
        }
        if (records.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(84.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Download, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("No downloads yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Pick an anime and select files to download",
                        color = subColor, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (active.isNotEmpty()) {
            item { SectionLabel("In progress (${active.size})") }
            items(active, key = { it.id }) { r ->
                DownloadRow(
                    r, states[r.id], speeds[r.id] ?: 0L,
                    onOpen = {},
                    onDelete = {
                        DlStore.cancel(ctx, r.id)
                        records = records.filter { x -> x.id != r.id }
                    }
                )
            }
        }
        if (done.isNotEmpty()) {
            item { SectionLabel("Completed (${done.size})") }
            items(done, key = { it.id }) { r ->
                DownloadRow(
                    r, states[r.id], 0L,
                    onOpen = {
                        if (!DlStore.openFile(ctx, r.id)) {
                            Toast.makeText(ctx, "No video player found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = {
                        DlStore.cancel(ctx, r.id)
                        records = records.filter { x -> x.id != r.id }
                    }
                )
            }
        }
        if (other.isNotEmpty()) {
            item { SectionLabel("Failed or cancelled (${other.size})") }
            items(other, key = { it.id }) { r ->
                DownloadRow(
                    r, states[r.id], 0L,
                    onOpen = {},
                    onDelete = {
                        DlStore.cancel(ctx, r.id)
                        records = records.filter { x -> x.id != r.id }
                    }
                )
            }
        }
    }
}
