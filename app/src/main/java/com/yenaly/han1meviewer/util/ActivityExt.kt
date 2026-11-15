package com.yenaly.han1meviewer.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

object ActivityManager {

    var currentActivity: WeakReference<Activity?> = WeakReference(null)
        internal set

    fun restart(killProcess: Boolean = true) {
        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            applicationContext.startActivity(intent)
        }
        if (killProcess) exitProcess(0)
    }
}

private fun Context.requireComponentActivity(): ComponentActivity {
    return (this as? ComponentActivity ?: findActivityOrNull<ComponentActivity>())
        ?: ActivityManager.currentActivity.get() as? ComponentActivity
        ?: error("No ComponentActivity found")
}

private val requestCode = AtomicInteger()

suspend fun <I, O> Context.awaitActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
): O {
    val key = "activity_rq#${requestCode.getAndIncrement()}"

    val activity = this.requireComponentActivity()
    val lifecycle = activity.lifecycle
    var launcher: ActivityResultLauncher<I>? = null
    val observer: LifecycleEventObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (Lifecycle.Event.ON_DESTROY === event) {
                launcher?.unregister()
                lifecycle.removeObserver(this)
            }
        }
    }

    return withContext(Dispatchers.Main) {
        lifecycle.addObserver(observer)
        suspendCancellableCoroutine { cont: CancellableContinuation<O> ->
            launcher = activity.activityResultRegistry.register(key, contract) {
                if (cont.isActive) {
                    launcher?.unregister()
                    lifecycle.removeObserver(observer)
                    cont.resume(it) { _, _, _ -> }
                }
            }.apply { launch(input) }

            cont.invokeOnCancellation {
                launcher.unregister()
                lifecycle.removeObserver(observer)
            }
        }
    }
}

suspend fun Context.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(
            this, key
        ) == PackageManager.PERMISSION_GRANTED
    ) return true
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), key)
}
