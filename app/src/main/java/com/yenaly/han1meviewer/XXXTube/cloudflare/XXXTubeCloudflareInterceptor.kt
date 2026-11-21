package com.yenaly.han1meviewer.XXXTube.cloudflare

import android.content.Context
import com.yenaly.han1meviewer.XXXTube.common.XXXTubeConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareChallengeInterceptor

class XXXTubeCloudflareInterceptor(
    context: Context,
) : CloudflareChallengeInterceptor(
    context = context,
    tag = "XXXCloudflareInterceptor",
    cookieStore = XXXTubeCloudflareCookieManager,
    manager = XXXTubeCloudflareManager,
    hostnames = { XXXTubeConstants.HOSTNAME },
    baseUrls = { listOf(XXXTubeConstants.BASE_URL) },
)
