package com.yenaly.han1meviewer.SitesHub.Cosplaytele.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

object CosplayteleCloudflareCookieManager : CloudflareCookieStore(
    tag = "CTCloudflareCookie",
    cookiePrefix = "ct_cf_cookie",
    expiryPrefix = "ct_cf_expiry",
)
