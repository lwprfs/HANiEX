package com.yenaly.han1meviewer.SitesHub.Strawberry.data.model

sealed interface StrawberryMirrorStatus {
    val host: String
    val checkedAt: Long

    data class Checking(
        override val host: String,
        override val checkedAt: Long = System.currentTimeMillis(),
    ) : StrawberryMirrorStatus

    data class Online(
        override val host: String,
        val httpCode: Int?,
        val detail: String,
        override val checkedAt: Long = System.currentTimeMillis(),
    ) : StrawberryMirrorStatus

    data class Offline(
        override val host: String,
        val reason: String,
        override val checkedAt: Long = System.currentTimeMillis(),
    ) : StrawberryMirrorStatus

    data class Error(
        override val host: String,
        val message: String,
        override val checkedAt: Long = System.currentTimeMillis(),
    ) : StrawberryMirrorStatus
}
