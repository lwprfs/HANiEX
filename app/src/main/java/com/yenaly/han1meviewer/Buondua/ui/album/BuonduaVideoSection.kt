package com.yenaly.han1meviewer.Buondua.ui.album

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yenaly.han1meviewer.Buondua.common.BuonduaDownloads
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaVideo
import com.yenaly.han1meviewer.Buondua.data.remote.BuonduaNetwork
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.util.copyTextToClipboard

@Composable
fun BuonduaVideoItem(
    albumCode: String,
    coverUrl: String,
    video: BuonduaVideo,
    inline: Boolean?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            when (inline) {
                true -> BuonduaInlinePlayer(url = video.url)
                null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.buondua_checking_video),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                false -> Unit
            }

            SelectionContainer {
                Text(
                    text = video.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    copyTextToClipboard(video.url, video.title)
                    GlobalToasts.show(
                        context.getString(R.string.buondua_link_copied),
                        level = GlobalToasts.ToastLevel.SUCCESS,
                    )
                }) { Text(stringResource(R.string.buondua_copy_link)) }
                Button(onClick = {
                    BuonduaDownloads.enqueueVideo(context, albumCode, video, coverUrl)
                }) { Text(stringResource(R.string.download)) }
            }
        }
    }
}

@Composable
private fun BuonduaInlinePlayer(url: String) {
    var started by remember(url) { mutableStateOf(false) }
    if (!started) {
        Button(
            onClick = { started = true },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.buondua_tap_to_play)) }
        return
    }
    val holder = remember(url) { arrayOfNulls<VideoView>(1) }
    DisposableEffect(url) {
        onDispose { runCatching { holder[0]?.stopPlayback() } }
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        factory = { ctx ->
            VideoView(ctx).also { view ->
                holder[0] = view
                val controller = MediaController(ctx)
                controller.setAnchorView(view)
                view.setMediaController(controller)

                view.setVideoURI(Uri.parse(url), BuonduaNetwork.mediaHeaders())
                view.setOnPreparedListener { it.isLooping = false; view.start() }
            }
        },
    )
}
