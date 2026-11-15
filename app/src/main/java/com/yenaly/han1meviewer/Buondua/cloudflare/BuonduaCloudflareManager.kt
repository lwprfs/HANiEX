package com.yenaly.han1meviewer.Buondua.cloudflare

import com.yenaly.han1meviewer.Buondua.common.BuonduaConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareVerificationManager

object BuonduaCloudflareManager : CloudflareVerificationManager(
    tag = "BDCloudflareManager",
    cookieStore = BuonduaCloudflareCookieManager,
    activityClass = BuonduaCloudflareActivity::class.java,
    baseUrl = { BuonduaConstants.BASE_URL },
)
