package com.yenaly.han1meviewer.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HA1_GITHUB_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SettingInfoItem
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.lazy.LazyColumn
import com.yenaly.han1meviewer.util.browse
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val EASTER_EGG_TAP_COUNT = 7

@Composable
fun AboutAppScreen() {
    val context = LocalContext.current
    var easterEggTapCount by remember { mutableIntStateOf(0) }
    val easterEggToast = stringResource(R.string.about_easter_egg_toast)

    LaunchedEffect(easterEggTapCount) {
        if (easterEggTapCount in 1 until EASTER_EGG_TAP_COUNT) {
            delay(2_000.milliseconds)
            easterEggTapCount = 0
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            AboutHeaderCard(
                onClick = {
                    easterEggTapCount++
                    if (easterEggTapCount >= EASTER_EGG_TAP_COUNT) {
                        easterEggTapCount = 0
                        GlobalToasts.show(
                            easterEggToast,
                            level = GlobalToasts.ToastLevel.INFO,
                        )
                    }
                },
            )
        }

        item { AboutGroupTitle(stringResource(R.string.about_app_info)) }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_build_type),
                valueText = BuildConfig.BUILD_TYPE,
                iconRes = R.drawable.baseline_custom_24,
            )
        }
        if (BuildConfig.COMMIT_SHA.isNotBlank()) {
            item {
                SettingInfoItem(
                    title = stringResource(R.string.about_build_commit),
                    valueText = BuildConfig.COMMIT_SHA.take(7),
                    iconRes = R.drawable.baseline_git_merge_24,
                )
            }
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_package_name),
                valueText = context.packageName,
                iconRes = R.drawable.ic_baseline_info_24,
            )
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_current_site),
                valueText = Preferences.baseUrl,
                iconRes = R.drawable.baseline_domain_24,
            )
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_android_version),
                valueText = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                iconRes = R.drawable.baseline_android_24,
            )
        }
        item {
            SettingInfoItem(
                title = stringResource(R.string.about_device_model),
                valueText = listOf(Build.MANUFACTURER, Build.MODEL)
                    .filter { it.isNotBlank() }
                    .joinToString(" "),
                iconRes = R.drawable.baseline_mobile_data_24,
            )
        }

        item { AboutGroupTitle(stringResource(R.string.about_project_support)) }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.about_github_repo),
                summary = stringResource(R.string.about_github_repo_summary),
                iconRes = R.drawable.ic_baseline_info_24,
                onClick = { context.browse(HA1_GITHUB_URL) },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.submit_bug),
                summary = stringResource(R.string.submit_bug_summary),
                iconRes = R.drawable.baseline_bug_report_24,
                onClick = { context.browse(HA1_GITHUB_ISSUE_URL) },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(R.string.forum),
                summary = stringResource(R.string.forum_summary),
                iconRes = R.drawable.baseline_forum_24,
                onClick = { context.browse(HA1_GITHUB_FORUM_URL) },
            )
        }

        item(key = "about_disclaimer_title") {
            AboutGroupTitle(stringResource(R.string.about_disclaimer_title))
        }

        item(key = "about_disclaimer_group") {
            DisclaimerGroup {
                DisclaimerRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.about_disclaimer_origin_title),
                    text = stringResource(R.string.about_disclaimer_origin),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp, end = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                DisclaimerRow(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.about_disclaimer_data_title),
                    text = stringResource(R.string.about_disclaimer_data),
                )
            }
        }

        item(key = "about_disclaimer_region") {
            HighlightDisclaimerCard(
                title = stringResource(R.string.about_disclaimer_region_title),
                text = stringResource(R.string.about_disclaimer_region),
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        item(key = "about_support_original") {
            AboutSupportNote(stringResource(R.string.about_support_original))
        }

        item(key = "about_last_updated") {
            Text(
                text = stringResource(
                    R.string.about_last_updated,
                    stringResource(R.string.about_last_updated_date),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 12.dp),
            )
        }

        item(key = "about_thanks") {
            AboutThanks(stringResource(R.string.about_thanks))
        }
    }
}

@Composable
private fun AboutGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = 8.dp,
            end = 8.dp,
            top = 16.dp,
            bottom = 4.dp,
        ),
    )
}
