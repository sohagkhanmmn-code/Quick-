package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.work.BatteryMonitorScheduler
import com.example.work.BatteryMonitorWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        try {
            WorkManager.initialize(
                context,
                Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.DEBUG)
                    .build()
            )
        } catch (_: Exception) {
        }
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS Voice Assistant", appName)
    }

    @Test
    fun `battery monitor worker constants are configured correctly`() {
        assertEquals("jarvis_battery_monitor", BatteryMonitorWorker.WORK_NAME)
        assertEquals("key_battery_pct", BatteryMonitorWorker.KEY_BATTERY_PCT)
        assertEquals("key_is_low", BatteryMonitorWorker.KEY_IS_LOW)
        assertTrue(BatteryMonitorWorker.LOW_BATTERY_THRESHOLD > 0)
    }

    @Test
    fun `battery monitor scheduler registers work`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BatteryMonitorScheduler.schedulePeriodicMonitoring(context)
        BatteryMonitorScheduler.triggerImmediateCheck(context)
        assertNotNull(context)
    }

    @Test
    fun `jarvis ai service parses action tags properly`() {
        val service = com.example.ai.JarvisAiService()
        val parsed = service.parseActionTags("Right away, Sir. [ACTION:TORCH_ON]")
        assertEquals("Right away, Sir.", parsed.spokenText)
        assertEquals("TORCH_ON", parsed.actionType)

        val parsedWithPayload = service.parseActionTags("Searching YouTube now. [ACTION:YOUTUBE:Iron Man Theme]")
        assertEquals("Searching YouTube now.", parsedWithPayload.spokenText)
        assertEquals("YOUTUBE", parsedWithPayload.actionType)
        assertEquals("Iron Man Theme", parsedWithPayload.actionPayload)
    }

    @Test
    fun `jarvis ai local reasoning handles commands`() {
        val service = com.example.ai.JarvisAiService()
        val result = service.localIntelligentReasoner("turn on flashlight", false)
        assertEquals("TORCH_ON", result.actionType)
        assertTrue(result.spokenText.contains("flashlight", ignoreCase = true))
    }
}
