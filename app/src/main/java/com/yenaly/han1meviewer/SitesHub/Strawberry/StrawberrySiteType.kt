package com.yenaly.han1meviewer.SitesHub.Strawberry

import com.yenaly.han1meviewer.SitesHub.Strawberry.common.StrawberryConstants

object StrawberrySiteType {

    const val HOSTNAME = "strawberry"

    fun isStrawberrySite(siteTypeName: String?): Boolean =
        siteTypeName.equals("STRAWBERRY", ignoreCase = true)

    fun displayUrl(): String = StrawberryConstants.STRAWBERRY_URL[0]

    fun defaultHostname(): String = StrawberryConstants.STRAWBERRY_HOSTNAME[0]
}
