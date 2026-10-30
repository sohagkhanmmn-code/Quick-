package com.example.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

object BatteryMonitorScheduler {

    fun schedulePeriodicMonitoring(context: Context) {
        val constraints = Constraints.Builder()
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<BatteryMonitorWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag("battery_monitor")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BatteryMonitorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun triggerImmediateCheck(context: Context) {
        val oneTimeRequest = OneTimeWorkRequestBuilder<BatteryMonitorWorker>()
            .addTag("battery_monitor_immediate")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${BatteryMonitorWorker.WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun getWorkInfosFlow(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(BatteryMonitorWorker.WORK_NAME)
    }

    fun getImmediateWorkInfosFlow(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow("${BatteryMonitorWorker.WORK_NAME}_immediate")
    }
}
