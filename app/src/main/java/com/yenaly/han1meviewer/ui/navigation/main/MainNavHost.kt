package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.ui.navigation.NavigationState
import com.yenaly.han1meviewer.ui.navigation.SanitizedPredictiveBackOwner
import com.yenaly.han1meviewer.ui.navigation.screen
import com.yenaly.han1meviewer.ui.navigation.topKey
import com.yenaly.han1meviewer.ui.navigation.settings.settingsEntries
import com.yenaly.han1meviewer.ui.navigation.settings.unifiedHistoryEntries
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.navigation.settings.LocalPlaylistsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MediaControlsSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SitePlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.TranslationCacheRoute
import com.yenaly.han1meviewer.ui.navigation.settings.TranslationSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UpdateSettingsRoute
import com.yenaly.han1meviewer.ui.screen.history.UnifiedHistoryScreen
import com.yenaly.han1meviewer.ui.screen.library.LocalPlaylistsScreen
import com.yenaly.han1meviewer.ui.screen.settings.MediaControlsSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.SitePlayerSettingsPage
import com.yenaly.han1meviewer.ui.screen.settings.TranslationCacheScreen
import com.yenaly.han1meviewer.ui.screen.settings.TranslationSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.UpdateStatusScreen
import com.yenaly.han1meviewer.util.translation.AppTranslation
import com.yenaly.han1meviewer.ui.navigation.settings.AboutAppRoute
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesTopBarActions
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategory
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategoryRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsHomeRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsScaffold
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.account.AccountScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.account.AvatarCropScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.CreatorCenterScreen
import com.yenaly.han1meviewer.ui.screen.library.ContentLibraryScreen
import com.yenaly.han1meviewer.ui.screen.main.ProfileScreen
import com.yenaly.han1meviewer.ui.screen.settings.AboutAppScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.viewmodel.CreatorCenterViewModel
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.viewmodel.UserAccountViewModel
import com.yenaly.han1meviewer.util.HAnalytics
import kotlinx.serialization.json.Json
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.DailyCheckInRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.DownloadRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.FavVideoRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.GetchuPreviewDetailRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.GetchuPreviewRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.HomeRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.MyPlaylistRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.PreviewCommentRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.PreviewRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.SearchRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.SubscriptionRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.VideoRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.WatchHistoryRouteScreen
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.navigation.main.WatchLaterRouteScreen

@Composable
fun MainNavHost(
    activity: MainActivity,
    navigator: Navigator,
    navigationState: NavigationState,
    isLoggedIn: Boolean,
    avatarUrl: String?,
    username: String?,
    currentSite: String,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
) {
    val currentKey = navigationState.currentKey
    val destinationSpec = MainDestinationSpec.fromKey(currentKey)
    var pendingAvatarCropResult by remember { mutableStateOf<String?>(null) }

    val onBack: () -> Unit = { navigator.goBack() }
    val onNavigateToVideo: (String) -> Unit = { code -> navigator.navigateSafely(VideoRoute(code)) }
    val onNavigateToLocalVideo: (String, String?) -> Unit =
        { code, uri -> navigator.navigateSafely(VideoRoute(code, uri)) }

    LaunchedEffect(destinationSpec) {
        destinationSpec?.let(onDestinationChanged)

        destinationSpec?.let {
            HAnalytics.logScreenView(
                screenName = it.name,
                screenClassName = it.routeClass.simpleName ?: it.name,
            )
        }
    }

    val isExpandedWidth = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    val entryProvider = entryProvider<NavKey> {
        screen<HomeRoute> {
            HomeRouteScreen(
                activity = activity,
                onNavigateToPreview = { navigator.navigateSafely(PreviewRoute) },
                onNavigateToSearch = { query -> navigator.navigateSafely(SearchRoute(query = query)) },
                onNavigateToSearchAdvanced = { params ->
                    navigator.navigateSafely(
                        SearchRoute(advancedSearchJson = Json.encodeToString(params))
                    )
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<ContentLibraryRoute> {
            ContentLibraryScreen(
                isLoggedIn = isLoggedIn,
                onRequireLogin = onRequireLogin,
                onNavigateToVideo = onNavigateToVideo,
                onNavigateToLocalVideo = onNavigateToLocalVideo,
            )
        }
        screen<ProfileRoute> {
            ProfileScreen(
                isLoggedIn = isLoggedIn,
                avatarUrl = avatarUrl,
                username = username,
                currentSite = currentSite,
                onAccountClick = {
                    if (isLoggedIn) {
                        navigator.navigateSafely(AccountRoute)
                    } else {
                        onRequireLogin()
                    }
                },
                onAvatarLongClick = onLogoutClick,
                onDailyCheckInClick = { navigator.navigateSafely(DailyCheckInRoute) },
                onCreatorCenterClick = {
                    if (isLoggedIn) {
                        navigator.navigateSafely(CreatorCenterRoute)
                    } else {
                        onRequireLogin()
                    }
                },
                onSettingsClick = { navigator.navigateSafely(HomeSettingsRoute) },
                onSwitchSiteClick = onSwitchSiteClick,
                onUpdateClick = { navigator.navigateSafely(UpdateSettingsRoute) },
                onTranslationClick = if (AppTranslation.available) {
                    { navigator.navigateSafely(TranslationSettingsRoute) }
                } else null,
            )
        }
        screen<WatchHistoryRoute> {
            WatchHistoryRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<MyFavVideoRoute> {
            FavVideoRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<MyWatchLaterRoute> {
            WatchLaterRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<MyPlaylistRoute> {
            MyPlaylistRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<SubscriptionRoute> {
            SubscriptionRouteScreen(
                onBack = onBack,
                onNavigateToSearch = { query -> navigator.navigateSafely(SearchRoute(query = query)) },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<DailyCheckInRoute> {
            DailyCheckInRouteScreen(
                activity = activity,
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<DownloadRoute> {
            DownloadRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
                onNavigateToLocalVideo = onNavigateToLocalVideo,
            )
        }
        screen<CreatorCenterRoute> {
            val creatorViewModel: CreatorCenterViewModel = viewModel()
            CreatorCenterScreen(
                viewModel = creatorViewModel,
                onBack = onBack,
                onOpenUploadedVideo = { item -> onNavigateToVideo(item.videoCode) },
                onOpenUploadingVideo = { item -> onNavigateToLocalVideo("-1", item.remoteVideoUrl) },
            )
        }
        screen<AccountRoute> {
            val accountViewModel: UserAccountViewModel = viewModel()
            AccountScreen(
                viewModel = accountViewModel,
                onBack = onBack,
                onOpenAvatarCrop = { sourceUri ->
                    navigator.navigateSafely(AvatarCropRoute(sourceUri))
                },
                pendingAvatarCropResult = pendingAvatarCropResult,
                onAvatarCropResultConsumed = { pendingAvatarCropResult = null },
                onRefreshHome = { activity.viewModel.getHomePage() },
                onLogout = { activity.requestLogout(closeCurrentPageOnConfirm = true) },
            )
        }
        screen<AvatarCropRoute> { key ->
            AvatarCropScreen(
                sourceUri = key.sourceUri,
                onBack = onBack,
                onConfirm = { file ->
                    pendingAvatarCropResult = file.absolutePath
                    onBack()
                },
            )
        }
        settingsEntries(
            activity = activity,
            navigator = navigator,
            homeFallback = HomeRoute,
            onOpenVideo = onNavigateToVideo,
        )
        unifiedHistoryEntries(activity = activity, navigator = navigator)
        screen<SearchRoute> { key ->
            SearchRouteScreen(
                route = key,
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<PreviewRoute> {
            PreviewRouteScreen(
                activity = activity,
                onBack = onBack,
                onNavigateToGetchuPreview = {
                    navigator.navigateSafely(GetchuPreviewRoute)
                },
                onNavigateToPreviewComment = { date, dateCode ->
                    navigator.navigateSafely(PreviewCommentRoute(date, dateCode))
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        screen<GetchuPreviewRoute> {
            GetchuPreviewRouteScreen(
                onBack = onBack,
                onNavigateToDetail = { id -> navigator.navigateSafely(GetchuPreviewDetailRoute(id)) },
            )
        }
        screen<GetchuPreviewDetailRoute> { key ->
            GetchuPreviewDetailRouteScreen(
                route = key,
                onBack = onBack,
                onNavigateToDetail = { id -> navigator.navigateSafely(GetchuPreviewDetailRoute(id)) },
                onNavigateToVideoUrl = { url -> navigator.navigateSafely(VideoRoute("-1", url)) },
            )
        }
        screen<PreviewCommentRoute> { key ->
            PreviewCommentRouteScreen(
                activity = activity,
                route = key,
                onBack = onBack,
            )
        }
        screen<VideoRoute> { key ->
            VideoRouteScreen(
                activity = activity,
                route = key,
            )
        }
    }

    SanitizedPredictiveBackOwner {
        NavDisplay(
            backStack = navigationState.entriesInUse,
            onBack = { navigator.goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>(),
            ),

            transitionSpec = {
                when {
                    targetState.topKey().isAutoPlayVideo() ->
                        EnterTransition.None togetherWith ExitTransition.None

                    suppressSettingsPaneTransition(isExpandedWidth) ->
                        EnterTransition.None togetherWith ExitTransition.None
                    else -> slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(450)) togetherWith
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            targetOffset = { it / 3 },
                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                        ) + scaleOut(targetScale = 0.9f) + fadeOut(animationSpec = tween(300))
                }
            },

            popTransitionSpec = { mainPopTransform(suppressSettingsPaneTransition(isExpandedWidth)) },
            predictivePopTransitionSpec = { _ ->
                mainPopTransform(suppressSettingsPaneTransition(isExpandedWidth))
            },
            entryProvider = entryProvider,
        )
    }
}

private fun Any?.isAutoPlayVideo(): Boolean = (this as? VideoRoute)?.autoPlay == true

private fun Any?.isSettingsKey(): Boolean =
    SettingsDestinationSpec.fromKey(this) != null || this is SettingsCategoryRoute

private fun AnimatedContentTransitionScope<Scene<NavKey>>.suppressSettingsPaneTransition(
    isExpandedWidth: Boolean,
): Boolean =
    isExpandedWidth && initialState.topKey().isSettingsKey() && targetState.topKey().isSettingsKey()

private fun AnimatedContentTransitionScope<Scene<NavKey>>.mainPopTransform(
    suppressed: Boolean,
): androidx.compose.animation.ContentTransform =
    if (suppressed) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            initialOffset = { it / 3 },
            animationSpec = tween(450, easing = FastOutSlowInEasing)
        ) + scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(450)) togetherWith
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
    }
