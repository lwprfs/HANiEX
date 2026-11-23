package com.yenaly.han1meviewer.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

const val NAV_KEY_METADATA = "com.yenaly.han1meviewer.NAV_KEY"

inline fun <reified K : NavKey> EntryProviderScope<NavKey>.screen(
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(metadata = { mapOf(NAV_KEY_METADATA to it) }, content = content)
}

fun Scene<*>.topKey(): Any? =
    entries.lastOrNull()?.metadata?.get(NAV_KEY_METADATA)

@Composable
fun SiteNavDisplay(
    navigator: Navigator,
    navigationState: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    SanitizedPredictiveBackOwner {
        NavDisplay(
            backStack = navigationState.entriesInUse,
            onBack = { navigator.goBack() },
            modifier = modifier,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>(),
            ),
            transitionSpec = { siteForwardTransform() },
            popTransitionSpec = { siteBackTransform() },
            predictivePopTransitionSpec = { _ -> siteBackTransform() },
            entryProvider = entryProvider,
        )
    }
}

private fun AnimatedContentTransitionScope<Scene<NavKey>>.siteForwardTransform(): ContentTransform =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(450, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(450)) togetherWith
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            targetOffset = { it / 3 },
            animationSpec = tween(450, easing = FastOutSlowInEasing)
        ) + scaleOut(targetScale = 0.9f) + fadeOut(animationSpec = tween(300))

private fun AnimatedContentTransitionScope<Scene<NavKey>>.siteBackTransform(): ContentTransform =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        initialOffset = { it / 3 },
        animationSpec = tween(450, easing = FastOutSlowInEasing)
    ) + scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(450)) togetherWith
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(450, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(300))
