package com.yenaly.han1meviewer.ui.screen.library

import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.ui.screen.library.offline.OfflineMediaScreen
import com.yenaly.han1meviewer.util.offline.OfflineKind

@Composable
fun GifDownloadsScreen(onBack: () -> Unit) {
    OfflineMediaScreen(kind = OfflineKind.SHORTS, onBack = onBack)
}
