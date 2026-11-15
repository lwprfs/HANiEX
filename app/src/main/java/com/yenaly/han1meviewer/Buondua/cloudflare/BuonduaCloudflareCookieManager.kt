package com.yenaly.han1meviewer.Buondua.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

object BuonduaCloudflareCookieManager : CloudflareCookieStore(
    tag = "BDCloudflareCookie",
    cookiePrefix = "bd_cf_cookie",
    expiryPrefix = "bd_cf_expiry",
)
