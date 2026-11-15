package com.yenaly.han1meviewer.Buondua.cloudflare

import android.content.Context
import com.yenaly.han1meviewer.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareChallengeInterceptor

class BuonduaCloudflareInterceptor(
    context: Context,
) : CloudflareChallengeInterceptor(
    context = context,
    tag = "BDCloudflareInterceptor",
    cookieStore = BuonduaCloudflareCookieManager,
    manager = BuonduaCloudflareManager,
    hostnames = { BuonduaConstants.HOSTNAME },
    baseUrls = { listOf(BuonduaConstants.BASE_URL) },
)
