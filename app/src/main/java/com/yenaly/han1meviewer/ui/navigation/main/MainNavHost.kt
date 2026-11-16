package com.yenaly.han1meviewer.ui.navigation.main

import com.yenaly.han1meviewer.util.translation.AppTranslation
import com.yenaly.han1meviewer.ui.navigation.popBackStackSafely
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.screen.account.AccountScreen
import com.yenaly.han1meviewer.ui.screen.account.AvatarCropScreen
import com.yenaly.han1meviewer.ui.screen.home.CreatorCenterScreen
import com.yenaly.han1meviewer.ui.screen.library.ContentLibraryScreen
import com.yenaly.han1meviewer.ui.screen.main.ProfileScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategoryRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.settings.TranslationSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.UpdateSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.registerSettingsRoutes
import com.yenaly.han1meviewer.ui.navigation.settings.registerUnifiedHistoryRoute
import com.yenaly.han1meviewer.ui.viewmodel.CreatorCenterViewModel
import com.yenaly.han1meviewer.ui.viewmodel.UserAccountViewModel
import kotlinx.serialization.json.Json

private fun NavBackStackEntry?.isSettingsRoute(): Boolean {
    return this?.destination?.hasRoute<SettingsCategoryRoute>() == true ||
            SettingsDestinationSpec.fromDestination(this?.destination) != null
}

@Composable
fun MainNavHost(
    activity: MainActivity,
    navController: NavHostController,
    isLoggedIn: Boolean,
    avatarUrl: String?,
    username: String?,
    currentSite: String,
    onRequireLogin: () -> Unit,
    onSwitchSiteClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDestinationChanged: (MainDestinationSpec) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinationSpec = MainDestinationSpec.fromDestination(backStackEntry?.destination)
    var pendingAvatarCropResult by remember { mutableStateOf<String?>(null) }

    val onBack: () -> Unit = { navController.popBackStackSafely() }
    val onNavigateToVideo: (String) -> Unit = { code -> navController.navigateSafely(VideoRoute(code)) }
    val onNavigateToLocalVideo: (String, String?) -> Unit =
        { code, uri -> navController.navigateSafely(VideoRoute(code, uri)) }

    LaunchedEffect(destinationSpec) {
        destinationSpec?.let(onDestinationChanged)
    }

    val isExpandedWidth = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val suppressSettingsPaneTransition: (NavBackStackEntry, NavBackStackEntry) -> Boolean =
        { initial, target ->
            isExpandedWidth && initial.isSettingsRoute() && target.isSettingsRoute()
        }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = {
            when {
                targetState.isAutoPlayVideo() -> EnterTransition.None

                suppressSettingsPaneTransition(initialState, targetState) -> EnterTransition.None
                else -> slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(450))
            }
        },
        exitTransition = {
            when {
                targetState.isAutoPlayVideo() -> ExitTransition.None
                suppressSettingsPaneTransition(initialState, targetState) -> ExitTransition.None
                else -> slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    targetOffset = { it / 3 },
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) + scaleOut(targetScale = 0.9f) + fadeOut(animationSpec = tween(300))
            }
        },
        popEnterTransition = {
            if (suppressSettingsPaneTransition(initialState, targetState)) {
                EnterTransition.None
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    initialOffset = { it / 3 },
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) + scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(450))
            }
        },
        popExitTransition = {
            if (suppressSettingsPaneTransition(initialState, targetState)) {
                ExitTransition.None
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        }
    ) {
        composable<HomeRoute> {
            HomeRouteScreen(
                activity = activity,
                onNavigateToPreview = { navController.navigateSafely(PreviewRoute) },
                onNavigateToSearch = { query -> navController.navigateSafely(SearchRoute(query = query)) },
                onNavigateToSearchAdvanced = { params ->
                    navController.navigateSafely(
                        SearchRoute(advancedSearchJson = Json.encodeToString(params))
                    )
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<ContentLibraryRoute> {
            ContentLibraryScreen(
                isLoggedIn = isLoggedIn,
                onRequireLogin = onRequireLogin,
                onNavigateToVideo = onNavigateToVideo,
                onNavigateToLocalVideo = onNavigateToLocalVideo,
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                isLoggedIn = isLoggedIn,
                avatarUrl = avatarUrl,
                username = username,
                currentSite = currentSite,
                onAccountClick = {
                    if (isLoggedIn) {
                        navController.navigateSafely(AccountRoute)
                    } else {
                        onRequireLogin()
                    }
                },
                onAvatarLongClick = onLogoutClick,
                onDailyCheckInClick = { navController.navigateSafely(DailyCheckInRoute) },
                onCreatorCenterClick = {
                    if (isLoggedIn) {
                        navController.navigateSafely(CreatorCenterRoute)
                    } else {
                        onRequireLogin()
                    }
                },
                onSettingsClick = { navController.navigateSafely(HomeSettingsRoute) },
                onSwitchSiteClick = onSwitchSiteClick,
                onUpdateClick = { navController.navigateSafely(UpdateSettingsRoute) },
                onTranslationClick = if (AppTranslation.available) {
                    { navController.navigateSafely(TranslationSettingsRoute) }
                } else null,
            )
        }
        composable<WatchHistoryRoute> {
            WatchHistoryRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<MyFavVideoRoute> {
            FavVideoRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<MyWatchLaterRoute> {
            WatchLaterRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<MyPlaylistRoute> {
            MyPlaylistRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<SubscriptionRoute> {
            SubscriptionRouteScreen(
                onBack = onBack,
                onNavigateToSearch = { query -> navController.navigateSafely(SearchRoute(query = query)) },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<DailyCheckInRoute> {
            DailyCheckInRouteScreen(
                activity = activity,
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<DownloadRoute> {
            DownloadRouteScreen(
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
                onNavigateToLocalVideo = onNavigateToLocalVideo,
            )
        }
        composable<CreatorCenterRoute> {
            val creatorViewModel: CreatorCenterViewModel = viewModel()
            CreatorCenterScreen(
                viewModel = creatorViewModel,
                onBack = onBack,
                onOpenUploadedVideo = { item -> onNavigateToVideo(item.videoCode) },
                onOpenUploadingVideo = { item -> onNavigateToLocalVideo("-1", item.remoteVideoUrl) },
            )
        }
        composable<AccountRoute> {
            val accountViewModel: UserAccountViewModel = viewModel()
            AccountScreen(
                viewModel = accountViewModel,
                onBack = onBack,
                onOpenAvatarCrop = { sourceUri ->
                    navController.navigateSafely(AvatarCropRoute(sourceUri))
                },
                pendingAvatarCropResult = pendingAvatarCropResult,
                onAvatarCropResultConsumed = { pendingAvatarCropResult = null },
                onRefreshHome = { activity.viewModel.getHomePage() },
                onLogout = { activity.requestLogout(closeCurrentPageOnConfirm = true) },
            )
        }
        composable<AvatarCropRoute> {
            val route = it.toRoute<AvatarCropRoute>()
            AvatarCropScreen(
                sourceUri = route.sourceUri,
                onBack = onBack,
                onConfirm = { file ->
                    pendingAvatarCropResult = file.absolutePath
                    onBack()
                },
            )
        }
        registerSettingsRoutes(
            activity = activity,
            navController = navController,
            homeFallback = HomeRoute,
            onOpenVideo = onNavigateToVideo,
        )
        registerUnifiedHistoryRoute(activity, navController)
        composable<SearchRoute> {
            SearchRouteScreen(
                route = it.toRoute(),
                onBack = onBack,
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<PreviewRoute> {
            PreviewRouteScreen(
                activity = activity,
                onBack = onBack,
                onNavigateToGetchuPreview = {
                    navController.navigateSafely(GetchuPreviewRoute)
                },
                onNavigateToPreviewComment = { date, dateCode ->
                    navController.navigateSafely(PreviewCommentRoute(date, dateCode))
                },
                onNavigateToVideo = onNavigateToVideo,
            )
        }
        composable<GetchuPreviewRoute> {
            GetchuPreviewRouteScreen(
                onBack = onBack,
                onNavigateToDetail = { id -> navController.navigateSafely(GetchuPreviewDetailRoute(id)) },
            )
        }
        composable<GetchuPreviewDetailRoute> {
            GetchuPreviewDetailRouteScreen(
                route = it.toRoute(),
                onBack = onBack,
                onNavigateToDetail = { id -> navController.navigateSafely(GetchuPreviewDetailRoute(id)) },
                onNavigateToVideoUrl = { url -> navController.navigateSafely(VideoRoute("-1", url)) },
            )
        }
        composable<PreviewCommentRoute> {
            PreviewCommentRouteScreen(
                activity = activity,
                route = it.toRoute(),
                onBack = onBack,
            )
        }
        composable<VideoRoute> {
            VideoRouteScreen(
                activity = activity,
                route = it.toRoute(),
            )
        }
    }
}

private fun NavBackStackEntry.isAutoPlayVideo(): Boolean =
    destination.hasRoute<VideoRoute>() && toRoute<VideoRoute>().autoPlay
