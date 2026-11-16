package com.yenaly.han1meviewer.ui.screen.settings

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategory
import com.yenaly.han1meviewer.util.translation.AppTranslation

/**
 * One searchable setting. Titles and summaries are string resources, so search follows the
 * active app language and the same texts the settings screens display.
 */
internal class SettingsSearchEntry(
    val category: SettingsCategory,
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int? = null,
    val isAvailable: () -> Boolean = { true },
)

/**
 * Index of every setting reachable from a category screen. When a setting is added to or
 * removed from a category screen, update its entry here.
 */
internal val SettingsSearchIndex: List<SettingsSearchEntry> = listOf(
    // Appearance
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.dark_theme),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.theme_color),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.pure_black_dark_mode, R.string.pure_black_dark_mode_summary),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.home_category_layout),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.site_video_grid_columns, R.string.hub_video_grid_columns_desc),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.search_grid_columns_title, R.string.search_grid_columns_summary),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.horizontal_card_count_title, R.string.horizontal_card_count_summary),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.search_pagination, R.string.search_pagination_summary),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.search_artist_ignore_video_type, R.string.search_artist_ignore_video_type_summary),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.related_video_as_tab, R.string.related_video_as_tab_summary),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.app_lang, R.string.app_lang_sum),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.rouvideo_tr_title, R.string.rouvideo_tr_privacy) { AppTranslation.available },
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.show_exit_confirm, R.string.show_exit_confirm_summary),
    SettingsSearchEntry(SettingsCategory.Appearance, R.string.site_visibility_title, R.string.site_visibility_summary),
    // Playback
    SettingsSearchEntry(SettingsCategory.Playback, R.string.video_language),
    SettingsSearchEntry(SettingsCategory.Playback, R.string.default_video_quilty),
    SettingsSearchEntry(SettingsCategory.Playback, R.string.allow_pip_title, R.string.allow_pip_disc),
    SettingsSearchEntry(SettingsCategory.Playback, R.string.resume_playback_title, R.string.resume_playback_summary),
    SettingsSearchEntry(SettingsCategory.Playback, R.string.show_played_indicator, R.string.show_played_indicator_summary),
    SettingsSearchEntry(SettingsCategory.Playback, R.string.watched_progress_threshold),
    SettingsSearchEntry(SettingsCategory.Playback, R.string.disable_mobile_data_warning, R.string.disable_mobile_data_warning_summary),
    SettingsSearchEntry(SettingsCategory.Playback, R.string.disable_comments_title, R.string.disable_comments_sum),
    SettingsSearchEntry(SettingsCategory.Playback, R.string.player_settings),
    // Network
    SettingsSearchEntry(SettingsCategory.Network, R.string.network_settings_site, R.string.network_settings_site_summary),
    SettingsSearchEntry(SettingsCategory.Network, R.string.domain_name),
    SettingsSearchEntry(SettingsCategory.Network, R.string.mirror_latency_entry),
    SettingsSearchEntry(SettingsCategory.Network, R.string.custom_mirror_site),
    SettingsSearchEntry(SettingsCategory.Network, R.string.proxy),
    SettingsSearchEntry(SettingsCategory.Network, R.string.use_backup_media_cdn, R.string.use_backup_media_cdn_summary),
    SettingsSearchEntry(SettingsCategory.Network, R.string.use_built_in_hosts, R.string.use_built_in_hosts_summary),
    SettingsSearchEntry(SettingsCategory.Network, R.string.custom_hosts),
    SettingsSearchEntry(SettingsCategory.Network, R.string.use_doh),
    SettingsSearchEntry(SettingsCategory.Network, R.string.test_doh, R.string.test_doh_summary),
    SettingsSearchEntry(SettingsCategory.Network, R.string.apply_deep_links, R.string.apply_deep_links_summary),
    // Download
    SettingsSearchEntry(SettingsCategory.Download, R.string.download_path),
    SettingsSearchEntry(SettingsCategory.Download, R.string.pref_export_downloads_title, R.string.pref_export_downloads_summary),
    SettingsSearchEntry(SettingsCategory.Download, R.string.download_count_limit),
    SettingsSearchEntry(SettingsCategory.Download, R.string.download_speed_limit),
    SettingsSearchEntry(SettingsCategory.Download, R.string.download_auto_group, R.string.download_auto_group_summary),
    SettingsSearchEntry(SettingsCategory.Download, R.string.collapse_downloaded_groups, R.string.collapse_downloaded_groups_summary),
    // Keyframes
    SettingsSearchEntry(SettingsCategory.Keyframes, R.string.h_keyframes_enable),
    SettingsSearchEntry(SettingsCategory.Keyframes, R.string.h_keyframe_manage),
    SettingsSearchEntry(SettingsCategory.Keyframes, R.string.shared_h_keyframes_enable, R.string.shared_h_keyframes_enable_tip),
    SettingsSearchEntry(SettingsCategory.Keyframes, R.string.shared_h_keyframes_use_first, R.string.shared_h_keyframes_use_first_tip),
    SettingsSearchEntry(SettingsCategory.Keyframes, R.string.shared_h_keyframe_manage, R.string.shared_h_keyframe_manage_tip),
    SettingsSearchEntry(SettingsCategory.Keyframes, R.string.show_prompt_when_countdown),
    SettingsSearchEntry(SettingsCategory.Keyframes, R.string.when_countdown_remind),
    // Privacy
    SettingsSearchEntry(SettingsCategory.Privacy, R.string.analytics_title, R.string.analytics_summary),
    SettingsSearchEntry(SettingsCategory.Privacy, R.string.use_lock_screen, R.string.use_lock_screen_sum),
    SettingsSearchEntry(SettingsCategory.Privacy, R.string.fake_app_icon, R.string.select_fake_icon),
    // Data
    SettingsSearchEntry(SettingsCategory.Data, R.string.hist_all_sites_title),
    SettingsSearchEntry(SettingsCategory.Data, R.string.backup_export_title, R.string.backup_export_summary),
    SettingsSearchEntry(SettingsCategory.Data, R.string.backup_import_title, R.string.backup_import_summary),
    SettingsSearchEntry(SettingsCategory.Data, R.string.clear_cache),
    // About
    SettingsSearchEntry(SettingsCategory.About, R.string.check_update),
    SettingsSearchEntry(SettingsCategory.About, R.string.use_ci_update_channel),
    SettingsSearchEntry(SettingsCategory.About, R.string.update_installer, R.string.update_installer_summary),
    SettingsSearchEntry(SettingsCategory.About, R.string.update_popup_interval_days),
    SettingsSearchEntry(SettingsCategory.About, R.string.submit_bug, R.string.submit_bug_summary),
    SettingsSearchEntry(SettingsCategory.About, R.string.forum, R.string.forum_summary),
    SettingsSearchEntry(SettingsCategory.About, R.string.open_source_license),
    SettingsSearchEntry(SettingsCategory.About, R.string.about),
)

internal data class SettingsSearchResult(
    val category: SettingsCategory,
    val title: String,
    val categoryTitle: String,
)

/** Settings whose title, summary or category name contains every word of [query]. */
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
internal fun rememberSettingsSearchResults(query: String): List<SettingsSearchResult> {
    val context = LocalContext.current
    return remember(query, context.resources.configuration) {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return@remember emptyList()
        SettingsSearchIndex.mapNotNull { entry ->
            if (!entry.isAvailable()) return@mapNotNull null
            val title = context.getString(entry.titleRes)
            val categoryTitle = context.getString(entry.category.titleRes)
            val haystack = buildString {
                append(title).append(' ')
                entry.summaryRes?.let { append(context.getString(it)).append(' ') }
                append(categoryTitle)
            }.lowercase()
            if (words.all { it in haystack }) {
                SettingsSearchResult(entry.category, title, categoryTitle)
            } else {
                null
            }
        }
    }
}

@Composable
internal fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        placeholder = { Text(stringResource(R.string.settings_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.settings_search_clear),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

/** A matching setting: its title with the category it lives in. Opens that category. */
@Composable
internal fun SettingsSearchResultItem(
    result: SettingsSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = result.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = result.categoryTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SettingsSearchEmpty(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.settings_search_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp),
    )
}
