package com.yenaly.han1meviewer.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DownloadActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val code = intent.getStringExtra(DownloadNotificationActions.EXTRA_VIDEO_CODE)
        val quality = intent.getStringExtra(DownloadNotificationActions.EXTRA_QUALITY)
        val pending = goAsync()
        scope.launch {
            try {
                HanimeDownloadManagerV2.init()
                handle(context.applicationContext, action, code, quality)
            } catch (e: Exception) {
                Log.e(TAG, "action $action failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun findEntity(code: String?, quality: String?): HanimeDownloadEntity? {
        if (code.isNullOrBlank()) return null
        return (if (quality != null) DatabaseRepo.HanimeDownload.find(code, quality) else null)
            ?: DatabaseRepo.HanimeDownload.find(code)
    }

    private suspend fun handle(context: Context, action: String, code: String?, quality: String?) {
        when (action) {
            DownloadNotificationActions.ACTION_PAUSE ->
                findEntity(code, quality)?.let { HanimeDownloadManagerV2.stopTask(it) }

            DownloadNotificationActions.ACTION_RESUME -> {
                findEntity(code, quality)?.let { HanimeDownloadManagerV2.resumeTask(it) }
                code?.let { DownloadNotificationActions.cancelPaused(context, it) }
            }

            DownloadNotificationActions.ACTION_CANCEL -> {
                findEntity(code, quality)?.let { HanimeDownloadManagerV2.deleteTask(it) }
                code?.let { DownloadNotificationActions.cancelPaused(context, it) }
            }

            DownloadNotificationActions.ACTION_PAUSE_ALL ->
                HanimeDownloadManagerV2.pauseAll(pendingEntities())

            DownloadNotificationActions.ACTION_START_ALL -> {
                val entities = pendingEntities()
                entities.forEach { DownloadNotificationActions.cancelPaused(context, it.videoCode) }
                HanimeDownloadManagerV2.resumeAll(entities)
            }
        }
    }

    private suspend fun pendingEntities(): List<HanimeDownloadEntity> =
        DownloadDatabase.instance.hanimeDownloadDao.loadAllDownloadingHanimeOnce()

    private companion object {
        const val TAG = "DownloadActionReceiver"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
