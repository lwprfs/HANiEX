package com.yenaly.han1meviewer.SitesHub.Buondua.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class BuonduaCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "BDCloudflareActivity"
    override val cookieStore: CloudflareCookieStore get() = BuonduaCloudflareCookieManager
}
