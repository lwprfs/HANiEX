package com.yenaly.han1meviewer.SitesHub.XXXTube.cloudflare

import com.yenaly.han1meviewer.SitesHub.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareVerificationManager

object XXXTubeCloudflareManager : CloudflareVerificationManager(
    tag = "XXXCloudflareManager",
    cookieStore = XXXTubeCloudflareCookieManager,
    activityClass = XXXTubeCloudflareActivity::class.java,
    baseUrl = { XXXTubeConstants.BASE_URL },
)
