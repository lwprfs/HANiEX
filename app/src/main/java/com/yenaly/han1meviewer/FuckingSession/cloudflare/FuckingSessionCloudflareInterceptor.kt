package com.yenaly.han1meviewer.FuckingSession.cloudflare

import android.content.Context
import com.yenaly.han1meviewer.FuckingSession.common.FuckingSessionConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareChallengeInterceptor

class FuckingSessionCloudflareInterceptor(
    context: Context,
) : CloudflareChallengeInterceptor(
    context = context,
    tag = "FSCloudflareInterceptor",
    cookieStore = FuckingSessionCloudflareCookieManager,
    manager = FuckingSessionCloudflareManager,
    hostnames = { FuckingSessionConstants.HOSTNAME },
    baseUrls = { listOf(FuckingSessionConstants.BASE_URL) },
)
