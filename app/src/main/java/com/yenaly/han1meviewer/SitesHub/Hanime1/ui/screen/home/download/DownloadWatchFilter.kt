package com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.download

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.util.offline.LocalWatchEntry

enum class DownloadWatchFilter(@StringRes val labelRes: Int) {
    ALL(R.string.dl_filter_all),
    UNWATCHED(R.string.dl_filter_unwatched),
    IN_PROGRESS(R.string.dl_filter_in_progress),
    WATCHED(R.string.dl_filter_watched),
}

fun DownloadWatchFilter.matches(entry: LocalWatchEntry?): Boolean {
    val completed = entry?.completed == true
    val started = !completed && (entry?.positionMs ?: 0L) > 0L
    return when (this) {
        DownloadWatchFilter.ALL -> true
        DownloadWatchFilter.WATCHED -> completed
        DownloadWatchFilter.IN_PROGRESS -> started
        DownloadWatchFilter.UNWATCHED -> !completed && !started
    }
}

@Composable
fun DownloadWatchFilterRow(
    selected: DownloadWatchFilter,
    onSelect: (DownloadWatchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DownloadWatchFilter.entries.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(stringResource(option.labelRes)) },
            )
        }
    }
}
