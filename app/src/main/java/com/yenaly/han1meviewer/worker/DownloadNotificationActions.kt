package com.yenaly.han1meviewer.worker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yenaly.han1meviewer.DOWNLOAD_NOTIFICATION_CHANNEL
import com.yenaly.han1meviewer.R

/**
 * Builds the Pause / Resume / Cancel / Pause all / Start all actions that are
 * attached to download notifications, and the "paused" notification itself.
 */
object DownloadNotificationActions {

    const val ACTION_PAUSE = "com.yenaly.han1meviewer.download.ACTION_PAUSE"
    const val ACTION_RESUME = "com.yenaly.han1meviewer.download.ACTION_RESUME"
    const val ACTION_CANCEL = "com.yenaly.han1meviewer.download.ACTION_CANCEL"
    const val ACTION_PAUSE_ALL = "com.yenaly.han1meviewer.download.ACTION_PAUSE_ALL"
    const val ACTION_START_ALL = "com.yenaly.han1meviewer.download.ACTION_START_ALL"

    const val EXTRA_VIDEO_CODE = "extra_video_code"
    const val EXTRA_QUALITY = "extra_quality"

    private const val PAUSED_ID_SALT = 0x5A17_0001

    /** Stable id for the "paused" notification of one video. */
    fun pausedNotificationId(videoCode: String): Int = videoCode.hashCode() xor PAUSED_ID_SALT

    private fun pending(
        context: Context,
        action: String,
        videoCode: String?,
        quality: String?,
    ): PendingIntent {
        val intent = Intent(context, DownloadActionReceiver::class.java).apply {
            this.action = action
            setPackage(context.packageName)
            videoCode?.let { putExtra(EXTRA_VIDEO_CODE, it) }
            quality?.let { putExtra(EXTRA_QUALITY, it) }
            // Make the PendingIntents unique per video + action.
            data = android.net.Uri.parse("han1me-download://$action/${videoCode.orEmpty()}")
        }
        return PendingIntent.getBroadcast(
            context,
            (action + videoCode.orEmpty()).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Pause + Cancel + Pause all, for an actively downloading task. */
    fun addDownloadingActions(
        context: Context,
        builder: NotificationCompat.Builder,
        videoCode: String,
        quality: String?,
    ): NotificationCompat.Builder = builder
        .addAction(
            R.drawable.ic_baseline_pause_24,
            context.getString(R.string.download_action_pause),
            pending(context, ACTION_PAUSE, videoCode, quality),
        )
        .addAction(
            R.drawable.ic_baseline_cancel_24,
            context.getString(R.string.download_action_cancel),
            pending(context, ACTION_CANCEL, videoCode, quality),
        )
        .addAction(
            R.drawable.ic_baseline_pause_24,
            context.getString(R.string.pause_all),
            pending(context, ACTION_PAUSE_ALL, null, null),
        )

    /**
     * Quiet "paused" notification: Pause is replaced by Resume and
     * Pause all is replaced by Start all.
     */
    @SuppressLint("MissingPermission")
    fun showPaused(
        context: Context,
        videoCode: String,
        quality: String?,
        title: String,
    ) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_baseline_download_24)
            .setContentTitle(context.getString(R.string.download_paused_notification_s, title))
            .setContentText(context.getString(R.string.paused))
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_baseline_play_arrow_24,
                context.getString(R.string.download_action_resume),
                pending(context, ACTION_RESUME, videoCode, quality),
            )
            .addAction(
                R.drawable.ic_baseline_cancel_24,
                context.getString(R.string.download_action_cancel),
                pending(context, ACTION_CANCEL, videoCode, quality),
            )
            .addAction(
                R.drawable.ic_baseline_play_arrow_24,
                context.getString(R.string.start_all),
                pending(context, ACTION_START_ALL, null, null),
            )
            .build()
        runCatching { nm.notify(pausedNotificationId(videoCode), notification) }
    }

    fun cancelPaused(context: Context, videoCode: String) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(pausedNotificationId(videoCode))
        }
    }
}
