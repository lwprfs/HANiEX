package com.yenaly.han1meviewer.ui.screen.settings

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.navigation.isHanimeFamily
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsSiteSectionRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SiteSettingsSection
import com.yenaly.han1meviewer.ui.navigation.settings.buildSiteSettingsItems
import com.yenaly.han1meviewer.ui.navigation.settings.siteAccountRows
import com.yenaly.han1meviewer.ui.navigation.settings.siteMainRows
import com.yenaly.han1meviewer.ui.screen.SiteCardSettings
import com.yenaly.han1meviewer.ui.screen.SiteHomeSettings
import com.yenaly.han1meviewer.ui.screen.SiteImageCardSettings
import com.yenaly.han1meviewer.ui.screen.SiteImageSettings
import com.yenaly.han1meviewer.ui.screen.SiteLayoutSettings
import com.yenaly.han1meviewer.ui.screen.SiteSearchSettings
import com.yenaly.han1meviewer.ui.screen.SiteShortsCardSettings

internal class SiteSearchEntry(
    val target: SettingsSearchTarget,
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int? = null,
    val titleArg: String? = null,
)

internal fun siteSearchEntries(site: SiteType, contentAvailable: Boolean): List<SiteSearchEntry> =
    buildList {
        fun screen(route: NavKey, vararg titles: Int) {
            titles.forEach { add(SiteSearchEntry(SettingsSearchTarget.Screen(route), it)) }
        }

        val items = buildSiteSettingsItems(site, contentAvailable)
        items.forEach { item ->
            val target = item.destination?.let { SettingsSearchTarget.Screen(it) }
                ?: SettingsSearchTarget.Reset
            add(
                SiteSearchEntry(
                    target = target,
                    titleRes = item.titleRes,
                    summaryRes = item.summaryRes.takeIf { item.summaryArg == null },
                    titleArg = item.titleArg,
                )
            )
        }

        (siteMainRows(site) + siteAccountRows(site, contentAvailable)).forEach { row ->
            add(
                SiteSearchEntry(
                    target = SettingsSearchTarget.Screen(row.route),
                    titleRes = row.titleRes,
                    summaryRes = row.summaryRes,
                )
            )
        }

        siteMainRows(site).forEach { row ->
            screen(row.route, *siteScreenContents(site, row.key))
        }

        if (site.isHanimeFamily()) {
            screen(
                SettingsSiteSectionRoute(site.name, SiteSettingsSection.Main.key),
                R.string.video_language,
                R.string.default_video_quilty,
                R.string.watched_progress_threshold,
                R.string.disable_comments_title,
                R.string.related_video_as_tab,
                R.string.search_pagination,
                R.string.search_artist_ignore_video_type,
            )
        }

        layoutSearchTitles(site).forEach {
            screen(SettingsSiteSectionRoute(site.name, SiteSettingsSection.LayoutCards.key), it)
        }

        screen(
            NetworkSettingsRoute,
            R.string.network_settings_site,
            R.string.domain_name,
            R.string.mirror_latency_entry,
            R.string.custom_mirror_site,
            R.string.proxy,
            R.string.use_built_in_hosts,
            R.string.custom_hosts,
            R.string.use_doh,
            R.string.test_doh,
            R.string.apply_deep_links,
        )
        if (site == SiteType.HANIME) {
            screen(NetworkSettingsRoute, R.string.use_backup_media_cdn)
        }
    }

private fun siteScreenContents(site: SiteType, rowKey: String): IntArray = when (site) {
    SiteType.COOMER -> intArrayOf(
        R.string.coomer_photo_columns, R.string.coomer_viewer_tap,
        R.string.coomer_troubleshooting, R.string.coomer_reset_defaults,
    )
    SiteType.COSER -> intArrayOf(
        R.string.coser_pref_quality, R.string.coser_photo_columns, R.string.coser_viewer_tap,
        R.string.coser_troubleshooting, R.string.coser_reset_defaults,
    )
    SiteType.COSPLAYTELE -> intArrayOf(
        R.string.cosplaytele_pref_quality, R.string.cosplaytele_photo_columns,
        R.string.cosplaytele_viewer_tap, R.string.cosplaytele_troubleshooting,
        R.string.cosplaytele_reset_defaults,
    )
    SiteType.LEAKGALLERY -> intArrayOf(
        R.string.leakgallery_default_section, R.string.leakgallery_photo_columns,
        R.string.leakgallery_viewer_tap, R.string.leakgallery_troubleshooting,
        R.string.leakgallery_reset_defaults,
    )
    SiteType.FUCKINGSESSION -> intArrayOf(
        R.string.fuckingsession_home_sections, R.string.fuckingsession_troubleshooting,
        R.string.fuckingsession_reset_defaults,
    )
    SiteType.XXXTUBE -> intArrayOf(
        R.string.xxxtube_home_sections, R.string.xxxtube_troubleshooting,
        R.string.xxxtube_reset_defaults,
    )
    SiteType.FULLVIDEOS -> intArrayOf(
        R.string.fullvideos_home_sections, R.string.fullvideos_pref_quality,
        R.string.fullvideos_troubleshooting, R.string.fullvideos_reset_defaults,
    )
    SiteType.WUMAOBI -> intArrayOf(
        R.string.wumaobi_home_sections, R.string.wumaobi_pref_quality,
        R.string.wumaobi_troubleshooting, R.string.wumaobi_reset_defaults,
    )
    SiteType.REDGIFS -> intArrayOf(
        R.string.redgifs_pref_blocked_tags, R.string.redgifs_pref_content,
        R.string.redgifs_pref_quality, R.string.redgifs_pref_verified_only,
    )
    SiteType.ROUVIDEO -> if (rowKey == "host") {
        intArrayOf(R.string.mirror_latency_entry)
    } else {
        intArrayOf(
            R.string.rouvideo_home_tabs, R.string.rouvideo_title_language,
            R.string.rouvideo_report_plays, R.string.rouvideo_troubleshooting,
            R.string.rouvideo_reset_defaults,
        )
    }
    SiteType.SUPJAV -> intArrayOf(
        R.string.supjav_pref_quality, R.string.supjav_auto_fallback, R.string.supjav_servers,
        R.string.supjav_site_language, R.string.supjav_subtitles,
        R.string.supjav_subtitle_language, R.string.supjav_subtitle_translate,
        R.string.supjav_troubleshooting, R.string.supjav_reset_defaults,
    )
    SiteType.XHAMSTER -> intArrayOf(R.string.xhamster_pref_quality)
    SiteType.HENTAIMAMA -> if (rowKey == "video") {
        intArrayOf(
            R.string.hm_preferred_server, R.string.hm_preferred_quality,
            R.string.hm_home_card_size, R.string.hm_recent_episodes_card_size,
            R.string.hm_upcoming_card_size, R.string.hm_clear_cloudflare_cookie,
        )
    } else {
        intArrayOf()
    }
    else -> intArrayOf()
}

private fun layoutSearchTitles(site: SiteType): List<Int> = buildList {
    if (site.isHanimeFamily()) add(R.string.home_category_layout)
    val grid = site in SiteLayoutSettings.gridColumnSites
    val cards = site in SiteCardSettings.sites
    val shorts = site in SiteShortsCardSettings.sites
    if (grid || cards) add(R.string.site_preset_title)
    if (grid || shorts) add(R.string.site_grid_columns_title)
    if (cards) {
        add(R.string.site_card_show_views)
        add(R.string.site_card_show_duration)
        add(R.string.site_card_show_subtitle)
        add(R.string.site_card_show_date)
    }
    if (cards || shorts || site in SiteImageCardSettings.sites) add(R.string.site_card_aspect_title)
    if (shorts) {
        add(R.string.site_shorts_cards_section)
        add(R.string.site_shorts_card_gif_badge)
    }
    if (site in SiteImageCardSettings.sites) add(R.string.site_image_cards_section)
    if (SiteSearchSettings.sortChoices(site) != null) add(R.string.site_default_sort_title)
    if (site in SiteHomeSettings.cacheSites) add(R.string.site_home_cache_title)
    SiteHomeSettings.sectionsOf(site).forEach { section ->
        add(
            when (section) {
                SiteHomeSettings.Section.FapFolderTrending -> R.string.fap_trending
                SiteHomeSettings.Section.FapFolderSuggested -> R.string.fap_suggested_communities
            }
        )
    }
    if (site == SiteType.XHAMSTER) {
        add(R.string.hub_xhamster_show_channels)
        add(R.string.hub_xhamster_show_top_creators)
    }
    if (site in SiteImageSettings.sites) {
        add(R.string.site_image_retries_title)
        add(R.string.site_image_fade_title)
        add(R.string.site_image_reduced_title)
    }
}
