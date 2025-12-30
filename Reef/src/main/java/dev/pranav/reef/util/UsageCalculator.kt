package dev.pranav.reef.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

object UsageCalculator {

    fun calculateUsage(
        usm: UsageStatsManager,
        startTime: Long,
        endTime: Long,
        targetPackage: String? = null
    ): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val lastResumeTime = mutableMapOf<String, Long>()

        val events = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (targetPackage != null && event.packageName != targetPackage) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastResumeTime[event.packageName] = event.timeStamp.coerceAtLeast(startTime)
                }

                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val resumeTime = lastResumeTime.remove(event.packageName)
                    if (resumeTime != null) {
                        val duration = event.timeStamp.coerceAtMost(endTime) - resumeTime
                        if (duration > 0) {
                            result[event.packageName] =
                                result.getOrDefault(event.packageName, 0L) + duration
                        }
                    }
                }
            }
        }

        val now = System.currentTimeMillis()
        for ((pkg, resumeTime) in lastResumeTime) {
            val effectiveEnd = minOf(endTime, now)
            val duration = effectiveEnd - resumeTime
            if (duration > 0) {
                result[pkg] = result.getOrDefault(pkg, 0L) + duration
            }
        }

        return result
    }
}
