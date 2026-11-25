package com.yenaly.han1meviewer.SitesHub.Buondua.ui.album

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SitesHub.Buondua.common.BuonduaDownloads
import com.yenaly.han1meviewer.SitesHub.Buondua.data.model.BuonduaVideo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.util.copyTextToClipboard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuonduaVideoControls(
    albumCode: String,
    coverUrl: String,
    videos: List<BuonduaVideo>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val current = videos.getOrNull(selected) ?: return
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (videos.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                videos.forEachIndexed { i, _ ->
                    FilterChip(
                        selected = i == selected,
                        onClick = { onSelect(i) },
                        label = { Text("${stringResource(R.string.buondua_section_videos)} ${i + 1}") },
                    )
                }
            }
        }
        Text(
            text = current.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        SelectionContainer {
            Text(
                text = current.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                copyTextToClipboard(current.url, current.title)
                GlobalToasts.show(
                    context.getString(R.string.buondua_link_copied),
                    level = GlobalToasts.ToastLevel.SUCCESS,
                )
            }) { Text(stringResource(R.string.buondua_copy_link)) }
            Button(onClick = {
                BuonduaDownloads.enqueueVideo(context, albumCode, current, coverUrl)
            }) { Text(stringResource(R.string.download)) }
        }
    }
}
