package com.yenaly.han1meviewer.ui.navigation.main

import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.ui.navigation.Navigator
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.topLevelKeyFor
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategory
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsCategoryRoute
import kotlinx.serialization.json.Json

fun NavHostController.navigateTopLevelDestination(
    destination: MainTopLevelDestination,
    isLoggedIn: Boolean,
    onRequireLogin: () -> Unit,
): Boolean {
    if (destination.requiresLogin && !isLoggedIn) {
        onRequireLogin()
        return false
    }

    navigateSafely(destination.route) {
        popUpTo(HomeRoute) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    return true
}

fun NavController.navigateSettingsCategoryDestination(category: SettingsCategory) {
    navigateSafely(SettingsCategoryRoute(category.key)) {
        popUpTo(HomeSettingsRoute) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

fun NavController.navigateSettingsDestination(route: Any) {
    navigateSafely(route) {
        launchSingleTop = true
    }
}

fun NavHostController.handleMainIntent(intent: Intent) {
    if (intent.action == com.yenaly.han1meviewer.worker.HUpdateWorker.ACTION_OPEN_UPDATE_STATUS) {
        intent.action = null
        navigateSafely(com.yenaly.han1meviewer.ui.navigation.settings.UpdateSettingsRoute)
        return
    }
    if (intent.action == Intent.ACTION_VIEW) {
        val uri = intent.data ?: return
        when (uri.scheme) {
            "http", "https" -> {
                val videoCode = uri.getQueryParameter("v")
                if (videoCode != null) {
                    navigateSafely(VideoRoute(videoCode))
                }
            }

            "file", "content" -> {
                navigateSafely(VideoRoute("-1", uri.toString()))
            }
        }
        return
    }

    intent.getStringExtra("startSearchFromTag")?.let { tag ->
        intent.removeExtra("startSearchFromTag")
        navigateSafely(SearchRoute(query = tag))
        return
    }

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    val map = intent.getSerializableExtra("startSearchFromMap") as? HashMap<String, String>
    if (map != null) {
        intent.removeExtra("startSearchFromMap")
        navigateSafely(SearchRoute(advancedSearchJson = Json.encodeToString(map)))
        return
    }

    val videoCode = intent.getStringExtra("startVideoCode")
    if (!videoCode.isNullOrEmpty()) {
        intent.removeExtra("startVideoCode")
        navigateSafely(VideoRoute(videoCode))
    }
}

// ---------------------------------------------------------------------------
// Navigation 3（Hanime / Javchu 主导航）
// ---------------------------------------------------------------------------

/** 切换顶层 Tab（底部 NavigationSuite）。目标 Tab 的返回栈与状态自动保留。 */
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

/**
 * 切换设置一级分类：只保留“设置首页 + 当前分类”，避免平板双栏反复点击分类时不断入栈
 * （对应 Navigation 2 时代的 `popUpTo(HomeSettingsRoute){inclusive=false}`）。
 */
fun Navigator.navigateSettingsCategoryDestination(category: SettingsCategory) {
    val stack = state.currentStack
    val baseIndex = stack.indexOfFirst { it == HomeSettingsRoute }
    if (baseIndex >= 0) {
        while (stack.size > baseIndex + 1) {
            stack.removeAt(stack.size - 1)
        }
        stack.add(SettingsCategoryRoute(category.key))
    } else {
        navigateSafely(SettingsCategoryRoute(category.key))
    }
}

/** 进入设置二级页面（普通入栈，保证返回时回到所属分类）。 */
fun Navigator.navigateSettingsDestination(route: androidx.navigation3.runtime.NavKey) {
    navigateSafely(route)
}

fun Navigator.handleMainIntent(intent: Intent) {
    // Intent/深链驱动的导航不走节流：冷启动首帧可能尚无用户手势，被节流拦截会被静默丢弃。
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
