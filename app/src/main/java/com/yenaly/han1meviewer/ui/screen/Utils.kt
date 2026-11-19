package com.yenaly.han1meviewer.ui.screen

import com.yenaly.han1meviewer.util.logHost

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import androidx.window.core.layout.WindowSizeClass
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.theme.SpacingLarge
import com.yenaly.han1meviewer.ui.theme.SpacingNormal
import com.yenaly.han1meviewer.ui.theme.VideoNormalCardMinWidth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun RetryableImage(
    model: Any,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    retryLimit: Int = 2,
    placeholder: Painter,
    error: Painter,
    contentScale: ContentScale? = ContentScale.Fit,
    imageLoader: ImageLoader? = null,
) {
    val context = LocalContext.current
    val resolvedLoader = imageLoader ?: SingletonImageLoader.get(context)

    val site = if (LocalInspectionMode.current) null else Preferences.siteType
    val siteOptions = site?.let { SiteImageSettings.options(it) }
    val limit = siteOptions?.retries ?: retryLimit
    val fade = siteOptions?.fade == true
    val isFullImage = contentScale == ContentScale.Fit || contentScale == ContentScale.Inside

    val reduced = siteOptions?.reduced == true && !isFullImage

    var retryCount by remember(model) { mutableIntStateOf(0) }
    val request = remember(model, retryCount, contentScale, fade, reduced) {
        ImageRequest.Builder(context)
            .data(model)
            .crossfade(isFullImage || fade)
            .apply {
                if (reduced) {
                    size(Size(SiteImageSettings.REDUCED_EDGE_PX, SiteImageSettings.REDUCED_EDGE_PX))
                }
            }
            .memoryCacheKey("$model#r$retryCount${if (reduced) "#s" else ""}")
            .diskCacheKey("$model#r$retryCount")
            .listener(
                onError = { _, result ->
                    Log.e("CoilError", "Image load failed for ${model.logHost()}", result.throwable)
                }
            )
            .build()
    }

    AsyncImage(
        model = request,
        imageLoader = resolvedLoader,
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = error,
        modifier = modifier,
        onError = {
            if (retryCount < limit) {
                retryCount++
            }
        },
        contentScale = contentScale ?: ContentScale.Fit,
    )
}

val LocalVideoGridAvailableWidth = compositionLocalOf<Dp?> { null }

private val SplitPaneMinVideoCardWidth = 120.dp

@Composable
fun getColumnCount(
    itemWidth: Int,
    contentPadding: Dp = SpacingNormal,
    itemSpacing: Dp = SpacingNormal,
): Int {
    val availableWidthDp = currentGridAvailableWidthDp()
    val usableWidth = (availableWidthDp - contentPadding * 2).coerceAtLeast(0.dp)
    return maxOf(
        1,
        ((usableWidth + itemSpacing) / (itemWidth.dp + itemSpacing)).toInt(),
    )
}

@Composable
fun rememberCardResponsiveWidth(
    horizontalPadding: Dp = SpacingLarge,
    itemSpacing: Dp = SpacingNormal
): Pair<Dp, Float> {
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current
    val currentWidthDp = with(density) { containerWidth.toDp() }

    val isPreview = LocalInspectionMode.current
    val itemsToShow = if (!isPreview) {
        Preferences.horizontalCardCountConfig.countForWidthDp(currentWidthDp.value.toInt())
    } else {
        val estimatedCardWidth = 160.dp
        maxOf(1f, ((currentWidthDp - (horizontalPadding * 2)) / (estimatedCardWidth + itemSpacing)))
    }

    val safeItemsToShow = maxOf(1f, itemsToShow)
    val cardWidth = (currentWidthDp - (horizontalPadding * 2) - (itemSpacing * (safeItemsToShow - 1))) / safeItemsToShow

    return Pair(cardWidth, safeItemsToShow)
}

@Composable
fun rememberVideoGridColumns(minCardWidth: Dp = VideoNormalCardMinWidth): Int =
    rememberGridColumns(minCardWidth, search = false)

@Composable
fun rememberSearchGridColumns(minCardWidth: Dp = VideoNormalCardMinWidth): Int =
    rememberGridColumns(minCardWidth, search = true)

private val MinUserGridCardWidth = 112.dp

@Composable
private fun rememberGridColumns(minCardWidth: Dp, search: Boolean): Int {
    val windowWidthDp = currentWindowWidthDp()
    val availableWidthDp = LocalVideoGridAvailableWidth.current ?: windowWidthDp
    val isPreview = LocalInspectionMode.current
    val isNarrowedContainer = availableWidthDp < windowWidthDp
    val fittingMinWidth = if (isNarrowedContainer) {
        minOf(minCardWidth, SplitPaneMinVideoCardWidth)
    } else {
        minCardWidth
    }
    val maxFittingColumns = videoGridColumnsForWidth(availableWidthDp, fittingMinWidth)
    val siteColumns = if (isPreview) SiteLayoutSettings.FOLLOW_APP else rememberSiteGridColumns(Preferences.siteType)

    if (isPreview) return maxOf(2, maxFittingColumns)

    if (siteColumns != SiteLayoutSettings.FOLLOW_APP) {

        return siteColumns.coerceAtMost(
            videoGridColumnsForWidth(availableWidthDp, MinUserGridCardWidth)
        ).coerceAtLeast(1)
    }

    val useTabletColumns = windowWidthDp.value >=
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    if (!useTabletColumns) return maxOf(2, maxFittingColumns)

    val config = if (search) Preferences.searchGridColumnsConfig else Preferences.videoGridColumnsConfig
    val preferredColumns = config
        .columnsForWidthDp(availableWidthDp.value.toInt())
        .coerceAtLeast(1)
    val absoluteMaxColumns = videoGridColumnsForWidth(availableWidthDp, MinUserGridCardWidth)
    return if (isNarrowedContainer) {
        minOf(preferredColumns, maxFittingColumns).coerceAtLeast(1)
    } else {
        minOf(preferredColumns, absoluteMaxColumns).coerceAtLeast(1)
    }
}

@Composable
private fun currentWindowWidthDp(): Dp = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width.toDp()
}

@Composable
private fun currentGridAvailableWidthDp(): Dp {
    val windowWidthDp = currentWindowWidthDp()
    return LocalVideoGridAvailableWidth.current ?: windowWidthDp
}

private fun videoGridColumnsForWidth(width: Dp, minCardWidth: Dp): Int {
    val usableWidth = (width - SpacingNormal * 2).coerceAtLeast(0.dp)
    return maxOf(
        1,
        ((usableWidth + SpacingNormal) / (minCardWidth + SpacingNormal)).toInt(),
    )
}

@Composable
fun rememberRandomLoadingHint(): String {
    val placeholders = stringArrayResource(R.array.loading_hints)
    return remember(placeholders) { placeholders.random() }
}

fun CoroutineScope.runAfterUiSettles(block: () -> Unit) {
    launch {
        withFrameNanos { }
        withFrameNanos { }
        block()
    }
}
