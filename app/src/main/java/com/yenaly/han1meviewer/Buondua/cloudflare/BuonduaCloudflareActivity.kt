package com.yenaly.han1meviewer.Buondua.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class BuonduaCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "BDCloudflareActivity"
    override val cookieStore: CloudflareCookieStore get() = BuonduaCloudflareCookieManager
}
