package com.yenaly.han1meviewer.Buondua.common

import android.content.Context
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaVideo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.worker.HanimeDownloadWorker

object BuonduaDownloads {

    fun videoCodeFor(albumCode: String, video: BuonduaVideo): String =
        "${albumCode.trim('/')}/Videos/${video.fileName.substringBeforeLast('.').ifBlank { "video" }}"

    private fun suffixOf(video: BuonduaVideo): String =
        video.fileName.substringAfterLast('.', "").lowercase()
            .takeIf { it.length in 2..4 } ?: "mp4"

    fun enqueueVideo(context: Context, albumCode: String, video: BuonduaVideo, coverUrl: String?) {
        HanimeDownloadManagerV2.addTask(
            HanimeDownloadWorker.Args(
                quality = "default",
                downloadUrl = video.url,
                videoType = suffixOf(video),
                hanimeName = video.title,
                videoCode = SiteCheckIn.dishCode(SiteType.BUONDUA, videoCodeFor(albumCode, video)),
                coverUrl = coverUrl.orEmpty(),
            )
        )
        GlobalToasts.show(
            context.getString(R.string.added_to_download_queue),
            level = GlobalToasts.ToastLevel.SUCCESS,
        )
    }
}
