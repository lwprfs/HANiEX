package com.yenaly.han1meviewer.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Extracts a frame from a local video file so cards never show an empty box
 * when the remote/downloaded thumbnail is missing or fails to load.
 */
object LocalVideoThumbnail {

    private const val MAX_EDGE = 480

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /** Videos we already failed on, so we don't retry on every recomposition. */
    private val failed = java.util.Collections.synchronizedSet(HashSet<String>())

    fun peek(videoUri: String): Bitmap? = cache.get(videoUri)

    suspend fun load(context: Context, videoUri: String): Bitmap? {
        cache.get(videoUri)?.let { return it }
        if (videoUri in failed) return null
        val bmp = withContext(Dispatchers.IO) { extract(context.applicationContext, videoUri) }
        if (bmp != null) cache.put(videoUri, bmp) else failed += videoUri
        return bmp
    }

    private fun extract(context: Context, videoUri: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(videoUri)
            when (uri.scheme) {
                "content", "file" -> retriever.setDataSource(context, uri)
                else -> retriever.setDataSource(File(videoUri).absolutePath)
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            // A frame slightly into the video avoids black intro frames.
            val timeUs = (durationMs * 1000L / 10L).coerceAtLeast(0L)
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return null
            val longest = maxOf(frame.width, frame.height)
            if (longest <= MAX_EDGE) frame else {
                val scale = MAX_EDGE.toFloat() / longest
                Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * scale).toInt().coerceAtLeast(1),
                    (frame.height * scale).toInt().coerceAtLeast(1),
                    true,
                ).also { if (it !== frame) frame.recycle() }
            }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}

/**
 * Shows [model] as the thumbnail. If [model] is null/blank or fails to load,
 * falls back to a frame taken from the local video at [videoUri].
 */
@Composable
fun ThumbnailWithVideoFallback(
    model: Any?,
    videoUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: androidx.compose.ui.graphics.painter.Painter? = null,
    error: androidx.compose.ui.graphics.painter.Painter? = null,
) {
    val context = LocalContext.current
    val hasModel = model != null && (model as? String)?.isNotBlank() != false
    var remoteFailed by remember(model) { mutableStateOf(false) }
    val needFallback = !videoUri.isNullOrBlank() && (!hasModel || remoteFailed)

    var frame by remember(videoUri) { mutableStateOf(videoUri?.let { LocalVideoThumbnail.peek(it) }) }
    LaunchedEffect(videoUri, needFallback) {
        if (needFallback && frame == null) {
            frame = LocalVideoThumbnail.load(context, videoUri!!)
        }
    }

    val bitmap = frame
    when {
        needFallback && bitmap != null -> Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
        hasModel && !remoteFailed -> AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            placeholder = placeholder,
            error = error,
            onError = { remoteFailed = true },
            modifier = modifier,
        )
        // Fallback still loading / unavailable: keep the previous error visuals.
        error != null -> Image(
            painter = error,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}
