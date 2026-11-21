package com.yenaly.han1meviewer.Coser.cloudflare

import android.content.Context
import com.yenaly.han1meviewer.Coser.common.CoserConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareChallengeInterceptor

class CoserCloudflareInterceptor(
    context: Context,
) : CloudflareChallengeInterceptor(
    context = context,
    tag = "CoserCFInterceptor",
    cookieStore = CoserCloudflareCookieManager,
    manager = CoserCloudflareManager,
    hostnames = { CoserConstants.HOSTNAME },
    baseUrls = { listOf(CoserConstants.BASE_URL) },
)
