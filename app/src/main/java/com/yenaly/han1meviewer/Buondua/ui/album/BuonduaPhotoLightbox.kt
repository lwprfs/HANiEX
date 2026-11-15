package com.yenaly.han1meviewer.Buondua.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.Buondua.common.BuonduaImageLoader
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaPhoto

@Composable
fun BuonduaPhotoLightbox(
    photos: List<BuonduaPhoto>,
    startIndex: Int,
    onDismiss: () -> Unit,
) {
    val loader = BuonduaImageLoader.get(LocalContext.current)
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, photos.lastIndex)) { photos.size }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = photos[page].url,
                    contentDescription = null,
                    imageLoader = loader,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = "${pager.currentPage + 1} / ${photos.size}",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
    }
}
