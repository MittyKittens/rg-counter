package com.mittykittens.rgcounter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives explicit hinge broadcasts from the Shizuku UserService and increments
 * the flip counter.
 *
 * Using a manifest BroadcastReceiver (rather than a background Service start) is
 * intentional: Android 12+ blocks shell uid from starting services in apps that
 * have been in the background more than ~1 minute. Manifest receivers are exempt
 * from that restriction and are delivered even to stopped apps.
 */
class HingeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            HingeUserService.ACTION_OPEN -> {
                Prefs.incrementOpen(context)
                Log.i(TAG, "hinge OPEN — opens=${Prefs.openCount(context)} total=${Prefs.totalCount(context)}")
            }
            HingeUserService.ACTION_CLOSE -> {
                Prefs.incrementClose(context)
                Log.i(TAG, "hinge CLOSE — closes=${Prefs.closeCount(context)} total=${Prefs.totalCount(context)}")
            }
            else -> return
        }
        // Tell the foreground service to refresh its notification with the new count.
        CounterService.updateNotification(context)
    }

    companion object {
        private const val TAG = "RgCounter"
    }
}
