package com.yenaly.han1meviewer.ui.screen.main

import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Subscriptions
import com.yenaly.han1meviewer.ui.navigation.site.SiteLibraryRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteProfileRoute
import com.yenaly.han1meviewer.ui.navigation.site.SiteSubscriptionRoute
import com.yenaly.han1meviewer.ui.screen.site.subscriptionTabIcon
import com.yenaly.han1meviewer.ui.screen.site.subscriptionTabTitle
import com.yenaly.han1meviewer.ui.screen.site.supportsSubscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.exception.CloudFlareBlockedException
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.logic.state.PageState
import com.yenaly.han1meviewer.ui.component.SiteSwitchConfirmDialog
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.CrossSiteOpenDialog
import com.yenaly.han1meviewer.ui.component.ChoiceDialog
import com.yenaly.han1meviewer.ui.component.GlobalDialogHost
import com.yenaly.han1meviewer.ui.component.GlobalToastHost
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.UpdateDialog
import com.yenaly.han1meviewer.ui.component.UsageNoticeDialog
import com.yenaly.han1meviewer.ui.navigation.NavigationManager
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.ui.navigation.SiteChromeVisibility
import com.yenaly.han1meviewer.ui.navigation.isHanimeFamily
import com.yenaly.han1meviewer.ui.navigation.rememberSiteNavigationState
import com.yenaly.han1meviewer.ui.navigation.siteHomeKey
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.main.UnifiedMainNavHost
import com.yenaly.han1meviewer.ui.navigation.main.handleMainIntent
import com.yenaly.han1meviewer.ui.navigation.main.navigateTopLevelDestination
import com.yenaly.han1meviewer.ui.navigation.settings.buildDomainOptions
import com.yenaly.han1meviewer.ui.screen.home.homepage.HomePageViewModel
import com.yenaly.han1meviewer.ui.theme.HanimeTheme
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.util.HAnalytics
import com.yenaly.han1meviewer.util.getUpdateIfExists
import com.yenaly.han1meviewer.util.installApkPackage
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.han1meviewer.logic.update.UpdateManager
import com.yenaly.han1meviewer.ui.navigation.settings.UpdateSettingsRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun MainActivityContent(
    activity: MainActivity,
    viewModel: HomePageViewModel,
    pendingNavigationRequests: Flow<Intent>,
    snackbarHostState: SnackbarHostState,
    showAuthGuard: Boolean,
    onLogoutClick: () -> Unit,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onNavigatorReady: (Navigator) -> Unit,
    siteChangeKey: Long = 0L,
) {
    HanimeTheme {
        val windowBackground = MaterialTheme.colorScheme.background
        LaunchedEffect(windowBackground) {
            activity.window.setBackgroundDrawable(windowBackground.toArgb().toDrawable())
        }

        val activeSite = Preferences.siteType
        val navigationState = key(activeSite, siteChangeKey) {
            rememberSiteNavigationState(activeSite)
        }
        val appNavigator = remember(navigationState) { Navigator(navigationState) }
        LaunchedEffect(appNavigator) {
            onNavigatorReady(appNavigator)
            NavigationManager.initializeNavigator(appNavigator, activeSite)
        }

        val scope = rememberCoroutineScope()
        var currentMainDestination by remember { mutableStateOf(MainDestinationSpec.Home) }
        var pendingUpdate by remember { mutableStateOf<Latest?>(null) }
        var showUsageNotice by remember { mutableStateOf(!Preferences.usageNoticeAccepted) }
        val updateDownloadBackground = stringResource(R.string.update_download_background)

        val homeState by viewModel.homePageFlow.collectAsStateWithLifecycle()
        val isLoggedIn by Preferences.loginStateFlow.collectAsStateWithLifecycle()
        val headerAvatarUrl = if (isLoggedIn) {
            (homeState as? PageState.Success)?.info?.page?.avatarUrl
        } else {
            null
        }
        val headerUsername = if (isLoggedIn) {
            (homeState as? PageState.Success)?.info?.page?.username
        } else {
            null
        }
        val selectedTopLevelDestination = currentMainDestination.topLevelDestination

        val isHanimeFamilySite = Preferences.siteType.isHanimeFamily()

        val currentSite = Preferences.siteType
        val currentKey = navigationState.currentKey
        val isAtSiteHome = !isHanimeFamilySite && currentKey == siteHomeKey(currentSite)
        val atSubscription = currentKey == SiteSubscriptionRoute
        val atLibrary = currentKey == SiteLibraryRoute
        val atProfile = currentKey == SiteProfileRoute

        val siteNavItems: List<SiteNavItem>? = if (isHanimeFamilySite) null else buildList<SiteNavItem> {
            add(
                SiteNavItem(
                    key = "home",
                    icon = Icons.Filled.Home,
                    title = stringResource(R.string.home_page),
                    selected = isAtSiteHome,
                    onClick = { NavigationManager.navigateToHome() },
                ),
            )
            if (currentSite.supportsSubscriptions()) {
                add(
                    SiteNavItem(
                        key = "subscription",
                        icon = currentSite.subscriptionTabIcon(),
                        title = currentSite.subscriptionTabTitle(),
                        selected = atSubscription,
                        onClick = { appNavigator.navigate(SiteSubscriptionRoute) },
                    ),
                )
            }
            add(
                SiteNavItem(
                    key = "library",
                    icon = Icons.Filled.VideoLibrary,
                    title = stringResource(R.string.content_library),
                    selected = atLibrary,
                    onClick = { appNavigator.navigate(SiteLibraryRoute) },
                ),
            )
            add(
                SiteNavItem(
                    key = "profile",
                    icon = Icons.Filled.Person,
                    title = stringResource(R.string.my_account),
                    selected = atProfile,
                    onClick = { appNavigator.navigate(SiteProfileRoute) },
                ),
            )
        }

        LaunchedEffect(appNavigator) {
            pendingNavigationRequests.collect { intent ->
                appNavigator.handleMainIntent(intent)
            }
        }

        LaunchedEffect(Unit) {
            AppViewModel.pendingUpdateDialog.collect { latest ->
                Preferences.lastUpdatePopupTime = kotlin.time.Clock.System.now().epochSeconds
                pendingUpdate = latest
            }
        }

        LaunchedEffect(viewModel) {
            viewModel.sessionExpiredMessage.collect { event ->
                if (event.message != null) {
                    GlobalToasts.show(event.message, level = GlobalToasts.ToastLevel.WARNING)
                } else {
                    GlobalToasts.show(activity.getString(event.fallbackResId), level = GlobalToasts.ToastLevel.WARNING)
                }
            }
        }

        LaunchedEffect(homeState) {
            if (homeState is PageState.Error) {
                val throwable = (homeState as PageState.Error).throwable
                if (throwable is CloudFlareBlockedException) {
                    Log.e("error", "被屏蔽时的处理")

                    HAnalytics.logEvent(FirebaseConstants.EVENT_CLOUDFLARE_BLOCKED)
                }
            }
        }

        MainActivityScaffold(
            selectedDestination = selectedTopLevelDestination,
            showNavigationSuite = if (isHanimeFamilySite) currentMainDestination.showNavigationSuite
            else (!SiteChromeVisibility.navigationSuiteSuppressed &&
                    (isAtSiteHome || atSubscription || atLibrary || atProfile)),
            siteItems = siteNavItems,
            snackbarHostState = snackbarHostState,
            onDestinationSelected = { destination ->
                if (isHanimeFamilySite) {
                    appNavigator.navigateTopLevelDestination(
                        destination = destination,
                        isLoggedIn = isLoggedIn,
                        onRequireLogin = onRequireLogin,
                    )
                }
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                UnifiedMainNavHost(
                    activity = activity,
                    navigator = appNavigator,
                    navigationState = navigationState,
                    isLoggedIn = isLoggedIn,
                    avatarUrl = headerAvatarUrl,
                    username = headerUsername,
                    currentSite = Preferences.baseUrl,
                    onRequireLogin = onRequireLogin,
                    onSwitchSiteClick = onSwitchSiteClick,
                    onLogoutClick = onLogoutClick,
                    onDestinationChanged = { destination ->
                        currentMainDestination = destination
                    },
                    siteChangeKey = siteChangeKey,
                )

                if (showAuthGuard) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                    )
                }

                pendingUpdate?.let { latest ->
                    UpdateDialog(
                        latest = latest,
                        onDismiss = { pendingUpdate = null },
                        onConfirm = {
                            pendingUpdate = null

                            HAnalytics.logEvent(
                                FirebaseConstants.EVENT_UPDATE_CONFIRM,
                                FirebaseConstants.PARAM_METHOD to latest.version,
                            )
                            scope.launch {

                                val file = activity.getUpdateIfExists(latest)
                                if (UpdateManager.state.value.phase == com.yenaly.han1meviewer.logic.update.UpdatePhase.DOWNLOADING) {

                                } else if (file != null) {
                                    UpdateManager.syncWithDisk(activity, latest)
                                } else {
                                    activity.requestPostNotificationPermission()
                                    UpdateManager.start(activity.applicationContext, latest)
                                }
                                appNavigator.navigateSafely(UpdateSettingsRoute)
                            }
                        },
                    )
                }

                UsageNoticeDialog(
                    visible = showUsageNotice,
                    onAccepted = {
                        Preferences.usageNoticeAccepted = true
                        showUsageNotice = false
                    },
                    onDeclined = { activity.finish() },
                )
            }
        }

        if (activity.showSiteSwitchConfirm) {
            SiteSwitchConfirmDialog(
                title = stringResource(R.string.confirm_switch_site),
                confirmText = stringResource(R.string.sure),
                dismissText = stringResource(R.string.no),
                onConfirm = { activity.confirmSiteSwitch() },
                onDismiss = { activity.dismissSiteSwitch() },
            )
        }

        if (activity.showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { activity.dismissLogoutConfirm() },
                title = { Text(stringResource(R.string.sure_to_logout)) },
                confirmButton = {
                    TextButton(onClick = { activity.confirmLogout() }) {
                        Text(stringResource(R.string.sure))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activity.dismissLogoutConfirm() }) {
                        Text(stringResource(R.string.no))
                    }
                },
            )
        }

        activity.crossSiteRequest?.let { request ->
            CrossSiteOpenDialog(
                request = request,
                onDismiss = { activity.dismissCrossSiteRequest() },
                onValidated = { activity.confirmCrossSiteOpen(request) },
            )
        }

        GlobalDialogHost()
        GlobalToastHost()
    }
}
