package com.yenaly.han1meviewer.ui.component.player

import android.app.Activity
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Rational
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.bridge.VideoPageHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yenaly.han1meviewer.ui.component.GlobalDialogs
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import kotlin.math.roundToInt

val LocalPlayerCompact = compositionLocalOf { false }

object PipSupport {

    var inPip by mutableStateOf(false)
        private set

    fun updateInPip(value: Boolean) {
        if (inPip != value) inPip = value
    }

    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun isPermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    Process.myUid(),
                    context.packageName,
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openPermissionSettings(context: Context) {
        val uri = Uri.parse("package:${context.packageName}")
        val intents = listOf(
            Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", uri),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri),
        )
        for (intent in intents) {
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }

    fun buildParams(aspect: Float, autoEnter: Boolean): PictureInPictureParams {
        val ratio = aspect.coerceIn(0.42f, 2.38f)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 10_000).roundToInt(), 10_000))
            .setActions(emptyList())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
        }
        return builder.build()
    }

    fun enter(activity: Activity, aspect: Float): Boolean = try {
        activity.enterPictureInPictureMode(buildParams(aspect, autoEnter = false))
    } catch (_: Exception) {
        false
    }

    fun verifyPermission(context: Context) {
        if (!SitePlayerSettings.isAutoPipEnabledAnywhere()) return
        if (!isSupported(context)) {
            SitePlayerSettings.disableAutoPipEverywhere()
            return
        }
        if (isPermissionGranted(context)) return
        SitePlayerSettings.disableAutoPipEverywhere()
        GlobalDialogs.show(
            GlobalDialogs.ConfirmRequest(
                title = context.getString(R.string.pip_permission_revoked_title),
                message = context.getString(R.string.pip_permission_revoked_message),
                confirmText = context.getString(R.string.confirm),
                onConfirm = {},
            )
        )
    }
}

private fun Context.findMainActivity(): MainActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is MainActivity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun SitePipEffect(
    site: SiteType,
    isPlaying: Boolean,
    aspectRatio: Float = 16f / 9f,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findMainActivity() }
    val config = SitePlayerSettings.config(site)
    val enabled = config.autoPip && PipSupport.isSupported(context)

    val currentEnabled by rememberUpdatedState(enabled)
    val currentPlaying by rememberUpdatedState(isPlaying)
    val currentAspect by rememberUpdatedState(aspectRatio)

    val host = remember(activity) {
        object : VideoPageHost {
            override fun showCommentBadge(count: Int) = Unit

            override fun shouldEnterPip(): Boolean =
                currentEnabled && currentPlaying && PipSupport.isPermissionGranted(context)

            override fun enterPipMode() {
                if (activity != null) PipSupport.enter(activity, currentAspect)
            }

            override fun onPipModeChanged(isInPip: Boolean) = Unit

            override fun togglePlayPause() = Unit
        }
    }

    DisposableEffect(activity, host) {
        activity?.registerCurrentVideoHost(host)
        onDispose {
            if (activity != null && activity.unregisterCurrentVideoHost(host)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching {
                        activity.setPictureInPictureParams(
                            PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
                        )
                    }
                }
            }
        }
    }

    val autoEnter = enabled && isPlaying
    LaunchedEffect(activity, autoEnter, aspectRatio) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            activity != null &&
            !activity.isInPictureInPictureMode
        ) {
            runCatching {
                activity.setPictureInPictureParams(
                    PipSupport.buildParams(
                        aspectRatio,
                        autoEnter = autoEnter && PipSupport.isPermissionGranted(activity),
                    )
                )
            }
        }
    }
}

@Stable
class PinchZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    private var width = 0f
    private var height = 0f

    fun setSize(w: Float, h: Float) {
        width = w
        height = h
        clampOffsets()
    }

    fun onTransform(zoomChange: Float, pan: Offset, centroid: Offset) {
        val oldScale = scale
        val newScale = (oldScale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        val appliedZoom = if (oldScale > 0f) newScale / oldScale else 1f
        val centerX = width / 2f
        val centerY = height / 2f
        scale = newScale
        offsetX = appliedZoom * offsetX + (centroid.x - centerX) * (1f - appliedZoom) + pan.x
        offsetY = appliedZoom * offsetY + (centroid.y - centerY) * (1f - appliedZoom) + pan.y
        clampOffsets()
    }

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    private fun clampOffsets() {
        if (scale <= 1f) {
            offsetX = 0f
            offsetY = 0f
            return
        }
        val maxX = (scale - 1f) * width / 2f
        val maxY = (scale - 1f) * height / 2f
        offsetX = offsetX.coerceIn(-maxX, maxX)
        offsetY = offsetY.coerceIn(-maxY, maxY)
    }

    private companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 4f
    }
}

fun Modifier.pinchZoomGestures(state: PinchZoomState, enabled: Boolean): Modifier {
    if (!enabled) return this
    return this
        .onSizeChanged { state.setSize(it.width.toFloat(), it.height.toFloat()) }
        .pointerInput(state) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.count { it.pressed } >= 2) {
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)
                        if (zoomChange != 1f || panChange != Offset.Zero) {
                            state.onTransform(zoomChange, panChange, centroid)
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
}

fun Modifier.pinchZoomLayer(state: PinchZoomState): Modifier = graphicsLayer {
    scaleX = state.scale
    scaleY = state.scale
    translationX = state.offsetX
    translationY = state.offsetY
}

@Composable
fun rememberPipEnableHandler(setEnabled: (Boolean) -> Unit): (Boolean) -> Unit {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentSetEnabled by rememberUpdatedState(setEnabled)
    var waitingForPermission by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && waitingForPermission) {
                waitingForPermission = false
                if (PipSupport.isPermissionGranted(context)) {
                    currentSetEnabled(true)
                } else {
                    currentSetEnabled(false)
                    GlobalToasts.show(
                        context.getString(R.string.request_pip_alert),
                        level = GlobalToasts.ToastLevel.WARNING,
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return { enable ->
        when {
            !enable -> currentSetEnabled(false)
            !PipSupport.isSupported(context) -> currentSetEnabled(false)
            PipSupport.isPermissionGranted(context) -> currentSetEnabled(true)
            else -> {
                currentSetEnabled(false)
                GlobalToasts.show(
                    context.getString(R.string.pip_permission_needed),
                    level = GlobalToasts.ToastLevel.WARNING,
                )
                waitingForPermission = true
                PipSupport.openPermissionSettings(context)
            }
        }
    }
}
