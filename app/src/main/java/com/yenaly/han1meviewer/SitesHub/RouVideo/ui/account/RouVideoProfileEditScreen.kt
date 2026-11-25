package com.yenaly.han1meviewer.SitesHub.RouVideo.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.SitesHub.RouVideo.data.remote.RouVideoNotSignedInException
import com.yenaly.han1meviewer.SitesHub.RouVideo.ui.login.rememberRouVideoLoginLauncher
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

@Composable
fun RouVideoProfileEditScreen(onBack: () -> Unit) {
    val auth by RouVideoAuth.state.collectAsStateWithLifecycle()
    val user = auth.user
    val scope = rememberCoroutineScope()
    val launchLogin = rememberRouVideoLoginLauncher()

    var name by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(user?.id) {
        if (user == null && RouVideoAuth.isLoggedIn) RouVideoAuth.refresh()
    }
    LaunchedEffect(user?.name) {
        if (!saved && name.isEmpty()) name = user?.name.orEmpty()
    }

    val savedText = stringResource(R.string.rouvideo_profile_saved)
    val failedText = stringResource(R.string.rouvideo_profile_save_failed)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.rouvideo_profile_title)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!saved && (!auth.loggedIn || user == null)) {
                RouVideoSignInPrompt(
                    onSignIn = { launchLogin(false) },
                    onRegister = { launchLogin(true) },
                )
                return@Column
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 40) { name = it; error = null } },
                label = { Text(stringResource(R.string.rouvideo_profile_display_name)) },
                supportingText = { Text(stringResource(R.string.rouvideo_profile_name_hint)) },
                singleLine = true,
                enabled = !saving && !saved,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = user?.email.orEmpty(),
                onValueChange = {},
                label = { Text(stringResource(R.string.rouvideo_profile_email)) },
                supportingText = { Text(stringResource(R.string.rouvideo_profile_email_hint)) },
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )

            val message = if (saved) savedText else error
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (saved) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = {
                    if (saving || name.isBlank()) return@Button
                    saving = true
                    error = null
                    scope.launch {
                        RouVideoAuth.updateProfileName(name)
                            .onSuccess { saved = true }
                            .onFailure {
                                error = if (it is RouVideoNotSignedInException) it.message
                                else it.message?.takeIf { m -> m.isNotBlank() } ?: failedText
                            }
                        saving = false
                    }
                },
                enabled = !saving && !saved && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (saving) R.string.rouvideo_profile_saving else R.string.rouvideo_profile_save
                    )
                )
            }
        }
    }
}
