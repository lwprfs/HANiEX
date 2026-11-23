package com.yenaly.han1meviewer.FapFolder.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.SiteChromeVisibility
import androidx.compose.ui.Modifier
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.navigation3.runtime.entryProvider
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SiteNavDisplay
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.settings.globalSettingsEntries
import com.yenaly.han1meviewer.ui.screen.site.siteHubEntries

import com.yenaly.han1meviewer.ui.component.animation.SittingCatLoadingAnimation
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.util.applicationContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.ui.component.content.LoginGate
import com.yenaly.han1meviewer.FapFolder.data.local.FapFolderHistoryRepo
import com.yenaly.han1meviewer.FapFolder.data.remote.FapFolderCredentialStore
import com.yenaly.han1meviewer.FapFolder.data.remote.FapFolderSessionGuard
import com.yenaly.han1meviewer.FapFolder.data.remote.FapFolderNetwork
import com.yenaly.han1meviewer.FapFolder.data.remote.FapFolderProbeFailure
import com.yenaly.han1meviewer.FapFolder.ui.account.FapFolderAccountScreen
import com.yenaly.han1meviewer.FapFolder.ui.account.FapFolderAccountSettingsScreen
import com.yenaly.han1meviewer.FapFolder.ui.account.FapFolderPrivacySettingsScreen
import com.yenaly.han1meviewer.FapFolder.ui.album.FapFolderAlbumScreen
import com.yenaly.han1meviewer.FapFolder.ui.followed.FapFolderFollowedCommunitiesScreen
import com.yenaly.han1meviewer.FapFolder.ui.gate.FapFolderGateState
import com.yenaly.han1meviewer.FapFolder.ui.group.FapFolderGroupScreen
import com.yenaly.han1meviewer.FapFolder.ui.hashtag.FapFolderHashtagScreen
import com.yenaly.han1meviewer.FapFolder.ui.history.FapFolderHistoryScreen
import com.yenaly.han1meviewer.FapFolder.ui.home.FapFolderHomeScreen
import com.yenaly.han1meviewer.FapFolder.ui.home.FapFolderHomeViewModel
import com.yenaly.han1meviewer.FapFolder.ui.login.FapFolderLoginScreen
import com.yenaly.han1meviewer.FapFolder.ui.login.FapFolderRegisterScreen
import com.yenaly.han1meviewer.FapFolder.ui.post.FapFolderPostDetailScreen
import com.yenaly.han1meviewer.FapFolder.ui.profile.FapFolderProfileScreen
import com.yenaly.han1meviewer.FapFolder.ui.saved.FapFolderSavedScreen
import com.yenaly.han1meviewer.FapFolder.ui.search.FapFolderSearchHistoryScreen
import com.yenaly.han1meviewer.FapFolder.ui.search.FapFolderSearchScreen
import com.yenaly.han1meviewer.FapFolder.ui.settings.FapFolderCookiesScreen
import com.yenaly.han1meviewer.R
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
fun FapFolderNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    var gateState by rememberSaveable(stateSaver = FapFolderGateState.Saver) {
        mutableStateOf<FapFolderGateState>(FapFolderGateState.Initializing)
    }
    var retryTick by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        FapFolderNetwork.init(activity.applicationContext)
        FapFolderHistoryRepo.init(activity.applicationContext)
    }

    LaunchedEffect(retryTick) {
        gateState = FapFolderGateState.Probing
        delay(16L)

        if (!FapFolderNetwork.hasAuthCookies() && !FapFolderCredentialStore.hasSaved()) {
            gateState = FapFolderGateState.Passed
            return@LaunchedEffect
        }

        val probe = runCatching { FapFolderSessionGuard.validateOrSignIn(activity).probe }
            .getOrNull()

        gateState = when {
            probe == null -> FapFolderGateState.Passed
            probe.ok -> FapFolderGateState.Passed
            probe.failure == FapFolderProbeFailure.NoCookies -> FapFolderGateState.Passed
            probe.suggestRelogin || probe.failure == FapFolderProbeFailure.SessionExpired ->
                FapFolderGateState.ContentError(
                    reason = probe.userMessage ?: applicationContext.getString(R.string.fap_gate_session_expired),
                    suggestRelogin = true,
                )
            probe.failure == FapFolderProbeFailure.CloudflareChallenge ->
                FapFolderGateState.ContentError(
                    reason = probe.userMessage ?: applicationContext.getString(R.string.fap_gate_cloudflare),
                    suggestWait = true,
                )
            probe.failure == FapFolderProbeFailure.SiteDown ->
                FapFolderGateState.ContentError(
                    reason = probe.userMessage ?: applicationContext.getString(R.string.fap_gate_down),
                    suggestSwitchSite = true,
                )
            else -> FapFolderGateState.Passed
        }
    }

    DisposableEffect(navigator) {
        FapFolderNavBridge.attach(navigator)
        onDispose { FapFolderNavBridge.detach() }
    }

    val state = gateState

    val gatePhase = when (state) {
        is FapFolderGateState.Passed -> 2
        is FapFolderGateState.ContentError -> 1
        else -> 0
    }
    DisposableEffect(gatePhase) {
        SiteChromeVisibility.navigationSuiteSuppressed = gatePhase != 2
        navigator.goHome()
        onDispose { SiteChromeVisibility.navigationSuiteSuppressed = false }
    }

    when (state) {
        is FapFolderGateState.Initializing -> {
            GatePlaceholder(stringResource(R.string.fap_gate_preparing))
            return
        }
        is FapFolderGateState.Probing -> {
            GatePlaceholder(stringResource(R.string.fap_gate_validating))
            return
        }
        is FapFolderGateState.ContentError -> {
            GateErrorHost(
                activity = activity,
                navigator = navigator,
                navigationState = navigationState,
                state = state,
                onRetry = { retryTick = System.currentTimeMillis() },
            )
            return
        }
        is FapFolderGateState.Passed -> Unit
    }

    val homeViewModel: FapFolderHomeViewModel = viewModel()

    val entryProvider = entryProvider<NavKey> {
        screen<FapFolderHomeRoute> {
            FapFolderHomeScreen(
                onOpenPost = { slug -> navigator.navigateSafely(FapFolderPostRoute(slug)) },
                onOpenVideoPost = { slug ->
                    navigator.navigateSafely(FapFolderPostRoute(slug, autoPlay = true))
                },
                onOpenSearch = { query ->
                    navigator.navigateSafely(
                        FapFolderSearchRoute(
                            query = query,
                            isHashtag = query?.startsWith("#") == true,
                        )
                    )
                },
                onOpenUser = { username ->
                    navigator.navigateSafely(FapFolderProfileRoute(username))
                },
                onOpenHashtag = { tag ->
                    navigator.navigateSafely(FapFolderHashtagRoute(tag))
                },
                onOpenGroup = { slug ->
                    navigator.navigateSafely(FapFolderGroupRoute(slug))
                },
                onOpenAlbum = { albumId ->
                    navigator.navigateSafely(FapFolderAlbumRoute(albumId))
                },
                onOpenHistory = { navigator.navigateSafely(FapFolderHistoryRoute) },
                onSessionExpired = {
                    retryTick = System.currentTimeMillis()
                },
                onLoadFailure = { },
                viewModel = homeViewModel,
            )
        }

        screen<FapFolderPostRoute> { entry ->
            val route = entry
            FapFolderPostDetailScreen(
                slugOrId = route.slugOrId,
                autoPlay = route.autoPlay,
                onBack = { navigator.goBack() },
                onOpenHashtag = { tag ->
                    navigator.navigateSafely(FapFolderHashtagRoute(tag))
                },
                onOpenGroup = { slug ->
                    navigator.navigateSafely(FapFolderGroupRoute(slug))
                },
                onOpenProfile = { username ->
                    navigator.navigateSafely(FapFolderProfileRoute(username))
                },
                onOpenAlbum = { albumId ->
                    navigator.navigateSafely(FapFolderAlbumRoute(albumId))
                },
                onOpenVideo = { slugOrId ->
                    navigator.navigateSafely(FapFolderPostRoute(slugOrId, autoPlay = true))
                },
                onReplaceVideo = { slugOrId ->
                    navigator.replaceTop(FapFolderPostRoute(slugOrId, autoPlay = true))
                },
                onOpenGroupPhotos = { slug ->
                    navigator.navigateSafely(FapFolderGroupRoute(slug, tab = "photos"))
                },
                onOpenProfilePhotos = { username ->
                    navigator.navigateSafely(FapFolderProfileRoute(username, tab = "photos"))
                },
            )
        }

        screen<FapFolderSearchRoute> { entry ->
            val route = entry
            FapFolderSearchScreen(
                initialQuery = route.query,
                initialHashtag = route.isHashtag,
                initialTab = route.tab,
                onBack = { navigator.goBack() },
                onOpenPost = { slug -> navigator.navigateSafely(FapFolderPostRoute(slug)) },
                onOpenHashtag = { tag -> navigator.navigateSafely(FapFolderHashtagRoute(tag)) },
                onOpenGroup = { slug -> navigator.navigateSafely(FapFolderGroupRoute(slug)) },
                onOpenProfile = { username ->
                    navigator.navigateSafely(FapFolderProfileRoute(username))
                },
            )
        }

        screen<FapFolderSearchHistoryRoute> {
            FapFolderSearchHistoryScreen(
                onBack = { navigator.goBack() },
                onRunSearch = { query, isHashtag, tab ->
                    if (isHashtag) {
                        navigator.navigateSafely(FapFolderHashtagRoute(query))
                    } else {
                        navigator.navigateSafely(
                            FapFolderSearchRoute(query = query, isHashtag = false, tab = tab)
                        )
                    }
                },
            )
        }

        screen<FapFolderHashtagRoute> { entry ->
            val route = entry
            FapFolderHashtagScreen(
                tag = route.tag,
                onBack = { navigator.goBack() },
                onOpenPost = { slug -> navigator.navigateSafely(FapFolderPostRoute(slug)) },
                onOpenVideoPost = { slug ->
                    navigator.navigateSafely(FapFolderPostRoute(slug, autoPlay = true))
                },
                onOpenHashtag = { tag -> navigator.navigateSafely(FapFolderHashtagRoute(tag)) },
                onOpenGroup = { slug -> navigator.navigateSafely(FapFolderGroupRoute(slug)) },
                onOpenProfile = { username ->
                    navigator.navigateSafely(FapFolderProfileRoute(username))
                },
            )
        }

        screen<FapFolderGroupRoute> { entry ->
            val route = entry
            FapFolderGroupScreen(
                slug = route.slug,
                initialTab = route.tab,
                onBack = { navigator.goBack() },
                onOpenPost = { slug -> navigator.navigateSafely(FapFolderPostRoute(slug)) },
                onOpenVideoPost = { slug ->
                    navigator.navigateSafely(FapFolderPostRoute(slug, autoPlay = true))
                },
                onOpenProfile = { username ->
                    navigator.navigateSafely(FapFolderProfileRoute(username))
                },
                onOpenAlbum = { albumId ->
                    navigator.navigateSafely(FapFolderAlbumRoute(albumId))
                },
                onOpenHashtag = { tag ->
                    navigator.navigateSafely(FapFolderHashtagRoute(tag))
                },
            )
        }

        screen<FapFolderProfileRoute> { entry ->
            val route = entry
            FapFolderProfileScreen(
                username = route.username,
                initialTab = route.tab,
                onBack = { navigator.goBack() },
                onOpenPost = { slug -> navigator.navigateSafely(FapFolderPostRoute(slug)) },
                onOpenGroup = { slug -> navigator.navigateSafely(FapFolderGroupRoute(slug)) },
                onOpenAlbum = { albumId ->
                    navigator.navigateSafely(FapFolderAlbumRoute(albumId))
                },
                onOpenHashtag = { tag ->
                    navigator.navigateSafely(FapFolderHashtagRoute(tag))
                },
                onOpenProfile = { username ->
                    navigator.navigateSafely(FapFolderProfileRoute(username))
                },
                onOpenVideoBySlug = { slug ->
                    navigator.navigateSafely(FapFolderPostRoute(slug, autoPlay = true))
                },
            )
        }

        screen<FapFolderAlbumRoute> { entry ->
            val route = entry
            FapFolderAlbumScreen(
                albumId = route.albumId,
                onBack = { navigator.goBack() },
                onOpenPost = { slug -> navigator.navigateSafely(FapFolderPostRoute(slug)) },
            )
        }

        screen<FapFolderHistoryRoute> {
            FapFolderHistoryScreen(
                onBack = { navigator.goBack() },
                onOpenPost = { slug -> navigator.navigateSafely(FapFolderPostRoute(slug)) },
            )
        }

        screen<FapFolderSettingsRoute> {
            val loggedIn = FapFolderNetwork.hasAuthCookies()
            SiteSettingsHubScreen(
                site = SiteType.FAPFOLDER,
                siteEntries = fapFolderHubEntries(navigator, isLoggedIn = loggedIn, full = true),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.FAPFOLDER.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }

        screen<FapFolderFollowedCommunitiesRoute> {
            LoginGate(
                loggedIn = rememberFapFolderLoggedIn(),
                title = stringResource(R.string.drawer_followed_communities),
                onBack = { navigator.goBack() },
                onSignIn = { navigator.navigateSafely(FapFolderLoginRoute) },
            ) {
                FapFolderFollowedCommunitiesScreen(
                    onBack = { navigator.goBack() },
                    onOpenGroup = { slug ->
                        navigator.navigateSafely(FapFolderGroupRoute(slug))
                    },
                )
            }
        }

        screen<FapFolderSavedRoute> {
            LoginGate(
                loggedIn = rememberFapFolderLoggedIn(),
                title = stringResource(R.string.saved),
                onBack = { navigator.goBack() },
                onSignIn = { navigator.navigateSafely(FapFolderLoginRoute) },
            ) {
                FapFolderSavedScreen(
                    onBack = { navigator.goBack() },
                    onOpenPost = { slug ->
                        navigator.navigateSafely(FapFolderPostRoute(slug))
                    },
                    onOpenVideoPost = { slug ->
                        navigator.navigateSafely(FapFolderPostRoute(slug, autoPlay = true))
                    },
                    onOpenHashtag = { tag ->
                        navigator.navigateSafely(FapFolderHashtagRoute(tag))
                    },
                    onOpenGroup = { slug ->
                        navigator.navigateSafely(FapFolderGroupRoute(slug))
                    },
                    onOpenProfile = { username ->
                        navigator.navigateSafely(FapFolderProfileRoute(username))
                    },
                )
            }
        }

        screen<FapFolderCookiesRoute> {
            FapFolderCookiesScreen(
                onBack = { navigator.goBack() },
                onOpenLogin = { navigator.navigateSafely(FapFolderLoginRoute) },
                onOpenRegister = { navigator.navigateSafely(FapFolderRegisterRoute) },
                onSwitchSite = { activity.requestSiteSwitch() },
                onCookiesSaved = {
                    retryTick = System.currentTimeMillis()
                    navigator.goBack()
                },
                onLoggedOut = {
                    retryTick = System.currentTimeMillis()
                },
                gateState = state,
            )
        }

        screen<FapFolderLoginRoute> {
            FapFolderLoginScreen(
                onBack = { navigator.goBack() },
                onLoginSuccess = {
                    retryTick = System.currentTimeMillis()
                    navigator.goHome()
                },
                onOpenCookies = {
                    navigator.navigateSafely(FapFolderCookiesRoute)
                },
                onOpenRegister = {
                    navigator.navigateSafely(FapFolderRegisterRoute)
                },
            )
        }

        screen<FapFolderRegisterRoute> {
            FapFolderRegisterScreen(
                onBack = { navigator.goBack() },
                onRegistered = {
                    retryTick = System.currentTimeMillis()
                    navigator.goHome()
                },
                onOpenLogin = {
                    navigator.goBack()
                    navigator.navigateSafely(FapFolderLoginRoute)
                },
            )
        }

        screen<FapFolderAccountRoute> {
            FapFolderAccountScreen(
                onBack = { navigator.goBack() },
                onOpenLogin = { navigator.navigateSafely(FapFolderLoginRoute) },
                onOpenRegister = { navigator.navigateSafely(FapFolderRegisterRoute) },
                onOpenCookies = { navigator.navigateSafely(FapFolderCookiesRoute) },
                onOpenMyProfile = { username ->
                    navigator.navigateSafely(FapFolderProfileRoute(username))
                },
                onOpenAccountSettings = {
                    navigator.navigateSafely(FapFolderAccountSettingsRoute)
                },
                onOpenPrivacy = { navigator.navigateSafely(FapFolderPrivacyRoute) },
                onOpenFollowed = {
                    navigator.navigateSafely(FapFolderFollowedCommunitiesRoute)
                },
                onOpenSaved = { navigator.navigateSafely(FapFolderSavedRoute) },
                onOpenHistory = { navigator.navigateSafely(FapFolderHistoryRoute) },
                onLoggedOut = {
                    retryTick = System.currentTimeMillis()
                    navigator.goHome()
                },
            )
        }

        screen<FapFolderAccountSettingsRoute> {
            FapFolderAccountSettingsScreen(onBack = { navigator.goBack() })
        }

        screen<FapFolderPrivacyRoute> {
            LoginGate(
                loggedIn = rememberFapFolderLoggedIn(),
                title = stringResource(R.string.privacy),
                onBack = { navigator.goBack() },
                onSignIn = { navigator.navigateSafely(FapFolderLoginRoute) },
            ) {
                FapFolderPrivacySettingsScreen(onBack = { navigator.goBack() })
            }
        }

        screen<FapFolderImageFolderRoute> {
            com.yenaly.han1meviewer.ui.screen.library.ImageDownloadsScreen(
                onBack = { navigator.goBack() },
            )
        }

        siteHubEntries(
            navigator = navigator,
            site = SiteType.FAPFOLDER,
            config = { fapFolderHubConfig(navigator, activity) },
        )

        globalSettingsEntries(
            activity = activity,
            navigator = navigator,
            homeFallback = SiteProfileRoute,
        )
    }

    SiteNavDisplay(
        navigator = navigator,
        navigationState = navigationState,
        entryProvider = entryProvider,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GatePlaceholder(message: String) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SittingCatLoadingAnimation(modifier = Modifier.size(70.dp))
            Text(
                text = "FapFolder",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GateErrorHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    state: FapFolderGateState.ContentError,
    onRetry: () -> Unit,
) {
    val gateContent: @Composable () -> Unit = {
        GateErrorScreen(
            state = state,
            onRetry = onRetry,
            onOpenCookies = { navigator.navigateSafely(FapFolderCookiesRoute) },
            onOpenLogin = { navigator.navigateSafely(FapFolderLoginRoute) },
            onSwitchSite = { activity.requestSiteSwitch() },
            onOpenSettings = { navigator.navigateSafely(FapFolderSettingsRoute) },
        )
    }
    val entryProvider = entryProvider<NavKey> {
        screen<FapFolderHomeRoute> { gateContent() }
        screen<FapFolderGateRoute> {
            GateErrorScreen(
                state = state,
                onRetry = onRetry,
                onOpenCookies = {
                    navigator.navigateSafely(FapFolderCookiesRoute)
                },
                onOpenLogin = {
                    navigator.navigateSafely(FapFolderLoginRoute)
                },
                onSwitchSite = { activity.requestSiteSwitch() },
                onOpenSettings = {
                    navigator.navigateSafely(FapFolderSettingsRoute)
                },
            )
        }
        screen<FapFolderSettingsRoute> {
            SiteSettingsHubScreen(
                site = SiteType.FAPFOLDER,
                siteEntries = fapFolderHubEntries(navigator, isLoggedIn = false, full = false),
                onBack = { navigator.goBack() },
                onOpenAppSettings = { navigator.navigateSafely(HomeSettingsRoute) },
                onOpenPlayerSettings = { navigator.navigateSafely(SitePlayerSettingsRoute(SiteType.FAPFOLDER.name)) },
                onOpenNetworkSettings = { navigator.navigateSafely(NetworkSettingsRoute) },
                onOpenUnifiedHistory = { navigator.navigateSafely(UnifiedHistoryRoute) },
            )
        }
        globalSettingsEntries(
            activity = activity,
            navigator = navigator,
            homeFallback = FapFolderSettingsRoute,
        )
        screen<FapFolderCookiesRoute> {
            FapFolderCookiesScreen(
                onBack = { navigator.goBack() },
                onOpenLogin = {
                    navigator.navigateSafely(FapFolderLoginRoute)
                },
                onOpenRegister = {
                    navigator.navigateSafely(FapFolderRegisterRoute)
                },
                onSwitchSite = { activity.requestSiteSwitch() },
                onCookiesSaved = onRetry,
                onLoggedOut = onRetry,
                gateState = state,
            )
        }
        screen<FapFolderLoginRoute> {
            FapFolderLoginScreen(
                onBack = { navigator.goBack() },
                onLoginSuccess = onRetry,
                onOpenCookies = {
                    navigator.navigateSafely(FapFolderCookiesRoute)
                },
                onOpenRegister = {
                    navigator.navigateSafely(FapFolderRegisterRoute)
                },
            )
        }
        screen<FapFolderRegisterRoute> {
            FapFolderRegisterScreen(
                onBack = { navigator.goBack() },
                onRegistered = onRetry,
                onOpenLogin = {
                    navigator.goBack()
                    navigator.navigateSafely(FapFolderLoginRoute)
                },
            )
        }
    }

    SiteNavDisplay(
        navigator = navigator,
        navigationState = navigationState,
        entryProvider = entryProvider,
    )
}

@Composable
private fun GateErrorScreen(
    state: FapFolderGateState.ContentError,
    onRetry: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenLogin: () -> Unit,
    onSwitchSite: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val headline = when {
        state.suggestRelogin -> stringResource(R.string.fap_headline_session_expired)
        state.suggestWait -> stringResource(R.string.fap_headline_wait)
        state.suggestSwitchSite -> stringResource(R.string.fap_headline_unavailable)
        else -> stringResource(R.string.fap_headline_not_ready)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            Image(
                modifier = Modifier.size(150.dp),
                painter = painterResource(R.drawable.h_chan_sad),
                contentDescription = headline,
            )
            Text(
                text = "FapFolder",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = state.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
            }

            if (state.suggestRelogin) {
                Button(
                    onClick = onOpenLogin,
                    modifier = Modifier.fillMaxWidth(),
                ) { GateButtonLabel(Icons.AutoMirrored.Filled.Login, stringResource(R.string.site_sign_in)) }
                FilledTonalButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                ) { GateButtonLabel(Icons.Default.Refresh, stringResource(R.string.retry)) }
            } else {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                ) { GateButtonLabel(Icons.Default.Refresh, if (state.suggestWait) stringResource(R.string.site_retry_later) else stringResource(R.string.retry)) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenCookies,
                    modifier = Modifier.weight(1f),
                ) { GateButtonLabel(Icons.Default.Cookie, stringResource(R.string.site_cookies)) }
                OutlinedButton(
                    onClick = onSwitchSite,
                    modifier = Modifier.weight(1f),
                ) { GateButtonLabel(Icons.Default.SwapHoriz, stringResource(R.string.site_switch_site)) }
            }
            TextButton(onClick = onOpenSettings) {
                GateButtonLabel(Icons.Default.Settings, stringResource(R.string.site_settings))
            }
        }
    }
}

@Composable
private fun GateButtonLabel(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text, maxLines = 1)
}

private fun fapFolderHubEntries(
    navigator: Navigator,
    isLoggedIn: Boolean,
    full: Boolean,
): List<SiteSettingsEntry> = buildList {
    add(
        SiteSettingsEntry(
            key = "cookies",
            title = applicationContext.getString(R.string.site_cookies),
            subtitle = applicationContext.getString(R.string.fap_cookies_subtitle),
            icon = Icons.Default.Cookie,
            onClick = { navigator.navigateSafely(FapFolderCookiesRoute) },
        )
    )
    if (!isLoggedIn) {
        add(
            SiteSettingsEntry(
                key = "login",
                title = applicationContext.getString(R.string.site_sign_in),
                subtitle = applicationContext.getString(R.string.fap_signin_subtitle),
                icon = Icons.AutoMirrored.Filled.Login,
                onClick = { navigator.navigateSafely(FapFolderLoginRoute) },
            )
        )
        add(
            SiteSettingsEntry(
                key = "register",
                title = applicationContext.getString(R.string.site_create_account),
                subtitle = applicationContext.getString(R.string.fap_create_account_subtitle),
                icon = Icons.Default.PersonAdd,
                onClick = { navigator.navigateSafely(FapFolderRegisterRoute) },
            )
        )
    }
    if (full) {
        add(
            SiteSettingsEntry(
                key = "account",
                title = applicationContext.getString(R.string.site_my_account),
                subtitle = if (isLoggedIn) {
                    applicationContext.getString(R.string.fap_account_subtitle_in)
                } else {
                    applicationContext.getString(R.string.fap_account_subtitle_out)
                },
                icon = Icons.Default.AccountCircle,
                onClick = { navigator.navigateSafely(FapFolderAccountRoute) },
            )
        )
        if (isLoggedIn) {
            add(
                SiteSettingsEntry(
                    key = "privacy",
                    title = applicationContext.getString(R.string.site_privacy),
                    subtitle = applicationContext.getString(R.string.fap_privacy_subtitle),
                    icon = Icons.Default.Lock,
                    onClick = { navigator.navigateSafely(FapFolderPrivacyRoute) },
                )
            )
        }
        add(
            SiteSettingsEntry(
                key = "history",
                title = applicationContext.getString(R.string.fap_history_title),
                subtitle = applicationContext.getString(R.string.fap_history_subtitle),
                icon = Icons.Default.History,
                onClick = { navigator.navigateSafely(FapFolderHistoryRoute) },
            )
        )
        add(
            SiteSettingsEntry(
                key = "search",
                title = applicationContext.getString(R.string.site_search),
                subtitle = applicationContext.getString(R.string.fap_search_subtitle),
                icon = Icons.Default.Search,
                onClick = { navigator.navigateSafely(FapFolderSearchRoute()) },
            )
        )
        add(
            SiteSettingsEntry(
                key = "search_history",
                title = applicationContext.getString(R.string.drawer_search_history),
                subtitle = applicationContext.getString(R.string.fap_search_history_subtitle),
                icon = Icons.Default.History,
                onClick = { navigator.navigateSafely(FapFolderSearchHistoryRoute) },
            )
        )
        add(
            SiteSettingsEntry(
                key = "followed",
                title = applicationContext.getString(R.string.drawer_followed_communities),
                subtitle = applicationContext.getString(R.string.fap_followed_subtitle),
                icon = Icons.Default.Groups,
                onClick = { navigator.navigateSafely(FapFolderFollowedCommunitiesRoute) },
            )
        )
        add(
            SiteSettingsEntry(
                key = "saved",
                title = applicationContext.getString(R.string.fap_saved_posts_title),
                subtitle = applicationContext.getString(R.string.fap_saved_posts_subtitle),
                icon = Icons.Default.Bookmark,
                onClick = { navigator.navigateSafely(FapFolderSavedRoute) },
            )
        )
    }
}

@Composable
private fun rememberFapFolderLoggedIn(): Boolean {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return remember(tick) { FapFolderNetwork.hasAuthCookies() }
}
