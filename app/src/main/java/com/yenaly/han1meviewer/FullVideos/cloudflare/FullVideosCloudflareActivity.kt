package com.yenaly.han1meviewer.FullVideos.cloudflare

import com.yenaly.han1meviewer.util.cloudflare.BaseCloudflareActivity
import com.yenaly.han1meviewer.util.cloudflare.CloudflareCookieStore

class FullVideosCloudflareActivity : BaseCloudflareActivity() {
    override val tag = "FVCloudflareActivity"
    override val cookieStore: CloudflareCookieStore get() = FullVideosCloudflareCookieManager
}
