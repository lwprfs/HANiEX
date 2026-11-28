package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.settings

import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.HighQuality
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberryQuality
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Person
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingRadioItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberryConstants
import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberrySitePreferences
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.local.StrawberryHistoryRepo
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.local.StrawberryProfileCache
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.model.StrawberryUserProfile
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryAuthRepo
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryCookieManager
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetworkRepo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.util.applicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrawberrySettingsScreen(
    onBack: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenAnnouncements: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenTags: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var showHostDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var preferredQuality by remember { mutableStateOf(StrawberrySitePreferences.preferredQuality) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }
    var currentHost by remember { mutableStateOf(StrawberrySitePreferences.currentHostname()) }
    val hasCookies = StrawberryCookieManager.hasCookies
    var profile by remember { mutableStateOf<StrawberryUserProfile?>(StrawberryProfileCache.current()) }
    var profileLoading by remember { mutableIntStateOf(0) }
    val usernameHint = remember(hasCookies) { StrawberryAuthRepo.currentUsernameHint() }

    fun refreshProfile() {
        val name = profile?.username
            ?: StrawberryProfileCache.getCurrentUsername()
            ?: usernameHint
            ?: return
        profileLoading += 1
        MainScope().launch {
            StrawberryNetworkRepo.getUserProfile(name).collect { s ->
                when (s) {
                    is VideoLoadingState.Success -> {
                        profile = s.info
                        StrawberryProfileCache.put(s.info.username, s.info)
                    }
                    else -> Unit
                }
                profileLoading = (profileLoading - 1).coerceAtLeast(0)
            }
        }
    }

    LaunchedEffect(hasCookies) {
        if (hasCookies && profile == null) refreshProfile()
    }

    fun performLogout() {
        loggingOut = true
        scope.launch {
            val result = StrawberryAuthRepo.performLogout()
            loggingOut = false
            showLogoutDialog = false
            StrawberryProfileCache.clearAll()
            profile = null
            when (result) {
                is StrawberryAuthRepo.AuthResult.Success -> {
                    GlobalToasts.show(applicationContext.getString(R.string.site_logged_out), level = GlobalToasts.ToastLevel.SUCCESS)
                    onLoggedOut()
                }
                is StrawberryAuthRepo.AuthResult.FormError,
                is StrawberryAuthRepo.AuthResult.NetworkError,
                is StrawberryAuthRepo.AuthResult.LoginRequired -> {
                    GlobalToasts.show(
                        applicationContext.getString(R.string.strawberry_session_cleared_locally),
                        level = GlobalToasts.ToastLevel.WARNING,
                    )
                    onLoggedOut()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.strawberry_strawberry_settings)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                run {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = profile != null) {
                                        profile?.let { onOpenProfile(it.username) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                val avatar = profile?.avatarUrl
                                if (!avatar.isNullOrBlank()) {
                                    AsyncImage(
                                        model = avatar,
                                        contentDescription = profile?.displayName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val label = profile?.displayName?.firstOrNull()?.uppercase()
                                                ?: usernameHint?.firstOrNull()?.uppercase()
                                                ?: "?"
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile?.displayName
                                            ?: usernameHint
                                            ?: if (hasCookies) stringResource(R.string.site_signed_in) else stringResource(R.string.site_not_signed_in),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (profileLoading > 0) {
                                        Spacer(Modifier.width(8.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                                val subline = buildString {
                                    profile?.let { p ->
                                        p.followerCount?.let { append(stringResource(R.string.strawberry_followers, it) + " ") }
                                        p.videoCount?.let { append(stringResource(R.string.strawberry_videos, it) + " ") }
                                        p.pictureCount?.let { append(stringResource(R.string.strawberry_pictures, it)) }
                                    }
                                    if (isEmpty()) {
                                        append(
                                            if (hasCookies) stringResource(R.string.strawberry_session_active_short)
                                            else stringResource(R.string.strawberry_no_cookies_device)
                                        )
                                    } else {
                                        trimEnd(' ', '·')
                                    }
                                }
                                Text(
                                    text = subline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (hasCookies) {
                                IconButton(onClick = { refreshProfile() }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.strawberry_refresh_profile),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = { showLogoutDialog = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = stringResource(R.string.logout),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                        SettingNavigationItem(
                            title = if (hasCookies) stringResource(R.string.strawberry_manage_cookies)
                            else stringResource(R.string.strawberry_add_cookies),
                            icon = Icons.Default.Cookie,
                            onClick = onOpenCookies,
                        )
                        if (!hasCookies) {
                            SettingNavigationItem(
                                title = stringResource(R.string.site_log_in),
                                icon = Icons.AutoMirrored.Filled.Login,
                                onClick = onOpenLogin,
                            )
                        } else if (profile != null) {
                            SettingNavigationItem(
                                title = stringResource(R.string.strawberry_view_my_profile),
                                icon = Icons.Default.Person,
                                onClick = { profile?.let { onOpenProfile(it.username) } },
                            )
                        }
                    }
                }
            }

            item { SettingSectionHeader(stringResource(R.string.site_drawer_section_browse)) }
            item { SettingHintText(stringResource(R.string.strawberry_announcements_chat_and_tags_moved_here)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.strawberry_announcements_title),
                    summary = stringResource(R.string.strawberry_site_news_and_mirror_updates),
                    icon = Icons.Default.Campaign,
                    onClick = onOpenAnnouncements,
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.drawer_chat),
                    summary = stringResource(R.string.strawberry_site_wide_community_chat),
                    icon = Icons.Default.Email,
                    onClick = onOpenChat,
                )
            }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.drawer_tags),
                    summary = stringResource(R.string.strawberry_browse_all_tags),
                    icon = Icons.Default.Info,
                    onClick = onOpenTags,
                )
            }

            item { SettingSectionHeader(stringResource(R.string.strawberry_stream_section)) }
            item { SettingHintText(stringResource(R.string.strawberry_stream_preference_hint)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.strawberry_stream_preference),
                    summary = StrawberryQuality.label(preferredQuality),
                    icon = Icons.Default.HighQuality,
                    onClick = { showQualityDialog = true },
                )
            }

            item { SettingSectionHeader(stringResource(R.string.strawberry_mirror_host)) }
            item { SettingHintText(stringResource(R.string.strawberry_strawberry_is_a_tor_only_site)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.strawberry_change_host),
                    summary = currentHost,
                    icon = Icons.Default.Dns,
                    onClick = { showHostDialog = true },
                )
            }

            item { SettingSectionHeader(stringResource(R.string.site_history)) }
            item {
                SettingNavigationItem(
                    title = stringResource(R.string.site_clear_history),
                    summary = stringResource(R.string.strawberry_clear_all_watch_history_stored_on),
                    icon = Icons.Default.Delete,
                    onClick = { showClearHistoryDialog = true },
                )
            }

            item { SettingSectionHeader(stringResource(R.string.strawberry_about_strawberry)) }
            item { SettingHintText(stringResource(R.string.strawberry_self_hosted_video_site_run_by)) }
        }
    }

    if (showHostDialog) {
        AlertDialog(
            onDismissRequest = { showHostDialog = false },
            title = { Text(stringResource(R.string.strawberry_choose_strawberry_host)) },
            text = {
                Column {
                    StrawberryConstants.STRAWBERRY_HOSTNAME.forEachIndexed { index, host ->
                        SettingRadioItem(
                            title = host,
                            selected = currentHost == host,
                            onClick = {
                                StrawberrySitePreferences.setMirrorIndex(index)
                                currentHost = host
                                StrawberryNetwork.rebuildNetwork()
                                showHostDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHostDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(stringResource(R.string.strawberry_stream_preference)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    StrawberryQuality.CHOICES.forEach { q ->
                        SettingRadioItem(
                            title = StrawberryQuality.label(q),
                            selected = preferredQuality == q,
                            onClick = {
                                StrawberrySitePreferences.preferredQuality = q
                                preferredQuality = q
                                showQualityDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.site_clear_history)) },
            text = { Text(stringResource(R.string.strawberry_this_will_remove_all_strawberry_watch)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearHistoryDialog = false
                    MainScope().launch { StrawberryHistoryRepo.deleteAll() }
                }) { Text(stringResource(R.string.site_clear), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!loggingOut) showLogoutDialog = false },
            title = { Text(stringResource(R.string.strawberry_log_out_of_strawberry)) },
            text = { Text(stringResource(R.string.site_your_session_cookies_will_be_removed)) },
            confirmButton = {
                TextButton(
                    onClick = { performLogout() },
                    enabled = !loggingOut,
                ) {
                    if (loggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    enabled = !loggingOut,
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
