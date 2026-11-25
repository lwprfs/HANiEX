package com.yenaly.han1meviewer.SitesHub.FullVideos.cloudflare

import android.content.Context
import com.yenaly.han1meviewer.SitesHub.FullVideos.common.FullVideosConstants
import com.yenaly.han1meviewer.util.cloudflare.CloudflareChallengeInterceptor

class FullVideosCloudflareInterceptor(
    context: Context,
) : CloudflareChallengeInterceptor(
    context = context,
    tag = "FVCloudflareInterceptor",
    cookieStore = FullVideosCloudflareCookieManager,
    manager = FullVideosCloudflareManager,
    hostnames = { FullVideosConstants.HOSTNAME },
    baseUrls = { listOf(FullVideosConstants.BASE_URL) },
)
