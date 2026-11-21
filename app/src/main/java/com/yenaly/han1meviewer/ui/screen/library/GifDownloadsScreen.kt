package com.yenaly.han1meviewer.ui.screen.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteEmptyContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.FapFolderImageLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class GifNode(
    val docId: String,
    val name: String,
    val isDir: Boolean,
    val uri: Uri,
    val size: Long,
)

private data class GifFolder(val site: String, val creator: String, val docId: String)

private fun listNodes(context: Context, tree: Uri, parentDocId: String): List<GifNode> {
    val out = ArrayList<GifNode>()
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocId)
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
    )
    runCatching {
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                out += GifNode(
                    docId = id,
                    name = c.getString(1).orEmpty(),
                    isDir = c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
                    uri = DocumentsContract.buildDocumentUriUsingTree(tree, id),
                    size = if (c.isNull(3)) 0L else c.getLong(3),
                )
            }
        }
    }
    return out
}

/** `<Site>/Gifs/<creator>` folders found in the image-download tree. */
private fun findGifFolders(context: Context, tree: Uri, rootDocId: String): List<GifFolder> {
    val result = ArrayList<GifFolder>()
    for (site in listNodes(context, tree, rootDocId).filter { it.isDir }) {
        val gifs = listNodes(context, tree, site.docId)
            .firstOrNull { it.isDir && it.name.equals(DownloadLayout.GIFS_FOLDER, ignoreCase = true) } ?: continue
        for (creator in listNodes(context, tree, gifs.docId).filter { it.isDir }) {
            result += GifFolder(site.name, creator.name, creator.docId)
        }
    }
    return result.sortedWith(compareBy({ it.site.lowercase() }, { it.creator.lowercase() }))
}

/** Gifs / shorts saved from the shorts player. Site > creator folder > clips, like the photo downloader. */
@Composable
fun GifDownloadsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<GifFolder?>(null) }
    var folders by remember { mutableStateOf<List<GifFolder>>(emptyList()) }
    var files by remember { mutableStateOf<List<GifNode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null) {
            FapFolderImageLocation.persist(context, tree)
            FapFolderImageLocation.confirmedThisSession = true
            refresh++
        }
    }

    val ready = refresh >= 0 && FapFolderImageLocation.isReady(context)
    val treeUri = remember(refresh) { FapFolderImageLocation.uri?.let { runCatching { it.toUri() }.getOrNull() } }
    val rootDocId = remember(treeUri) {
        treeUri?.let { runCatching { DocumentsContract.getTreeDocumentId(it) }.getOrNull() }
    }

    LaunchedEffect(refresh, selected, treeUri) {
        val tree = treeUri
        val root = rootDocId
        if (!ready || tree == null || root == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        withContext(Dispatchers.IO) {
            val current = selected
            if (current == null) {
                folders = findGifFolders(context, tree, root)
            } else {
                files = listNodes(context, tree, current.docId)
                    .filter { !it.isDir && DownloadLayout.isVideoFileName(it.name) }
                    .sortedBy { it.name.lowercase() }
            }
        }
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SiteTopAppBar(
                title = {
                    Text(
                        selected?.let { "${it.site} / ${it.creator}" }
                            ?: stringResource(R.string.gif_downloads_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { SiteBackButton(onClick = { if (selected != null) selected = null else onBack() }) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !ready -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.gif_downloads_choose_folder), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { picker.launch(null) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.gif_downloads_pick_folder))
                    }
                }
                loading -> SiteLoadingContent()
                selected == null && folders.isEmpty() -> SiteEmptyContent(hint = stringResource(R.string.gif_downloads_empty))
                selected == null -> LazyColumn(Modifier.fillMaxSize()) {
                    items(folders, key = { it.docId }) { folder ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selected = folder }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null)
                            Column {
                                Text(folder.creator, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    folder.site,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                files.isEmpty() -> SiteEmptyContent(hint = stringResource(R.string.gif_downloads_empty))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(files, key = { it.docId }) { file ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(file.uri, "video/*")
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    )
                                }
                            }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Column(Modifier.weight(1f)) {
                                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "%.1f MB".format(file.size / 1024.0 / 1024.0),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = {
                                runCatching { DocumentsContract.deleteDocument(context.contentResolver, file.uri) }
                                refresh++
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }
}
