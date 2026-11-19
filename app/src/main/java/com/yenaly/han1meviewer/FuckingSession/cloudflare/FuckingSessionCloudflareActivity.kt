package com.yenaly.han1meviewer.FuckingSession.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class FuckingSessionCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "FSCloudflareActivity"
    override val cookieStore: CloudflareCookieStore get() = FuckingSessionCloudflareCookieManager
}
