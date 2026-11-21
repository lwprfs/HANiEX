package com.yenaly.han1meviewer.Coser.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

object CoserCloudflareCookieManager : CloudflareCookieStore(
    tag = "CoserCFCookie",
    cookiePrefix = "coser_cf_cookie",
    expiryPrefix = "coser_cf_expiry",
)
