package com.yenaly.han1meviewer.SitesHub.FuckingSession.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

object FuckingSessionCloudflareCookieManager : CloudflareCookieStore(
    tag = "FSCloudflareCookie",
    cookiePrefix = "fs_cf_cookie",
    expiryPrefix = "fs_cf_expiry",
)
