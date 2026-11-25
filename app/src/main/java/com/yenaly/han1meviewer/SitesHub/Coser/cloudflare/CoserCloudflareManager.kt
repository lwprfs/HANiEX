package com.yenaly.han1meviewer.SitesHub.Coser.cloudflare

import com.yenaly.han1meviewer.SitesHub.Coser.common.CoserConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareVerificationManager

object CoserCloudflareManager : CloudflareVerificationManager(
    tag = "CoserCFManager",
    cookieStore = CoserCloudflareCookieManager,
    activityClass = CoserCloudflareActivity::class.java,
    baseUrl = { CoserConstants.BASE_URL },
)
