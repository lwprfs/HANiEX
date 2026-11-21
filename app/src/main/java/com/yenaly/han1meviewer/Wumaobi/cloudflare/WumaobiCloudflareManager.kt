package com.yenaly.han1meviewer.Wumaobi.cloudflare

import com.yenaly.han1meviewer.Wumaobi.common.WumaobiConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareVerificationManager

object WumaobiCloudflareManager : CloudflareVerificationManager(
    tag = "WMBCloudflareManager",
    cookieStore = WumaobiCloudflareCookieManager,
    activityClass = WumaobiCloudflareActivity::class.java,
    baseUrl = { WumaobiConstants.BASE_URL },
)
