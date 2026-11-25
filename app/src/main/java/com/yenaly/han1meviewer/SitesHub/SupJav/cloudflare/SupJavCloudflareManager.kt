package com.yenaly.han1meviewer.SitesHub.SupJav.cloudflare

import com.yenaly.han1meviewer.SitesHub.SupJav.common.SupJavConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareVerificationManager

object SupJavCloudflareManager : CloudflareVerificationManager(
    tag = "SJCloudflareManager",
    cookieStore = SupJavCloudflareCookieManager,
    activityClass = SupJavCloudflareActivity::class.java,
    baseUrl = { SupJavConstants.BASE_URL },
)
