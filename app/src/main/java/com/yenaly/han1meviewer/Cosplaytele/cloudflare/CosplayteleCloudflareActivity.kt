package com.yenaly.han1meviewer.Cosplaytele.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class CosplayteleCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "CTCloudflareActivity"
    override val cookieStore: CloudflareCookieStore get() = CosplayteleCloudflareCookieManager
}
