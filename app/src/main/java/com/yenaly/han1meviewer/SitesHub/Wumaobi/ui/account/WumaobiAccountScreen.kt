package com.yenaly.han1meviewer.SitesHub.Wumaobi.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.Wumaobi.data.remote.WumaobiAuth
import com.yenaly.han1meviewer.ui.component.SettingHintText
import com.yenaly.han1meviewer.ui.component.SettingNavigationItem
import com.yenaly.han1meviewer.ui.component.SettingSectionHeader
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WumaobiAccountScreen(
    onBack: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    val auth by WumaobiAuth.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.wumaobi_account_title)) },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (auth.loggedIn) {
                SignedInContent(
                    userName = auth.userName,
                    onOpenFavorites = onOpenFavorites,
                )
            } else {
                SignInForm()
            }
        }
    }
}

@Composable
private fun SignedInContent(userName: String, onOpenFavorites: () -> Unit) {
    val scope = rememberCoroutineScope()
    SettingSectionHeader(
        userName.ifBlank { stringResource(R.string.wumaobi_account_signed_in) }
    )
    SettingNavigationItem(
        title = stringResource(R.string.wumaobi_favorites),
        summary = stringResource(R.string.wumaobi_favorites_summary),
        icon = Icons.Filled.Favorite,
        onClick = onOpenFavorites,
    )
    SettingNavigationItem(
        title = stringResource(R.string.wumaobi_sign_out),
        icon = Icons.AutoMirrored.Filled.Logout,
        onClick = { scope.launch { WumaobiAuth.logout() } },
    )
}

@Composable
private fun SignInForm() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var registerMode by rememberSaveable { mutableStateOf(false) }
    var userName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var mail by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun submit() {
        if (busy) return
        if (userName.isBlank() || password.isEmpty()) {
            Toast.makeText(context, R.string.wumaobi_account_fill_in, Toast.LENGTH_SHORT).show()
            return
        }
        busy = true
        scope.launch {
            val result = if (registerMode) {
                WumaobiAuth.register(userName, password, mail)
            } else {
                WumaobiAuth.login(userName, password)
            }
            busy = false
            result.onFailure {
                Toast.makeText(
                    context,
                    it.message ?: it.javaClass.simpleName,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                if (registerMode) R.string.wumaobi_account_register else R.string.wumaobi_account_sign_in
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text(stringResource(R.string.wumaobi_account_username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.wumaobi_account_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (registerMode) {
            OutlinedTextField(
                value = mail,
                onValueChange = { mail = it },
                label = { Text(stringResource(R.string.wumaobi_account_mail_optional)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = ::submit, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp).size(16.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    stringResource(
                        if (registerMode) R.string.wumaobi_account_register
                        else R.string.wumaobi_account_sign_in
                    )
                )
            }
            TextButton(onClick = { registerMode = !registerMode }, enabled = !busy) {
                Text(
                    stringResource(
                        if (registerMode) R.string.wumaobi_account_have_account
                        else R.string.wumaobi_account_no_account
                    )
                )
            }
        }
        SettingHintText(stringResource(R.string.wumaobi_account_hint))
    }
}
