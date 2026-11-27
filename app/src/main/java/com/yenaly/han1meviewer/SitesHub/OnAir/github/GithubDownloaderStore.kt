package com.yenaly.han1meviewer.SitesHub.OnAir.github

import android.content.Context
import androidx.core.content.edit
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirCookieManager
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirNetwork
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GithubDownloaderStore {

    const val LINKS_FILE = "links_haniex.txt"
    const val COOKIES_FILE = "cookies_haniex.txt"
    const val DOWNLOADED_IDS_FILE = "downloaded_id.txt"
    const val COMMIT_TAG = "[haniex]"

    private val prefs by lazy {
        applicationContext.getSharedPreferences("onair_github_downloader", Context.MODE_PRIVATE)
    }

    var repoUrl: String
        get() = prefs.getString("repo_url", "").orEmpty()
        set(v) = prefs.edit { putString("repo_url", v.trim()) }

    var branch: String
        get() = prefs.getString("branch", "").orEmpty()
        set(v) = prefs.edit { putString("branch", v.trim()) }

    var token: String
        get() = prefs.getString("token", "").orEmpty()
        set(v) = prefs.edit { putString("token", v.trim()) }

    var authorName: String
        get() = prefs.getString("author_name", "").orEmpty()
        set(v) = prefs.edit { putString("author_name", v.trim()) }

    var authorEmail: String
        get() = prefs.getString("author_email", "").orEmpty()
        set(v) = prefs.edit { putString("author_email", v.trim()) }

    var repoReady: Boolean
        get() = prefs.getBoolean("repo_ready", false)
        set(v) = prefs.edit { putBoolean("repo_ready", v) }

    var lastSync: Long
        get() = prefs.getLong("last_sync", 0L)
        set(v) = prefs.edit { putLong("last_sync", v) }

    fun parseRepo(url: String): Pair<String, String>? {
        val m = Regex("""github\.com[/:]([^/\s]+)/([^/\s#?]+)""", RegexOption.IGNORE_CASE)
            .find(url.trim()) ?: return null
        val owner = m.groupValues[1]
        val repo = m.groupValues[2].removeSuffix(".git")
        if (owner.isBlank() || repo.isBlank()) return null
        return owner to repo
    }

    private fun root(): File = File(applicationContext.filesDir, "github_downloader").apply { mkdirs() }
    fun repoDir(): File = File(root(), "repo")
    private fun localLinksFile(): File = File(root(), LINKS_FILE)

    private val _queued = MutableStateFlow<Set<String>>(emptySet())
    val queuedIds: StateFlow<Set<String>> get() = _queued

    private val _repoDownloaded = MutableStateFlow<Set<String>>(emptySet())
    val repoDownloadedIds: StateFlow<Set<String>> get() = _repoDownloaded

    @Volatile
    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            reload()
            loaded = true
        }
    }

    fun reload() {
        _queued.value = readLinks().mapNotNull { extractId(it) }.toSet()
        _repoDownloaded.value = runCatching {
            File(repoDir(), DOWNLOADED_IDS_FILE).readLines()
                .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
        }.getOrDefault(emptySet())
    }

    fun extractId(line: String): String? =
        Regex("""/(?:watch|video)/(\d+)""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1)
            ?: line.trim().takeIf { it.matches(Regex("\\d{1,12}")) }

    fun linkFor(videoId: String): String =
        "${OnAirNetwork.baseUrl.trimEnd('/')}/watch/$videoId?quality=original"

    fun readLinks(): List<String> = runCatching {
        localLinksFile().takeIf { it.exists() }?.readLines().orEmpty()
            .map { it.trim() }.filter { it.isNotEmpty() }
    }.getOrDefault(emptyList())

    fun writeLinks(lines: List<String>) {
        localLinksFile().writeText(lines.joinToString("\n") + if (lines.isEmpty()) "" else "\n")
        reload()
    }

    @Synchronized
    fun addVideo(videoId: String): Boolean {
        ensureLoaded()
        if (videoId in _queued.value || videoId in _repoDownloaded.value) return false
        writeLinks(readLinks() + linkFor(videoId))
        return true
    }

    @Synchronized
    fun setLinksFromText(text: String): Int {
        var dropped = 0
        val byId = LinkedHashMap<String, String>()
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            val id = extractId(line)
            if (id == null) dropped++
            else byId.putIfAbsent(id, if (line.startsWith("http", true)) line else linkFor(id))
        }
        writeLinks(byId.values.toList())
        return dropped
    }

    @Synchronized
    fun removeVideo(videoId: String) {
        writeLinks(readLinks().filterNot { extractId(it) == videoId })
    }

    fun cookiesAsJson(): String? {
        runCatching { OnAirCookieManager.syncFromJar() }
        val raw = OnAirCookieManager.cookiesFlow.value.ifBlank { OnAirCookieManager.cookiesRaw }.trim()
        if (raw.isBlank()) return null
        val cookies = runCatching { OnAirCookieManager.parseToCookies(raw, OnAirNetwork.baseUrl) }
            .getOrDefault(emptyList())
        if (cookies.isEmpty()) return null
        val arr = JSONArray()
        cookies.forEach { c ->
            arr.put(JSONObject().apply {
                put("domain", c.domain)
                put("hostOnly", c.hostOnly)
                put("name", c.name)
                put("value", c.value)
                put("path", c.path)
                put("secure", c.secure)
                put("httpOnly", c.httpOnly)
                if (c.persistent) put("expiresAt", c.expiresAt)
            })
        }
        return arr.toString(2)
    }
}
