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

/**
 * 进程内 Activity 管理，用于重启应用以及获取当前 Activity。
 */
object ActivityManager {

    /** 当前处于前台的 Activity，由 [com.yenaly.han1meviewer.base.BaseApplication] 维护。 */
    var currentActivity: WeakReference<Activity?> = WeakReference(null)
        internal set

    /**
     * 重启应用。
     *
     * @param killProcess 是否在拉起新实例后结束当前进程
     */
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

/**
 * 获取当前 Context 所在的 [ComponentActivity]，必要时回退到 [ActivityManager.currentActivity]。
 */
private fun Context.requireComponentActivity(): ComponentActivity {
    return (this as? ComponentActivity ?: findActivityOrNull<ComponentActivity>())
        ?: ActivityManager.currentActivity.get() as? ComponentActivity
        ?: error("No ComponentActivity found")
}

private val requestCode = AtomicInteger()

/**
 * 以挂起方式等待 ActivityResult 返回。
 *
 * 注意：内部通过 [ActivityManager.currentActivity] 回退获取宿主 Activity，
 * 因此调用时请确保当前有可用的 Activity。
 */
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
            // 协程被取消时（如页面销毁、作用域结束）清理 launcher 与生命周期观察者，
            // 避免泄漏与向已取消的 continuation resume
            cont.invokeOnCancellation {
                launcher.unregister()
                lifecycle.removeObserver(observer)
            }
        }
    }
}

/**
 * 请求指定权限，已授权时直接返回 true。
 */
suspend fun Context.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(
            this, key
        ) == PackageManager.PERMISSION_GRANTED
    ) return true
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), key)
}
