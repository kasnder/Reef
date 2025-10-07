package dev.pranav.reef.util

import android.app.usage.UsageStatsManager
import android.util.Log
import androidx.core.content.edit

object RoutineLimits {
    private const val ROUTINE_LIMITS_KEY = "routine_limits"
    private const val ACTIVE_ROUTINE_KEY = "active_routine_id"
    private const val ROUTINE_START_TIME_KEY = "routine_start_time"
    private val routineLimits = mutableMapOf<String, Long>()

    fun setRoutineLimits(limits: Map<String, Int>, routineId: String) {
        // Clear existing routine limits
        routineLimits.clear()

        Log.d("RoutineLimits", "Setting routine limits for routine: $routineId")

        // Set new limits (convert minutes to milliseconds)
        limits.forEach { (packageName, minutes) ->
            routineLimits[packageName] = minutes * 60 * 1000L
            Log.d(
                "RoutineLimits",
                "Set limit for $packageName: ${minutes}m (${minutes * 60 * 1000L}ms)"
            )
        }

        // Save to preferences
        saveRoutineLimits()

        // Mark this routine as active and record start time
        prefs.edit {
            putString(ACTIVE_ROUTINE_KEY, routineId)
            putLong(ROUTINE_START_TIME_KEY, System.currentTimeMillis())
        }
        Log.d(
            "RoutineLimits",
            "Marked routine $routineId as active, start time: ${System.currentTimeMillis()}"
        )
    }

    fun clearRoutineLimits() {
        Log.d("RoutineLimits", "Clearing all routine limits")
        routineLimits.clear()
        saveRoutineLimits()
        prefs.edit {
            remove(ACTIVE_ROUTINE_KEY)
            remove(ROUTINE_START_TIME_KEY)
        }
    }

    fun getRoutineLimit(packageName: String): Long {
        val limit = routineLimits[packageName] ?: 0L
        Log.d("RoutineLimits", "Getting routine limit for $packageName: $limit ms")
        return limit
    }

    fun hasRoutineLimit(packageName: String): Boolean {
        val hasLimit = routineLimits.containsKey(packageName)
        Log.d("RoutineLimits", "Checking routine limit for $packageName: $hasLimit")
        return hasLimit
    }

    fun getRoutineUsageTime(packageName: String, usageStatsManager: UsageStatsManager): Long {
        val routineStartTime = prefs.getLong(ROUTINE_START_TIME_KEY, 0L)
        if (routineStartTime == 0L) {
            Log.d("RoutineLimits", "No routine start time found, returning 0")
            return 0L
        }

        val endTime = System.currentTimeMillis()

        // Query usage stats with fine-grained intervals from routine start to now
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            routineStartTime,
            endTime
        )

        // Filter for the specific package and calculate usage only in this time window
        val packageStats = stats.filter { it.packageName == packageName }

        var totalUsage = 0L
        packageStats.forEach { stat ->
            // For each usage stat, calculate the overlap with our routine period
            val statStart = stat.firstTimeStamp.coerceAtLeast(routineStartTime)
            val statEnd = stat.lastTimeStamp.coerceAtMost(endTime)

            // Only count usage that falls within the routine period
            if (statStart < statEnd) {
                totalUsage += stat.totalTimeInForeground
            }
        }

        Log.d(
            "RoutineLimits",
            "Usage time for $packageName since routine start ($routineStartTime to $endTime): $totalUsage ms (${packageStats.size} stat entries)"
        )

        return totalUsage
    }

    fun getRoutineLimits(): Map<String, Long> {
        return routineLimits.toMap()
    }

    fun getActiveRoutineId(): String? {
        return prefs.getString(ACTIVE_ROUTINE_KEY, null)
    }

    fun isRoutineActive(): Boolean {
        return getActiveRoutineId() != null && routineLimits.isNotEmpty()
    }

    private fun saveRoutineLimits() {
        // Clear existing routine limits
        val keys = prefs.all.keys.filter { it.startsWith("routine_limit_") }
        prefs.edit {
            keys.forEach { remove(it) }
        }

        // Save new limits
        prefs.edit {
            routineLimits.forEach { (packageName, limit) ->
                putLong("routine_limit_$packageName", limit)
            }
        }
    }

    fun loadRoutineLimits() {
        routineLimits.clear()
        val allPrefs = prefs.all

        allPrefs.forEach { (key, value) ->
            if (key.startsWith("routine_limit_") && value is Long) {
                val packageName = key.removePrefix("routine_limit_")
                routineLimits[packageName] = value
            }
        }
    }
}
