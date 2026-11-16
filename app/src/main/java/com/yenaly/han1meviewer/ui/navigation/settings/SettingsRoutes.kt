package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.annotation.StringRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.yenaly.han1meviewer.R
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
object HomeSettingsRoute

@Serializable
data class SettingsCategoryRoute(val categoryKey: String)

@Serializable
object PlayerSettingsRoute

@Serializable
data class SitePlayerSettingsRoute(val site: String)

@Serializable
object NetworkSettingsRoute

@Serializable
object DownloadSettingsRoute

@Serializable
object MpvPlayerSettingsRoute

@Serializable
object HKeyframesRoute

@Serializable
object SharedHKeyframesRoute

@Serializable
object HKeyframeSettingsRoute

@Serializable
object UpdateSettingsRoute

@Serializable
object TranslationSettingsRoute

@Serializable
object MediaControlsSettingsRoute

@Serializable
object TranslationCacheRoute

@Serializable
object AboutAppRoute

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
        fun fromDestination(destination: NavDestination?): SettingsDestinationSpec? {
            if (destination == null) return null
            return entries.firstOrNull { destination.hasRoute(it.routeClass) }
        }
    }
}

sealed interface SettingsScreenDestination {
    val titleRes: Int
    val screenClassName: String
    val category: SettingsCategory?

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

    data class Sub(
        val value: SettingsDestinationSpec,
    ) : SettingsScreenDestination {
        override val titleRes: Int get() = value.titleRes
        override val screenClassName: String get() = value.screenClassName
        override val category: SettingsCategory? get() = value.category
    }
}

fun NavBackStackEntry.resolveSettingsScreenDestination(): SettingsScreenDestination {
    if (destination.hasRoute<SettingsCategoryRoute>()) {
        val category = runCatching { toRoute<SettingsCategoryRoute>() }
            .getOrNull()
            ?.let { SettingsCategory.fromKey(it.categoryKey) }
        if (category != null) return SettingsScreenDestination.Category(category)
    }
    val spec = SettingsDestinationSpec.fromDestination(destination)
    return if (spec != null && spec != SettingsDestinationSpec.Home) {
        SettingsScreenDestination.Sub(spec)
    } else {
        SettingsScreenDestination.Home
    }
}
