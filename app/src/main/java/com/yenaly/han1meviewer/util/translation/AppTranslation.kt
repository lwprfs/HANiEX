package com.yenaly.han1meviewer.util.translation

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.base.AppLocale
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AppTranslation {

    enum class Kind(val key: String, val maxEntries: Int, val long: Boolean = false) {
        TAG("tag", 2000),
        TITLE("title", 3000),
        DESC("desc", 300, long = true),
        COMMENT("comment", 1000, long = true),
    }

    data class Entry(
        val site: SiteType,
        val kind: Kind,
        val source: String,
        val text: String,
        val time: Long,
        val edited: Boolean = false,
    )

    data class Stats(
        val tags: Int,
        val titles: Int,
        val descriptions: Int,
        val comments: Int,
        val bytes: Long,
    ) {
        val total: Int get() = tags + titles + descriptions + comments
    }

    private data class Pending(val site: SiteType, val kind: Kind, val source: String)

    // Legacy (pre per-site) preference keys. They are only read as defaults so that
    // people who already configured translation keep their choices on every site.
    private const val LEGACY_ENABLED = "rouvideo_tr_enabled"
    private const val LEGACY_TAGS = "rouvideo_tr_tags"
    private const val LEGACY_TITLES = "rouvideo_tr_titles"
    private const val LEGACY_DESC = "rouvideo_tr_desc"
    private const val LEGACY_COMMENTS = "rouvideo_tr_comments"
    private const val LEGACY_SITES_OFF = "rouvideo_tr_sites_off"
    private const val LEGACY_FILE = "rouvideo_translations.json"

    private const val BATCH_ITEMS = 15
    private const val BATCH_CHARS = 500
    private const val MAX_PENDING = 300
    private const val RATE_LIMIT_PAUSE_MS = 10 * 60_000L
    private const val ERROR_PAUSE_MS = 30_000L
    private const val GAP_MS = 400L

    private val FOREIGN = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]")

    private val sp get() = Preferences.preferenceSp

    /** Sites that have translation, in the order they are shown in settings. */
    val SUPPORTED_SITES: List<SiteType> =
        listOf(SiteType.HANIME, SiteType.JAVCHU, SiteType.ROUVIDEO, SiteType.WUMAOBI)

    /** The kinds of text that a site actually shows, so settings only list what can work. */
    fun kindsFor(site: SiteType): List<Kind> = when (site) {
        SiteType.ROUVIDEO -> listOf(Kind.TAG, Kind.TITLE, Kind.DESC, Kind.COMMENT)
        SiteType.WUMAOBI -> listOf(Kind.TAG, Kind.TITLE)
        else -> listOf(Kind.TITLE, Kind.DESC, Kind.COMMENT)
    }

    /** Settings of one site. Each site has its own switches. */
    class SiteConfig internal constructor(val site: SiteType) {
        private fun k(name: String) = "tr_${site.name.lowercase()}_$name"

        private fun legacyOn(): Boolean {
            val off = sp.getString(LEGACY_SITES_OFF, null).orEmpty().split(',')
            return sp.getBoolean(LEGACY_ENABLED, true) && site.name !in off
        }

        private val enabledState = mutableStateOf(sp.getBoolean(k("enabled"), legacyOn()))
        private val tagsState = mutableStateOf(sp.getBoolean(k("tags"), sp.getBoolean(LEGACY_TAGS, true)))
        private val titlesState = mutableStateOf(
            sp.getBoolean(k("titles"), site == SiteType.WUMAOBI || sp.getBoolean(LEGACY_TITLES, false))
        )
        private val descState = mutableStateOf(sp.getBoolean(k("desc"), sp.getBoolean(LEGACY_DESC, false)))
        private val commentsState = mutableStateOf(
            sp.getBoolean(k("comments"), sp.getBoolean(LEGACY_COMMENTS, false))
        )

        var enabled: Boolean
            get() = enabledState.value
            set(v) { enabledState.value = v; sp.edit().putBoolean(k("enabled"), v).apply() }

        var tags: Boolean
            get() = tagsState.value
            set(v) { tagsState.value = v; sp.edit().putBoolean(k("tags"), v).apply() }

        var titles: Boolean
            get() = titlesState.value
            set(v) { titlesState.value = v; sp.edit().putBoolean(k("titles"), v).apply() }

        var descriptions: Boolean
            get() = descState.value
            set(v) { descState.value = v; sp.edit().putBoolean(k("desc"), v).apply() }

        var comments: Boolean
            get() = commentsState.value
            set(v) { commentsState.value = v; sp.edit().putBoolean(k("comments"), v).apply() }

        fun isOn(kind: Kind): Boolean = when (kind) {
            Kind.TAG -> tags
            Kind.TITLE -> titles
            Kind.DESC -> descriptions
            Kind.COMMENT -> comments
        }

        fun set(kind: Kind, on: Boolean) {
            when (kind) {
                Kind.TAG -> tags = on
                Kind.TITLE -> titles = on
                Kind.DESC -> descriptions = on
                Kind.COMMENT -> comments = on
            }
        }
    }

    private val configs = HashMap<SiteType, SiteConfig>()

    @Synchronized
    fun config(site: SiteType): SiteConfig = configs.getOrPut(site) { SiteConfig(site) }

    fun siteEnabled(site: SiteType): Boolean =
        site in SUPPORTED_SITES && config(site).enabled

    fun setSiteEnabled(site: SiteType, on: Boolean) {
        if (site in SUPPORTED_SITES) config(site).enabled = on
    }

    val available: Boolean
        get() = AppLocale.preferredLanguage.language.equals("en", ignoreCase = true)

    fun isActive(kind: Kind, site: SiteType = SiteType.ROUVIDEO): Boolean =
        available && site in SUPPORTED_SITES && kind in kindsFor(site) &&
            config(site).let { it.enabled && it.isOn(kind) }

    private val lock = Any()
    private val entries = HashMap<SiteType, LinkedHashMap<String, Entry>>()
    private val pending = LinkedHashMap<String, Pending>()
    private val failed = HashSet<String>()
    private val deriveQueue = ArrayDeque<Pending>()
    private val deriveTried = HashSet<String>()
    private val dirty = HashSet<SiteType>()
    private var deriveDirty = true

    private val pieces = object : LinkedHashMap<String, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > 500
    }

    private val version = mutableIntStateOf(0)

    @Volatile
    private var loaded = false

    @Volatile
    private var pausedUntil = 0L

    @Volatile
    private var usedNetwork = false

    private var worker: Job? = null
    private var saveJob: Job? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .dns(HDns())
            .proxySelector(HProxySelector())
            .build()
    }

    private fun fileOf(site: SiteType): File =
        File(applicationContext.filesDir, "translations_${site.name.lowercase()}.json")

    private fun siteEntries(site: SiteType): LinkedHashMap<String, Entry> =
        entries.getOrPut(site) { LinkedHashMap() }

    /** Key inside one site's entry map. */
    private fun key(kind: Kind, source: String) = kind.key + "\u0001" + source

    /** Key for the global queues (pending / failed / tried): includes the site. */
    private fun gkey(site: SiteType, kind: Kind, source: String) =
        site.name + "\u0002" + key(kind, source)

    fun title(text: String, site: SiteType = SiteType.ROUVIDEO): String = shown(Kind.TITLE, text, site)

    fun description(text: String, site: SiteType = SiteType.ROUVIDEO): String =
        shown(Kind.DESC, text, site)

    fun comment(text: String, site: SiteType = SiteType.ROUVIDEO): String =
        shown(Kind.COMMENT, text, site)

    fun tag(text: String, site: SiteType = SiteType.ROUVIDEO): String = shown(Kind.TAG, text, site)

    private fun shown(kind: Kind, text: String, site: SiteType): String {
        if (text.isBlank() || !FOREIGN.containsMatchIn(text) || !isActive(kind, site)) return text
        return translate(kind, text, site)
    }

    fun translate(kind: Kind, source: String, site: SiteType = SiteType.ROUVIDEO): String {
        version.intValue
        ensureLoaded()
        val k = key(kind, source)
        val g = gkey(site, kind, source)
        synchronized(lock) {
            entries[site]?.get(k)?.let { return it.text }
            if (FOREIGN.containsMatchIn(source) && g !in failed) {
                if (g !in pending) deriveDirty = true
                pending.remove(g)
                pending[g] = Pending(site, kind, source)
                while (pending.size > MAX_PENDING) pending.remove(pending.keys.first())
            } else {
                return source
            }
        }
        schedule()
        return source
    }

    fun pausedForSeconds(): Long {
        version.intValue
        return ((pausedUntil - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
    }

    fun stats(site: SiteType): Stats {
        version.intValue
        ensureLoaded()
        var t = 0; var ti = 0; var d = 0; var c = 0
        synchronized(lock) {
            entries[site]?.values?.forEach {
                when (it.kind) {
                    Kind.TAG -> t++
                    Kind.TITLE -> ti++
                    Kind.DESC -> d++
                    Kind.COMMENT -> c++
                }
            }
        }
        return Stats(t, ti, d, c, runCatching { fileOf(site).length() }.getOrDefault(0L))
    }

    fun snapshot(site: SiteType, kind: Kind? = null): List<Entry> {
        version.intValue
        ensureLoaded()
        return synchronized(lock) {
            entries[site]?.values.orEmpty().filter { kind == null || it.kind == kind }
        }.sortedByDescending { it.time }
    }

    fun delete(entry: Entry) {
        synchronized(lock) {
            entries[entry.site]?.remove(key(entry.kind, entry.source))
            val g = gkey(entry.site, entry.kind, entry.source)
            failed.remove(g)
            deriveTried.remove(g)
            dirty.add(entry.site)
        }
        changed()
    }

    /**
     * Replaces the translated text of a cached entry with what the user typed. The entry is
     * flagged as edited; it is kept until deleted or cleared and is never re-translated.
     */
    fun edit(entry: Entry, newText: String) {
        val text = newText.trim()
        if (text.isEmpty()) return
        synchronized(lock) {
            val map = siteEntries(entry.site)
            val k = key(entry.kind, entry.source)
            val current = map[k] ?: entry
            if (current.text == text) return
            map[k] = current.copy(text = text, time = System.currentTimeMillis(), edited = true)
            val g = gkey(entry.site, entry.kind, entry.source)
            failed.remove(g)
            pending.remove(g)
            dirty.add(entry.site)
            deriveDirty = true
        }
        changed()
    }

    fun clear(site: SiteType, kind: Kind? = null) {
        synchronized(lock) {
            val prefix = site.name + "\u0002"
            if (kind == null) {
                entries[site]?.clear()
                failed.removeAll { it.startsWith(prefix) }
                pending.keys.removeAll { it.startsWith(prefix) }
                deriveQueue.removeAll { it.site == site }
                deriveTried.removeAll { it.startsWith(prefix) }
            } else {
                entries[site]?.entries?.removeAll { it.value.kind == kind }
                failed.removeAll { it.startsWith(prefix + kind.key + "\u0001") }
                deriveTried.removeAll { it.startsWith(prefix + kind.key + "\u0001") }
            }
            dirty.add(site)
            deriveDirty = true
        }
        pausedUntil = 0L
        changed()
    }

    private fun schedule() {
        synchronized(lock) {
            if (worker?.isActive == true) return
            worker = scope.launch {
                delay(300)
                while (true) {
                    val wait = pausedUntil - System.currentTimeMillis()
                    if (wait > 0) delay(wait)
                    if (!processNext()) break
                    if (usedNetwork) {
                        usedNetwork = false
                        delay(GAP_MS)
                    }
                }
            }
        }
    }

    private fun processNext(): Boolean {

        val derive = synchronized(lock) {
            if (deriveQueue.isEmpty() && deriveDirty) {
                deriveDirty = false
                val hits = pending.entries.toList().asReversed().filter { (g, v) ->
                    v.kind == Kind.TITLE && g !in deriveTried && isActive(v.kind, v.site) &&
                        findNeighbor(v.site, v.source) != null
                }
                hits.forEach { pending.remove(it.key); deriveQueue.addLast(it.value) }
            }
            deriveQueue.removeFirstOrNull()
        }
        if (derive != null) {
            try {
                val text = deriveFromNeighbor(derive.site, derive.source)
                synchronized(lock) {
                    if (text != null) {
                        putEntry(derive.site, derive.kind, derive.source, text)
                    } else {
                        val g = gkey(derive.site, derive.kind, derive.source)
                        deriveTried.add(g)
                        pending[g] = derive
                    }
                }
                if (text != null) changed()
            } catch (e: Exception) {
                onNetworkError(e, listOf(derive))
            }
            return true
        }

        val group = synchronized(lock) {
            val candidates = pending.entries.toList().asReversed()
                .filter { isActive(it.value.kind, it.value.site) }
            pending.keys.retainAll(candidates.map { it.key }.toSet())
            val first = candidates.firstOrNull() ?: return false
            val picked = mutableListOf(first)
            if (!first.value.kind.long) {
                var chars = first.value.source.length
                for (c in candidates.drop(1)) {
                    if (picked.size >= BATCH_ITEMS) break
                    val cv = c.value
                    if (cv.kind.long) continue
                    if (chars + cv.source.length > BATCH_CHARS) continue

                    if (cv.kind == Kind.TITLE && c.key !in deriveTried &&
                        picked.any {
                            it.value.kind == Kind.TITLE && it.value.site == cv.site &&
                                split(cv.source, it.value.source) != null
                        }
                    ) continue
                    picked += c; chars += cv.source.length
                }
            }
            picked.forEach { pending.remove(it.key) }
            picked.map { it.value }
        }
        try {
            val out = if (group.first().kind.long) {
                listOf(translateLong(group.first().source))
            } else {
                translateGroup(group.map { it.source.replace('\n', ' ') })
            }
            synchronized(lock) {
                group.forEachIndexed { i, p ->
                    val text = out.getOrNull(i).orEmpty().trim()
                    if (text.isEmpty() || text == p.source) {
                        failed.add(gkey(p.site, p.kind, p.source))
                    } else if (siteEntries(p.site)[key(p.kind, p.source)]?.edited != true) {
                        putEntry(p.site, p.kind, p.source, text)
                    }
                }
            }
            changed()
        } catch (e: Exception) {
            onNetworkError(e, group)
        }
        return true
    }

    private fun onNetworkError(e: Exception, group: List<Pending>) {
        synchronized(lock) {
            group.forEach { p ->
                if (pending.size < MAX_PENDING) {
                    pending.putIfAbsent(gkey(p.site, p.kind, p.source), p)
                }
            }
        }
        pausedUntil = System.currentTimeMillis() +
            if (e is RateLimited) RATE_LIMIT_PAUSE_MS else ERROR_PAUSE_MS
        version.intValue++
    }

    private fun putEntry(site: SiteType, kind: Kind, source: String, text: String) {
        val map = siteEntries(site)
        val k = key(kind, source)
        map.remove(k)
        map[k] = Entry(
            site, kind, source,
            if (kind == Kind.TAG) titleCase(text) else text,
            System.currentTimeMillis(),
        )
        dirty.add(site)
        deriveDirty = true
        Kind.entries.forEach { trim(site, it) }
    }

    private fun trim(site: SiteType, kind: Kind) {
        val map = siteEntries(site)
        var count = map.values.count { it.kind == kind }
        if (count <= kind.maxEntries) return
        val it = map.entries.iterator()
        while (it.hasNext() && count > kind.maxEntries) {
            if (it.next().value.kind == kind) { it.remove(); count-- }
        }
    }

    private fun titleCase(s: String): String =
        s.split(' ').joinToString(" ") { w ->
            if (w.isEmpty() || w.any { it.isUpperCase() }) w else w.replaceFirstChar { it.uppercase() }
        }

    private class Split(val dX: String, val dS: String, val prefix: Int, val suffix: Int)

    private fun split(x: String, s: String): Split? {
        if (x == s) return null
        val maxShared = minOf(x.length, s.length)
        var p = 0
        while (p < maxShared && x[p] == s[p]) p++
        var q = 0
        while (q < maxShared - p && x[x.length - 1 - q] == s[s.length - 1 - q]) q++

        fun digit(str: String, i: Int) = i in str.indices && str[i].isDigit()
        while (p > 0 && digit(x, p - 1) && (digit(x, p) || digit(s, p))) p--
        while (q > 0 && digit(x, x.length - q) &&
            (digit(x, x.length - q - 1) || digit(s, s.length - q - 1))
        ) q--

        val shared = p + q
        if (shared < 4 || shared * 2 < x.length || shared * 2 < s.length) return null
        val dS = s.substring(p, s.length - q)
        if (dS.isEmpty()) return null
        return Split(x.substring(p, x.length - q), dS, p, q)
    }

    private fun findNeighbor(site: SiteType, x: String): Pair<Entry, Split>? {
        var best: Pair<Entry, Split>? = null
        var bestShared = -1
        for (e in entries[site]?.values.orEmpty()) {
            if (e.kind != Kind.TITLE) continue
            val sp = split(x, e.source) ?: continue
            val shared = sp.prefix + sp.suffix
            if (shared > bestShared) { best = e to sp; bestShared = shared }
        }
        return best
    }

    private fun deriveFromNeighbor(site: SiteType, x: String): String? {
        val (neighbor, sp) = synchronized(lock) { findNeighbor(site, x) } ?: return null
        val trS = fragment(sp.dS)
        val trX = fragment(sp.dX)
        val text = neighbor.text
        val digits = trS.isNotEmpty() && trS.all { it.isDigit() }
        val result = if (digits) {
            val re = Regex("(?<!\\d)" + Regex.escape(trS) + "(?!\\d)")
            if (re.findAll(text).count() != 1) return null
            text.replace(re, Regex.escapeReplacement(trX))
        } else {
            if (trS.isEmpty()) return null
            val first = text.indexOf(trS)
            if (first < 0 || text.indexOf(trS, first + 1) >= 0) return null
            text.substring(0, first) + trX + text.substring(first + trS.length)
        }
        return result.replace(Regex("\\s{2,}"), " ").trim().takeIf { it.isNotEmpty() }
    }

    private fun fragment(piece: String): String {
        if (piece.isEmpty() || !FOREIGN.containsMatchIn(piece)) return piece
        synchronized(lock) { pieces[piece]?.let { return it } }
        val out = request(piece.replace('\n', ' ')).joinToString(" ").trim()
        synchronized(lock) { pieces[piece] = out }
        return out
    }

    private class RateLimited : java.io.IOException("HTTP 429")

    private fun request(text: String): List<String> {
        usedNetwork = true
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t" +
            "&q=" + URLEncoder.encode(text, "UTF-8")
        val body = client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (r.code == 429) throw RateLimited()
            if (!r.isSuccessful) throw java.io.IOException("HTTP ${r.code}")
            r.body.string()
        }
        val segments = JSONArray(body).getJSONArray(0)
        val sb = StringBuilder()
        for (i in 0 until segments.length()) {
            val seg = segments.optJSONArray(i) ?: continue
            if (!seg.isNull(0)) sb.append(seg.getString(0))
        }
        return sb.toString().replace("\r\n", "\n").trimEnd('\n').split('\n')
    }

    private fun translateGroup(texts: List<String>): List<String> {
        val out = request(texts.joinToString("\n"))
        if (out.size == texts.size) return out
        if (texts.size == 1) return listOf(out.joinToString(" "))
        val mid = texts.size / 2
        return translateGroup(texts.subList(0, mid)) + translateGroup(texts.subList(mid, texts.size))
    }

    private fun translateLong(text: String): String {
        val chunks = mutableListOf<String>()
        val cur = StringBuilder()
        fun flushChunk() { if (cur.isNotEmpty()) { chunks += cur.toString(); cur.clear() } }
        for (line in text.split('\n')) {
            var rest = line
            while (rest.length > 450) {
                flushChunk(); chunks += rest.take(450); rest = rest.drop(450)
            }
            if (cur.length + rest.length + 1 > 450) flushChunk()
            if (cur.isNotEmpty()) cur.append('\n')
            cur.append(rest)
        }
        flushChunk()
        return chunks.joinToString("\n") { request(it).joinToString("\n") }
    }

    private fun changed() {
        version.intValue++
        synchronized(lock) {
            if (saveJob?.isActive == true) return
            saveJob = scope.launch {
                delay(1000)
                save()
            }
        }
    }

    private fun save() {
        runCatching {
            val toSave = synchronized(lock) {
                val sites = dirty.toList()
                dirty.clear()
                sites.associateWith { site ->
                    JSONArray().also { arr ->
                        entries[site]?.values?.forEach {
                            arr.put(
                                JSONObject().put("k", it.kind.key).put("s", it.source)
                                    .put("t", it.text).put("ts", it.time).put("e", it.edited)
                            )
                        }
                    }
                }
            }
            toSave.forEach { (site, arr) ->
                val f = fileOf(site)
                if (arr.length() == 0) f.delete() else f.writeText(arr.toString(), Charsets.UTF_8)
            }
            // The old shared cache has been split into per-site files by now.
            File(applicationContext.filesDir, LEGACY_FILE).delete()
        }
        version.intValue++
    }

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            // Entries cached before translation became per-site all belong to RouVideo,
            // the site this feature was written for.
            val legacy = File(applicationContext.filesDir, LEGACY_FILE)
            SUPPORTED_SITES.forEach { site ->
                val f = fileOf(site)
                val source = when {
                    f.exists() -> f
                    site == SiteType.ROUVIDEO && legacy.exists() -> legacy
                    else -> return@forEach
                }
                val arr = JSONArray(source.readText(Charsets.UTF_8))
                synchronized(lock) {
                    val map = siteEntries(site)
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val kind = Kind.entries.firstOrNull { it.key == o.getString("k") } ?: continue
                        val src = o.getString("s")
                        map[key(kind, src)] = Entry(
                            site, kind, src, o.getString("t"), o.optLong("ts"), o.optBoolean("e", false),
                        )
                    }
                    if (source === legacy) dirty.add(site)
                }
            }
            if (synchronized(lock) { dirty.isNotEmpty() }) {
                scope.launch {
                    delay(500)
                    save()
                }
            }
        }
    }
}
