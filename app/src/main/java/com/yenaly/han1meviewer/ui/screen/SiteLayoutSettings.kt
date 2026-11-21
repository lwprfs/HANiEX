package com.yenaly.han1meviewer.ui.screen

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.util.applicationContext

object SiteLayoutSettings {

    const val FOLLOW_APP = 0
    val COLUMN_OPTIONS = listOf(2, 3, 4, 5, 6)

    val gridColumnSites: Set<SiteType> = setOf(
        SiteType.MISSAV,
        SiteType.ONAIR,
        SiteType.FAPFOLDER,
        SiteType.SUPJAV,
        SiteType.ROUVIDEO,
        SiteType.XHAMSTER,
        SiteType.BUONDUA,
        SiteType.FULLVIDEOS,
        SiteType.FUCKINGSESSION,
        SiteType.WUMAOBI,
        SiteType.XXXTUBE,
    )

    fun gridColumnsKey(site: SiteType): String = "site_${site.name.lowercase()}_grid_columns"

    fun gridColumns(site: SiteType): Int =
        Preferences.preferenceSp.getInt(gridColumnsKey(site), FOLLOW_APP)
            .takeIf { it in COLUMN_OPTIONS } ?: FOLLOW_APP

    fun setGridColumns(site: SiteType, columns: Int) {
        Preferences.preferenceSp.edit {
            if (columns in COLUMN_OPTIONS) putInt(gridColumnsKey(site), columns)
            else remove(gridColumnsKey(site))
        }
    }

    fun reset(site: SiteType) {
        Preferences.preferenceSp.edit { remove(gridColumnsKey(site)) }
        SiteCardSettings.reset(site)
        SiteHomeSettings.reset(site)
        SiteSearchSettings.reset(site)
        SiteImageSettings.reset(site)
        SiteImageCardSettings.reset(site)
        SiteShortsCardSettings.reset(site)
    }

    enum class Preset(
        val columns: Int,
        val shownDetails: Set<SiteCardSettings.Detail>,
    ) {
        Balanced(FOLLOW_APP, SiteCardSettings.Detail.entries.toSet()),
        Compact(3, setOf(SiteCardSettings.Detail.Views, SiteCardSettings.Detail.Duration)),
        Spacious(2, SiteCardSettings.Detail.entries.toSet()),
        Minimal(FOLLOW_APP, setOf(SiteCardSettings.Detail.Duration)),
    }

    fun applyPreset(site: SiteType, preset: Preset) {
        if (site in gridColumnSites) setGridColumns(site, preset.columns)
        if (site in SiteCardSettings.sites) {
            SiteCardSettings.Detail.entries.forEach {
                SiteCardSettings.setShown(site, it, it in preset.shownDetails)
            }
        }
    }
}

object SiteCardSettings {

    enum class Detail(val keySuffix: String) {
        Views("views"),
        Duration("duration"),
        Subtitle("subtitle"),
        Date("date"),
    }

    val sites: Set<SiteType> = setOf(
        SiteType.HENTAIMAMA,
        SiteType.ONAIR,
        SiteType.SUPJAV,
        SiteType.ROUVIDEO,
        SiteType.XHAMSTER,
        SiteType.FULLVIDEOS,
        SiteType.FUCKINGSESSION,
        SiteType.WUMAOBI,
        SiteType.XXXTUBE,
    )

    private var version by mutableIntStateOf(0)
    private var listening = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith("site_") && key.contains("_card_")) version++
    }

    private fun key(site: SiteType, detail: Detail) =
        "site_${site.name.lowercase()}_card_show_${detail.keySuffix}"

    fun isShown(site: SiteType, detail: Detail): Boolean {
        if (!listening) {
            Preferences.preferenceSp.registerOnSharedPreferenceChangeListener(listener)
            listening = true
        }
        version
        return Preferences.preferenceSp.getBoolean(key(site, detail), true)
    }

    enum class Aspect(val keyValue: String, val ratio: Float?) {
        Default("default", null),
        Wide("16_9", 16f / 9f),
        Classic("4_3", 4f / 3f),
        Square("1_1", 1f),
        Portrait("3_4", 3f / 4f),
    }

    private fun aspectKey(site: SiteType) = "site_${site.name.lowercase()}_card_aspect"

    fun aspect(site: SiteType): Aspect {
        isShown(site, Detail.Views)
        val stored = Preferences.preferenceSp.getString(aspectKey(site), null)
        return Aspect.entries.firstOrNull { it.keyValue == stored } ?: Aspect.Default
    }

    fun setAspect(site: SiteType, aspect: Aspect) {
        Preferences.preferenceSp.edit {
            if (aspect == Aspect.Default) remove(aspectKey(site))
            else putString(aspectKey(site), aspect.keyValue)
        }
    }

    fun setShown(site: SiteType, detail: Detail, shown: Boolean) {
        Preferences.preferenceSp.edit {
            if (shown) remove(key(site, detail)) else putBoolean(key(site, detail), false)
        }
    }

    fun reset(site: SiteType) {
        Preferences.preferenceSp.edit {
            Detail.entries.forEach { remove(key(site, it)) }
            remove(aspectKey(site))
        }
    }
}

@Composable
fun rememberSiteGridColumns(site: SiteType): Int {
    val sp = Preferences.preferenceSp
    var value by remember(site) { mutableIntStateOf(SiteLayoutSettings.gridColumns(site)) }
    DisposableEffect(site) {
        val key = SiteLayoutSettings.gridColumnsKey(site)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == key) value = SiteLayoutSettings.gridColumns(site)
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        value = SiteLayoutSettings.gridColumns(site)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return value
}

object SiteHomeSettings {

    enum class Section(val site: SiteType, val keySuffix: String) {
        FapFolderTrending(SiteType.FAPFOLDER, "trending"),
        FapFolderSuggested(SiteType.FAPFOLDER, "suggested"),
    }

    fun sectionsOf(site: SiteType): List<Section> = Section.entries.filter { it.site == site }

    private var version by mutableIntStateOf(0)
    private var listening = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith("site_") && key.contains("_home_show_")) version++
    }

    private fun key(section: Section) =
        "site_${section.site.name.lowercase()}_home_show_${section.keySuffix}"

    fun isShown(section: Section): Boolean {
        if (!listening) {
            Preferences.preferenceSp.registerOnSharedPreferenceChangeListener(listener)
            listening = true
        }
        version
        return Preferences.preferenceSp.getBoolean(key(section), true)
    }

    fun setShown(section: Section, shown: Boolean) {
        Preferences.preferenceSp.edit {
            if (shown) remove(key(section)) else putBoolean(key(section), false)
        }
    }

    val cacheSites: Set<SiteType> = setOf(SiteType.MISSAV, SiteType.HENTAIMAMA)
    val cacheMinuteChoices = listOf(1, 5, 15, 30, 60)
    const val DEFAULT_CACHE_MINUTES = 5

    private fun cacheKey(site: SiteType) = "site_${site.name.lowercase()}_home_cache_minutes"

    fun cacheMinutes(site: SiteType): Int =
        Preferences.preferenceSp.getInt(cacheKey(site), DEFAULT_CACHE_MINUTES)
            .takeIf { it in cacheMinuteChoices } ?: DEFAULT_CACHE_MINUTES

    fun cacheTtlMs(site: SiteType): Long = cacheMinutes(site) * 60_000L

    fun setCacheMinutes(site: SiteType, minutes: Int) {
        Preferences.preferenceSp.edit {
            if (minutes == DEFAULT_CACHE_MINUTES || minutes !in cacheMinuteChoices) remove(cacheKey(site))
            else putInt(cacheKey(site), minutes)
        }
    }

    fun reset(site: SiteType) {
        Preferences.preferenceSp.edit {
            sectionsOf(site).forEach { remove(key(it)) }
            remove(cacheKey(site))
        }
    }
}

object SiteSearchSettings {

    fun sortChoices(site: SiteType): List<Pair<String, String>>? = when (site) {
        SiteType.ONAIR -> com.yenaly.han1meviewer.OnAir.data.model.OnAirOptions.sorts
            .map { it.label to it.value }
        SiteType.MISSAV -> com.yenaly.han1meviewer.MissAV.common.MissAvOptions.sortOptions
            .map { it.name to it.searchKey }
        SiteType.ROUVIDEO -> com.yenaly.han1meviewer.RouVideo.common.RouVideoConstants.Sort.entries
            .map { applicationContext.getString(it.labelRes) to it.name }
        else -> null
    }

    private fun key(site: SiteType) = "site_${site.name.lowercase()}_default_sort"

    fun defaultSort(site: SiteType): String? {
        val stored = Preferences.preferenceSp.getString(key(site), null)
        return stored?.takeIf { value -> sortChoices(site)?.any { it.second == value } == true }
    }

    fun setDefaultSort(site: SiteType, value: String?) {
        Preferences.preferenceSp.edit {
            if (value.isNullOrBlank()) remove(key(site)) else putString(key(site), value)
        }
    }

    fun reset(site: SiteType) {
        Preferences.preferenceSp.edit { remove(key(site)) }
    }
}

object SiteImageSettings {

    class Options(val retries: Int, val fade: Boolean, val reduced: Boolean)

    val retryChoices = listOf(0, 2, 4)
    const val DEFAULT_RETRIES = 2

    const val REDUCED_EDGE_PX = 360

    val sites: Set<SiteType> = setOf(
        SiteType.MISSAV, SiteType.HENTAIMAMA, SiteType.ONAIR, SiteType.FAPFOLDER, SiteType.SUPJAV,
        SiteType.ROUVIDEO, SiteType.XHAMSTER, SiteType.BUONDUA, SiteType.COSPLAYTELE, SiteType.COSER,
        SiteType.FULLVIDEOS, SiteType.FUCKINGSESSION, SiteType.WUMAOBI, SiteType.XXXTUBE,
    )

    private var version by mutableIntStateOf(0)
    private var listening = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith("site_") && key.contains("_image_")) version++
    }

    private fun key(site: SiteType, name: String) = "site_${site.name.lowercase()}_image_$name"

    fun retries(site: SiteType): Int {
        observe()
        return Preferences.preferenceSp.getInt(key(site, "retries"), DEFAULT_RETRIES)
            .takeIf { it in retryChoices } ?: DEFAULT_RETRIES
    }

    fun fade(site: SiteType): Boolean {
        observe()
        return Preferences.preferenceSp.getBoolean(key(site, "fade"), false)
    }

    fun reduced(site: SiteType): Boolean {
        observe()
        return Preferences.preferenceSp.getBoolean(key(site, "reduced"), false)
    }

    fun options(site: SiteType): Options? =
        if (site in sites) Options(retries(site), fade(site), reduced(site)) else null

    fun setRetries(site: SiteType, value: Int) {
        Preferences.preferenceSp.edit {
            if (value == DEFAULT_RETRIES || value !in retryChoices) remove(key(site, "retries"))
            else putInt(key(site, "retries"), value)
        }
    }

    fun setFade(site: SiteType, on: Boolean) {
        Preferences.preferenceSp.edit {
            if (on) putBoolean(key(site, "fade"), true) else remove(key(site, "fade"))
        }
    }

    fun setReduced(site: SiteType, on: Boolean) {
        Preferences.preferenceSp.edit {
            if (on) putBoolean(key(site, "reduced"), true) else remove(key(site, "reduced"))
        }
    }

    fun reset(site: SiteType) {
        Preferences.preferenceSp.edit {
            remove(key(site, "retries"))
            remove(key(site, "fade"))
            remove(key(site, "reduced"))
        }
    }

    private fun observe() {
        if (!listening) {
            Preferences.preferenceSp.registerOnSharedPreferenceChangeListener(listener)
            listening = true
        }
        version
    }
}

object SiteImageCardSettings {

    enum class Detail(val keySuffix: String) {
        Title("title"),
        Badges("badges"),
        Subtitle("subtitle"),
    }

    val sites: Set<SiteType> = setOf(SiteType.BUONDUA, SiteType.COSPLAYTELE, SiteType.XHAMSTER)

    private var version by mutableIntStateOf(0)
    private var listening = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.contains("_imgcard_")) version++
    }

    private fun touch() {
        if (!listening) {
            Preferences.preferenceSp.registerOnSharedPreferenceChangeListener(listener)
            listening = true
        }
        version
    }

    private fun key(site: SiteType, detail: Detail) =
        "site_${site.name.lowercase()}_imgcard_show_${detail.keySuffix}"

    private fun aspectKey(site: SiteType) = "site_${site.name.lowercase()}_imgcard_aspect"

    fun isShown(site: SiteType, detail: Detail): Boolean {
        touch()
        return Preferences.preferenceSp.getBoolean(key(site, detail), true)
    }

    fun setShown(site: SiteType, detail: Detail, shown: Boolean) {
        Preferences.preferenceSp.edit {
            if (shown) remove(key(site, detail)) else putBoolean(key(site, detail), false)
        }
    }

    fun aspect(site: SiteType): SiteCardSettings.Aspect {
        touch()
        val stored = Preferences.preferenceSp.getString(aspectKey(site), null)
        return SiteCardSettings.Aspect.entries.firstOrNull { it.keyValue == stored } ?: SiteCardSettings.Aspect.Default
    }

    fun setAspect(site: SiteType, aspect: SiteCardSettings.Aspect) {
        Preferences.preferenceSp.edit {
            if (aspect == SiteCardSettings.Aspect.Default) remove(aspectKey(site))
            else putString(aspectKey(site), aspect.keyValue)
        }
    }

    fun reset(site: SiteType) {
        Preferences.preferenceSp.edit {
            Detail.entries.forEach { remove(key(site, it)) }
            remove(aspectKey(site))
        }
    }
}

object SiteShortsCardSettings {

    enum class Detail(val keySuffix: String, val defaultShown: Boolean) {
        Views("views", true),
        Duration("duration", true),
        GifBadge("gif", true),
        Title("title", false),
    }

    val sites: Set<SiteType> = setOf(SiteType.XHAMSTER, SiteType.REDGIFS)
    val COLUMN_CHOICES = listOf(2, 3, 4)
    const val DEFAULT_COLUMNS = 3

    private var version by mutableIntStateOf(0)
    private var listening = false

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.contains("_shortcard_")) version++
    }

    private fun touch() {
        if (!listening) {
            Preferences.preferenceSp.registerOnSharedPreferenceChangeListener(listener)
            listening = true
        }
        version
    }

    private fun key(site: SiteType, detail: Detail) =
        "site_${site.name.lowercase()}_shortcard_show_${detail.keySuffix}"

    private fun aspectKey(site: SiteType) = "site_${site.name.lowercase()}_shortcard_aspect"
    private fun columnsKey(site: SiteType) = "site_${site.name.lowercase()}_shortcard_columns"

    fun isShown(site: SiteType, detail: Detail): Boolean {
        touch()
        return Preferences.preferenceSp.getBoolean(key(site, detail), detail.defaultShown)
    }

    fun setShown(site: SiteType, detail: Detail, shown: Boolean) {
        Preferences.preferenceSp.edit {
            if (shown == detail.defaultShown) remove(key(site, detail)) else putBoolean(key(site, detail), shown)
        }
    }

    fun aspect(site: SiteType): SiteCardSettings.Aspect {
        touch()
        val stored = Preferences.preferenceSp.getString(aspectKey(site), null)
        return SiteCardSettings.Aspect.entries.firstOrNull { it.keyValue == stored } ?: SiteCardSettings.Aspect.Default
    }

    fun ratio(site: SiteType): Float = aspect(site).ratio ?: (9f / 16f)

    fun setAspect(site: SiteType, aspect: SiteCardSettings.Aspect) {
        Preferences.preferenceSp.edit {
            if (aspect == SiteCardSettings.Aspect.Default) remove(aspectKey(site))
            else putString(aspectKey(site), aspect.keyValue)
        }
    }

    fun columns(site: SiteType): Int {
        touch()
        return Preferences.preferenceSp.getInt(columnsKey(site), DEFAULT_COLUMNS).coerceIn(2, 4)
    }

    fun setColumns(site: SiteType, columns: Int) {
        Preferences.preferenceSp.edit {
            if (columns == DEFAULT_COLUMNS) remove(columnsKey(site)) else putInt(columnsKey(site), columns)
        }
    }

    fun reset(site: SiteType) {
        Preferences.preferenceSp.edit {
            Detail.entries.forEach { remove(key(site, it)) }
            remove(aspectKey(site))
            remove(columnsKey(site))
        }
    }
}
