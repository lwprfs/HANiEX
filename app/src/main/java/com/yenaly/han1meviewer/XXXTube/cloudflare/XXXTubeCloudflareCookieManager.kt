package com.yenaly.han1meviewer.XXXTube.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

object XXXTubeCloudflareCookieManager : CloudflareCookieStore(
    tag = "XXXCloudflareCookie",
    cookiePrefix = "xxx_cf_cookie",
    expiryPrefix = "xxx_cf_expiry",
)
