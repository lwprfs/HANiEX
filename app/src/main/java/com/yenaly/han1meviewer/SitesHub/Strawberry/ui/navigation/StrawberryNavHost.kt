package com.yenaly.han1meviewer.SitesHub.Strawberry.ui.navigation

import com.yenaly.han1meviewer.ui.navigation.settings.LocalSiteContentAvailable
import androidx.compose.runtime.CompositionLocalProvider
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsRedirect
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
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.local.StrawberryProfileCache
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryAuthProbe
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryNetwork
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.StrawberryTorChecker
import com.yenaly.han1meviewer.SitesHub.Strawberry.data.remote.TorFailure
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.announcement.StrawberryAnnouncementsScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.auth.StrawberryLoginScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.auth.StrawberryRegisterScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.chat.StrawberryChatScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.gate.StrawberryGateScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.gate.StrawberryGateState
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.history.StrawberryHistoryScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.home.StrawberryHomeScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.home.StrawberryHomeViewModel
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.picture.StrawberryPicturesScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.search.StrawberrySearchScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.search.StrawberrySearchViewModel
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.github.GithubDownloaderScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.settings.StrawberryCookieSettingsScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.settings.StrawberrySettingsScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.tag.StrawberryTagScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.tag.StrawberryTagsIndexScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.user.StrawberryUserScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.video.StrawberryVideoScreen
import com.yenaly.han1meviewer.SitesHub.Strawberry.ui.video.StrawberryVideoViewModel
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import kotlinx.coroutines.delay

@Composable
fun StrawberryNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    var gateState by rememberSaveable(stateSaver = StrawberryGateState.Saver) {
        mutableStateOf(StrawberryGateState.Initializing)
    }
    var retryTick by rememberSaveable { mutableStateOf(0L) }
    var showTorModal by rememberSaveable { mutableStateOf(false) }
    var torModalReason by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(retryTick) {
        StrawberryNetwork.init(activity.applicationContext)

        gateState = StrawberryGateState.CheckingTor
        delay(16)

        val tor = StrawberryTorChecker.check()
        if (!tor.isTorExit) {
            val message = when (tor.failure) {
                TorFailure.Timeout ->
                    applicationContext.getString(R.string.strawberry_tor_timeout)
                TorFailure.NetworkUnreachable ->
                    applicationContext.getString(R.string.strawberry_tor_unreachable)
                TorFailure.NonTorExit ->
                    applicationContext.getString(R.string.strawberry_tor_non_exit)
                else ->
                    tor.message.ifBlank { applicationContext.getString(R.string.strawberry_tor_required) }
            }
            gateState = StrawberryGateState.TorBlocked(
                reason = message,
                torFailure = when (tor.failure) {
                    TorFailure.Timeout -> StrawberryGateState.TorFailureKind.Timeout
                    TorFailure.NetworkUnreachable -> StrawberryGateState.TorFailureKind.NetworkUnreachable
                    TorFailure.NonTorExit -> StrawberryGateState.TorFailureKind.NonTorExit
                    else -> StrawberryGateState.TorFailureKind.Unknown
                },
            )
            torModalReason = message
            showTorModal = true
            return@LaunchedEffect
        }

        gateState = StrawberryGateState.Probing
        delay(16)
        val probe = StrawberryAuthProbe.probe(activity)
        gateState = if (probe.ok) {
            StrawberryGateState.Passed
        } else {
            StrawberryGateState.ContentError(
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
            title = { Text(stringResource(R.string.strawberry_tor_not_connected)) },
            text = {
                Text(
                    torModalReason.ifBlank {
                        stringResource(R.string.strawberry_tor_modal_default)
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
                        navigator.navigateSingleTop(StrawberryCookieRoute)
                    }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }

    val recoverableError: (String) -> Unit = { _ -> }
    val gatePassed = gateState is StrawberryGateState.Passed
    val activeState = gateState

    DisposableEffect(gatePassed) {
        SiteChromeVisibility.navigationSuiteSuppressed = !gatePassed
        navigator.goHome()
        onDispose { SiteChromeVisibility.navigationSuiteSuppressed = false }
    }

    val entryProvider = entryProvider<NavKey> {
        screen<StrawberryGateRoute> {
            StrawberryGateContent(
                state = activeState,
                activity = activity,
                navigator = navigator,
                onRetry = { retryTick = System.currentTimeMillis() },
            )
        }
        screen<StrawberryHomeRoute> {
            if (gatePassed) {
                val homeViewModel: StrawberryHomeViewModel = viewModel()

                LaunchedEffect(Unit) {
                    if (StrawberryProfileCache.getCurrentUsername() != null &&
                        StrawberryProfileCache.current() == null
                    ) {
                        homeViewModel.refreshCurrentUser()
                    }
                }
                StrawberryHomeScreen(
                    onOpenVideo = { id -> navigator.navigateSafely(StrawberryVideoRoute(id)) },
                    onOpenSearch = { q -> navigator.navigateSafely(StrawberrySearchRoute(query = q)) },
                    onOpenTag = { t -> navigator.navigateSafely(StrawberryTagRoute(t)) },
                    onOpenTags = { navigator.navigateSafely(StrawberryTagsIndexRoute) },
                    onOpenPictures = { navigator.navigateSafely(StrawberryPicturesRoute) },
                    onOpenUser = { u -> navigator.navigateSafely(StrawberryUserRoute(u)) },
                    onOpenHistory = { navigator.navigateSafely(StrawberryHistoryRoute) },
                    onOpenAnnouncement = { navigator.navigateSafely(StrawberryAnnouncementsRoute) },
                    onLoadFailure = recoverableError,
                    viewModel = homeViewModel,
                )
            } else {
                StrawberryGateContent(
                    state = activeState,
                    activity = activity,
                    navigator = navigator,
                    onRetry = { retryTick = System.currentTimeMillis() },
                )
            }
        }

        screen<StrawberrySearchRoute> { entry ->
            val route = entry
            val vm: StrawberrySearchViewModel = viewModel()
            LaunchedEffect(route) {
                if (!route.query.isNullOrBlank()) vm.setQuery(route.query)
                if (!route.field.isNullOrBlank()) vm.setField(route.field)
                if (!route.length.isNullOrBlank()) vm.setLength(route.length)
                if (!route.date.isNullOrBlank()) vm.setDate(route.date)
                if (!route.sort.isNullOrBlank()) vm.setSort(route.sort)
                vm.setTagFilter(route.tag)
            }
            StrawberrySearchScreen(
                viewModel = vm,
                onBack = { navigator.goBack() },
                onOpenVideo = { id -> navigator.navigateSafely(StrawberryVideoRoute(id)) },
                onOpenTag = { t -> navigator.navigateSafely(StrawberryTagRoute(t)) },
                onOpenUser = { u -> navigator.navigateSafely(StrawberryUserRoute(u)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<StrawberryTagRoute> { entry ->
            val route = entry
            StrawberryTagScreen(
                tagName = route.tagName,
                onBack = { navigator.goBack() },
                onOpenVideo = { id -> navigator.navigateSafely(StrawberryVideoRoute(id)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<StrawberryTagsIndexRoute> {
            StrawberryTagsIndexScreen(
                onBack = { navigator.goBack() },
                onOpenTag = { t -> navigator.navigateSafely(StrawberryTagRoute(t)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<StrawberryPicturesRoute> {
            StrawberryPicturesScreen(
                onBack = { navigator.goBack() },
                onOpenUser = { u -> navigator.navigateSafely(StrawberryUserRoute(u)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<StrawberryUserRoute> { entry ->
            val route = entry
            StrawberryUserScreen(
                username = route.username,
                onBack = { navigator.goBack() },
                onOpenVideo = { id -> navigator.navigateSafely(StrawberryVideoRoute(id)) },
                onLoadFailure = recoverableError,
            )
        }

        screen<StrawberryHistoryRoute> {
            StrawberryHistoryScreen(
                onBack = { navigator.goBack() },
                onOpenVideo = { id, _ -> navigator.navigateSafely(StrawberryVideoRoute(id)) },
            )
        }

        screen<StrawberryAnnouncementsRoute> {
            StrawberryAnnouncementsScreen(onBack = { navigator.goBack() })
        }

        screen<StrawberryChatRoute> {
            StrawberryChatScreen(
                onBack = { navigator.goBack() },
                onOpenUser = { u -> navigator.navigateSafely(StrawberryUserRoute(u)) },
            )
        }

        screen<StrawberrySettingsRoute> {
            SettingsRedirect(navigator)
        }

        screen<StrawberryAccountSettingsRoute> {
            StrawberrySettingsScreen(
                onBack = { navigator.goBack() },
                onOpenCookies = { navigator.navigateSafely(StrawberryCookieRoute) },
                onOpenLogin = { navigator.navigateSafely(StrawberryLoginRoute) },
                onOpenAnnouncements = { navigator.navigateSafely(StrawberryAnnouncementsRoute) },
                onOpenChat = { navigator.navigateSafely(StrawberryChatRoute) },
                onOpenTags = { navigator.navigateSafely(StrawberryTagsIndexRoute) },
                onLoggedOut = {
                    StrawberryProfileCache.invalidate()
                    gateState = StrawberryGateState.ContentError(
                        reason = applicationContext.getString(R.string.strawberry_signed_out),
                        suggestRelogin = true,
                    )
                    navigator.goHome()
                },
                onOpenProfile = { username ->
                    navigator.navigateSafely(StrawberryUserRoute(username))
                },
            )
        }

        screen<StrawberryGithubDownloaderRoute> {
            GithubDownloaderScreen(onBack = { navigator.goBack() })
        }

        screen<StrawberryCookieRoute> {
            if (gatePassed) {
                StrawberryCookieSettingsScreen(
                    onBack = { navigator.goBack() },
                    onOpenLogin = { navigator.navigateSafely(StrawberryLoginRoute) },
                    onOpenRegister = { navigator.navigateSafely(StrawberryRegisterRoute) },
                    onSwitchSite = { activity.requestSiteSwitch() },
                    onCookiesSaved = { retryTick = System.currentTimeMillis() },
                    onLoggedOut = { retryTick = System.currentTimeMillis() },
                )
            } else {
                StrawberryCookieSettingsScreen(
                    onBack = { navigator.goBack() },
                    onOpenLogin = { navigator.navigateSingleTop(StrawberryLoginRoute) },
                    onOpenRegister = { navigator.navigateSingleTop(StrawberryRegisterRoute) },
                    onSwitchSite = { activity.requestSiteSwitch() },
                    onCookiesSaved = { retryTick = System.currentTimeMillis() },
                    onLoggedOut = { retryTick = System.currentTimeMillis() },
                    gateState = activeState,
                )
            }
        }

        screen<StrawberryLoginRoute> {
            StrawberryLoginScreen(
                onBack = { navigator.goBack() },
                onOpenRegister = {
                    if (gatePassed) navigator.navigateSafely(StrawberryRegisterRoute)
                    else navigator.navigateSingleTop(StrawberryRegisterRoute)
                },
                onSuccess = {
                    if (gatePassed) {
                        StrawberryProfileCache.invalidate()
                        retryTick = System.currentTimeMillis()
                        navigator.goHome()
                    } else {
                        retryTick = System.currentTimeMillis()
                    }
                },
            )
        }

        screen<StrawberryRegisterRoute> {
            StrawberryRegisterScreen(
                onBack = { navigator.goBack() },
                onOpenLogin = {
                    if (gatePassed) navigator.navigateSafely(StrawberryLoginRoute)
                    else navigator.navigateSingleTop(StrawberryLoginRoute)
                },
                onSuccess = {
                    if (gatePassed) {
                        StrawberryProfileCache.invalidate()
                        retryTick = System.currentTimeMillis()
                        navigator.goHome()
                    } else {
                        retryTick = System.currentTimeMillis()
                    }
                },
            )
        }

        screen<StrawberryVideoRoute> { entry ->
            val route = entry
            val vm: StrawberryVideoViewModel = viewModel()
            StrawberryVideoScreen(
                videoId = route.videoId,
                preferredQuality = route.preferredQuality,
                viewModel = vm,
                onBack = { navigator.goBack() },
                onOpenVideo = { id ->
                    if (id != route.videoId) {
                        navigator.navigateSafely(StrawberryVideoRoute(id))
                    }
                },
                onOpenUser = { u -> navigator.navigateSafely(StrawberryUserRoute(u)) },
                onOpenTag = { t -> navigator.navigateSafely(StrawberryTagRoute(t)) },
                onLoadFailure = recoverableError,
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.STRAWBERRY,
            config = { strawberryHubConfig(navigator, activity) },
        )

        globalSettingsEntries(
            activity = activity,
            navigator = navigator,
            homeFallback = if (gatePassed) SiteProfileRoute else StrawberrySettingsRoute,
        )
    }

    CompositionLocalProvider(LocalSiteContentAvailable provides gatePassed) {
        SiteNavDisplay(
            navigator = navigator,
            navigationState = navigationState,
            entryProvider = entryProvider,
            modifier = modifier,
            socialSite = com.yenaly.han1meviewer.SiteType.STRAWBERRY,
        )
    }
}

@Composable
private fun StrawberryGateContent(
    state: StrawberryGateState,
    activity: MainActivity,
    navigator: Navigator,
    onRetry: () -> Unit,
) {
    StrawberryGateScreen(
        state = state,
        onSwitchSite = { activity.requestSiteSwitch() },
        onOpenCookies = { navigator.navigateSingleTop(StrawberryCookieRoute) },
        onRetry = onRetry,
        onOpenSettings = { navigator.navigateSingleTop(StrawberrySettingsRoute) },
    )
}
