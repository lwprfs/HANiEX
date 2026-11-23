package com.yenaly.han1meviewer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.yenaly.han1meviewer.ui.navigation.main.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.main.MainDestinationSpec
import com.yenaly.han1meviewer.ui.navigation.main.MainTopLevelDestination

class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    val currentStack: NavBackStack<NavKey>
        get() = backStacks.getValue(topLevelRoute)

    val currentKey: NavKey?
        get() = backStacks[topLevelRoute]?.lastOrNull()

    val entriesInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            backStacks.getValue(startRoute).toList()
        } else {
            backStacks.getValue(startRoute) + backStacks.getValue(topLevelRoute)
        }
}

class Navigator(val state: NavigationState) {

    private var lastNavigateAtMillis = 0L

    val topLevelRoutes: Set<NavKey> = state.backStacks.keys

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.currentStack.add(route)
        }
    }

    fun navigateSafely(route: NavKey) {
        val now = System.currentTimeMillis()
        if (now - lastNavigateAtMillis < NAVIGATE_THROTTLE_MILLIS) return
        if (route == state.currentStack.lastOrNull()) return
        lastNavigateAtMillis = now
        navigate(route)
    }

    fun goBack(): Boolean {
        val stack = state.currentStack
        if (stack.size > 1) {
            stack.removeLastOrNull()
            return true
        }
        if (state.topLevelRoute != state.startRoute) {
            state.topLevelRoute = state.startRoute
            return true
        }
        return false
    }

    fun replaceTop(route: NavKey) {
        val stack = state.currentStack
        if (stack.isNotEmpty()) {
            stack[stack.lastIndex] = route
        } else {
            stack.add(route)
        }
    }

    fun navigateSingleTop(route: NavKey) {
        if (route == state.currentStack.lastOrNull()) return
        navigate(route)
    }

    fun popTo(route: NavKey, inclusive: Boolean = false): Boolean {
        val stack = state.currentStack
        val index = stack.indexOfLast { it == route }
        if (index < 0) return false
        val keep = if (inclusive) index else index + 1
        while (stack.size > keep) {
            stack.removeAt(stack.size - 1)
        }
        if (stack.isEmpty()) stack.add(state.topLevelRoute)
        return true
    }

    fun isAt(route: NavKey): Boolean = state.currentKey == route

    fun goHome() {
        state.topLevelRoute = state.startRoute
        val homeStack = state.backStacks.getValue(state.startRoute)
        while (homeStack.size > 1) {
            homeStack.removeLastOrNull()
        }
    }

    fun isAtTabRoot(): Boolean = state.currentStack.size <= 1

    companion object {
        private const val NAVIGATE_THROTTLE_MILLIS = 450L
    }
}

@Composable
fun rememberNavigationState(
    startRoute: NavKey = HomeRoute,
    topLevelRoutes: Set<NavKey>,
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute,
        topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

fun topLevelKeyFor(destination: MainTopLevelDestination, state: NavigationState): NavKey =
    state.backStacks.keys.first { key ->
        MainDestinationSpec.fromKey(key)?.topLevelDestination == destination
    }
