package com.yenaly.han1meviewer.Buondua.common

import android.content.Context
import com.yenaly.han1meviewer.Buondua.data.model.BuonduaVideo
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.SiteType
import com.yenaly.han1meviewer.logic.SiteCheckIn
import com.yenaly.han1meviewer.Buondua.data.remote.BuonduaNetwork
import com.yenaly.han1meviewer.Buondua.data.remote.BuonduaRepo
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import com.yenaly.han1meviewer.util.DownloadLayout
import com.yenaly.han1meviewer.util.SiteImageSaver
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

    private fun photoSegments(title: String) = listOf("Buondua", "Photos", DownloadLayout.sanitize(title))

    suspend fun downloadAllPhotos(
        context: Context,
        code: String,
        title: String,
        loadedUrls: List<String>,
        totalPages: Int,
    ) {
        GlobalToasts.show(
            context.getString(R.string.buondua_photos_download_started),
            level = GlobalToasts.ToastLevel.INFO,
        )
        val urls = BuonduaRepo.allPhotoUrls(code, loadedUrls, totalPages)
        SiteImageSaver.saveBatchInBackground(
            context = context,
            urls = urls,
            segments = photoSegments(title),
            baseName = DownloadLayout.sanitize(title, "photo"),
            client = BuonduaNetwork.streamClient,
            headers = BuonduaNetwork.mediaHeaders(),
        ) { result ->
            val msg = when (result) {
                is SiteImageSaver.SaveResult.Saved ->
                    "${result.count} saved, ${result.skipped} skipped, ${result.failed} failed"
                is SiteImageSaver.SaveResult.Error -> result.message
            }
            GlobalToasts.show(
                msg,
                level = if (result is SiteImageSaver.SaveResult.Error)
                    GlobalToasts.ToastLevel.ERROR else GlobalToasts.ToastLevel.SUCCESS,
            )
        }
    }

    suspend fun savePhoto(context: Context, title: String, url: String, index: Int): SiteImageSaver.SaveResult =
        SiteImageSaver.saveOne(
            context = context,
            url = url,
            segments = photoSegments(title),
            baseName = "photo_${index + 1}",
            client = BuonduaNetwork.streamClient,
            headers = BuonduaNetwork.mediaHeaders(),
        )
}
