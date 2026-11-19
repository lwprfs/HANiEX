package com.yenaly.han1meviewer.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Helpers that keep user-identifying data (watch/download history, search queries,
 * full URLs) out of logcat while still letting log lines be correlated within one run.
 */
private val logSalt: ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

/** Short, per-process salted hash. Same value -> same tag in one run; not reversible. */
fun Any?.logId(): String {
    val s = this?.toString() ?: return "null"
    val md = MessageDigest.getInstance("SHA-256")
    md.update(logSalt)
    val digest = md.digest(s.toByteArray())
    return "#" + digest.take(3).joinToString("") { "%02x".format(it) }
}

/** Host only; strips path, query and any tokens. */
fun Any?.logHost(): String {
    val s = this?.toString() ?: return "null"
    return s.toHttpUrlOrNull()?.host ?: "<redacted>"
}
