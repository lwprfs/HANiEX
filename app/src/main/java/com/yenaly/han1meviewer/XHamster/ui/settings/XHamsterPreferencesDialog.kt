package com.yenaly.han1meviewer.XHamster.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.XHamster.common.XHamsterSession
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterPrefsRepo
import com.yenaly.han1meviewer.XHamster.data.remote.XHamsterPrefsRepo.Option
import com.yenaly.han1meviewer.ui.component.SiteBarIconButton
import kotlinx.coroutines.launch

@Composable
fun XHamsterPreferencesButton() {
    var show by remember { mutableStateOf(false) }
    val epoch by XHamsterSession.epoch.collectAsState()
    val geo = remember(epoch, show) { XHamsterSession.geo }
    val orientation = remember(epoch, show) { XHamsterSession.orientation }
    val badgeText = geo.takeIf { it.isNotBlank() }?.uppercase()
        ?: when (orientation) {
            "gay" -> "G"
            "shemale" -> "T"
            else -> "S"
        }
    BadgedBox(badge = { Badge { Text(badgeText) } }) {
        SiteBarIconButton(
            imageVector = Icons.Default.Tune,
            contentDescription = "Site preferences",
            onClick = { show = true },
        )
    }
    if (show) XHamsterPreferencesDialog(onDismiss = { show = false })
}

@Composable
fun XHamsterPreferencesDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var applying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var orientation by remember { mutableStateOf(XHamsterSession.orientation) }
    var geo by remember { mutableStateOf(XHamsterSession.geo) }
    var lang by remember { mutableStateOf(Option(XHamsterSession.lang, XHamsterSession.langLabel)) }
    var languages by remember { mutableStateOf<List<Option>>(emptyList()) }
    var countries by remember { mutableStateOf<List<Option>>(emptyList()) }
    var picker by remember { mutableStateOf<String?>(null) }

    val initialOrientation = remember { XHamsterSession.orientation }
    val initialGeo = remember { XHamsterSession.geo }
    val initialLang = remember { XHamsterSession.lang }

    LaunchedEffect(Unit) {
        XHamsterPrefsRepo.load().onSuccess { p ->
            orientation = p.orientation
            geo = p.geo
            lang = p.lang
            languages = p.languages
        }.onFailure { error = it.message }
        loading = false
        XHamsterPrefsRepo.countries(lang.value).onSuccess { countries = it }
    }

    val countryLabel = countries.firstOrNull { it.value.equals(geo, true) }?.label
        ?: geo.uppercase().ifBlank { "Auto" }

    AlertDialog(
        onDismissRequest = { if (!applying) onDismiss() },
        title = { Text("Site preferences") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  Loading current settings…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Preferred orientation", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    XHamsterPrefsRepo.ORIENTATIONS.forEach { o ->
                        FilterChip(
                            selected = orientation == o.value,
                            onClick = { orientation = o.value },
                            label = { Text(o.label) },
                        )
                    }
                }
                Text("Country", style = MaterialTheme.typography.titleSmall)
                OutlinedButton(
                    onClick = { picker = "country" },
                    enabled = countries.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(countryLabel) }
                Text("Language", style = MaterialTheme.typography.titleSmall)
                OutlinedButton(
                    onClick = { picker = "lang" },
                    enabled = languages.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(lang.label) }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !applying && !loading,
                onClick = {
                    applying = true
                    error = null
                    scope.launch {
                        var changed = false
                        var failure: Throwable? = null
                        if (orientation != initialOrientation) {
                            XHamsterPrefsRepo.setOrientation(orientation)
                                .onSuccess { changed = true }.onFailure { failure = it }
                        }
                        if (failure == null && geo != initialGeo && geo.isNotBlank()) {
                            XHamsterPrefsRepo.setCountry(geo)
                                .onSuccess { changed = true }.onFailure { failure = it }
                        }
                        if (failure == null && lang.value != initialLang) {
                            XHamsterPrefsRepo.setLanguage(lang)
                                .onSuccess { changed = true }.onFailure { failure = it }
                        }
                        applying = false
                        val failed = failure
                        if (failed != null) {
                            error = failed.message ?: "Could not apply settings"
                        } else {
                            if (changed) XHamsterSession.bump()
                            onDismiss()
                        }
                    }
                },
            ) { Text(if (applying) "Applying…" else "Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !applying) { Text("Cancel") }
        },
    )

    when (picker) {
        "country" -> OptionPickerDialog(
            title = "Country",
            options = countries,
            selected = geo,
            onSelect = { geo = it.value; picker = null },
            onDismiss = { picker = null },
        )

        "lang" -> OptionPickerDialog(
            title = "Language",
            options = languages,
            selected = lang.value,
            onSelect = { lang = it; picker = null },
            onDismiss = { picker = null },
        )
    }
}

@Composable
private fun OptionPickerDialog(
    title: String,
    options: List<Option>,
    selected: String,
    onSelect: (Option) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, options) {
        if (query.isBlank()) options else options.filter { it.label.contains(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                    items(filtered, key = { it.value }) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(option.label, modifier = Modifier.weight(1f))
                            if (option.value.equals(selected, true)) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
