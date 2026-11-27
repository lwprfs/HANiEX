package com.yenaly.han1meviewer.ui.navigation.settings

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HanimeApplication
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.shizuku.ShizukuInstallResult
import com.yenaly.han1meviewer.shizuku.ShizukuUpdateInstaller
import com.yenaly.han1meviewer.shizuku.UpdateInstaller
import com.yenaly.han1meviewer.logic.BackupManager
import com.yenaly.han1meviewer.ui.component.BackupSectionDialog
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.base.AppFontScale
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.SitesHub.Hanime1.ui.screen.home.homepage.saveHomeCategoryPreferences
import com.yenaly.han1meviewer.ui.screen.runAfterUiSettles
import com.yenaly.han1meviewer.ui.screen.settings.HomeSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.dialog.LicenseDialog
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.util.ActivityManager
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.han1meviewer.util.browse
import com.yenaly.han1meviewer.util.folderSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCategoryRouteScreen(
    category: SettingsCategory,
    activity: MainActivity,
    onNavigateToPlayerSettings: () -> Unit,
    onNavigateToUnifiedHistory: () -> Unit = {},
    onNavigateToHKeyframes: () -> Unit,
    onNavigateToSharedHKeyframes: () -> Unit,
    onNavigateToAboutPage: () -> Unit,
    hanimeSection: SiteSettingsSection? = null,
) {
    when (category) {
        SettingsCategory.Network -> {
            NetworkSettingsRouteScreen()
            return
        }

        SettingsCategory.Download -> {
            DownloadSettingsRouteScreen(activity = activity)
            return
        }

        SettingsCategory.Keyframes -> {
            HKeyframeSettingsRouteScreen(
                onNavigateToHKeyframes = onNavigateToHKeyframes,
                onNavigateToSharedHKeyframes = onNavigateToSharedHKeyframes,
            )
            return
        }

        else -> Unit
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val versionState by AppViewModel.versionFlow.collectAsStateWithLifecycle()
    var refreshKey by remember { mutableIntStateOf(0) }
    var cacheKey by remember { mutableIntStateOf(0) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showLicenseScreen by remember { mutableStateOf(false) }
    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showLauncherPicker by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showExportSelection by remember { mutableStateOf(false) }
    var pendingExportSections by remember {
        mutableStateOf<Set<BackupManager.Section>>(BackupManager.Section.entries.toSet())
    }
    var importSections by remember { mutableStateOf<Set<BackupManager.Section>?>(null) }

    fun recreateAfterDialogDismiss() {
        coroutineScope.runAfterUiSettles { activity.recreate() }
    }

    val backupExportSuccess = stringResource(R.string.backup_export_success)
    val backupExportFailed = stringResource(R.string.backup_export_failed)
    val requestPipAlert = stringResource(R.string.request_pip_alert)
    val notSetSysLock = stringResource(R.string.not_set_sys_lock)
    val cacheEmpty = stringResource(R.string.cache_empty)
    val backupImportSuccess = stringResource(R.string.backup_import_success)
    val backupImportFailed = stringResource(R.string.backup_import_failed)
    val clearSuccess = stringResource(R.string.clear_success)
    val clearFailed = stringResource(R.string.clear_failed)
    val fakeIconHint = stringResource(R.string.fake_icon_hint)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            runCatching { BackupManager.exportTo(context, uri, pendingExportSections) }
                .onSuccess { withContext(Dispatchers.Main) { GlobalToasts.show(backupExportSuccess, level = GlobalToasts.ToastLevel.SUCCESS) } }
                .onFailure { withContext(Dispatchers.Main) { GlobalToasts.show(backupExportFailed, level = GlobalToasts.ToastLevel.ERROR) } }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    val hanimeAppName = stringResource(R.string.hanime_app_name)
    val fakeNameCalc = stringResource(R.string.app_name_fake_calc)
    val fakeNameCornhub = stringResource(R.string.app_name_fake_cornhub)
    val fakeNameXXT = stringResource(R.string.app_name_fake_xxt)

    val launcherItems = remember(context) {
        listOf(
            LauncherItem(
                name = hanimeAppName,
                iconRes = R.drawable.ic_launcher_new,
                alias = "com.yenaly.han1meviewer.LauncherAliasDefault",
            ),
            LauncherItem(
                name = fakeNameCalc,
                iconRes = R.drawable.ic_launcher_calc,
                alias = "com.yenaly.han1meviewer.LauncherFakeCalc",
            ),
            LauncherItem(
                name = fakeNameCornhub,
                iconRes = R.drawable.ic_launcher_cornhub,
                alias = "com.yenaly.han1meviewer.LauncherFakeCornhub",
            ),
            LauncherItem(
                name = fakeNameXXT,
                iconRes = R.drawable.ic_launcher_xxt,
                alias = "com.yenaly.han1meviewer.LauncherFakeXxt",
            ),
        )
    }

    var cacheSummary by remember { mutableStateOf("") }

    LaunchedEffect(cacheKey) {
        cacheSummary = withContext(Dispatchers.IO) {
            generateClearCacheSummary(context, context.cacheDir?.folderSize ?: 0L).toString()
        }
    }

    val updateSummary = remember(versionState, context) {

        when (val state = versionState) {
            is WebsiteState.Error -> {

                state.throwable.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.check_update_failed)
            }

            is WebsiteState.Loading -> context.getString(R.string.checking_update)

            is WebsiteState.Success -> {
                val info = state.info
                if (info == null) {
                    context.getString(R.string.already_latest_update)
                } else {
                    context.getString(R.string.check_update_success, info.version)
                }
            }
        }
    }
    val uiState = remember(refreshKey, updateSummary, cacheSummary, launcherItems, context) {
        buildHomeSettingsUiState(
            context = context,
            launcherItems = launcherItems,
            updateSummary = updateSummary,
            cacheSummary = cacheSummary,
        )
    }

    HomeSettingsScreen(
        category = category,
        hanimeSection = hanimeSection,
        state = uiState,
        onVideoLanguageChange = { value ->
            if (value != Preferences.videoLanguage) {
                saveString(SettingsPreferenceKeys.VIDEO_LANGUAGE, value)
                showRestartConfirmDialog = true
            }
        },
        onVideoQualityChange = { value ->
            saveString(SettingsPreferenceKeys.DEFAULT_VIDEO_QUALITY, value)
            refreshKey++
            GlobalToasts.show(context.getString(R.string.success_value, value), level = GlobalToasts.ToastLevel.SUCCESS)
        },
        onDarkModeChange = { value ->
            if (value != Preferences.useDarkMode) {
                saveString(SettingsPreferenceKeys.USE_DARK_MODE, value)

                refreshKey++

                coroutineScope.runAfterUiSettles {
                    ThemeUtils.applyDarkModeFromPreferences(context)
                }
            }
        },
        onAllowPipModeChange = { enabled ->
            if (enabled && !isPipPermissionGranted(context)) {
                GlobalToasts.show(requestPipAlert, level = GlobalToasts.ToastLevel.WARNING)
                openPipPermissionSettings(context)
                saveBoolean(SettingsPreferenceKeys.ALLOW_PIP_MODE, false)
                refreshKey++
                return@HomeSettingsScreen
            }
            saveBoolean(SettingsPreferenceKeys.ALLOW_PIP_MODE, enabled)
            refreshKey++
        },
        onAllowResumePlaybackChange = {
            saveBoolean(SettingsPreferenceKeys.ALLOW_RESUME_PLAYBACK, it)
            refreshKey++
        },
        onShowPlayedIndicatorChange = {
            saveBoolean(SettingsPreferenceKeys.SHOW_PLAYED_INDICATOR, it)
            refreshKey++
        },
        onWatchedProgressThresholdChange = {
            saveInt(SettingsPreferenceKeys.WATCHED_PROGRESS_THRESHOLD, it)
            refreshKey++
        },
        onSearchPaginationChange = {
            saveBoolean(SettingsPreferenceKeys.SEARCH_PAGINATION, it)
            refreshKey++
        },
        onRelatedVideoAsTabChange = {
            saveBoolean(SettingsPreferenceKeys.RELATED_VIDEO_AS_TAB, it)
            refreshKey++
        },
        onSearchArtistIgnoreVideoTypeChange = {
            saveBoolean(SettingsPreferenceKeys.SEARCH_ARTIST_IGNORE_VIDEO_TYPE, it)
            refreshKey++
        },
        onDisableMobileDataWarningChange = {
            saveBoolean(SettingsPreferenceKeys.DISABLE_MOBILE_DATA_WARNING, it)
            refreshKey++
        },
        onShowExitConfirmChange = {
            saveBoolean(SettingsPreferenceKeys.SHOW_EXIT_CONFIRM, it)
            refreshKey++
        },
        onDisablePredictiveBackChange = {
            saveBoolean(SettingsPreferenceKeys.DISABLE_PREDICTIVE_BACK, it)
            refreshKey++
        },
        onDisableCommentsChange = {
            saveBoolean(SettingsPreferenceKeys.DISABLE_COMMENTS, it)
            refreshKey++
        },
        onVideoGridColumnsConfigChange = { config ->
            saveInt(SettingsPreferenceKeys.VIDEO_GRID_COLUMNS_COMPACT, config.compactColumns)
            saveInt(SettingsPreferenceKeys.VIDEO_GRID_COLUMNS_MEDIUM, config.mediumColumns)
            saveInt(SettingsPreferenceKeys.VIDEO_GRID_COLUMNS_EXPANDED, config.expandedColumns)
            saveInt(SettingsPreferenceKeys.VIDEO_GRID_COLUMNS_LARGE, config.largeColumns)
            refreshKey++
        },
        onSearchGridColumnsConfigChange = { config ->
            saveInt(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_COMPACT, config.compactColumns)
            saveInt(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_MEDIUM, config.mediumColumns)
            saveInt(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_EXPANDED, config.expandedColumns)
            saveInt(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_LARGE, config.largeColumns)
            refreshKey++
        },
        onHorizontalCardCountConfigChange = { config ->
            saveString(SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_NARROW, config.narrowCount.toString())
            saveString(SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_COMPACT, config.compactCount.toString())
            saveString(SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_MEDIUM, config.mediumCount.toString())
            saveString(SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_EXPANDED, config.expandedCount.toString())
            refreshKey++
        },
        onThemeColorChange = { key ->
            saveString(SettingsPreferenceKeys.THEME_COLOR, key)
            refreshKey++
            recreateAfterDialogDismiss()
        },
        onPureBlackDarkModeChange = { enabled ->
            if (enabled != Preferences.pureBlackDarkMode) {
                saveBoolean(SettingsPreferenceKeys.PURE_BLACK_DARK_MODE, enabled)
                refreshKey++
                recreateAfterDialogDismiss()
            }
        },
        onHomeCategoryPreferencesChange = { order, hiddenKeys ->
            saveHomeCategoryPreferences(order, hiddenKeys)
            refreshKey++
        },
        onUseCIUpdateChannelChange = { value ->
            saveBoolean(SettingsPreferenceKeys.USE_CI_UPDATE_CHANNEL, value)
            refreshKey++
            AppViewModel.getLatestVersion()
        },
        onUpdateInstallerChange = { value ->
            if (value == UpdateInstaller.SHIZUKU) {
                coroutineScope.launch {

                    val msgRes = when (ShizukuUpdateInstaller.requestPermission(context)) {
                        ShizukuInstallResult.Success -> null
                        ShizukuInstallResult.NotInstalled -> R.string.shizuku_not_installed_short
                        ShizukuInstallResult.NotRunning -> R.string.shizuku_not_running_short
                        else -> R.string.shizuku_permission_denied_short
                    }
                    if (msgRes == null) {
                        saveString(SettingsPreferenceKeys.UPDATE_INSTALLER, value)
                        refreshKey++
                        GlobalToasts.show(
                            context.getString(R.string.shizuku_permission_granted),
                            level = GlobalToasts.ToastLevel.SUCCESS,
                        )
                    } else {
                        GlobalToasts.show(context.getString(msgRes), level = GlobalToasts.ToastLevel.WARNING)
                    }
                }
            } else {
                saveString(SettingsPreferenceKeys.UPDATE_INSTALLER, value)
                refreshKey++
            }
        },
        onUseAnalyticsChange = { value ->
            if (!value) {
                showAnalyticsDialog = true
                return@HomeSettingsScreen
            }
            saveBoolean(SettingsPreferenceKeys.USE_ANALYTICS, true)
            refreshKey++
            Firebase.analytics.setAnalyticsCollectionEnabled(true)
        },
        onUseLockScreenChange = { value ->
            if (value) {
                if (!isDeviceSecureCompat(context)) {
                    GlobalToasts.show(notSetSysLock, level = GlobalToasts.ToastLevel.WARNING)
                    refreshKey++
                    return@HomeSettingsScreen
                }
            }
            saveBoolean(SettingsPreferenceKeys.USE_LOCK_SCREEN, value)
            refreshKey++
        },
        onOpenPlayerSettings = onNavigateToPlayerSettings,
        onOpenUnifiedHistory = onNavigateToUnifiedHistory,
        onAppLanguageChange = { value ->
            val old = Preferences.preferenceSp.getString(SettingsPreferenceKeys.APP_LANGUAGE, "system") ?: "system"
            if (old != value) {
                Preferences.preferenceSp.edit { putString(SettingsPreferenceKeys.APP_LANGUAGE, value) }
                refreshKey++
                recreateAfterDialogDismiss()
            }
        },
        onAppFontScaleChange = { key ->
            val pref = AppFontScale.fromKey(key)
            if (pref != Preferences.appFontScale) {
                Preferences.preferenceSp.edit {
                    putString(SettingsPreferenceKeys.APP_FONT_SCALE, pref.key)
                }
                refreshKey++
                recreateAfterDialogDismiss()
            }
        },
        onIgnoreSystemDisplaySizeChange = { enabled ->
            if (enabled != Preferences.ignoreSystemDisplaySize) {
                saveBoolean(SettingsPreferenceKeys.IGNORE_SYSTEM_DISPLAY_SIZE, enabled)
                refreshKey++
                recreateAfterDialogDismiss()
            }
        },
        onCheckUpdate = {
            val currentVersion = versionState
            if (currentVersion is WebsiteState.Success && currentVersion.info != null) {
                AppViewModel.showUpdateDialogIfAvailable()
            } else {
                AppViewModel.getLatestVersion(forceShow = true)
            }
        },
        onUpdatePopupIntervalDaysChange = {
            Preferences.preferenceSp.edit { putInt(SettingsPreferenceKeys.UPDATE_POPUP_INTERVAL_DAYS, it) }
            refreshKey++
        },
        onOpenFakeLauncherIcon = { showLauncherPicker = true },
        onOpenOpenSourceLicense = { showLicenseScreen = true },
        onOpenAboutPage = onNavigateToAboutPage,
        onClearCache = {
            val cacheDir = context.cacheDir
            val folderSize = cacheDir?.folderSize ?: 0L
            if (folderSize == 0L) {
                GlobalToasts.show(cacheEmpty, level = GlobalToasts.ToastLevel.INFO)
                return@HomeSettingsScreen
            }
            showClearCacheConfirm = true
        },
        onExportBackup = { showExportSelection = true },
        onImportBackup = {
            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onSubmitBug = { context.browse(HA1_GITHUB_ISSUE_URL) },
        onOpenForum = { context.browse(HA1_GITHUB_FORUM_URL) },
    )

    ConfirmDialog(
        visible = pendingImportUri != null && importSections == null,
        title = stringResource(R.string.backup_import_title),
        message = stringResource(R.string.backup_import_confirm_message),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            importSections = BackupManager.Section.entries.toSet()
        },
        onDismiss = { pendingImportUri = null },
    )

    BackupSectionDialog(
        visible = showExportSelection,
        title = stringResource(R.string.backup_choose_export),
        confirmText = stringResource(R.string.backup_action_export),
        dismissText = stringResource(R.string.cancel),
        onConfirm = { sections ->
            showExportSelection = false
            pendingExportSections = sections
            exportLauncher.launch("Han1meViewer-backup-${System.currentTimeMillis()}.json")
        },
        onDismiss = { showExportSelection = false },
    )

    BackupSectionDialog(
        visible = importSections != null && pendingImportUri != null,
        title = stringResource(R.string.backup_choose_import),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = { sections ->
            val uri = pendingImportUri ?: return@BackupSectionDialog
            pendingImportUri = null
            importSections = null
            coroutineScope.launch(Dispatchers.IO) {
                runCatching { BackupManager.importFrom(context, uri, sections) }
                    .onSuccess {
                        withContext(Dispatchers.Main) {
                            GlobalToasts.show(backupImportSuccess, level = GlobalToasts.ToastLevel.SUCCESS)
                            refreshKey++
                            activity.recreate()
                        }
                    }
                    .onFailure {
                        withContext(Dispatchers.Main) {
                            GlobalToasts.show(backupImportFailed, level = GlobalToasts.ToastLevel.ERROR)
                        }
                    }
            }
        },
        onDismiss = { pendingImportUri = null; importSections = null },
    )

    ConfirmDialog(
        visible = showClearCacheConfirm,
        title = stringResource(R.string.sure_to_clear),
        message = stringResource(R.string.sure_to_clear_cache),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            showClearCacheConfirm = false
            coroutineScope.launch(Dispatchers.IO) {
                val cacheDir = context.cacheDir
                val success = cacheDir?.deleteRecursively() == true
                withContext(Dispatchers.Main) {
                    cacheKey++
                    refreshKey++
                    if (success) {
                        GlobalToasts.show(clearSuccess, level = GlobalToasts.ToastLevel.SUCCESS)
                    } else {
                        GlobalToasts.show(clearFailed, level = GlobalToasts.ToastLevel.ERROR)
                    }
                }
            }
        },
        onDismiss = { showClearCacheConfirm = false },
    )

    if (showLicenseScreen) {
        LicenseDialog(
            onDismiss = { showLicenseScreen = false }
        )
    }

    if (showAnalyticsDialog) {
        AnalyticsInfoDialog(
            onDeny = {
                saveBoolean(SettingsPreferenceKeys.USE_ANALYTICS, false)
                refreshKey++
                Firebase.analytics.setAnalyticsCollectionEnabled(false)
                showAnalyticsDialog = false
            },
            onDismiss = { showAnalyticsDialog = false },
        )
    }

    ConfirmDialog(
        visible = showRestartConfirmDialog,
        title = stringResource(R.string.attention),
        message = stringResource(R.string.restart_needed),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        cancelable = false,
        onConfirm = {
            showRestartConfirmDialog = false
            coroutineScope.runAfterUiSettles {
                ActivityManager.restart(killProcess = true)
            }
        },
        onDismiss = { showRestartConfirmDialog = false },
    )

    if (showLauncherPicker) {
        LauncherPickerDialog(
            items = launcherItems,
            onSelect = { item ->
                Preferences.preferenceSp.edit {
                    putString(SettingsPreferenceKeys.FAKE_LAUNCHER_ICON, item.alias)
                }
                (context.applicationContext as? HanimeApplication)?.switchLauncher(item.alias)
                GlobalToasts.show(fakeIconHint, level = GlobalToasts.ToastLevel.INFO)
                refreshKey++
                showLauncherPicker = false
            },
            onDismiss = { showLauncherPicker = false },
        )
    }
}
