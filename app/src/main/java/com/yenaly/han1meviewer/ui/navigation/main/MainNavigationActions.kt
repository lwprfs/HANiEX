package com.yenaly.han1meviewer.ui.navigation.main

import android.content.Intent
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.ui.navigation.topLevelKeyFor
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategory
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategoryRoute
import kotlinx.serialization.json.Json

fun Navigator.navigateTopLevelDestination(
    destination: MainTopLevelDestination,
    isLoggedIn: Boolean,
    onRequireLogin: () -> Unit,
): Boolean {
    if (destination.requiresLogin && !isLoggedIn) {
        onRequireLogin()
        return false
    }

    navigate(topLevelKeyFor(destination, state))
    return true
}

fun Navigator.navigateSettingsCategoryDestination(category: SettingsCategory) {
    navigateFromSettingsHome(SettingsCategoryRoute(category.key))
}

fun Navigator.navigateFromSettingsHome(route: androidx.navigation3.runtime.NavKey) {
    val stack = state.currentStack
    val baseIndex = stack.indexOfFirst { it == HomeSettingsRoute }
    if (baseIndex >= 0) {
        while (stack.size > baseIndex + 1) {
            stack.removeAt(stack.size - 1)
        }
        stack.add(route)
    } else {
        navigateSafely(route)
    }
}

fun Navigator.navigateSettingsDestination(route: androidx.navigation3.runtime.NavKey) {
    navigateSafely(route)
}

fun Navigator.handleMainIntent(intent: Intent) {

    if (intent.action == com.yenaly.han1meviewer.worker.HUpdateWorker.ACTION_OPEN_UPDATE_STATUS) {
        intent.action = null
        navigate(com.yenaly.han1meviewer.ui.navigation.settings.UpdateSettingsRoute)
        return
    }
    if (intent.action == Intent.ACTION_VIEW) {
        val uri = intent.data ?: return
        when (uri.scheme) {
            "http", "https" -> {
                val videoCode = uri.getQueryParameter("v")
                if (videoCode != null) {
                    navigate(VideoRoute(videoCode))
                }
            }

            "file", "content" -> {
                navigate(VideoRoute("-1", uri.toString()))
            }
        }
        return
    }

    intent.getStringExtra("startSearchFromTag")?.let { tag ->
        intent.removeExtra("startSearchFromTag")
        navigate(SearchRoute(query = tag))
        return
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    val map = intent.getSerializableExtra("startSearchFromMap") as? HashMap<String, String>
    if (map != null) {
        intent.removeExtra("startSearchFromMap")
        navigate(SearchRoute(advancedSearchJson = Json.encodeToString(map)))
        return
    }

    val videoCode = intent.getStringExtra("startVideoCode")
    if (!videoCode.isNullOrEmpty()) {
        intent.removeExtra("startVideoCode")
        navigate(VideoRoute(videoCode))
    }
}
