package com.yenaly.han1meviewer.FapFolder.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.util.offline.OfflineFs

@Composable
fun rememberFapFolderImageGate(): (onReady: () -> Unit) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { onReady: () -> Unit ->
            if (OfflineFs.isReady(context)) {
                onReady()
            } else {
                GlobalToasts.show(
                    context.getString(R.string.offline_pick_download_folder),
                    level = GlobalToasts.ToastLevel.WARNING,
                )
            }
        }
    }
}
