package com.yenaly.han1meviewer.SitesHub.Coser.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserConstants
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteChipRow

@Composable
fun CoserFilterBar(
    filters: CoserConstants.Filters,
    onChange: (CoserConstants.Filters) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lengthLabel = stringResource(R.string.coser_filter_length)
    val qualityLabel = stringResource(R.string.coser_filter_quality)
    val sortLabel = stringResource(R.string.coser_filter_sort)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SiteChipRow(
            options = CoserConstants.LENGTH_OPTIONS,
            selected = CoserConstants.LENGTH_OPTIONS.first { it.first == filters.mins },
            label = { if (it.first == 0) lengthLabel else it.second },
            onSelect = { onChange(filters.copy(mins = it.first)) },
            key = { "m${it.first}" },
            horizontalPadding = 0.dp,
        )
        SiteChipRow(
            options = CoserConstants.QUALITY_OPTIONS,
            selected = CoserConstants.QUALITY_OPTIONS.first { it.first == filters.quality },
            label = { if (it.first == 0) qualityLabel else it.second },
            onSelect = { onChange(filters.copy(quality = it.first)) },
            key = { "q${it.first}" },
            horizontalPadding = 0.dp,
        )
        SiteChipRow(
            options = CoserConstants.SORT_OPTIONS,
            selected = CoserConstants.SORT_OPTIONS.first { it.first == filters.sort },
            label = { if (it.first.isEmpty()) sortLabel else it.second },
            onSelect = { onChange(filters.copy(sort = it.first)) },
            key = { "s${it.first}" },
            horizontalPadding = 0.dp,
        )
    }
}

@Composable
fun CoserFilterHeader(
    filters: CoserConstants.Filters,
    onChange: (CoserConstants.Filters) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(!filters.isDefault) }
    Column(modifier = modifier.fillMaxWidth()) {
        androidx.compose.material3.FilterChip(
            selected = open || !filters.isDefault,
            onClick = { open = !open },
            label = { androidx.compose.material3.Text(stringResource(R.string.coser_filters)) },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.padding(0.dp),
                )
            },
        )
        if (open) CoserFilterBar(filters = filters, onChange = onChange)
    }
}

val CoserFiltersSaver = androidx.compose.runtime.saveable.listSaver<CoserConstants.Filters, Any>(
    save = { listOf(it.mins, it.quality, it.sort) },
    restore = { CoserConstants.Filters(it[0] as Int, it[1] as Int, it[2] as String) },
)
