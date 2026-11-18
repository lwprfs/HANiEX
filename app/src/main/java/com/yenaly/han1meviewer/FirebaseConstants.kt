package com.yenaly.han1meviewer

object FirebaseConstants {

    const val ADV_SEARCH_OPT = "advanced_search_options"

    const val H_KEYFRAMES = "h_keyframes"

    const val EVENT_NETWORK_ERROR = "network_error"

    const val EVENT_PLAYER_ERROR = "player_error"

    const val EVENT_PLAYBACK_START = "playback_start"

    const val EVENT_VIDEO_QUALITY_SWITCH = "video_quality_switch"

    const val EVENT_VIDEO_PIP = "video_pip"

    const val EVENT_COMMENT_POST = "comment_post"

    const val EVENT_DAILY_CHECKIN = "daily_checkin"

    const val EVENT_LOGOUT = "logout"

    const val EVENT_SITE_SWITCH = "site_switch"

    const val EVENT_DOWNLOAD_START = "download_start"

    const val EVENT_DOWNLOAD_RESULT = "download_result"

    const val EVENT_CLOUDFLARE_CHALLENGE = "cloudflare_challenge"

    const val EVENT_CLOUDFLARE_BLOCKED = "cloudflare_blocked"

    const val EVENT_CLIPBOARD_LINK_OPEN = "clipboard_link_open"

    const val EVENT_UPDATE_CONFIRM = "update_confirm"

    const val EVENT_SUBSCRIBE_ARTIST = "subscribe_artist"

    const val PARAM_SOURCE = "source"

    const val PARAM_EXCEPTION_CLASS = "exception_class"

    const val PARAM_EXCEPTION_MSG = "exception_msg"

    const val PARAM_HTTP_CODE = "http_code"

    const val PARAM_VIDEO_CODE = "video_code"

    const val PARAM_QUALITY = "quality"

    const val PARAM_FROM_QUALITY = "from_quality"

    const val PARAM_METHOD = "method"

    const val PARAM_RESULT = "result"

    const val PARAM_REASON = "reason"

    const val PARAM_PAGE = "page"

    const val PARAM_TOTAL_PAGES = "total_pages"

    const val PARAM_RESULT_COUNT = "result_count"

    const val PARAM_TAG_COUNT = "tag_count"

    const val PARAM_BRAND_COUNT = "brand_count"

    const val PARAM_SEARCH_TYPE = "search_type"

    const val PARAM_BROAD = "broad"

    const val PARAM_SORT = "sort"

    const val PARAM_GENRE = "genre"

    const val PARAM_DURATION = "duration"

    const val PARAM_FROM_DOWNLOAD = "from_download"

    const val PARAM_IS_LOCAL = "is_local"

    const val PARAM_KERNEL = "kernel"

    const val PARAM_REDOWNLOAD = "redownload"

    const val PARAM_IS_CANCEL = "is_cancel"

    const val PARAM_FROM_URL = "from_url"

    const val PARAM_TO_URL = "to_url"

    const val LOGIN_STATE = "login_state"

    const val APP_LANGUAGE = "app_language"

    const val VERSION_SOURCE = "version_source"

    const val RUNNING_DOWNLOAD_WORK_COUNT = "running_download_work_count"

    const val CURRENT_SITE = "current_site"

    const val USE_CUSTOM_MIRROR = "use_custom_mirror"

    const val PROXY_TYPE = "proxy_type"

    const val DOH_ENABLED = "doh_enabled"

    const val DOH_PRESET = "doh_preset"

    const val USE_BUILTIN_HOSTS = "use_builtin_hosts"

    const val USE_BACKUP_CDN = "use_backup_cdn"

    const val ENABLE_CI_UPDATE = "enable_ci_update"

    val remoteConfigDefaults: Map<String, Any> = mapOf(
        ENABLE_CI_UPDATE to true
    )

}
