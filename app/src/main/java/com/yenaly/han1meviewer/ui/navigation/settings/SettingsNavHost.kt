package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.Navigator
import androidx.window.core.layout.WindowSizeClass
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.main.navigateSettingsCategoryDestination
import com.yenaly.han1meviewer.ui.screen.settings.SettingsCategoryItem
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchEmpty
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchField
import com.yenaly.han1meviewer.ui.screen.settings.SettingsSearchResultItem
import com.yenaly.han1meviewer.ui.screen.settings.rememberSettingsSearchResults
import com.yenaly.han1meviewer.util.findActivityOrNull
import com.yenaly.han1meviewer.util.logScreenViewEvent

@Composable
fun SettingsScaffold(
    navigator: Navigator,
    fallbackDestination: NavKey,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val currentScreen = navigator.state.currentKey.resolveSettingsScreenDestination()

    SettingsScaffoldCore(
        currentScreen = currentScreen,
        onBack = {

            if (!navigator.goBack()) {
                navigator.navigate(fallbackDestination)
            }
        },
        onSelectCategory = { navigator.navigateSettingsCategoryDestination(it) },
        actions = actions,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsScaffoldCore(
    currentScreen: SettingsScreenDestination,
    onBack: () -> Unit,
    onSelectCategory: (SettingsCategory) -> Unit,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivityOrNull<MainActivity>()
        ?: error("No MainActivity found")

    LaunchedEffect(currentScreen) {
        activity.logScreenViewEvent(currentScreen.screenClassName)
    }

    val useTwoPane = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    if (useTwoPane) {
        SettingsTwoPaneScaffold(
            onSelectCategory = onSelectCategory,
            currentScreen = currentScreen,
            actions = actions,
            onBack = onBack,
            content = content,
        )
    } else {
        SettingsSinglePaneScaffold(
            currentScreen = currentScreen,
            actions = actions,
            onBack = onBack,
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSinglePaneScaffold(
    currentScreen: SettingsScreenDestination,
    actions: @Composable () -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(currentScreen.titleRes)) },
                navigationIcon = {
                    FilledIconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = { actions() },
                modifier = Modifier.statusBarsPadding(),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun SettingsTwoPaneScaffold(
    onSelectCategory: (SettingsCategory) -> Unit,
    currentScreen: SettingsScreenDestination,
    actions: @Composable () -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()

    val fixedTwoPaneValue = remember {
        ThreePaneScaffoldValue(
            primary = PaneAdaptedValue.Expanded,
            secondary = PaneAdaptedValue.Expanded,
            tertiary = PaneAdaptedValue.Hidden,
        )
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = fixedTwoPaneValue,
        listPane = {
            AnimatedPane {
                SettingsListPane(
                    selectedCategory = currentScreen.category,
                    onSelect = onSelectCategory,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                SettingsSinglePaneScaffold(
                    currentScreen = currentScreen,
                    actions = actions,
                    onBack = onBack,
                    content = content,
                )
            }
        },
    )
}

@Composable
private fun SettingsListPane(
    selectedCategory: SettingsCategory?,
    onSelect: (SettingsCategory) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.settings_pane_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        var query by rememberSaveable { mutableStateOf("") }
        val results = rememberSettingsSearchResults(query)
        SettingsSearchField(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (query.isBlank()) {
                items(
                    items = SettingsCategory.entries,
                    key = { it.key },
                ) { category ->
                    SettingsCategoryItem(
                        category = category,
                        selected = category == selectedCategory,
                        onClick = { onSelect(category) },
                    )
                }
            } else if (results.isEmpty()) {
                item(key = "search_empty") { SettingsSearchEmpty() }
            } else {
                items(results, key = { "${it.category.key}/${it.title}" }) { result ->
                    SettingsSearchResultItem(
                        result = result,
                        onClick = {
                            query = ""
                            onSelect(result.category)
                        },
                    )
                }
            }
        }
    }
}
