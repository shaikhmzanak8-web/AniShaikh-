package com.mzanak.animedl

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SearchTab(m: SearchModel, st: Prefs, onPick: (Anime) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    fun run() {
        if (m.q.startsWith("http")) return
        scope.launch {
            m.loading = true
            m.res = try { Api.search(m.q.trim().ifBlank { null }, m.genre) } catch (e: Exception) { emptyList() }
            m.loading = false
            m.searched = true
        }
    }
    LaunchedEffect(Unit) { if (!m.searched) run() }
    Column(Modifier.fillMaxSize()) {
        Text(
            "Browse",
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 10.dp),
            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold
        )
        OutlinedTextField(
            value = m.q,
            onValueChange = { text: String -> m.q = text },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Search anime or paste a direct link") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (m.q.isNotEmpty()) {
                    IconButton(onClick = { m.q = ""; run() }) { Icon(Icons.Filled.Close, "Clear") }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { run() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GENRES) { g ->
                FilterChip(
                    selected = m.genre == g,
                    onClick = { m.genre = if (m.genre == g) null else g; run() },
                    label = { Text(g) }
                )
            }
        }
        if (m.q.startsWith("http")) {
            Button(
                onClick = {
                    val u = m.q.trim()
                    val nm = u.substringBefore('?').substringAfterLast('/').ifBlank { "file" }
                    Api.enqueue(ctx, DlFile("", nm, u, 0L), st.wifiOnly, "Direct link", "")
                    Toast.makeText(ctx, "Download started", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Filled.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("Download this link")
            }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                m.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                m.searched && m.res.isEmpty() -> Text(
                    "No results", Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(m.res, key = { it.id }) { PosterCard(it, Modifier.fillMaxWidth(), onPick) }
                }
            }
        }
    }
}
