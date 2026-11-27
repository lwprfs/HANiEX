package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.util.translation.AppTranslation
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object HomeSettingsRoute : NavKey

@Serializable
data class SettingsCategoryRoute(val categoryKey: String) : NavKey

@Serializable
data class SettingsSiteSectionRoute(val site: String, val section: String) : NavKey

@Serializable
object PlayerSettingsRoute : NavKey

@Serializable
data class SitePlayerSettingsRoute(val site: String) : NavKey

@Serializable
data class SiteSocialStyleRoute(val site: String) : NavKey

@Serializable
object NetworkSettingsRoute : NavKey

@Serializable
object DownloadSettingsRoute : NavKey

@Serializable
object MpvPlayerSettingsRoute : NavKey

@Serializable
object HKeyframesRoute : NavKey

@Serializable
object SharedHKeyframesRoute : NavKey

@Serializable
object HKeyframeSettingsRoute : NavKey

@Serializable
object UpdateSettingsRoute : NavKey

@Serializable
object TranslationSettingsRoute : NavKey

@Serializable
data class TranslationSiteRoute(val site: String) : NavKey

fun translationRouteFor(site: SiteType): NavKey =
    if (site in AppTranslation.SUPPORTED_SITES) TranslationSiteRoute(site.name)
    else TranslationSettingsRoute

@Serializable
object MediaControlsSettingsRoute : NavKey

@Serializable
data class TranslationCacheRoute(val site: String) : NavKey

@Serializable
object AboutAppRoute : NavKey

enum class SettingsDestinationSpec(
    @param:StringRes val titleRes: Int,
    val screenClassName: String,
    val routeClass: KClass<*>,
    val category: SettingsCategory? = null,
) {
    Home(
        titleRes = R.string.settings,
        screenClassName = "HomeSettingsScreen",
        routeClass = HomeSettingsRoute::class,
    ),
    SiteSection(
        titleRes = R.string.settings,
        screenClassName = "SiteSettingsSectionScreen",
        routeClass = SettingsSiteSectionRoute::class,
    ),
    Player(
        titleRes = R.string.player_settings,
        screenClassName = "PlayerSettingsScreen",
        routeClass = PlayerSettingsRoute::class,
        category = SettingsCategory.Playback,
    ),
    Network(
        titleRes = R.string.network_settings,
        screenClassName = "NetworkSettingsScreen",
        routeClass = NetworkSettingsRoute::class,
        category = SettingsCategory.Network,
    ),
    Download(
        titleRes = R.string.download_settings,
        screenClassName = "DownloadSettingsScreen",
        routeClass = DownloadSettingsRoute::class,
        category = SettingsCategory.Download,
    ),
    Mpv(
        titleRes = R.string.mpv_advanced_settings,
        screenClassName = "MpvPlayerSettingsScreen",
        routeClass = MpvPlayerSettingsRoute::class,
        category = SettingsCategory.Playback,
    ),
    HKeyframes(
        titleRes = R.string.h_keyframe_manage,
        screenClassName = "HKeyframesScreen",
        routeClass = HKeyframesRoute::class,
        category = SettingsCategory.Keyframes,
    ),
    SharedHKeyframes(
        titleRes = R.string.shared_h_keyframe_manage,
        screenClassName = "SharedHKeyframesScreen",
        routeClass = SharedHKeyframesRoute::class,
        category = SettingsCategory.Keyframes,
    ),
    Update(
        titleRes = R.string.upd_screen_title,
        screenClassName = "UpdateStatusScreen",
        routeClass = UpdateSettingsRoute::class,
    ),
    HKeyframeSettings(
        titleRes = R.string.h_keyframe_settings,
        screenClassName = "HKeyframeSettingsScreen",
        routeClass = HKeyframeSettingsRoute::class,
        category = SettingsCategory.Keyframes,
    ),
    AboutApp(
        titleRes = R.string.about,
        screenClassName = "AboutAppScreen",
        routeClass = AboutAppRoute::class,
        category = SettingsCategory.About,
    );

    companion object {
        fun fromKey(key: Any?): SettingsDestinationSpec? {
            if (key == null) return null
            return entries.firstOrNull { it.routeClass.isInstance(key) }
        }
    }
}

sealed interface SettingsScreenDestination {
    val titleRes: Int
    val screenClassName: String
    val category: SettingsCategory?
    val titleArg: String? get() = null

    data object Home : SettingsScreenDestination {
        override val titleRes: Int = R.string.settings
        override val screenClassName: String = "HomeSettingsScreen"
        override val category: SettingsCategory? = null
    }

    data class Category(
        val value: SettingsCategory,
    ) : SettingsScreenDestination {
        override val titleRes: Int get() = value.titleRes
        override val screenClassName: String get() = value.screenClassName
        override val category: SettingsCategory get() = value
    }

    data class Site(
        val site: SiteType,
        val section: SiteSettingsSection,
    ) : SettingsScreenDestination {
        override val titleRes: Int
            get() = when (section) {
                SiteSettingsSection.Account -> R.string.settings_site_account
                SiteSettingsSection.LayoutCards -> R.string.settings_site_layout_cards
                else -> R.string.hub_site_settings_title
            }
        override val titleArg: String?
            get() = if (titleRes == R.string.hub_site_settings_title) siteSettingsLabel(site) else null
        override val screenClassName: String = "SiteSettingsSectionScreen"
        override val category: SettingsCategory? = null
    }

    data class Sub(
        val value: SettingsDestinationSpec,
    ) : SettingsScreenDestination {
        override val titleRes: Int get() = value.titleRes
        override val screenClassName: String get() = value.screenClassName
        override val category: SettingsCategory? get() = value.category
    }
}

fun NavKey?.resolveSettingsScreenDestination(): SettingsScreenDestination {
    if (this is SettingsCategoryRoute) {
        val category = SettingsCategory.fromKey(categoryKey)
        if (category != null) return SettingsScreenDestination.Category(category)
    }
    if (this is SettingsSiteSectionRoute) {
        val siteType = SiteType.entries.firstOrNull { it.name == site }
        val siteSection = SiteSettingsSection.fromKey(section)
        if (siteType != null && siteSection != null) {
            return SettingsScreenDestination.Site(siteType, siteSection)
        }
    }
    val spec = SettingsDestinationSpec.fromKey(this)
    return if (spec != null && spec != SettingsDestinationSpec.Home) {
        SettingsScreenDestination.Sub(spec)
    } else {
        SettingsScreenDestination.Home
    }
}
