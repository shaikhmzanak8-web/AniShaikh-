package com.mzanak.animedl

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

data class DlRecord(val id: Long, val name: String, val anime: String, val cover: String)
data class DlState(val status: Int, val done: Long, val total: Long)

fun sizeText(b: Long): String = when {
    b <= 0L -> "0 MB"
    b >= 1_000_000_000L -> "%.2f GB".format(b / 1e9)
    else -> "%.1f MB".format(b / 1e6)
}

fun dlActive(s: DlState?): Boolean =
    s != null && (s.status == DownloadManager.STATUS_RUNNING ||
        s.status == DownloadManager.STATUS_PENDING ||
        s.status == DownloadManager.STATUS_PAUSED)

fun dlDone(s: DlState?): Boolean = s != null && s.status == DownloadManager.STATUS_SUCCESSFUL

object DlStore {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("downloads", Context.MODE_PRIVATE)

    fun load(ctx: Context): List<DlRecord> {
        val raw = prefs(ctx).getString("list", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DlRecord(o.getLong("id"), o.getString("name"), o.optString("anime", ""), o.optString("cover", ""))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(ctx: Context, list: List<DlRecord>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name).put("anime", it.anime).put("cover", it.cover))
        }
        prefs(ctx).edit().putString("list", arr.toString()).apply()
    }

    fun add(ctx: Context, r: DlRecord) {
        save(ctx, listOf(r) + load(ctx))
    }

    fun forget(ctx: Context, id: Long) {
        save(ctx, load(ctx).filter { it.id != id })
    }

    fun states(ctx: Context, ids: List<Long>): Map<Long, DlState> {
        if (ids.isEmpty()) return emptyMap()
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val out = HashMap<Long, DlState>()
        try {
            val c = dm.query(DownloadManager.Query().setFilterById(*ids.toLongArray()))
            if (c != null) {
                c.use {
                    val iId = it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                    val iStatus = it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    val iDone = it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val iTotal = it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    while (it.moveToNext()) {
                        out[it.getLong(iId)] = DlState(it.getInt(iStatus), it.getLong(iDone), it.getLong(iTotal))
                    }
                }
            }
        } catch (e: Exception) {
            // ignore, the list simply shows no progress this tick
        }
        return out
    }

    fun cancel(ctx: Context, id: Long) {
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        try {
            dm.remove(id)
        } catch (e: Exception) {
            // already gone
        }
        forget(ctx, id)
    }

    fun openFile(ctx: Context, id: Long): Boolean {
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(id) ?: return false
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "video/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }
}
