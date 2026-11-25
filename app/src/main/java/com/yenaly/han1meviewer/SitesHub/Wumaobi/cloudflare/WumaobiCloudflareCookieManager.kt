package com.yenaly.han1meviewer.SitesHub.Wumaobi.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

object WumaobiCloudflareCookieManager : CloudflareCookieStore(
    tag = "WMBCloudflareCookie",
    cookiePrefix = "wmb_cf_cookie",
    expiryPrefix = "wmb_cf_expiry",
)
