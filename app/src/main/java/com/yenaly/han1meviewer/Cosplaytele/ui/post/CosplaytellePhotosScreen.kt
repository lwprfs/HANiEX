package com.yenaly.han1meviewer.Cosplaytele.ui.post

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleImageLoader
import com.yenaly.han1meviewer.Cosplaytele.common.CosplayteleSettings
import com.yenaly.han1meviewer.Cosplaytele.data.remote.CosplayteleRepo
import com.yenaly.han1meviewer.Cosplaytele.ui.list.COSPLAYTELE_CARD_RATIO
import com.yenaly.han1meviewer.FapFolder.ui.components.rememberFapFolderImageGate
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.ui.component.SiteBackButton
import com.yenaly.han1meviewer.ui.component.SiteErrorContent
import com.yenaly.han1meviewer.ui.component.SiteLoadingContent
import com.yenaly.han1meviewer.ui.component.SiteTopAppBar
import com.yenaly.han1meviewer.ui.screen.RetryableImage
import com.yenaly.han1meviewer.util.SiteImageSaver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CosplaytellePhotosViewModel : ViewModel() {
    private val _state = MutableStateFlow<CosplayteleDetailState>(CosplayteleDetailState.Loading)
    val state: StateFlow<CosplayteleDetailState> = _state.asStateFlow()

    private var slug = ""
    private var job: Job? = null

    fun load(slug: String, force: Boolean = false) {
        if (!force && this.slug == slug && _state.value is CosplayteleDetailState.Success) return
        this.slug = slug
        job?.cancel()
        _state.value = CosplayteleDetailState.Loading
        job = viewModelScope.launch {
            val result = if (force) CosplayteleRepo.detail(slug) else CosplayteleRepo.detailCached(slug)
            result
                .onSuccess { _state.value = CosplayteleDetailState.Success(it) }
                .onFailure { _state.value = CosplayteleDetailState.Error(it.message ?: it.javaClass.simpleName) }
        }
    }
}

@Composable
fun CosplaytellePhotosScreen(
    slug: String,
    onBack: () -> Unit,
    viewModel: CosplaytellePhotosViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageGate = rememberFapFolderImageGate()
    val imageLoader = CosplayteleImageLoader.get(context)
    val state by viewModel.state.collectAsStateWithLifecycle()
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var savingAll by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.fap_photo_saved)

    LaunchedEffect(slug) { viewModel.load(slug) }

    val detail = (state as? CosplayteleDetailState.Success)?.detail

    fun saveAll() {
        val info = detail ?: return
        if (savingAll || info.photos.isEmpty()) return
        imageGate {
            savingAll = true
            scope.launch {
                val result = SiteImageSaver.saveBatch(
                    context = context,
                    urls = info.photos,
                    segments = listOf("Cosplaytele", cosplayteleSafeName(info.title)),
                    baseName = "photo",
                    client = CosplayteleImageLoader.downloadClient(),
                    headers = CosplayteleImageLoader.downloadHeaders(info.pageUrl),
                    onProgress = null,
                )
                savingAll = false
                when (result) {
                    is SiteImageSaver.SaveResult.Error ->
                        GlobalToasts.show(result.message, level = GlobalToasts.ToastLevel.ERROR)
                    is SiteImageSaver.SaveResult.Saved -> GlobalToasts.show(
                        "$savedMsg (${result.count})", level = GlobalToasts.ToastLevel.SUCCESS,
                    )
                }
            }
        }
    }

    viewerIndex?.let { start ->
        if (detail != null) {
            CosplaytelePhotoViewer(
                photos = detail.photos,
                initialIndex = start,
                title = detail.title,
                pageUrl = detail.pageUrl,
                onDismiss = { viewerIndex = null },
            )
        }
    }

    Scaffold(
        topBar = {
            SiteTopAppBar(
                title = {
                    Text(
                        text = if (detail != null) stringResource(R.string.cosplaytele_section_photos, detail.photos.size)
                        else stringResource(R.string.cosplaytele_section_photos, 0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { SiteBackButton(onClick = onBack) },
                actions = {
                    IconButton(enabled = detail != null && !savingAll, onClick = { saveAll() }) {
                        if (savingAll) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Download, stringResource(R.string.xhamster_download_all))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is CosplayteleDetailState.Loading -> SiteLoadingContent()
                is CosplayteleDetailState.Error -> SiteErrorContent(
                    message = s.message,
                    onRetry = { viewModel.load(slug, force = true) },
                )
                is CosplayteleDetailState.Success -> {
                    val columns = remember { CosplayteleSettings.photoColumns }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 8.dp, top = 8.dp, end = 8.dp,
                            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        ),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        itemsIndexed(s.detail.photos, key = { index, _ -> "photo_$index" }) { index, url ->
                            RetryableImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(COSPLAYTELE_CARD_RATIO)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewerIndex = index },
                                placeholder = painterResource(R.drawable.h_chan_loading),
                                error = painterResource(R.drawable.h_chan_load_failed),
                                contentScale = ContentScale.Crop,
                                imageLoader = imageLoader,
                            )
                        }
                    }
                }
            }
        }
    }
}
