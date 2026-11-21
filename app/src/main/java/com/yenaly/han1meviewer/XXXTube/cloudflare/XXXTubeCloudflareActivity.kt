package com.yenaly.han1meviewer.XXXTube.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class XXXTubeCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "XXXCloudflareActivity"
    override val cookieStore: CloudflareCookieStore get() = XXXTubeCloudflareCookieManager
}
