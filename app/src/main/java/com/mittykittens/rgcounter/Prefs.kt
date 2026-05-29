package com.mittykittens.rgcounter

import android.content.Context

object Prefs {
    private const val NAME = "rgcounter_prefs"
    private const val KEY_OPEN_COUNT  = "open_count"
    private const val KEY_CLOSE_COUNT = "close_count"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun openCount(ctx: Context): Int  = prefs(ctx).getInt(KEY_OPEN_COUNT, 0)
    fun closeCount(ctx: Context): Int = prefs(ctx).getInt(KEY_CLOSE_COUNT, 0)
    fun totalCount(ctx: Context): Int = openCount(ctx) + closeCount(ctx)

    fun incrementOpen(ctx: Context) {
        prefs(ctx).edit().putInt(KEY_OPEN_COUNT, openCount(ctx) + 1).apply()
    }

    fun incrementClose(ctx: Context) {
        prefs(ctx).edit().putInt(KEY_CLOSE_COUNT, closeCount(ctx) + 1).apply()
    }

    fun resetCounts(ctx: Context) {
        prefs(ctx).edit()
            .putInt(KEY_OPEN_COUNT, 0)
            .putInt(KEY_CLOSE_COUNT, 0)
            .apply()
    }
}
