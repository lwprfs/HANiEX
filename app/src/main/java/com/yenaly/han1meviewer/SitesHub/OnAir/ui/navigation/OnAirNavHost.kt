package com.yenaly.han1meviewer.SitesHub.OnAir.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.SiteChromeVisibility
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.util.applicationContext
import com.yenaly.han1meviewer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.SitesHub.OnAir.data.local.OnAirProfileCache
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirAuthProbe
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirNetwork
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.OnAirTorChecker
import com.yenaly.han1meviewer.SitesHub.OnAir.data.remote.TorFailure
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.announcement.OnAirAnnouncementsScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.auth.OnAirLoginScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.auth.OnAirRegisterScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.chat.OnAirChatScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.gate.OnAirGateScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.gate.OnAirGateState
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.history.OnAirHistoryScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.home.OnAirHomeScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.home.OnAirHomeViewModel
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.picture.OnAirPicturesScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.search.OnAirSearchScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.search.OnAirSearchViewModel
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.github.GithubDownloaderScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.settings.OnAirCookieSettingsScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.settings.OnAirSettingsScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.tag.OnAirTagScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.tag.OnAirTagsIndexScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.user.OnAirUserScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.video.OnAirVideoScreen
import com.yenaly.han1meviewer.SitesHub.OnAir.ui.video.OnAirVideoViewModel
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsEntry
import com.yenaly.han1meviewer.ui.screen.settings.SiteSettingsHubScreen
import kotlinx.coroutines.delay

@Composable
fun OnAirNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    var gateState by rememberSaveable(stateSaver = OnAirGateState.Saver) {
        mutableStateOf(OnAirGateState.Initializing)
    }
    var retryTick by rememberSaveable { mutableStateOf(0L) }
    var showTorModal by rememberSaveable { mutableStateOf(false) }
    var torModalReason by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(retryTick) {
        OnAirNetwork.init(activity.applicationContext)

        gateState = OnAirGateState.CheckingTor
        delay(16)

        val tor = OnAirTorChecker.check()
        if (!tor.isTorExit) {
            val message = when (tor.failure) {
                TorFailure.Timeout ->
                    applicationContext.getString(R.string.onair_tor_timeout)
                TorFailure.NetworkUnreachable ->
                    applicationContext.getString(R.string.onair_tor_unreachable)
                TorFailure.NonTorExit ->
                    applicationContext.getString(R.string.onair_tor_non_exit)
                else ->
                    tor.message.ifBlank { applicationContext.getString(R.string.onair_tor_required) }
            }
            gateState = OnAirGateState.TorBlocked(
                reason = message,
                torFailure = when (tor.failure) {
                    TorFailure.Timeout -> OnAirGateState.TorFailureKind.Timeout
                    TorFailure.NetworkUnreachable -> OnAirGateState.TorFailureKind.NetworkUnreachable
                    TorFailure.NonTorExit -> OnAirGateState.TorFailureKind.NonTorExit
                    else -> OnAirGateState.TorFailureKind.Unknown
                },
            )
            torModalReason = message
            showTorModal = true
            return@LaunchedEffect
        }

        gateState = OnAirGateState.Probing
        delay(16)
        val probe = OnAirAuthProbe.probe(activity)
        gateState = if (probe.ok) {
            OnAirGateState.Passed
        } else {
            OnAirGateState.ContentError(
                reason = probe.userMessage.orEmpty(),
                suggestSwitchSite = probe.suggestSwitchSite,
                suggestWait = probe.suggestWait,
                suggestRelogin = probe.suggestRelogin,
                probeFailure = probe.failure,
            )
        }
    }

    if (showTorModal) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.onair_tor_not_connected)) },
            text = {
                Text(
                    torModalReason.ifBlank {
                        stringResource(R.string.onair_tor_modal_default)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showTorModal = false
                    retryTick = System.currentTimeMillis()
                }) { Text(stringResource(R.string.retry)) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        showTorModal = false
                        activity.requestSiteSwitch()
                    }) { Text(stringResource(R.string.site_switch_site)) }
                    TextButton(onClick = {
                        showTorModal = false
                        navigator.navigateSingleTop(OnAirCookieRoute)
                    }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }

    val recoverableError: (String) -> Unit = { _ -> }
    val gatePassed = gateState is OnAirGateState.Passed
    val activeState = gateState

    DisposableEffect(gatePassed) {
        SiteChromeVisibility.navigationSuiteSuppressed = !gatePassed
        navigator.goHome()
        onDispose { SiteChromeVisibility.navigationSuiteSuppressed = false }
    }

    val entryProvider = entryProvider<NavKey> {
        screen<OnAirGateRoute> {
            OnAirGateContent(
                state = activeState,
                activity = activity,
                navigator = navigator,
                onRetry = { retryTick = System.currentTimeMillis() },
            )
        }
        screen<OnAirHomeRoute> {
            if (gatePassed) {
                val homeViewModel: OnAirHomeViewModel = viewModel()

                LaunchedEffect(Unit) {
                    if (OnAirProfileCache.getCurrentUsername() != null &&
                        OnAirProfileCache.current() == null
                    ) {
                        homeViewModel.refreshCurrentUser()
                    }
                }
                OnAirHomeScreen(
                    onOpenVideo = { id -> navigator.navigateSafely(OnAirVideoRoute(id)) },
                    onOpenSearch = { q -> navigator.navigateSafely(OnAirSearchRoute(query = q)) },
                    onOpenTag = { t -> navigator.navigateSafely(OnAirTagRoute(t)) },
                    onOpenTags = { navigator.navigateSafely(OnAirTagsIndexRoute) },
                    onOpenPictures = { navigator.navigateSafely(OnAirPicturesRoute) },
                    onOpenUser = { u -> navigator.navigateSafely(OnAirUserRoute(u)) },
                    onOpenHistory = { navigator.navigateSafely(OnAirHistoryRoute) },
                    onOpenAnnouncement = { navigator.navigateSafely(OnAirAnnouncementsRoute) },
                    onLoadFailure = recoverableError,
                    viewModel = homeViewModel,
                )
            } else {
                OnAirGateContent(
                    state = activeState,
                    activity = activity,
                    navigator = navigator,
                    onRetry = { retryTick = System.currentTimeMillis() },
                )
            }
        }

        screen<OnAirSearchRoute> { entry ->
            val route = entry
            val vm: OnAirSearchViewModel = viewModel()
            LaunchedEffect(route) {
                if (!route.query.isNullOrBlank()) vm.setQuery(route.query)
                if (!route.field.isNullOrBlank()) vm.setField(route.field)
                if (!route.length.isNullOrBlank()) vm.setLength(route.length)
                if (!route.date.isNullOrBlank()) vm.setDate(route.date)
                if (!route.sort.isNullOrBlank()) vm.setSort(route.sort)
                vm.setTagFilter(route.tag)
            }
            OnAirSearchScreen(
                viewModel = vm,
                onBack = { navigator.goBack() },
                onOpenVideo = { id -> navigator.navigateSafely(OnAirVideoRoute(id)) },
                onOpenTag = { t -> navigator.navigateSafely(OnAirTagRoute(t)) },
                onOpenUser = { u -> navigator.navigateSafely(OnAirUserRoute(u)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<OnAirTagRoute> { entry ->
            val route = entry
            OnAirTagScreen(
                tagName = route.tagName,
                onBack = { navigator.goBack() },
                onOpenVideo = { id -> navigator.navigateSafely(OnAirVideoRoute(id)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<OnAirTagsIndexRoute> {
            OnAirTagsIndexScreen(
                onBack = { navigator.goBack() },
                onOpenTag = { t -> navigator.navigateSafely(OnAirTagRoute(t)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<OnAirPicturesRoute> {
            OnAirPicturesScreen(
                onBack = { navigator.goBack() },
                onOpenUser = { u -> navigator.navigateSafely(OnAirUserRoute(u)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<OnAirUserRoute> { entry ->
            val route = entry
            OnAirUserScreen(
                username = route.username,
                onBack = { navigator.goBack() },
                onOpenVideo = { id -> navigator.navigateSafely(OnAirVideoRoute(id)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<OnAirHistoryRoute> {
            OnAirHistoryScreen(
                onBack = { navigator.goBack() },
                onOpenVideo = { id, _ -> navigator.navigateSafely(OnAirVideoRoute(id)) },
            )
        }

        screen<OnAirAnnouncementsRoute> {
            OnAirAnnouncementsScreen(onBack = { navigator.goBack() })
        }

        screen<OnAirChatRoute> {
            OnAirChatScreen(
                onBack = { navigator.goBack() },
                onOpenUser = { u -> navigator.navigateSafely(OnAirUserRoute(u)) },
            )
        }

        screen<OnAirSettingsRoute> {
            SiteSettingsHubScreen(
                site = SiteType.ONAIR,
                siteEntries = onAirHubEntries(navigator, signedInContent = gatePassed),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.ONAIR.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<OnAirAccountSettingsRoute> {
            OnAirSettingsScreen(
                onBack = { navigator.goBack() },
                onOpenCookies = { navigator.navigateSafely(OnAirCookieRoute) },
                onOpenLogin = { navigator.navigateSafely(OnAirLoginRoute) },
                onOpenAnnouncements = { navigator.navigateSafely(OnAirAnnouncementsRoute) },
                onOpenChat = { navigator.navigateSafely(OnAirChatRoute) },
                onOpenTags = { navigator.navigateSafely(OnAirTagsIndexRoute) },
                onLoggedOut = {
                    OnAirProfileCache.invalidate()
                    gateState = OnAirGateState.ContentError(
                        reason = applicationContext.getString(R.string.onair_signed_out),
                        suggestRelogin = true,
                    )
                    navigator.goHome()
                },
                onOpenProfile = { username ->
                    navigator.navigateSafely(OnAirUserRoute(username))
                },
            )
        }

        screen<OnAirGithubDownloaderRoute> {
            GithubDownloaderScreen(onBack = { navigator.goBack() })
        }

        screen<OnAirCookieRoute> {
            if (gatePassed) {
                OnAirCookieSettingsScreen(
                    onBack = { navigator.goBack() },
                    onOpenLogin = { navigator.navigateSafely(OnAirLoginRoute) },
                    onOpenRegister = { navigator.navigateSafely(OnAirRegisterRoute) },
                    onSwitchSite = { activity.requestSiteSwitch() },
                    onCookiesSaved = { retryTick = System.currentTimeMillis() },
                    onLoggedOut = { retryTick = System.currentTimeMillis() },
                )
            } else {
                OnAirCookieSettingsScreen(
                    onBack = { navigator.goBack() },
                    onOpenLogin = { navigator.navigateSingleTop(OnAirLoginRoute) },
                    onOpenRegister = { navigator.navigateSingleTop(OnAirRegisterRoute) },
                    onSwitchSite = { activity.requestSiteSwitch() },
                    onCookiesSaved = { retryTick = System.currentTimeMillis() },
                    onLoggedOut = { retryTick = System.currentTimeMillis() },
                    gateState = activeState,
                )
            }
        }

        screen<OnAirLoginRoute> {
            OnAirLoginScreen(
                onBack = { navigator.goBack() },
                onOpenRegister = {
                    if (gatePassed) navigator.navigateSafely(OnAirRegisterRoute)
                    else navigator.navigateSingleTop(OnAirRegisterRoute)
                },
                onSuccess = {
                    if (gatePassed) {
                        OnAirProfileCache.invalidate()
                        retryTick = System.currentTimeMillis()
                        navigator.goHome()
                    } else {
                        retryTick = System.currentTimeMillis()
                    }
                },
            )
        }

        screen<OnAirRegisterRoute> {
            OnAirRegisterScreen(
                onBack = { navigator.goBack() },
                onOpenLogin = {
                    if (gatePassed) navigator.navigateSafely(OnAirLoginRoute)
                    else navigator.navigateSingleTop(OnAirLoginRoute)
                },
                onSuccess = {
                    if (gatePassed) {
                        OnAirProfileCache.invalidate()
                        retryTick = System.currentTimeMillis()
                        navigator.goHome()
                    } else {
                        retryTick = System.currentTimeMillis()
                    }
                },
            )
        }

        screen<OnAirVideoRoute> { entry ->
            val route = entry
            val vm: OnAirVideoViewModel = viewModel()
            OnAirVideoScreen(
                videoId = route.videoId,
                preferredQuality = route.preferredQuality,
                viewModel = vm,
                onBack = { navigator.goBack() },
                onOpenVideo = { id ->
                    if (id != route.videoId) {
                        navigator.navigateSafely(OnAirVideoRoute(id))
                    }
                },
                onOpenUser = { u -> navigator.navigateSafely(OnAirUserRoute(u)) },
                onOpenTag = { t -> navigator.navigateSafely(OnAirTagRoute(t)) },
                onLoadFailure = recoverableError,
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.ONAIR,
            config = { onAirHubConfig(navigator, activity) },
        )

        globalSettingsEntries(
            activity = activity,
            navigator = navigator,
            homeFallback = if (gatePassed) SiteProfileRoute else OnAirSettingsRoute,
        )
    }

    SiteNavDisplay(
        navigator = navigator,
        navigationState = navigationState,
        entryProvider = entryProvider,
        modifier = modifier,
        socialSite = com.yenaly.han1meviewer.SiteType.ONAIR,
    )
}

@Composable
private fun OnAirGateContent(
    state: OnAirGateState,
    activity: MainActivity,
    navigator: Navigator,
    onRetry: () -> Unit,
) {
    OnAirGateScreen(
        state = state,
        onSwitchSite = { activity.requestSiteSwitch() },
        onOpenCookies = { navigator.navigateSingleTop(OnAirCookieRoute) },
        onRetry = onRetry,
        onOpenSettings = { navigator.navigateSingleTop(OnAirSettingsRoute) },
    )
}

private fun onAirHubEntries(
    navigator: Navigator,
    signedInContent: Boolean,
): List<SiteSettingsEntry> = buildList {
    if (signedInContent) {
        add(
            SiteSettingsEntry(
                key = "account",
                title = applicationContext.getString(R.string.drawer_account_session),
                subtitle = applicationContext.getString(R.string.onair_account_subtitle),
                icon = Icons.Default.Person,
                onClick = { navigator.navigateSafely(OnAirAccountSettingsRoute) },
            )
        )
    }
    add(
        SiteSettingsEntry(
            key = "cookies",
            title = applicationContext.getString(R.string.drawer_cookies_sign_in),
            subtitle = applicationContext.getString(R.string.onair_cookies_subtitle),
            icon = Icons.Default.Lock,
            onClick = { navigator.navigateSafely(OnAirCookieRoute) },
        )
    )
    if (signedInContent) {
        add(
            SiteSettingsEntry(
                key = "tags",
                title = applicationContext.getString(R.string.drawer_tags),
                subtitle = applicationContext.getString(R.string.onair_tags_subtitle),
                icon = Icons.AutoMirrored.Filled.Label,
                onClick = { navigator.navigateSafely(OnAirTagsIndexRoute) },
            )
        )
        add(
            SiteSettingsEntry(
                key = "announcements",
                title = applicationContext.getString(R.string.onair_announcements_title),
                subtitle = applicationContext.getString(R.string.onair_announcements_subtitle),
                icon = Icons.Default.Campaign,
                onClick = { navigator.navigateSafely(OnAirAnnouncementsRoute) },
            )
        )
        add(
            SiteSettingsEntry(
                key = "chat",
                title = applicationContext.getString(R.string.drawer_chat),
                subtitle = applicationContext.getString(R.string.onair_chat_subtitle),
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = { navigator.navigateSafely(OnAirChatRoute) },
            )
        )
        add(
            SiteSettingsEntry(
                key = "history",
                title = applicationContext.getString(R.string.onair_history_title),
                subtitle = applicationContext.getString(R.string.onair_history_subtitle),
                icon = Icons.Default.History,
                onClick = { navigator.navigateSafely(OnAirHistoryRoute) },
            )
        )
    }
}
