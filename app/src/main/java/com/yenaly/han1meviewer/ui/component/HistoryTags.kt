package com.yenaly.han1meviewer.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.util.offline.LocalDownloadedCodes
import com.yenaly.han1meviewer.util.offline.LocalWatchStore

enum class HistoryTagKind { DOWNLOADED, WATCHED, TIME }

data class HistoryTag(val text: String, val kind: HistoryTagKind)

private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
fun rememberHistoryTags(
    site: SiteType,
    rawCode: String,
    positionMs: Long,
    durationMs: Long,
    completed: Boolean,
    watchDate: Long = 0L,
): List<HistoryTag> {
    val dish = remember(site, rawCode) { SiteCheckIn.dishCode(site, rawCode) }
    val downloadedCodes by LocalDownloadedCodes.codes.collectAsStateWithLifecycle()
    val localEntries by LocalWatchStore.entries.collectAsStateWithLifecycle()
    val local = localEntries[dish]

    var pos = positionMs
    var dur = durationMs
    var done = completed
    if (local != null && local.watchedAt >= watchDate) {
        pos = local.positionMs
        dur = if (local.durationMs > 0) local.durationMs else durationMs
        done = local.completed
    }

    val tDownloaded = stringResource(R.string.tag_downloaded)
    val tWatched = stringResource(R.string.tag_watched)
    val timeText = if (pos > 0L) {
        stringResource(
            R.string.tag_watched_at,
            if (dur > 0L) "${formatClock(pos)} / ${formatClock(dur)}" else formatClock(pos),
        )
    } else null

    return buildList {
        if (dish in downloadedCodes) add(HistoryTag(tDownloaded, HistoryTagKind.DOWNLOADED))
        if (done) add(HistoryTag(tWatched, HistoryTagKind.WATCHED))
        if (timeText != null) add(HistoryTag(timeText, HistoryTagKind.TIME))
    }
}

@Composable
fun rememberLocalWatchTags(dishCode: String): List<HistoryTag> {
    val localEntries by LocalWatchStore.entries.collectAsStateWithLifecycle()
    val local = localEntries[dishCode]
    val tWatched = stringResource(R.string.tag_watched)
    val pos = local?.positionMs ?: 0L
    val dur = local?.durationMs ?: 0L
    val timeText = if (pos > 0L) {
        stringResource(
            R.string.tag_watched_at,
            if (dur > 0L) "${formatClock(pos)} / ${formatClock(dur)}" else formatClock(pos),
        )
    } else null
    return buildList {
        if (local?.completed == true) add(HistoryTag(tWatched, HistoryTagKind.WATCHED))
        if (timeText != null) add(HistoryTag(timeText, HistoryTagKind.TIME))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryTagRow(tags: List<HistoryTag>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            val (container, content) = when (tag.kind) {
                HistoryTagKind.DOWNLOADED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                HistoryTagKind.WATCHED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                HistoryTagKind.TIME -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            }
            Surface(color = container, contentColor = content, shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = tag.text,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
