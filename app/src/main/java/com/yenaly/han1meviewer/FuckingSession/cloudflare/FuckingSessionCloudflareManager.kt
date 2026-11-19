package com.yenaly.han1meviewer.FuckingSession.cloudflare

import com.yenaly.han1meviewer.FuckingSession.common.FuckingSessionConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareVerificationManager

object FuckingSessionCloudflareManager : CloudflareVerificationManager(
    tag = "FSCloudflareManager",
    cookieStore = FuckingSessionCloudflareCookieManager,
    activityClass = FuckingSessionCloudflareActivity::class.java,
    baseUrl = { FuckingSessionConstants.BASE_URL },
)
