package com.mittykittens.rgcounter

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku

/**
 * Manages the lifecycle of the Shizuku connection and the HingeUserService daemon.
 *
 * Call [init] once from Application or MainActivity. After that, Shizuku events
 * drive binding automatically. [tryBindOrRequest] can be called explicitly to
 * re-attempt after the user grants permission.
 */
object ShizukuBridge {

    private const val TAG = "RgCounter"
    private const val PERM_REQ_CODE = 1001

    // Bump whenever HingeUserService code changes — Shizuku restarts the daemon when
    // the version number increments, ensuring the new class is loaded.
    private const val SERVICE_VERSION = 1

    @Volatile var service: IUserService? = null
        private set

    private val args by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(
                "com.mittykittens.rgcounter",
                HingeUserService::class.java.name
            )
        )
            .daemon(true)
            .processNameSuffix("hinge_watcher")
            .debuggable(false)
            .version(SERVICE_VERSION)
    }

    private val connection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                val svc = IUserService.Stub.asInterface(binder)
                service = svc
                Log.i(TAG, "UserService connected")
                runCatching { svc.startWatch() }
                    .onFailure { Log.w(TAG, "startWatch err: ${it.message}") }
                listener?.onConnected()
            } else {
                Log.w(TAG, "UserService binder invalid")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            Log.i(TAG, "UserService disconnected")
            listener?.onDisconnected()
        }
    }

    interface Listener {
        fun onConnected()
        fun onDisconnected()
        fun onPermissionResult(granted: Boolean)
    }

    @Volatile var listener: Listener? = null

    private val permListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == PERM_REQ_CODE) {
            val granted = result == PackageManager.PERMISSION_GRANTED
            listener?.onPermissionResult(granted)
            if (granted) bind()
        }
    }

    fun init(context: Context) {
        Shizuku.addRequestPermissionResultListener(permListener)
        Shizuku.addBinderReceivedListenerSticky { tryBindOrRequest() }
        Shizuku.addBinderDeadListener {
            service = null
            listener?.onDisconnected()
        }
    }

    fun tryBindOrRequest() {
        if (!Shizuku.pingBinder()) {
            Log.w(TAG, "Shizuku not running")
            return
        }
        if (hasPermission()) bind() else Shizuku.requestPermission(PERM_REQ_CODE)
    }

    fun hasPermission(): Boolean {
        if (!Shizuku.pingBinder()) return false
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    fun isShizukuAlive(): Boolean = Shizuku.pingBinder()

    private fun bind() {
        try {
            Shizuku.bindUserService(args, connection)
        } catch (e: Throwable) {
            Log.e(TAG, "bindUserService failed", e)
        }
    }

    fun unbind() {
        runCatching { Shizuku.unbindUserService(args, connection, true) }
        service = null
    }
}
