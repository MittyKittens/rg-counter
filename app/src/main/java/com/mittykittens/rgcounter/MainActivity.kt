package com.mittykittens.rgcounter

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mittykittens.rgcounter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), ShizukuBridge.Listener {

    private lateinit var binding: ActivityMainBinding
    private val ui = Handler(Looper.getMainLooper())

    private val refresh = object : Runnable {
        override fun run() {
            render()
            ui.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ShizukuBridge.listener = this
        ShizukuBridge.init(applicationContext)

        binding.btnStartStop.setOnClickListener { onStartStop() }
        binding.btnReset.setOnClickListener { onReset() }
    }

    override fun onResume() {
        super.onResume()
        ui.post(refresh)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(refresh)
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuBridge.listener = null
    }

    // ── ShizukuBridge.Listener ──────────────────────────────────────────────

    override fun onConnected() = runOnUiThread {
        CounterService.startOrUpdate(this)
        render()
    }

    override fun onDisconnected() = runOnUiThread {
        render()
    }

    override fun onPermissionResult(granted: Boolean) = runOnUiThread {
        if (!granted) Toast.makeText(this, "Shizuku permission denied", Toast.LENGTH_SHORT).show()
        render()
    }

    // ── UI ──────────────────────────────────────────────────────────────────

    private fun render() {
        val shizukuInstalled = isShizukuInstalled()
        val shizukuAlive     = ShizukuBridge.isShizukuAlive()
        val permGranted      = ShizukuBridge.hasPermission()
        val svc              = ShizukuBridge.service
        val watching         = svc != null && runCatching { svc.isRunning }.getOrDefault(false)
        val lastEvt          = runCatching { svc?.lastEvent() ?: "—" }.getOrDefault("—")

        binding.txtShizukuInstalled.text = "Shizuku installed: ${yesNo(shizukuInstalled)}"
        binding.txtShizukuRunning.text   = "Shizuku running:   ${yesNo(shizukuAlive)}"
        binding.txtPermission.text       = "Permission:        ${yesNo(permGranted)}"
        binding.txtWatching.text         = "Watcher active:    ${yesNo(watching)}"
        binding.txtLastEvent.text        = "Last event: $lastEvt"

        binding.txtCountTotal.text  = "Total flips:  ${Prefs.totalCount(this)}"
        binding.txtCountOpens.text  = "Hinge opens:  ${Prefs.openCount(this)}"
        binding.txtCountCloses.text = "Hinge closes: ${Prefs.closeCount(this)}"

        binding.btnStartStop.text = if (watching) "Stop watcher" else "Start watcher"
        binding.btnStartStop.isEnabled = permGranted || watching
    }

    private fun onStartStop() {
        val svc = ShizukuBridge.service
        if (svc != null && runCatching { svc.isRunning }.getOrDefault(false)) {
            ShizukuBridge.unbind()
            CounterService.stop(this)
        } else {
            ShizukuBridge.tryBindOrRequest()
        }
        render()
    }

    private fun onReset() {
        Prefs.resetCounts(this)
        CounterService.updateNotification(this)
        render()
    }

    private fun isShizukuInstalled(): Boolean = try {
        packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun yesNo(v: Boolean) = if (v) "yes" else "no"
}
