package com.yenaly.han1meviewer.SitesHub.XHamster.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yenaly.han1meviewer.SitesHub.XHamster.common.XHamsterSession
import com.yenaly.han1meviewer.SitesHub.XHamster.data.remote.XHamsterAuth
import com.yenaly.han1meviewer.SitesHub.XHamster.data.remote.XHamsterPrefsRepo
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SettingSwitchItem
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XHamsterPrivacyScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val loggedIn = XHamsterAuth.isLoggedIn
    var historyActive by remember { mutableStateOf(true) }
    var translation by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        XHamsterPrefsRepo.load().onSuccess { p ->
            p.historyActive?.let { historyActive = it }
            p.titlesTranslation?.let { translation = it }
        }
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text("Privacy") },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { SettingSectionHeader("Watch history") }
            item {
                SettingSwitchItem(
                    title = "Save watch history",
                    summary = if (loggedIn) {
                        if (historyActive) "On — videos you watch are saved. Turn off to pause."
                        else "Paused — nothing you watch is saved. Turn on to resume."
                    } else "Log in to pause or resume your site watch history.",
                    checked = historyActive,
                    onCheckedChange = { on ->
                        if (!loggedIn || busy) return@SettingSwitchItem
                        val previous = historyActive
                        historyActive = on
                        busy = true
                        scope.launch {
                            XHamsterPrefsRepo.setWatchHistoryActive(on)
                                .onSuccess { error = null; XHamsterSession.bump() }
                                .onFailure { historyActive = previous; error = it.message }
                            busy = false
                        }
                    },
                )
            }
            item { SettingSectionHeader("Titles") }
            item {
                SettingSwitchItem(
                    title = "Translate video titles",
                    summary = "Show video titles in your language when a translation exists.",
                    checked = translation,
                    onCheckedChange = { on ->
                        if (busy) return@SettingSwitchItem
                        val previous = translation
                        translation = on
                        busy = true
                        scope.launch {
                            XHamsterPrefsRepo.setTitlesTranslation(on)
                                .onSuccess { error = null; XHamsterSession.bump() }
                                .onFailure { translation = previous; error = it.message }
                            busy = false
                        }
                    },
                )
            }
            error?.let { item { SettingHintText("Could not apply: $it") } }
            item { SettingHintText("Changes reload the home page so they take effect immediately.") }
        }
    }
}
