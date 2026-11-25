package com.yenaly.han1meviewer.ui.component

import android.content.Context
import coil3.ImageLoader
import com.yenaly.han1meviewer.SitesHub.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleImageLoader
import com.yenaly.han1meviewer.SitesHub.Coomer.common.CoomerImageLoader
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserImageLoader
import com.yenaly.han1meviewer.SitesHub.HentaiMama.common.HentaiMamaImageLoader
import com.yenaly.han1meviewer.SitesHub.RouVideo.common.RouVideoImageLoader
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosImageLoader
import com.yenaly.han1meviewer.SitesHub.FuckingSession.common.FuckingSessionImageLoader
import com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeImageLoader
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SitesHub.SupJav.common.SupJavImageLoader
import com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterImageLoader

object SiteImageLoaders {

    fun forSite(site: SiteType?, context: Context): ImageLoader? = when (site) {
        SiteType.BUONDUA -> BuonduaImageLoader.get(context)
        SiteType.COSPLAYTELE -> CosplayteleImageLoader.get(context)
        SiteType.COSER -> CoserImageLoader.get(context)
        SiteType.COOMER -> CoomerImageLoader.get(context)
        SiteType.XHAMSTER -> XHamsterImageLoader.get(context)
        SiteType.HENTAIMAMA -> HentaiMamaImageLoader.get(context)
        SiteType.ROUVIDEO -> RouVideoImageLoader.get(context)
        SiteType.SUPJAV -> SupJavImageLoader.get(context)
        SiteType.FULLVIDEOS -> FullVideosImageLoader.get(context)
        SiteType.FUCKINGSESSION -> FuckingSessionImageLoader.get(context)
        SiteType.REDGIFS -> com.yenaly.han1meviewer.SitesHub.RedGifs.common.RedGifsImageLoader.get(context)
        SiteType.XXXTUBE -> com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeImageLoader.get(context)
        SiteType.WUMAOBI -> com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiImageLoader.get(context)
        else -> null
    }

    fun forSiteName(site: String, context: Context): ImageLoader? =
        forSite(runCatching { SiteType.valueOf(site) }.getOrNull(), context)
}
