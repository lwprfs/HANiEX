package com.yenaly.han1meviewer.logic

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.annotation.StringRes
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.FapFolder.common.FapFolderConstants
import com.yenaly.han1meviewer.SitesHub.FapFolder.common.FapFolderSecurePrefs
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.local.FapFolderDatabase
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.local.FapFolderHistoryEntity
import com.yenaly.han1meviewer.SitesHub.FapFolder.data.remote.FapFolderCookieJar
import com.yenaly.han1meviewer.SitesHub.HentaiMama.data.local.HentaiMamaDatabase
import com.yenaly.han1meviewer.SitesHub.HentaiMama.data.local.HentaiMamaHistoryEntity
import com.yenaly.han1meviewer.SitesHub.MissAV.data.local.MissAvDatabase
import com.yenaly.han1meviewer.SitesHub.MissAV.data.local.MissAvHistoryEntity
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirDatabase
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirHistoryEntity
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SitesHub.SupJav.data.local.SupJavDatabase
import com.yenaly.han1meviewer.SitesHub.SupJav.data.local.SupJavHistoryEntity
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.local.RouVideoDatabase
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.local.RouVideoHistoryEntity
import com.yenaly.han1meviewer.SitesHub.XHamster.data.local.XHamsterDatabase
import com.yenaly.han1meviewer.SitesHub.XHamster.data.local.XHamsterHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Buondua.data.local.BuonduaDatabase
import com.yenaly.han1meviewer.SitesHub.Buondua.data.local.BuonduaHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.local.CosplayteleDatabase
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.data.local.CosplayteleHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Coomer.data.local.CoomerDatabase
import com.yenaly.han1meviewer.SitesHub.Coomer.data.local.CoomerHistoryEntity
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.local.LeakGalleryDatabase
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.local.LeakGalleryHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Coser.data.local.CoserDatabase
import com.yenaly.han1meviewer.SitesHub.Coser.data.local.CoserHistoryEntity
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.local.FullVideosDatabase
import com.yenaly.han1meviewer.SitesHub.FullVideos.data.local.FullVideosHistoryEntity
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.local.FuckingSessionDatabase
import com.yenaly.han1meviewer.SitesHub.FuckingSession.data.local.FuckingSessionHistoryEntity
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.local.WumaobiDatabase
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.local.WumaobiHistoryEntity
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.local.XXXTubeDatabase
import com.yenaly.han1meviewer.SitesHub.XXXTube.data.local.XXXTubeHistoryEntity
import com.yenaly.han1meviewer.logic.local.LocalPlaylist
import com.yenaly.han1meviewer.logic.local.LocalPlaylistStore
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.dao.HistoryDatabase
import com.yenaly.han1meviewer.logic.dao.MiscellanyDatabase
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.SideDishEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.download.DownloadCategoryEntity
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeCategoryCrossRef
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStream

object BackupManager {
    private const val BACKUP_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    enum class Section(@StringRes val labelRes: Int, val group: Group) {
        SETTINGS(R.string.backup_section_settings, Group.DATA),
        CHECK_IN(R.string.backup_section_check_in, Group.DATA),
        KEYFRAMES(R.string.backup_section_keyframes, Group.DATA),
        HANIME_HISTORY(R.string.backup_section_hanime_history, Group.DATA),
        DOWNLOADS(R.string.backup_section_downloads, Group.DATA),
        MISSAV_HISTORY(R.string.backup_section_missav_history, Group.DATA),
        HENTAIMAMA_HISTORY(R.string.backup_section_hentaimama_history, Group.DATA),
        ONAIR_HISTORY(R.string.backup_section_onair_history, Group.DATA),
        FAPFOLDER_HISTORY(R.string.backup_section_fapfolder_history, Group.DATA),
        SUPJAV_HISTORY(R.string.backup_section_supjav_history, Group.DATA),
        ROUVIDEO_HISTORY(R.string.backup_section_rouvideo_history, Group.DATA),
        XHAMSTER_HISTORY(R.string.backup_section_xhamster_history, Group.DATA),
        BUONDUA_HISTORY(R.string.backup_section_buondua_history, Group.DATA),
        COSPLAYTELE_HISTORY(R.string.backup_section_cosplaytele_history, Group.DATA),
        COSER_HISTORY(R.string.backup_section_coser_history, Group.DATA),
        FULLVIDEOS_HISTORY(R.string.backup_section_fullvideos_history, Group.DATA),
        FUCKINGSESSION_HISTORY(R.string.backup_section_fuckingsession_history, Group.DATA),
        WUMAOBI_HISTORY(R.string.backup_section_wumaobi_history, Group.DATA),
        XXXTUBE_HISTORY(R.string.backup_section_xxxtube_history, Group.DATA),
        COOMER_HISTORY(R.string.backup_section_coomer_history, Group.DATA),
        LEAKGALLERY_HISTORY(R.string.backup_section_leakgallery_history, Group.DATA),
        LOCAL_PLAYLISTS(R.string.backup_section_local_playlists, Group.DATA),
        COOKIES_HANIME(R.string.backup_section_cookies_hanime, Group.COOKIES),
        COOKIES_MISSAV(R.string.backup_section_cookies_missav, Group.COOKIES),
        COOKIES_HENTAIMAMA(R.string.backup_section_cookies_hentaimama, Group.COOKIES),
        COOKIES_ONAIR(R.string.backup_section_cookies_onair, Group.COOKIES),
        COOKIES_FAPFOLDER(R.string.backup_section_cookies_fapfolder, Group.COOKIES),
        COOKIES_ROUVIDEO(R.string.backup_section_cookies_rouvideo, Group.COOKIES),
        COOKIES_XHAMSTER(R.string.backup_section_cookies_xhamster, Group.COOKIES);

        enum class Group { DATA, COOKIES }
    }

    private const val KEY_ONAIR_COOKIES = "onair_cookies_raw"

    private fun cookieSectionOfKey(key: String): Section? = when {
        key == com.yenaly.han1meviewer.LOGIN_COOKIE ||
                key == com.yenaly.han1meviewer.CLOUDFLARE_COOKIE -> Section.COOKIES_HANIME
        key.startsWith("missav_cf_") -> Section.COOKIES_MISSAV
        key.startsWith("hm_cf_") -> Section.COOKIES_HENTAIMAMA
        key == KEY_ONAIR_COOKIES -> Section.COOKIES_ONAIR
        key == "rouvideo_auth_cookies" || key.startsWith("rouvideo_user_") -> Section.COOKIES_ROUVIDEO
        key == "xhamster_cookies_v1" || key == "xhamster_user_cache" -> Section.COOKIES_XHAMSTER
        else -> null
    }

    @Serializable
    private data class BackupData(
        val version: Int = BACKUP_VERSION,
        val appVersionCode: Int = BuildConfig.VERSION_CODE,
        val appVersionName: String = BuildConfig.VERSION_NAME,
        val exportedAt: Long = System.currentTimeMillis(),
        val settings: Map<String, PreferenceValue>? = null,
        val hKeyframes: List<HKeyframeEntity>? = null,
        val checkInRecords: List<CheckInRecordEntity>? = null,
        val sideDishes: List<SideDishEntity>? = null,
        val watchHistories: List<WatchHistoryEntity>? = null,
        val downloadGroups: List<DownloadGroupEntity>? = null,
        val downloads: List<HanimeDownloadEntity>? = null,
        val downloadCategories: List<DownloadCategoryEntity>? = null,
        val downloadCategoryCrossRefs: List<HanimeCategoryCrossRef>? = null,
        val cookies: Map<String, Map<String, PreferenceValue>>? = null,
        val fapFolderCookies: String? = null,
        val missAvHistories: List<MissAvHistoryEntity>? = null,
        val hentaiMamaHistories: List<HentaiMamaHistoryEntity>? = null,
        val onAirHistories: List<OnAirHistoryEntity>? = null,
        val fapFolderHistories: List<FapFolderHistoryEntity>? = null,
        val supJavHistories: List<SupJavHistoryEntity>? = null,
        val rouVideoHistories: List<RouVideoHistoryEntity>? = null,
        val xHamsterHistories: List<XHamsterHistoryEntity>? = null,
        val buonduaHistories: List<BuonduaHistoryEntity>? = null,
        val cosplayteleHistories: List<CosplayteleHistoryEntity>? = null,
        val coserHistories: List<CoserHistoryEntity>? = null,
        val fullVideosHistories: List<FullVideosHistoryEntity>? = null,
        val fuckingSessionHistories: List<FuckingSessionHistoryEntity>? = null,
        val wumaobiHistories: List<WumaobiHistoryEntity>? = null,
        val xxxTubeHistories: List<XXXTubeHistoryEntity>? = null,
        val coomerHistories: List<CoomerHistoryEntity>? = null,
        val leakgalleryHistories: List<LeakGalleryHistoryEntity>? = null,
        val localPlaylists: List<LocalPlaylist>? = null,
    )

    @Serializable
    private sealed interface PreferenceValue {
        @Serializable
        data class BooleanValue(val value: Boolean) : PreferenceValue

        @Serializable
        data class FloatValue(val value: Float) : PreferenceValue

        @Serializable
        data class IntValue(val value: Int) : PreferenceValue

        @Serializable
        data class LongValue(val value: Long) : PreferenceValue

        @Serializable
        data class StringValue(val value: String) : PreferenceValue

        @Serializable
        data class StringSetValue(val value: Set<String>) : PreferenceValue
    }

    suspend fun exportTo(
        context: Context,
        uri: Uri,
        sections: Set<Section> = Section.entries.toSet(),
    ) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            exportTo(context, outputStream, sections)
        } ?: error("Unable to open backup file")
    }

    suspend fun importFrom(
        context: Context,
        uri: Uri,
        sections: Set<Section> = Section.entries.toSet(),
    ) {
        val backup = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            json.decodeFromString<BackupData>(inputStream.bufferedReader().readText())
        } ?: error("Unable to open backup file")

        backup.hKeyframes?.takeIf { Section.KEYFRAMES in sections }?.let { hKeyframes ->
            MiscellanyDatabase.instance.hKeyframeDao.apply {
                deleteAll()
                insertAll(hKeyframes)
            }
        }

        backup.checkInRecords?.takeIf { Section.CHECK_IN in sections }?.let { checkInRecords ->
            CheckInRecordDatabase.getDatabase(context).checkInDao().apply {
                deleteAll()
                insertAll(checkInRecords.map { it.normalizeSideDishes() })
            }
        }

        backup.sideDishes?.takeIf { Section.CHECK_IN in sections }?.let { sideDishes ->
            CheckInRecordDatabase.getDatabase(context).sideDishDao().apply {
                deleteAll()
                insertAll(sideDishes)
            }
        }

        backup.watchHistories?.takeIf { Section.HANIME_HISTORY in sections }?.let { watchHistories ->
            HistoryDatabase.instance.watchHistory.apply {
                deleteAll()
                insertAll(watchHistories)
            }
        }

        if (Section.DOWNLOADS in sections && (backup.downloadGroups != null ||
                    backup.downloads != null ||
                    backup.downloadCategories != null || backup.downloadCategoryCrossRefs != null)
        ) {
            val downloadGroups = backup.downloadGroups.orEmpty()
            val groupIds = downloadGroups.mapTo(mutableSetOf()) { it.id } +
                    DownloadGroupEntity.DEFAULT_GROUP_ID
            val downloads = backup.downloads.orEmpty().map { download ->
                if (download.groupId in groupIds) {
                    download
                } else {
                    download.copy(groupId = DownloadGroupEntity.DEFAULT_GROUP_ID)
                }
            }
            val downloadCategories = backup.downloadCategories.orEmpty()
            val downloadIds = downloads.mapTo(mutableSetOf()) { it.id }
            val categoryIds = downloadCategories.mapTo(mutableSetOf()) { it.id }
            val crossRefs = backup.downloadCategoryCrossRefs.orEmpty().filter { crossRef ->
                crossRef.videoId in downloadIds && crossRef.categoryId in categoryIds
            }

            DownloadDatabase.instance.apply {
                downloadCategoryDao.deleteAllCrossRefs()
                hanimeDownloadDao.deleteAll()
                downloadCategoryDao.deleteAllCategories()
                downloadGroupDao.deleteAll()
                downloadGroupDao.insertAll(downloadGroups)
                downloadGroupDao.insertDefaultGroup()
                downloadCategoryDao.insertAllCategories(downloadCategories)
                hanimeDownloadDao.insertAll(downloads)
                downloadCategoryDao.insertAllCrossRefs(crossRefs)
            }
        }

        backup.missAvHistories?.takeIf { Section.MISSAV_HISTORY in sections }?.let {
            MissAvDatabase.getInstance(context).missAvHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.hentaiMamaHistories?.takeIf { Section.HENTAIMAMA_HISTORY in sections }?.let {
            HentaiMamaDatabase.getInstance(context).historyDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.onAirHistories?.takeIf { Section.ONAIR_HISTORY in sections }?.let {
            OnAirDatabase.getInstance(context).onAirHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.fapFolderHistories?.takeIf { Section.FAPFOLDER_HISTORY in sections }?.let {
            FapFolderDatabase.getInstance(context).fapFolderHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }

        backup.supJavHistories?.takeIf { Section.SUPJAV_HISTORY in sections }?.let {
            SupJavDatabase.getInstance(context).supJavHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.rouVideoHistories?.takeIf { Section.ROUVIDEO_HISTORY in sections }?.let {
            RouVideoDatabase.getInstance(context).rouVideoHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.xHamsterHistories?.takeIf { Section.XHAMSTER_HISTORY in sections }?.let {
            XHamsterDatabase.getInstance(context).xHamsterHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.buonduaHistories?.takeIf { Section.BUONDUA_HISTORY in sections }?.let {
            BuonduaDatabase.getInstance(context).buonduaHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.cosplayteleHistories?.takeIf { Section.COSPLAYTELE_HISTORY in sections }?.let {
            CosplayteleDatabase.getInstance(context).cosplayteleHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.coserHistories?.takeIf { Section.COSER_HISTORY in sections }?.let {
            CoserDatabase.getInstance(context).coserHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.fullVideosHistories?.takeIf { Section.FULLVIDEOS_HISTORY in sections }?.let {
            FullVideosDatabase.getInstance(context).fullVideosHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.fuckingSessionHistories?.takeIf { Section.FUCKINGSESSION_HISTORY in sections }?.let {
            FuckingSessionDatabase.getInstance(context).fuckingSessionHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.wumaobiHistories?.takeIf { Section.WUMAOBI_HISTORY in sections }?.let {
            WumaobiDatabase.getInstance(context).wumaobiHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.coomerHistories?.takeIf { Section.COOMER_HISTORY in sections }?.let {
            CoomerDatabase.getInstance(context).coomerHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.leakgalleryHistories?.takeIf { Section.LEAKGALLERY_HISTORY in sections }?.let {
            LeakGalleryDatabase.getInstance(context).leakgalleryHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.xxxTubeHistories?.takeIf { Section.XXXTUBE_HISTORY in sections }?.let {
            XXXTubeDatabase.getInstance(context).xxxTubeHistoryDao().apply {
                deleteAll(); insertAll(it)
            }
        }
        backup.localPlaylists?.takeIf { Section.LOCAL_PLAYLISTS in sections }?.let {
            LocalPlaylistStore.replaceAll(it)
        }

        backup.settings?.takeIf { Section.SETTINGS in sections }?.let { applyPrefs(it) }

        backup.cookies?.forEach { (sectionName, prefs) ->
            val section = Section.entries.firstOrNull { it.name == sectionName } ?: return@forEach
            if (section in sections) applyPrefs(prefs)
        }

        if (Section.COOKIES_FAPFOLDER in sections) {
            backup.fapFolderCookies?.takeIf { it.isNotBlank() }?.let { raw ->
                FapFolderSecurePrefs.instance.edit(commit = true) {
                    putString(FapFolderConstants.PREF_FAPFOLDER_COOKIES_RAW, raw)
                }
                runCatching { FapFolderCookieJar().restoreFromPreferences() }
            }
        }
    }

    private fun applyPrefs(settings: Map<String, PreferenceValue>) {
        Preferences.preferenceSp.edit {
            settings.forEach { (key, value) ->
                when (value) {
                    is PreferenceValue.BooleanValue -> putBoolean(key, value.value)
                    is PreferenceValue.FloatValue -> putFloat(key, value.value)
                    is PreferenceValue.IntValue -> putInt(key, value.value)
                    is PreferenceValue.LongValue -> putLong(key, value.value)
                    is PreferenceValue.StringSetValue -> putStringSet(key, value.value)
                    is PreferenceValue.StringValue -> putString(key, value.value)
                }
            }
        }
    }

    private suspend fun exportTo(
        context: Context,
        outputStream: OutputStream,
        sections: Set<Section>,
    ) {
        val allPrefs = Preferences.preferenceSp.all.mapValuesNotNull { (_, value) ->
            value.toPreferenceValue()
        }
        val settings = allPrefs.filterKeys { cookieSectionOfKey(it) == null }
        val cookies = Section.entries
            .filter { it.group == Section.Group.COOKIES && it in sections }
            .associate { section ->
                section.name to allPrefs.filterKeys { cookieSectionOfKey(it) == section }
            }
        val checkInDb = CheckInRecordDatabase.getDatabase(context)
        val backup = BackupData(
            settings = if (Section.SETTINGS in sections) settings else null,
            hKeyframes = if (Section.KEYFRAMES in sections)
                MiscellanyDatabase.instance.hKeyframeDao.getAll() else null,
            checkInRecords = if (Section.CHECK_IN in sections)
                checkInDb.checkInDao().getAllRecords() else null,
            sideDishes = if (Section.CHECK_IN in sections)
                checkInDb.sideDishDao().getAll() else null,
            watchHistories = if (Section.HANIME_HISTORY in sections)
                HistoryDatabase.instance.watchHistory.getAll() else null,
            downloadGroups = if (Section.DOWNLOADS in sections)
                DownloadDatabase.instance.downloadGroupDao.getAllGroupsOnce() else null,
            downloads = if (Section.DOWNLOADS in sections)
                DownloadDatabase.instance.hanimeDownloadDao.getAll() else null,
            downloadCategories = if (Section.DOWNLOADS in sections)
                DownloadDatabase.instance.downloadCategoryDao.getAllCategoriesOnce() else null,
            downloadCategoryCrossRefs = if (Section.DOWNLOADS in sections)
                DownloadDatabase.instance.downloadCategoryDao.getAllCrossRefs() else null,
            cookies = cookies.takeIf { it.isNotEmpty() },
            fapFolderCookies = if (Section.COOKIES_FAPFOLDER in sections) {
                runCatching {
                    FapFolderSecurePrefs.instance.getString(
                        FapFolderConstants.PREF_FAPFOLDER_COOKIES_RAW, null
                    ) ?: FapFolderSecurePrefs.readFallback(
                        FapFolderConstants.PREF_FAPFOLDER_COOKIES_RAW
                    )
                }.getOrNull()
            } else null,
            missAvHistories = if (Section.MISSAV_HISTORY in sections)
                MissAvDatabase.getInstance(context).missAvHistoryDao().getAllHistorySync() else null,
            hentaiMamaHistories = if (Section.HENTAIMAMA_HISTORY in sections)
                HentaiMamaDatabase.getInstance(context).historyDao().getAll() else null,
            onAirHistories = if (Section.ONAIR_HISTORY in sections)
                OnAirDatabase.getInstance(context).onAirHistoryDao().getAll() else null,
            fapFolderHistories = if (Section.FAPFOLDER_HISTORY in sections)
                FapFolderDatabase.getInstance(context).fapFolderHistoryDao().getAll() else null,
            supJavHistories = if (Section.SUPJAV_HISTORY in sections)
                SupJavDatabase.getInstance(context).supJavHistoryDao().getAllHistorySync() else null,
            rouVideoHistories = if (Section.ROUVIDEO_HISTORY in sections)
                RouVideoDatabase.getInstance(context).rouVideoHistoryDao().getAllHistorySync() else null,
            xHamsterHistories = if (Section.XHAMSTER_HISTORY in sections)
                XHamsterDatabase.getInstance(context).xHamsterHistoryDao().getAllHistorySync() else null,
            buonduaHistories = if (Section.BUONDUA_HISTORY in sections)
                BuonduaDatabase.getInstance(context).buonduaHistoryDao().getAllHistorySync() else null,
            cosplayteleHistories = if (Section.COSPLAYTELE_HISTORY in sections)
                CosplayteleDatabase.getInstance(context).cosplayteleHistoryDao().getAllHistorySync() else null,
            coserHistories = if (Section.COSER_HISTORY in sections)
                CoserDatabase.getInstance(context).coserHistoryDao().getAllHistorySync() else null,
            fullVideosHistories = if (Section.FULLVIDEOS_HISTORY in sections)
                FullVideosDatabase.getInstance(context).fullVideosHistoryDao().getAllHistorySync() else null,
            fuckingSessionHistories = if (Section.FUCKINGSESSION_HISTORY in sections)
                FuckingSessionDatabase.getInstance(context).fuckingSessionHistoryDao().getAllHistorySync() else null,
            wumaobiHistories = if (Section.WUMAOBI_HISTORY in sections)
                WumaobiDatabase.getInstance(context).wumaobiHistoryDao().getAllHistorySync() else null,
            coomerHistories = if (Section.COOMER_HISTORY in sections)
                CoomerDatabase.getInstance(context).coomerHistoryDao().getAllHistorySync() else null,
            leakgalleryHistories = if (Section.LEAKGALLERY_HISTORY in sections)
                LeakGalleryDatabase.getInstance(context).leakgalleryHistoryDao().getAllHistorySync() else null,
            xxxTubeHistories = if (Section.XXXTUBE_HISTORY in sections)
                XXXTubeDatabase.getInstance(context).xxxTubeHistoryDao().getAllHistorySync() else null,
            localPlaylists = if (Section.LOCAL_PLAYLISTS in sections)
                LocalPlaylistStore.playlists.value else null,
        )
        outputStream.bufferedWriter().use { writer ->
            writer.write(json.encodeToString(backup))
        }
    }

    private inline fun <K, V, R : Any> Map<K, V>.mapValuesNotNull(
        transform: (Map.Entry<K, V>) -> R?
    ): Map<K, R> {
        return mapNotNull { entry -> transform(entry)?.let { entry.key to it } }.toMap()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.toPreferenceValue(): PreferenceValue? {
        return when (this) {
            is Boolean -> PreferenceValue.BooleanValue(this)
            is Float -> PreferenceValue.FloatValue(this)
            is Int -> PreferenceValue.IntValue(this)
            is Long -> PreferenceValue.LongValue(this)
            is String -> PreferenceValue.StringValue(this)
            is Set<*> -> PreferenceValue.StringSetValue(this.filterIsInstance<String>().toSet())
            else -> null
        }
    }

    private fun CheckInRecordEntity.normalizeSideDishes(): CheckInRecordEntity {
        if (sideDishes.isBlank()) return this
        val normalized = sideDishes
            .replace("\\u001E", "\u001E")
            .replace("\\u001e", "\u001E")
            .split(",")
            .joinToString(",") { item ->
                if (item.contains("\u001E") || !item.contains("|")) {
                    item
                } else {
                    val title = item.substringBefore("|")
                    val videoCode = item.substringAfter("|", "")
                    if (videoCode.isBlank()) title else "$title\u001E$videoCode"
                }
            }
        return copy(sideDishes = normalized)
    }
}
