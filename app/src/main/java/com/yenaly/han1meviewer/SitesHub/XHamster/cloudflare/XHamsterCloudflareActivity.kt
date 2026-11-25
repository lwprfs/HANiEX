package com.yenaly.han1meviewer.SitesHub.XHamster.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class XHamsterCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "XHCloudflareActivity"
    override val cookieStore: CloudflareCookieStore get() = XHamsterCloudflareCookieManager
}
