package com.yenaly.han1meviewer.RouVideo.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.RouVideo.data.model.RouVideoSessionCheck
import com.yenaly.han1meviewer.RouVideo.data.remote.RouVideoAuth
import com.yenaly.han1meviewer.RouVideo.ui.library.RouVideoLibraryTab
import com.yenaly.han1meviewer.RouVideo.ui.login.rememberRouVideoLoginLauncher
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

@Composable
fun RouVideoAccountScreen(
    onBack: () -> Unit,
    onOpenLibrary: (RouVideoLibraryTab) -> Unit,
    onEditProfile: () -> Unit,
) {
    val auth by RouVideoAuth.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val launchLogin = rememberRouVideoLoginLauncher()
    var busy by remember { mutableStateOf(false) }
    val expiredText = stringResource(R.string.rouvideo_account_session_expired)
    val signedOutText = stringResource(R.string.rouvideo_logout_done)

    LaunchedEffect(Unit) {
        if (RouVideoAuth.isLoggedIn) {
            val check = RouVideoAuth.refresh()
            if (check is RouVideoSessionCheck.LoggedOut) snackbar.showSnackbar(expiredText)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.rouvideo_account_title)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!auth.loggedIn) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    RouVideoSignInPrompt(
                        onSignIn = { launchLogin(false) },
                        onRegister = { launchLogin(true) },
                    )
                }
                return@Column
            }

            val user = auth.user
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user?.initial ?: "U",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(
                                R.string.rouvideo_account_signed_in_as,
                                user?.displayName ?: "RouVideo",
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        user?.email?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }

            AccountEntry(Icons.Default.Subscriptions, stringResource(R.string.rouvideo_my_follows)) {
                onOpenLibrary(RouVideoLibraryTab.FOLLOWS)
            }
            AccountEntry(Icons.Default.Favorite, stringResource(R.string.rouvideo_my_favorites)) {
                onOpenLibrary(RouVideoLibraryTab.FAVORITES)
            }
            AccountEntry(Icons.Default.History, stringResource(R.string.rouvideo_cloud_history)) {
                onOpenLibrary(RouVideoLibraryTab.HISTORY)
            }
            if (user != null) {
                AccountEntry(Icons.Default.Edit, stringResource(R.string.rouvideo_edit_profile)) {
                    onEditProfile()
                }
            }

            OutlinedButton(
                onClick = {
                    if (busy) return@OutlinedButton
                    busy = true
                    scope.launch {
                        RouVideoAuth.logout()
                        busy = false
                        snackbar.showSnackbar(signedOutText)
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text(
                    text = stringResource(R.string.rouvideo_logout),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AccountEntry(icon: ImageVector, title: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
