package com.yenaly.han1meviewer.SitesHub.Coser.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class CoserCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "CoserCFActivity"
    override val cookieStore: CloudflareCookieStore get() = CoserCloudflareCookieManager
}
