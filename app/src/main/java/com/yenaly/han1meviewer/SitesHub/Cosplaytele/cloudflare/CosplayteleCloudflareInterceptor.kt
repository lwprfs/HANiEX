package com.yenaly.han1meviewer.SitesHub.Cosplaytele.cloudflare

import android.content.Context
import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareChallengeInterceptor

class CosplayteleCloudflareInterceptor(
    context: Context,
) : CloudflareChallengeInterceptor(
    context = context,
    tag = "CTCloudflareInterceptor",
    cookieStore = CosplayteleCloudflareCookieManager,
    manager = CosplayteleCloudflareManager,
    hostnames = { CosplayteleConstants.HOSTNAME },
    baseUrls = { listOf(CosplayteleConstants.BASE_URL) },
)
