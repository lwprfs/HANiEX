package com.yenaly.han1meviewer.ui.component

import android.content.Context
import coil3.ImageLoader
import com.yenaly.han1meviewer.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleImageLoader
import com.yenaly.han1meviewer.Coomer.common.CoomerImageLoader
import com.yenaly.han1meviewer.Coser.common.CoserImageLoader
import com.yenaly.han1meviewer.HentaiMama.common.HentaiMamaImageLoader
import com.yenaly.han1meviewer.RouVideo.common.RouVideoImageLoader
import com.yenaly.han1meviewer.FullVideos.common.FullVideosImageLoader
import com.yenaly.han1meviewer.FuckingSession.common.FuckingSessionImageLoader
import com.yenaly.han1meviewer.XXXTube.common.XXXTubeImageLoader
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.SupJav.common.SupJavImageLoader
import com.yenaly.han1meviewer.XHamster.common.XHamsterImageLoader

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
        SiteType.REDGIFS -> com.yenaly.han1meviewer.RedGifs.common.RedGifsImageLoader.get(context)
        SiteType.XXXTUBE -> com.yenaly.han1meviewer.XXXTube.common.XXXTubeImageLoader.get(context)
        SiteType.WUMAOBI -> com.yenaly.han1meviewer.Wumaobi.common.WumaobiImageLoader.get(context)
        else -> null
    }

    fun forSiteName(site: String, context: Context): ImageLoader? =
        forSite(runCatching { SiteType.valueOf(site) }.getOrNull(), context)
}
