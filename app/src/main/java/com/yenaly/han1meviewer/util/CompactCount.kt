package com.yenaly.han1meviewer.util

fun Long?.compactCount(): String? {
    val value = this ?: return null
    if (value < 0) return null
    return when {
        value >= 1_000_000_000L -> trimDecimal(value / 1_000_000_000.0) + "B"
        value >= 1_000_000L -> trimDecimal(value / 1_000_000.0) + "M"
        value >= 1_000L -> trimDecimal(value / 1_000.0) + "K"
        else -> value.toString()
    }
}

private fun trimDecimal(d: Double): String {
    val s = "%.1f".format(d)
    return if (s.endsWith(".0")) s.dropLast(2) else s
}
