package com.yenaly.han1meviewer.XHamster.ui.entities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterAccountRepo
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterAccountRepo.Choice
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterAuth
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ChoiceField(
    label: String,
    choices: List<Choice>,
    value: String?,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
) {
    var open by remember { mutableStateOf(false) }
    val shown = choices.firstOrNull { it.value == value }?.label ?: value.orEmpty()
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = shown,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    enabled = enabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.label) },
                    onClick = { open = false; onSelect(c.value) },
                )
            }
        }
    }
}

@Composable
fun XHamsterAccountEditor(
    onBack: () -> Unit,
    onOpenInBrowser: (path: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var reload by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf<XHamsterAccountRepo.ProfileForm?>(null) }
    var privacy by remember { mutableStateOf<XHamsterAccountRepo.PrivacyForm?>(null) }

    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var orientation by remember { mutableStateOf<String?>(null) }
    var relation by remember { mutableStateOf<String?>(null) }
    val privacyValues = remember { mutableStateMapOf<String, String>() }

    var savingProfile by remember { mutableStateOf(false) }
    var savingPrivacy by remember { mutableStateOf(false) }
    var savingPassword by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val loginRequired = stringResource(R.string.xhamster_login_required)
    val savedText = stringResource(R.string.xh_acc_saved)
    val failedFmt = stringResource(R.string.xh_acc_save_failed, "%s")
    val passwordMismatch = stringResource(R.string.xh_acc_password_mismatch)
    val passwordShort = stringResource(R.string.xh_acc_password_short)
    val passwordDone = stringResource(R.string.xh_acc_password_done)

    fun failure(t: Throwable) {
        scope.launch { snackbar.showSnackbar(failedFmt.replace("%s", t.message ?: t.javaClass.simpleName)) }
    }

    LaunchedEffect(reload) {
        loading = true
        error = null
        if (!XHamsterAuth.isLoggedIn) {
            error = loginRequired
        } else {
            XHamsterAccountRepo.loadProfile().onSuccess { p ->
                profile = p
                username = p.username; fullName = p.fullName; city = p.city
                occupation = p.occupation; gender = p.gender
                orientation = p.orientation; relation = p.relation
            }.onFailure { error = it.message ?: it.javaClass.simpleName }
            XHamsterAccountRepo.loadPrivacy().onSuccess { f ->
                privacy = f
                privacyValues.clear()
                f.fields.forEach { privacyValues[it.key] = it.value }
            }.onFailure { if (profile == null) error = it.message ?: it.javaClass.simpleName }
        }
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SiteTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.xhamster_account_settings),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            val p = profile
            when {
                loading && p == null && privacy == null -> SiteLoadingContent()
                p == null && privacy == null ->
                    SiteErrorContent(message = error, onRetry = { reload++ })

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (p != null) {
                        SectionCard(stringResource(R.string.xh_acc_profile_edit)) {
                            p.email?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedTextField(
                                value = username, onValueChange = { username = it },
                                label = { Text(stringResource(R.string.xh_acc_username)) },
                                enabled = !p.usernameLocked, singleLine = true,
                                supportingText = if (p.usernameLocked) {
                                    { Text(stringResource(R.string.xh_acc_username_locked)) }
                                } else null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = fullName, onValueChange = { fullName = it },
                                label = { Text(stringResource(R.string.xh_acc_full_name)) },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = city, onValueChange = { city = it },
                                label = { Text(stringResource(R.string.xh_acc_city)) },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = occupation, onValueChange = { occupation = it },
                                label = { Text(stringResource(R.string.xh_acc_occupation)) },
                                singleLine = true, modifier = Modifier.fillMaxWidth(),
                            )
                            if (p.genderChoices.isNotEmpty()) ChoiceField(
                                stringResource(R.string.xh_acc_gender), p.genderChoices, gender,
                                { gender = it },
                            )
                            if (p.orientationChoices.isNotEmpty()) ChoiceField(
                                stringResource(R.string.xh_acc_orientation), p.orientationChoices,
                                orientation, { orientation = it },
                            )
                            ChoiceField(
                                stringResource(R.string.xh_acc_relationship), p.relationChoices,
                                relation, { relation = it },
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        savingProfile = true
                                        XHamsterAccountRepo.saveProfile(
                                            p, username.trim(), fullName.trim(), city.trim(),
                                            occupation.trim(), gender, orientation, relation,
                                        ).onSuccess { snackbar.showSnackbar(savedText); reload++ }
                                            .onFailure(::failure)
                                        savingProfile = false
                                    }
                                },
                                enabled = !savingProfile && username.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (savingProfile) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text(stringResource(R.string.xh_acc_save))
                            }
                        }
                    }

                    privacy?.let { form ->
                        if (form.fields.isNotEmpty()) SectionCard(stringResource(R.string.xh_acc_privacy_title)) {
                            form.fields.forEach { f ->
                                ChoiceField(f.title, f.choices, privacyValues[f.key] ?: f.value,
                                    { privacyValues[f.key] = it })
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        savingPrivacy = true
                                        XHamsterAccountRepo.savePrivacy(form, privacyValues.toMap())
                                            .onSuccess { snackbar.showSnackbar(savedText) }
                                            .onFailure(::failure)
                                        savingPrivacy = false
                                    }
                                },
                                enabled = !savingPrivacy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (savingPrivacy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text(stringResource(R.string.xh_acc_save))
                            }
                        }
                    }

                    SectionCard(stringResource(R.string.xh_acc_password_title)) {
                        OutlinedTextField(
                            value = currentPassword, onValueChange = { currentPassword = it },
                            label = { Text(stringResource(R.string.xh_acc_current_password)) },
                            singleLine = true, visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = newPassword, onValueChange = { newPassword = it },
                            label = { Text(stringResource(R.string.xh_acc_new_password)) },
                            singleLine = true, visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = confirmPassword, onValueChange = { confirmPassword = it },
                            label = { Text(stringResource(R.string.xh_acc_confirm_password)) },
                            singleLine = true, visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                when {
                                    newPassword.length < 6 ->
                                        scope.launch { snackbar.showSnackbar(passwordShort) }

                                    newPassword != confirmPassword ->
                                        scope.launch { snackbar.showSnackbar(passwordMismatch) }

                                    else -> scope.launch {
                                        savingPassword = true
                                        XHamsterAccountRepo.changePassword(currentPassword, newPassword)
                                            .onSuccess {
                                                currentPassword = ""; newPassword = ""; confirmPassword = ""
                                                snackbar.showSnackbar(passwordDone)
                                            }.onFailure(::failure)
                                        savingPassword = false
                                    }
                                }
                            },
                            enabled = !savingPassword && currentPassword.isNotEmpty() && newPassword.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (savingPassword) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text(stringResource(R.string.xh_acc_save))
                        }
                    }

                    OutlinedButton(
                        onClick = { onOpenInBrowser("/my/edit/profile") },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.xh_acc_open_web)) }
                }
            }
        }
    }
}
