package com.yenaly.han1meviewer.Wumaobi.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class WumaobiCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "WMBCloudflareActivity"
    override val cookieStore: CloudflareCookieStore get() = WumaobiCloudflareCookieManager
}
