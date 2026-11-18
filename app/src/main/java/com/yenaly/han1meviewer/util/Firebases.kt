package com.yenaly.han1meviewer.util

import android.app.Activity
import android.util.Log

fun Activity.logScreenViewEvent(screenClassName: String) {
    val screenName = this.javaClass.simpleName + "-" + screenClassName
    Log.d("logScreenViewEvent", "screenName: $screenName")
    HAnalytics.logScreenView(screenName, screenClassName)
}
