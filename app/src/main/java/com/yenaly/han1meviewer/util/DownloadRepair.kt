package com.yenaly.han1meviewer.util

import android.content.Context
import android.util.Log
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DownloadRepair {

    private const val TAG = "DownloadRepair"

    suspend fun usableVideoUri(context: Context, entity: HanimeDownloadEntity): String =
        withContext(Dispatchers.IO) {
            if (SafFileManager.isUsableVideoUri(context, entity.videoUri)) return@withContext entity.videoUri
            val files = SafFileManager.listVideoFiles(context, entity.videoCode)
            if (files.isEmpty()) return@withContext entity.videoUri
            val sameQuality = files.filter { (_, name, _) ->
                DownloadScanner.parseVideoFileName(name).second.equals(entity.quality, ignoreCase = true)
            }
            val best = (sameQuality.ifEmpty { files }).first()
            runCatching {
                DatabaseRepo.HanimeDownload.updateKeepingGroup(
                    entity.copy(videoUri = best.first, length = best.third, downloadedLength = best.third)
                )
            }.onFailure { Log.w(TAG, "could not repair entry", it) }
            best.first
        }
}
