package com.yenaly.han1meviewer.SitesHub.Wumaobi.cloudflare

import android.content.Context
import com.yenaly.han1meviewer.SitesHub.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareChallengeInterceptor

class WumaobiCloudflareInterceptor(
    context: Context,
) : CloudflareChallengeInterceptor(
    context = context,
    tag = "WMBCloudflareInterceptor",
    cookieStore = WumaobiCloudflareCookieManager,
    manager = WumaobiCloudflareManager,
    hostnames = { WumaobiConstants.HOSTNAME },
    baseUrls = { listOf(WumaobiConstants.BASE_URL) },
)
