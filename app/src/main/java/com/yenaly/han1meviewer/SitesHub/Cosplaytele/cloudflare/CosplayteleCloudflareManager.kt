package com.yenaly.han1meviewer.SitesHub.Cosplaytele.cloudflare

import com.yenaly.han1meviewer.SitesHub.Cosplaytele.common.CosplayteleConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareVerificationManager

object CosplayteleCloudflareManager : CloudflareVerificationManager(
    tag = "CTCloudflareManager",
    cookieStore = CosplayteleCloudflareCookieManager,
    activityClass = CosplayteleCloudflareActivity::class.java,
    baseUrl = { CosplayteleConstants.BASE_URL },
)
