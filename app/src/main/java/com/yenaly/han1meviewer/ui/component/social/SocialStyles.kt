package com.yenaly.han1meviewer.ui.component.social

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.SiteType

enum class SocialListStyle(val key: String) {

    CLASSIC("classic"),

    SHOWCASE("showcase"),

    CENTERED("centered"),

    COMPACT("compact"),

    FLAT("flat"),
}

enum class SocialProfileStyle(val key: String) {

    CLASSIC("classic"),

    CENTERED("centered"),

    COMPACT("compact"),

    IMMERSIVE("immersive"),
}

data class SocialStyleConfig(
    val list: SocialListStyle = SocialListStyle.CLASSIC,
    val profile: SocialProfileStyle = SocialProfileStyle.CLASSIC,
    val community: SocialProfileStyle = SocialProfileStyle.CLASSIC,
) {
    companion object {
        val Default = SocialStyleConfig()
    }
}

val LocalSocialStyle = compositionLocalOf { SocialStyleConfig.Default }

object SiteSocialStyle {

    val supportedSites: List<SiteType> = listOf(
        SiteType.FAPFOLDER,
        SiteType.XHAMSTER,
        SiteType.ONAIR,
        SiteType.STRAWBERRY,
        SiteType.REDGIFS,
    )

    fun isSupported(site: SiteType): Boolean = site in supportedSites

    fun hasCommunityList(site: SiteType): Boolean =
        site != SiteType.ONAIR && site != SiteType.STRAWBERRY

    fun hasCommunityPage(site: SiteType): Boolean =
        site == SiteType.FAPFOLDER || site == SiteType.XHAMSTER

    var revision by mutableIntStateOf(0)
        private set

    private fun key(site: SiteType, kind: String) =
        "site_social_${site.name.lowercase()}_$kind"

    fun listStyle(site: SiteType): SocialListStyle {
        val saved = Preferences.preferenceSp.getString(key(site, "list"), null)
        return SocialListStyle.entries.firstOrNull { it.key == saved } ?: SocialListStyle.CLASSIC
    }

    fun profileStyle(site: SiteType): SocialProfileStyle = readProfile(key(site, "profile"))

    fun communityStyle(site: SiteType): SocialProfileStyle = readProfile(key(site, "community"))

    private fun readProfile(prefKey: String): SocialProfileStyle {
        val saved = Preferences.preferenceSp.getString(prefKey, null)
        return SocialProfileStyle.entries.firstOrNull { it.key == saved }
            ?: SocialProfileStyle.CLASSIC
    }

    fun setListStyle(site: SiteType, style: SocialListStyle) {
        Preferences.preferenceSp.edit { putString(key(site, "list"), style.key) }
        revision++
    }

    fun setProfileStyle(site: SiteType, style: SocialProfileStyle) {
        Preferences.preferenceSp.edit { putString(key(site, "profile"), style.key) }
        revision++
    }

    fun setCommunityStyle(site: SiteType, style: SocialProfileStyle) {
        Preferences.preferenceSp.edit { putString(key(site, "community"), style.key) }
        revision++
    }

    fun reset(site: SiteType) {
        Preferences.preferenceSp.edit {
            remove(key(site, "list"))
            remove(key(site, "profile"))
            remove(key(site, "community"))
        }
        revision++
    }

    fun config(site: SiteType): SocialStyleConfig = SocialStyleConfig(
        list = listStyle(site),
        profile = profileStyle(site),
        community = communityStyle(site),
    )
}
