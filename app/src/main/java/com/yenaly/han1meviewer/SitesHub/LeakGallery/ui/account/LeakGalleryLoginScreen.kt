package com.yenaly.han1meviewer.SitesHub.LeakGallery.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.model.LeakGalleryLoginResult
import com.yenaly.han1meviewer.SitesHub.LeakGallery.data.remote.LeakGalleryRepo
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

/**
 * LeakGallery sign in / sign up. Mirrors the website: username (or email) + password +
 * Turnstile captcha, then an optional 2FA step (authenticator code or recovery code).
 */
@Composable
fun LeakGalleryLoginScreen(
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    startOnRegister: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var register by rememberSaveable { mutableStateOf(startOnRegister) }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    var captchaKey by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var tempToken by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var useRecovery by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }

    val success = stringResource(R.string.leakgallery_login_success)
    val registered = stringResource(R.string.leakgallery_register_success)
    val vUser = stringResource(R.string.leakgallery_validate_username)
    val vPass = stringResource(R.string.leakgallery_validate_password)
    val vEmail = stringResource(R.string.leakgallery_validate_email)
    val vMatch = stringResource(R.string.leakgallery_validate_mismatch)
    val vCaptcha = stringResource(R.string.leakgallery_captcha_hint)
    val mInvalid = stringResource(R.string.leakgallery_login_invalid)
    val mCaptcha = stringResource(R.string.leakgallery_login_captcha_rejected)
    val mUnavailable = stringResource(R.string.leakgallery_login_unavailable)
    val mBanned = stringResource(R.string.leakgallery_login_banned)
    val mTooMany = stringResource(R.string.leakgallery_register_too_many)
    val mExists = stringResource(R.string.leakgallery_register_exists)
    val mGeneric = stringResource(R.string.leakgallery_error_generic)
    val m2faInvalid = stringResource(R.string.leakgallery_2fa_invalid)
    val mSocial = stringResource(R.string.leakgallery_login_social)

    fun refreshCaptcha() {
        captcha = ""
        captchaKey++
    }

    fun fail(r: LeakGalleryLoginResult.Failure, second: Boolean = false) {
        error = when (r.kind) {
            LeakGalleryLoginResult.Kind.INVALID -> if (second) m2faInvalid else mInvalid
            LeakGalleryLoginResult.Kind.SOCIAL_ACCOUNT -> mSocial.format(r.message ?: "Google")
            LeakGalleryLoginResult.Kind.CAPTCHA -> mCaptcha
            LeakGalleryLoginResult.Kind.UNAVAILABLE -> mUnavailable
            LeakGalleryLoginResult.Kind.BANNED -> mBanned
            LeakGalleryLoginResult.Kind.TOO_MANY -> mTooMany
            LeakGalleryLoginResult.Kind.EXISTS -> r.message ?: mExists
            LeakGalleryLoginResult.Kind.OTHER -> r.message ?: mGeneric
        }
        if (!second) refreshCaptcha()
    }

    fun handle(r: LeakGalleryLoginResult, second: Boolean = false) {
        when (r) {
            is LeakGalleryLoginResult.Success -> {
                scope.launch {
                    LeakGalleryRepo.me()
                    LeakGalleryRepo.premium()
                    GlobalToasts.show(
                        if (register) registered else success,
                        level = GlobalToasts.ToastLevel.SUCCESS,
                    )
                    onLoggedIn()
                }
            }
            is LeakGalleryLoginResult.TwoFactor -> {
                tempToken = r.tempToken
                code = ""
                error = null
            }
            is LeakGalleryLoginResult.Failure -> fail(r, second)
        }
    }

    fun validate(): String? = when {
        username.trim().length < 4 && register -> vUser
        username.isBlank() -> vUser
        register && !email.contains('@') -> vEmail
        password.length < 7 && register -> vPass
        password.isBlank() -> vPass
        register && password != confirm -> vMatch
        captcha.isBlank() -> vCaptcha
        else -> null
    }

    fun submit() {
        val problem = validate()
        if (problem != null) {
            error = problem
            return
        }
        error = null
        busy = true
        scope.launch {
            val r = if (register) {
                LeakGalleryRepo.register(username.trim(), email.trim(), password, captcha)
            } else {
                LeakGalleryRepo.login(username.trim(), password, captcha)
            }
            handle(r)
            busy = false
        }
    }

    fun submitSecondFactor() {
        val t = tempToken ?: return
        if (code.isBlank()) return
        error = null
        busy = true
        scope.launch {
            val r = if (useRecovery) {
                LeakGalleryRepo.loginBackupCode(t, code.trim())
            } else {
                LeakGalleryRepo.login2fa(t, code.trim())
            }
            handle(r, second = true)
            busy = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (register) R.string.leakgallery_sign_up else R.string.leakgallery_sign_in,
                        ),
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tempToken != null) {
                Text(
                    stringResource(R.string.leakgallery_2fa_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(
                        if (useRecovery) R.string.leakgallery_2fa_recovery_hint else R.string.leakgallery_2fa_hint,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = {
                        Text(
                            stringResource(
                                if (useRecovery) R.string.leakgallery_recovery_code else R.string.leakgallery_2fa_code,
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (useRecovery) KeyboardType.Text else KeyboardType.Number,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { ErrorText(it) }
                Button(
                    onClick = { submitSecondFactor() },
                    enabled = !busy && code.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (busy) Spinner() else Text(stringResource(R.string.leakgallery_verify))
                }
                TextButton(onClick = {
                    useRecovery = !useRecovery
                    code = ""
                    error = null
                }) {
                    Text(
                        stringResource(
                            if (useRecovery) R.string.leakgallery_2fa_use_code else R.string.leakgallery_2fa_use_recovery,
                        ),
                    )
                }
                TextButton(onClick = {
                    tempToken = null
                    error = null
                    refreshCaptcha()
                }) { Text(stringResource(R.string.leakgallery_back_to_login)) }
                return@Column
            }

            Text(
                stringResource(
                    if (register) R.string.leakgallery_register_hint else R.string.leakgallery_signin_hint,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = {
                    Text(
                        stringResource(
                            if (register) R.string.leakgallery_username else R.string.leakgallery_username_or_email,
                        ),
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (register) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.leakgallery_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.leakgallery_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (register) {
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(stringResource(R.string.leakgallery_confirm_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                TextButton(onClick = { showForgot = true }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.leakgallery_forgot_password))
                }
            }

            LeakGalleryCaptcha(
                onToken = { captcha = it },
                onError = {
                    captcha = ""
                    error = mCaptcha
                },
                resetKey = captchaKey,
                dark = dark,
            )

            error?.let { ErrorText(it) }

            Button(
                onClick = { submit() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) Spinner() else Text(
                    stringResource(if (register) R.string.leakgallery_sign_up else R.string.leakgallery_sign_in),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(
                        if (register) R.string.leakgallery_have_account else R.string.leakgallery_no_account,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = {
                    register = !register
                    error = null
                    refreshCaptcha()
                }) {
                    Text(
                        stringResource(
                            if (register) R.string.leakgallery_sign_in else R.string.leakgallery_sign_up,
                        ),
                    )
                }
            }
        }
    }

    if (showForgot) {
        ForgotPasswordDialog(
            initialEmail = if (username.contains('@')) username else email,
            onDismiss = { showForgot = false },
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun Spinner() {
    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
}

@Composable
private fun ForgotPasswordDialog(initialEmail: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(initialEmail) }
    var busy by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.leakgallery_forgot_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sent) {
                    Text(stringResource(R.string.leakgallery_forgot_sent))
                } else {
                    Text(stringResource(R.string.leakgallery_forgot_hint))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; invalid = false },
                        label = { Text(stringResource(R.string.leakgallery_email)) },
                        singleLine = true,
                        isError = invalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (invalid) ErrorText(stringResource(R.string.leakgallery_validate_email))
                }
            }
        },
        confirmButton = {
            if (!sent) {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        if (!email.contains('@')) {
                            invalid = true
                            return@TextButton
                        }
                        busy = true
                        scope.launch {
                            LeakGalleryRepo.forgotPassword(email.trim())
                            busy = false
                            sent = true
                        }
                    },
                ) { Text(stringResource(R.string.leakgallery_send_reset)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    )
}
