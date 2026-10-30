package com.example.work

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class BatteryMonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "jarvis_battery_monitor"
        const val KEY_BATTERY_PCT = "key_battery_pct"
        const val KEY_IS_LOW = "key_is_low"
        const val KEY_IS_CHARGING = "key_is_charging"
        const val LOW_BATTERY_THRESHOLD = 20

        const val PREFS_NAME = "jarvis_power_prefs"
        const val PREF_LAST_BATTERY_PCT = "last_battery_pct"
        const val PREF_IS_LOW_BATTERY = "is_low_battery"
    }

    override suspend fun doWork(): Result {
        val batteryIntent = applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            80
        }

        val isLow = batteryPct <= LOW_BATTERY_THRESHOLD && !isCharging

        // Save to SharedPreferences so the app reads it across restarts
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(PREF_LAST_BATTERY_PCT, batteryPct)
            .putBoolean(PREF_IS_LOW_BATTERY, isLow)
            .apply()

        val outputData = workDataOf(
            KEY_BATTERY_PCT to batteryPct,
            KEY_IS_LOW to isLow,
            KEY_IS_CHARGING to isCharging
        )

        return Result.success(outputData)
    }
}
