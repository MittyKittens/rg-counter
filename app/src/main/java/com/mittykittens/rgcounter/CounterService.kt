package com.mittykittens.rgcounter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Foreground service that shows a persistent notification in the status bar
 * displaying the current hinge-flip count. Running as a foreground service
 * keeps the app process alive so HingeReceiver can always be dispatched quickly
 * even when the app is in the background.
 *
 * Start via [startOrUpdate]; the service will not be started if Shizuku is not
 * active — but the notification will be refreshed from [updateNotification] on
 * every hinge event.
 */
class CounterService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        startForeground(
            NOTIFICATION_ID,
            buildNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        Log.i(TAG, "CounterService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Refresh the notification text on every start/update command.
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(this))
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "CounterService stopped")
    }

    companion object {
        private const val TAG = "RgCounter"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "rgcounter_channel"

        /** Start the service if not running, or update its notification if already running. */
        fun startOrUpdate(context: Context) {
            val intent = Intent(context, CounterService::class.java)
            context.startForegroundService(intent)
        }

        /** Signal an already-running service to refresh its notification count. */
        fun updateNotification(context: Context) {
            startOrUpdate(context)
        }

        /** Stop the foreground service (e.g. when the watcher is stopped). */
        fun stop(context: Context) {
            context.stopService(Intent(context, CounterService::class.java))
        }

        private fun ensureChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "RG Counter",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows the current hinge-flip count"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }

        fun buildNotification(context: Context): Notification {
            ensureChannel(context)
            val total  = Prefs.totalCount(context)
            val opens  = Prefs.openCount(context)
            val closes = Prefs.closeCount(context)

            val tapIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_rotate)
                .setContentTitle("Hinge flips: $total")
                .setContentText("Opens: $opens  •  Closes: $closes")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(tapIntent)
                .build()
        }
    }
}
