package com.yenaly.han1meviewer.ui.component

import android.content.Context
import coil3.ImageLoader
import com.yenaly.han1meviewer.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleImageLoader
import com.yenaly.han1meviewer.HentaiMama.common.HentaiMamaImageLoader
import com.yenaly.han1meviewer.RouVideo.common.RouVideoImageLoader
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SupJav.common.SupJavImageLoader
import com.yenaly.han1meviewer.XHamster.common.XHamsterImageLoader

/**
 * Cover images of several sites are hot-link protected (Referer / Cloudflare cookie / UA).
 * Screens that mix sites (offline playlists, unified history) must therefore load each cover
 * with the loader of the site it came from, not the default Coil singleton.
 *
 * Returns null when the site needs nothing special, so callers can pass the result straight
 * to `RetryableImage(imageLoader = ...)` (null = default loader).
 */
object SiteImageLoaders {

    fun forSite(site: SiteType?, context: Context): ImageLoader? = when (site) {
        SiteType.BUONDUA -> BuonduaImageLoader.get(context)
        SiteType.COSPLAYTELE -> CosplayteleImageLoader.get(context)
        SiteType.XHAMSTER -> XHamsterImageLoader.get(context)
        SiteType.HENTAIMAMA -> HentaiMamaImageLoader.get(context)
        SiteType.ROUVIDEO -> RouVideoImageLoader.get(context)
        SiteType.SUPJAV -> SupJavImageLoader.get(context)
        else -> null
    }

    /** Same as [forSite] but takes the raw `LocalPlaylistItem.site` string. */
    fun forSiteName(site: String, context: Context): ImageLoader? =
        forSite(runCatching { SiteType.valueOf(site) }.getOrNull(), context)
}
