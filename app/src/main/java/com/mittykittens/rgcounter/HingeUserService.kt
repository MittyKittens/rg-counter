package com.mittykittens.rgcounter

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku UserService — runs as the `shell` uid, which has read access to
 * /dev/input/event2 (gpio-keys driver on the Anbernic RG Rotate).
 *
 * The hinge Hall sensor emits EV_KEY events on that device:
 *   KEY_F12 (scancode 0x58) value=1 → hinge closed
 *   KEY_F12 (scancode 0x58) value=0 → hinge opened (device already awake)
 *   KEY_F9  (scancode 0x43) value=1 → hinge opened (device waking from sleep)
 *
 * On each transition this service fires an explicit broadcast that
 * HingeReceiver in the main app catches to update the counter and notification.
 */
class HingeUserService : IUserService.Stub {

    constructor()
    constructor(@Suppress("UNUSED_PARAMETER") context: android.content.Context) : this()

    @Volatile private var watcherThread: Thread? = null
    @Volatile private var watcherProcess: Process? = null
    @Volatile private var lastEvent: String = "none"

    override fun destroy() {
        stopWatch()
    }

    override fun startWatch() {
        if (watcherThread != null) return
        val t = Thread({ runWatcher() }, "rgcounter-hinge-watcher")
        t.isDaemon = true
        watcherThread = t
        t.start()
        Log.i(TAG, "watcher started")
    }

    override fun stopWatch() {
        watcherProcess?.destroy()
        watcherProcess = null
        watcherThread = null
        Log.i(TAG, "watcher stopped")
    }

    override fun isRunning(): Boolean = watcherThread != null

    override fun lastEvent(): String = lastEvent

    private fun runWatcher() {
        while (watcherThread != null) {
            try {
                val proc = ProcessBuilder("/system/bin/getevent", EVENT_DEVICE)
                    .redirectErrorStream(true).start()
                watcherProcess = proc
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                reader.useLines { lines ->
                    for (line in lines) {
                        handle(line)
                        if (watcherThread == null) break
                    }
                }
                proc.waitFor()
            } catch (e: Exception) {
                Log.w(TAG, "watcher iter failed: ${e.message}")
                Thread.sleep(2000)
            }
        }
    }

    private fun handle(rawLine: String) {
        val parts = rawLine.trim().split(Regex("\\s+"))
        if (parts.size < 3) return
        val type = parts[0].toIntOrNull(16) ?: return
        val code = parts[1].toIntOrNull(16) ?: return
        val value = parts[2].toLongOrNull(16)?.toInt() ?: return
        if (type != EV_KEY) return

        val action: String? = when (code) {
            KEY_F12 -> when (value) {
                1 -> ACTION_CLOSE
                0 -> ACTION_OPEN
                else -> null
            }
            KEY_F9 -> if (value == 1) ACTION_OPEN else null
            else -> null
        }
        if (action == null) return

        lastEvent = "${System.currentTimeMillis()} $action (code=0x${code.toString(16)} v=$value)"
        broadcast(action)
        Log.i(TAG, "fired $action; code=0x${code.toString(16)} v=$value")
    }

    private fun broadcast(action: String) {
        try {
            val proc = ProcessBuilder(
                "/system/bin/am", "broadcast",
                "--include-stopped-packages",
                "-n", "$APP_PKG/.HingeReceiver",
                "-a", action
            ).redirectErrorStream(true).start()
            proc.waitFor()
            if (proc.exitValue() != 0) {
                val out = proc.inputStream.bufferedReader().readText().trim()
                Log.w(TAG, "broadcast $action exit=${proc.exitValue()} out=$out")
            }
        } catch (e: Exception) {
            Log.w(TAG, "broadcast failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "RgCounter"
        private const val APP_PKG = "com.mittykittens.rgcounter"

        // /dev/input/event2 = gpio-keys on the Anbernic RG Rotate (Unisoc T618, Android 12).
        // Verified by `adb shell getevent -lp`: deviceId=5, name="gpio-keys".
        // Contains KEY_F9, KEY_F12, KEY_VOLUMEDOWN, KEY_VOLUMEUP, KEY_POWER.
        private const val EVENT_DEVICE = "/dev/input/event2"

        private const val EV_KEY = 0x01
        private const val KEY_F12 = 0x58  // hinge-closed while held; released when opened
        private const val KEY_F9  = 0x43  // wake-from-sleep hinge-open event

        const val ACTION_OPEN  = "rgcounter.HINGE_OPEN"
        const val ACTION_CLOSE = "rgcounter.HINGE_CLOSE"
    }
}
