package com.yenaly.han1meviewer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.net.toUri
import androidx.core.util.TypedValueCompat

lateinit var applicationContext: Context
    internal set

val application: Application
    get() = applicationContext as Application

val Number.dp: Int
    get() {
        val px = TypedValueCompat.dpToPx(toFloat(), applicationContext.resources.displayMetrics)
        return (if (px >= 0) px + 0.5f else px - 0.5f).toInt()
    }

val appScreenWidth: Int
    get() = applicationContext.resources.displayMetrics.widthPixels

val appScreenHeight: Int
    get() = applicationContext.resources.displayMetrics.heightPixels

@get:SuppressLint("DiscouragedApi", "InternalInsetResource")
val statusBarHeight: Int
    get() {
        val resources = applicationContext.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

@get:SuppressLint("DiscouragedApi", "InternalInsetResource")
val navBarHeight: Int
    get() {
        val resources = applicationContext.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

inline fun <reified T : Activity> Context.findActivityOrNull(): T? {
    var context = this
    while (context is ContextWrapper) {
        if (context is T) {
            return context
        }
        context = context.baseContext
    }
    return null
}

infix fun Context.browse(uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}
