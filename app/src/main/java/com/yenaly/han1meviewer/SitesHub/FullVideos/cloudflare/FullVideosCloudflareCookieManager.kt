package com.yenaly.han1meviewer.SitesHub.FullVideos.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

object FullVideosCloudflareCookieManager : CloudflareCookieStore(
    tag = "FVCloudflareCookie",
    cookiePrefix = "fv_cf_cookie",
    expiryPrefix = "fv_cf_expiry",
)
