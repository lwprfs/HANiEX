package com.yenaly.han1meviewer.Buondua.ui.album

import com.yenaly.han1meviewer.ui.component.pinchZoomGestures
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.Buondua.common.BuonduaDownloads
import com.yenaly.han1meviewer.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaPhoto
import com.yenaly.han1meviewer.FapFolder.ui.components.rememberFapFolderImageGate
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.util.SiteImageSaver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BuonduaPhotoLightbox(
    photos: List<BuonduaPhoto>,
    startIndex: Int,
    title: String = "",
    onNearEnd: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    if (photos.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageGate = rememberFapFolderImageGate()
    val loader = BuonduaImageLoader.get(context)
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, photos.lastIndex)) { photos.size }
    var controls by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.fap_photo_saved)
    val alreadyMsg = stringResource(R.string.fap_photo_already_saved)

    BackHandler(onBack = onDismiss)
    LaunchedEffect(pager.currentPage, photos.size) {
        if (pager.currentPage >= photos.size - 3) onNearEnd()
    }
    LaunchedEffect(pager.currentPage) { zoomed = false }
    LaunchedEffect(controls) {
        if (controls) {
            delay(3000)
            controls = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = !zoomed,
            ) { page ->
                ZoomablePhoto(
                    url = photos[page].url,
                    isActive = pager.currentPage == page,
                    onZoomChange = { z -> if (pager.currentPage == page) zoomed = z },
                    onTap = { controls = !controls },
                )
            }

            AnimatedVisibility(visible = controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            if (saving) return@IconButton
                            val index = pager.currentPage
                            imageGate {
                                saving = true
                                scope.launch {
                                    val result = BuonduaDownloads.savePhoto(context, title, photos[index].url, index)
                                    saving = false
                                    when (result) {
                                        is SiteImageSaver.SaveResult.Error -> GlobalToasts.show(
                                            result.message, level = GlobalToasts.ToastLevel.ERROR,
                                        )
                                        is SiteImageSaver.SaveResult.Saved -> GlobalToasts.show(
                                            if (result.count > 0) savedMsg else alreadyMsg,
                                            level = GlobalToasts.ToastLevel.SUCCESS,
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(Icons.Default.Download, stringResource(R.string.fap_save_photo), tint = Color.White)
                    }
                    Text(
                        text = "${pager.currentPage + 1} / ${photos.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                            .padding(bottom = 24.dp)
                            .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomablePhoto(
    url: String,
    isActive: Boolean,
    onZoomChange: (Boolean) -> Unit,
    onTap: () -> Unit,
) {
    val loader = BuonduaImageLoader.get(LocalContext.current)
    var scale by remember(url) { mutableFloatStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(scale, isActive) { if (isActive) onZoomChange(scale > 1.01f) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = url,
            contentDescription = null,
            imageLoader = loader,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(url, scale) {
                    detectTapGestures(
                        onTap = { if (scale <= 1.01f) onTap() },
                        onDoubleTap = {
                            if (scale > 1.01f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else scale = 2f
                        },
                    )
                }
                .pinchZoomGestures(
                    key = url,
                    scale = { scale },
                    offset = { offset },
                    maxScale = 4f,
                ) { newScale, newOffset ->
                    scale = newScale
                    offset = newOffset
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
