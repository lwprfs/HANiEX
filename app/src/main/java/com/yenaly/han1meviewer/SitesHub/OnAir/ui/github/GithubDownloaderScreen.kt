package com.yenaly.han1meviewer.SitesHub.OnAir.ui.github

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SitesHub.OnAir.github.GithubDownloaderRepo
import com.yenaly.han1meviewer.SitesHub.OnAir.github.GithubDownloaderStore
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import kotlinx.coroutines.launch

@Composable
fun GithubDownloaderScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { GithubDownloaderStore.ensureLoaded() }

    var repoUrl by remember { mutableStateOf(GithubDownloaderStore.repoUrl) }
    var branch by remember { mutableStateOf(GithubDownloaderStore.branch) }
    var token by remember { mutableStateOf(GithubDownloaderStore.token) }
    var authorName by remember { mutableStateOf(GithubDownloaderStore.authorName) }
    var authorEmail by remember { mutableStateOf(GithubDownloaderStore.authorEmail) }
    var repoReady by remember { mutableStateOf(GithubDownloaderStore.repoReady) }
    var note by remember { mutableStateOf("") }
    var manualLink by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var editingRepo by remember { mutableStateOf(!repoReady) }
    var handEdit by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val queued by GithubDownloaderStore.queuedIds.collectAsState()

    fun saveSettings() {
        GithubDownloaderStore.repoUrl = repoUrl
        GithubDownloaderStore.branch = branch
        GithubDownloaderStore.token = token
        GithubDownloaderStore.authorName = authorName
        GithubDownloaderStore.authorEmail = authorEmail
    }

    fun run(label: String, block: suspend () -> String) {
        if (busy) return
        busy = true
        status = label
        scope.launch {
            status = runCatching { block() }.getOrElse { "Failed: ${it.message ?: it.javaClass.simpleName}" }
            busy = false
        }
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = { Text(stringResource(R.string.onair_github_downloader_title)) },
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Column {
                        Text("DEVELOPER ONLY", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "This tool is for the app developer. Other users: please ignore this page.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Text("Links to download (links_haniex.txt)", style = MaterialTheme.typography.titleMedium)
            Text(
                if (repoReady) "Long-press a video card on Home, Search or Tag to add it here. " +
                        "You can also add, delete or edit links by hand."
                else "Set up the repo below first. Until then, long-pressing a card just copies its link.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = manualLink, onValueChange = { manualLink = it },
                    label = { Text("Add a video link or id") },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = {
                    val id = GithubDownloaderStore.extractId(manualLink)
                    if (id == null) {
                        GlobalToasts.show("Not a valid OnAir video link", level = GlobalToasts.ToastLevel.WARNING)
                    } else {
                        val added = GithubDownloaderStore.addVideo(id)
                        if (!added) {
                            GlobalToasts.show("Already queued or downloaded", level = GlobalToasts.ToastLevel.INFO)
                        }
                        manualLink = ""
                    }
                }) { Text("Add") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${queued.size} link(s)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    if (!handEdit) draft = GithubDownloaderStore.readLinks().joinToString("\n")
                    handEdit = !handEdit
                }) { Text(if (handEdit) "Cancel edit" else "Edit by hand") }
                if (queued.isNotEmpty() && !handEdit) {
                    OutlinedButton(onClick = { GithubDownloaderStore.writeLinks(emptyList()) }) { Text("Clear all") }
                }
            }
            if (handEdit) {
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it },
                    label = { Text("One link per line") },
                    minLines = 4, maxLines = 12, modifier = Modifier.fillMaxWidth(),
                )
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    val dropped = GithubDownloaderStore.setLinksFromText(draft)
                    handEdit = false
                    if (dropped > 0) {
                        GlobalToasts.show("$dropped line(s) ignored (no video id)", level = GlobalToasts.ToastLevel.WARNING)
                    }
                }) { Text("Save links") }
            } else if (queued.isEmpty()) {
                Text("No links yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Card {
                    Column {
                        queued.toList().forEachIndexed { i, id ->
                            if (i > 0) HorizontalDivider()
                            Row(
                                Modifier.fillMaxWidth().padding(start = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(GithubDownloaderStore.linkFor(id), Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                IconButton(onClick = { GithubDownloaderStore.removeVideo(id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider()

            if (editingRepo) {
                Text("Connect your repo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Give the repo link. The app downloads a copy of the repo, then pushes " +
                            "links_haniex.txt and cookies_haniex.txt to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = repoUrl, onValueChange = { repoUrl = it },
                    label = { Text("Repo link (https://github.com/owner/repo)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = branch, onValueChange = { branch = it },
                    label = { Text("Branch (leave empty for the default branch)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token, onValueChange = { token = it },
                    label = { Text("GitHub token (Contents: read & write)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = { Text("Stored only on this device. Needed to push.") },
                )
                OutlinedTextField(
                    value = authorName, onValueChange = { authorName = it },
                    label = { Text("Commit author name (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = authorEmail, onValueChange = { authorEmail = it },
                    label = { Text("Commit author email (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Button(
                    enabled = !busy && GithubDownloaderStore.parseRepo(repoUrl) != null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        saveSettings()
                        run("Downloading repo…") {
                            val n = GithubDownloaderRepo.setUp()
                            repoReady = true
                            editingRepo = false
                            branch = GithubDownloaderStore.branch
                            "Repo downloaded ($n files, branch ${GithubDownloaderStore.branch})"
                        }
                    },
                ) { Text("Download repo into the app") }
            } else {
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Repo connected", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${GithubDownloaderStore.repoUrl}  ·  ${GithubDownloaderStore.branch.ifBlank { "default" }}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !busy, onClick = {
                                run("Refreshing repo…") {
                                    val n = GithubDownloaderRepo.refresh()
                                    "Repo refreshed ($n files)"
                                }
                            }) { Text("Refresh repo") }
                            OutlinedButton(enabled = !busy, onClick = { editingRepo = true }) {
                                Text("Edit settings")
                            }
                        }
                    }
                }

                HorizontalDivider()
                Text("Push to start the workflow", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Each push first adds already-downloaded ids to downloaded_id.txt, then sends the " +
                            "links and always replaces cookies_haniex.txt with the live cookies from the Cookies section. " +
                            "The link list is emptied after a successful push.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Commit note (optional)") },
                    supportingText = { Text("The commit message always starts with ${GithubDownloaderStore.COMMIT_TAG}") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = !busy && queued.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        saveSettings()
                        run("Pushing…") {
                            val r = GithubDownloaderRepo.push(note)
                            "Pushed ${r.links} link(s), ${r.cookies} live cookie(s)" +
                                    (if (r.newDoneIds > 0) ", +${r.newDoneIds} downloaded id(s)" else "") +
                                    ".\nWorkflow triggered by ${GithubDownloaderStore.COMMIT_TAG}. Link list cleared.\n${r.commitUrl}"
                        }
                    },
                ) { Text("Push ${GithubDownloaderStore.COMMIT_TAG} & start download") }
            }

            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(status.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                }
            } else if (!status.isNullOrBlank()) {
                Text(status.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
