package com.mzanak.animedl

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Anime(
    val id: Int,
    val title: String,
    val cover: String,
    val banner: String,
    val episodes: Int?,
    val status: String,
    val score: Int?,
    val year: Int?
)

data class Link(val site: String, val url: String)
data class Detail(val desc: String, val genres: List<String>, val links: List<Link>)
data class DlFile(val itemTitle: String, val name: String, val url: String, val size: Long)

val SECTIONS = listOf(
    "trending" to "Trending Now",
    "airing" to "Airing Now",
    "recent" to "Recent Releases",
    "popular" to "Most Popular",
    "top" to "Top Rated",
    "upcoming" to "Coming Soon"
)

private fun JSONObject.str(key: String): String {
    val v = optString(key, "")
    return if (v == "null") "" else v
}

private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)

object Api {
    private const val ANILIST = "https://graphql.anilist.co"
    private const val FIELDS = "id title{romaji english} coverImage{large} bannerImage episodes status averageScore seasonYear"
    private val VIDEO = listOf(".mp4", ".mkv", ".avi", ".ogv", ".webm", ".m4v")

    private suspend fun http(url: String, body: String? = null): String = withContext(Dispatchers.IO) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 25000
        c.setRequestProperty("User-Agent", "AniShaikh/2.0")
        c.setRequestProperty("Accept", "application/json")
        if (body != null) {
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun body(query: String, vars: JSONObject? = null): String {
        val o = JSONObject().put("query", query)
        if (vars != null) o.put("variables", vars)
        return o.toString()
    }

    private fun statusLabel(s: String): String = when (s) {
        "RELEASING" -> "Airing"
        "FINISHED" -> "Finished"
        "NOT_YET_RELEASED" -> "Upcoming"
        "CANCELLED" -> "Cancelled"
        "HIATUS" -> "Hiatus"
        else -> ""
    }

    private fun parse(arr: JSONArray): List<Anime> = (0 until arr.length()).map { i ->
        val m = arr.getJSONObject(i)
        val t = m.getJSONObject("title")
        val en = t.str("english")
        Anime(
            id = m.getInt("id"),
            title = if (en.isNotBlank()) en else t.str("romaji"),
            cover = m.getJSONObject("coverImage").str("large"),
            banner = m.str("bannerImage"),
            episodes = m.intOrNull("episodes"),
            status = statusLabel(m.str("status")),
            score = m.intOrNull("averageScore"),
            year = m.intOrNull("seasonYear")
        )
    }

    private fun homeQuery(): String {
        fun part(alias: String, args: String) =
            "$alias:Page(perPage:20){media(type:ANIME,isAdult:false,$args){$FIELDS}}"
        return "query{" +
            part("trending", "sort:TRENDING_DESC") +
            part("airing", "status:RELEASING,sort:POPULARITY_DESC") +
            part("recent", "status:RELEASING,sort:START_DATE_DESC") +
            part("popular", "sort:POPULARITY_DESC") +
            part("top", "sort:SCORE_DESC") +
            part("upcoming", "status:NOT_YET_RELEASED,sort:POPULARITY_DESC") +
            "}"
    }

    private fun parseHome(raw: String): Map<String, List<Anime>> {
        val data = JSONObject(raw).getJSONObject("data")
        return SECTIONS.associate { (key, _) -> key to parse(data.getJSONObject(key).getJSONArray("media")) }
    }

    suspend fun cachedHome(cache: File): Map<String, List<Anime>>? = withContext(Dispatchers.IO) {
        try {
            if (cache.exists()) parseHome(cache.readText()) else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun home(cache: File): Map<String, List<Anime>> {
        val raw = http(ANILIST, body(homeQuery()))
        val parsed = parseHome(raw)
        withContext(Dispatchers.IO) { cache.writeText(raw) }
        return parsed
    }

    suspend fun search(q: String?, genre: String?): List<Anime> {
        val sort = if (q.isNullOrBlank()) "POPULARITY_DESC" else "SEARCH_MATCH"
        val query = "query(\$s:String,\$g:String){Page(perPage:30){media(type:ANIME,isAdult:false,search:\$s,genre:\$g,sort:$sort){$FIELDS}}}"
        val vars = JSONObject()
        if (!q.isNullOrBlank()) vars.put("s", q)
        if (genre != null) vars.put("g", genre)
        val arr = JSONObject(http(ANILIST, body(query, vars)))
            .getJSONObject("data").getJSONObject("Page").getJSONArray("media")
        return parse(arr)
    }

    suspend fun detail(id: Int): Detail {
        val query = "query(\$id:Int){Media(id:\$id){description(asHtml:false) genres externalLinks{site url type}}}"
        val m = JSONObject(http(ANILIST, body(query, JSONObject().put("id", id))))
            .getJSONObject("data").getJSONObject("Media")
        val desc = m.str("description")
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .trim()
        val gs = m.optJSONArray("genres")
        val ls = m.optJSONArray("externalLinks")
        val genres: List<String> =
            if (gs == null) emptyList() else (0 until gs.length()).map { gs.getString(it) }
        val links: List<Link> =
            if (ls == null) emptyList()
            else (0 until ls.length()).map { ls.getJSONObject(it) }
                .filter { it.str("type") == "STREAMING" && it.str("url").isNotBlank() }
                .map { Link(it.str("site"), it.str("url")) }
                .distinctBy { it.site }
        return Detail(desc, genres, links)
    }

    // Legal, freely downloadable videos from the Internet Archive
    suspend fun archive(title: String): List<DlFile> {
        val clean = title.replace("\"", "").replace(":", " ")
        val q = URLEncoder.encode("title:(\"$clean\") AND mediatype:movies AND subject:(anime OR animation)", "UTF-8")
        val docs = JSONObject(
            http("https://archive.org/advancedsearch.php?q=$q&fl%5B%5D=identifier&fl%5B%5D=title&rows=5&output=json")
        ).getJSONObject("response").getJSONArray("docs")
        val out = mutableListOf<DlFile>()
        for (i in 0 until docs.length()) {
            val d = docs.getJSONObject(i)
            val id = d.getString("identifier")
            val itemTitle = d.optString("title", id)
            val files = try {
                JSONObject(http("https://archive.org/metadata/$id")).getJSONArray("files")
            } catch (e: Exception) {
                continue
            }
            for (j in 0 until files.length()) {
                val f = files.getJSONObject(j)
                val n = f.getString("name")
                if (VIDEO.any { n.endsWith(it, true) }) {
                    val enc = n.split("/").joinToString("/") { s -> URLEncoder.encode(s, "UTF-8").replace("+", "%20") }
                    out += DlFile(itemTitle, n, "https://archive.org/download/$id/$enc", f.optString("size", "0").toLongOrNull() ?: 0L)
                }
            }
        }
        return out
            .sortedBy { f -> VIDEO.indexOfFirst { e -> f.name.endsWith(e, true) } }
            .distinctBy { it.name.substringBeforeLast('.').lowercase() }
            .take(200)
    }

    // audio: filenames/titles containing "dub" count as dubbed. quality: 0 saver, 1 standard, 2 high = file size caps
    fun filter(all: List<DlFile>, dub: Boolean, quality: Int): List<DlFile> {
        val byAudio = all.filter {
            val d = (it.name + it.itemTitle).lowercase().contains("dub")
            if (dub) d else !d
        }
        val cap = longArrayOf(150_000_000L, 600_000_000L, Long.MAX_VALUE)[quality.coerceIn(0, 2)]
        val capped = byAudio.filter { it.size <= cap }
        return (capped.ifEmpty { byAudio }).sortedBy { it.name }
    }

    fun fmt(b: Long): String = when {
        b <= 0 -> ""
        b >= 1_000_000_000 -> "%.1f GB".format(b / 1e9)
        else -> "%.0f MB".format(b / 1e6)
    }

    fun enqueue(ctx: Context, f: DlFile, wifiOnly: Boolean, anime: String, cover: String): Long {
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = f.name.substringAfterLast('/')
        val r = DownloadManager.Request(Uri.parse(f.url))
            .setTitle(fileName)
            .setDescription(anime.ifBlank { "Ani Shaikh" })
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Anime/$fileName")
        if (wifiOnly) r.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        val id = dm.enqueue(r)
        DlStore.add(ctx, DlRecord(id, fileName, anime, cover))
        return id
    }
}
