package com.yenaly.han1meviewer.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.security.MessageDigest
import java.security.SecureRandom

private val logSalt: ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

fun Any?.logId(): String {
    val s = this?.toString() ?: return "null"
    val md = MessageDigest.getInstance("SHA-256")
    md.update(logSalt)
    val digest = md.digest(s.toByteArray())
    return "#" + digest.take(3).joinToString("") { "%02x".format(it) }
}

fun Any?.logHost(): String {
    val s = this?.toString() ?: return "null"
    return s.toHttpUrlOrNull()?.host ?: "<redacted>"
}
