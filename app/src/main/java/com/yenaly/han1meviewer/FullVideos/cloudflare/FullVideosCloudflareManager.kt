package com.yenaly.han1meviewer.FullVideos.cloudflare

import com.yenaly.han1meviewer.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareVerificationManager

object FullVideosCloudflareManager : CloudflareVerificationManager(
    tag = "FVCloudflareManager",
    cookieStore = FullVideosCloudflareCookieManager,
    activityClass = FullVideosCloudflareActivity::class.java,
    baseUrl = { FullVideosConstants.BASE_URL },
)
