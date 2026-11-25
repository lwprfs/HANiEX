package com.yenaly.han1meviewer.ui.screen.library

import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.screen.library.offline.OfflineMediaScreen
import com.yenaly.han1meviewer.util.offline.OfflineKind

val IMAGE_DOWNLOAD_SITES = setOf(
    SiteType.FAPFOLDER, SiteType.BUONDUA, SiteType.XHAMSTER, SiteType.COSPLAYTELE, SiteType.COSER,
    SiteType.COOMER, SiteType.LEAKGALLERY,
)

@Composable
fun ImageDownloadsScreen(onBack: () -> Unit) {
    OfflineMediaScreen(kind = OfflineKind.PHOTOS, onBack = onBack)
}
