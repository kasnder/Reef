package dev.pranav.reef.accessibility

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import dev.pranav.reef.R
import dev.pranav.reef.routine.Routines
import dev.pranav.reef.scheduleWatcher
import dev.pranav.reef.util.BLOCKER_CHANNEL_ID
import dev.pranav.reef.util.NotificationHelper.createNotificationChannel
import dev.pranav.reef.util.Whitelist
import dev.pranav.reef.util.isPrefsInitialized
import dev.pranav.reef.util.prefs

@SuppressLint("AccessibilityPolicy")
class BlockerService: AccessibilityService() {

    private val routineChangeReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Routines.ACTION_CHANGED) {
                val sessionsJson = intent.getStringExtra("sessions") ?: "[]"
                Log.d("BlockerService", "Routine state changed - received sessions: $sessionsJson")

                prefs.edit { putString("active_routine_sessions", sessionsJson) }

                Log.d("BlockerService", "Updated active sessions in SharedPreferences")
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        createNotificationChannel()

        if (!isPrefsInitialized) {
            val deviceContext = createDeviceProtectedStorageContext()
            prefs = deviceContext.getSharedPreferences("prefs", MODE_PRIVATE)
        }

        val filter = IntentFilter(Routines.ACTION_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(routineChangeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(routineChangeReceiver, filter)
        }

        scheduleWatcher(this)
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager
        if (keyguardManager.isKeyguardLocked) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return
        if (event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION) return

        val pkg = event.packageName?.toString() ?: return

        if (pkg == packageName) return
        if (Whitelist.isWhitelisted(pkg)) return

        if (prefs.getBoolean("focus_mode", false)) {
            Log.d("BlockerService", "Blocking $pkg in focus mode")
            performGlobalAction(GLOBAL_ACTION_HOME)
            showFocusModeNotification(pkg)
            return
        }

        // Check for website blocking in Firefox
        if (isFirefoxPackage(pkg)) {
            val url = extractUrlFromEvent(event)
            if (url != null) {
                val domain = extractDomain(url)
                if (domain != null && Routines.isWebsiteBlocked(domain)) {
                    Log.d("BlockerService", "Blocking website $domain in Firefox")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    showWebsiteBlockedNotification(domain)
                    return
                }
            }
        }

        val blockReason = UsageTracker.checkBlockReason(this, pkg)
        if (blockReason == UsageTracker.BlockReason.NONE) return

        Log.d("BlockerService", "Blocking $pkg due to ${blockReason.name}")

        performGlobalAction(GLOBAL_ACTION_HOME)

        showBlockedNotification(pkg, blockReason)
    }

    private fun isFirefoxPackage(packageName: String): Boolean {
        return packageName == "org.mozilla.firefox" ||
               packageName == "org.mozilla.firefox_beta" ||
               packageName == "org.mozilla.fenix" ||
               packageName == "org.mozilla.fenix.nightly" ||
               packageName == "org.mozilla.focus"
    }

    private fun extractUrlFromEvent(event: AccessibilityEvent): String? {
        // Try to extract URL from the event's source node
        val source = event.source ?: return null

        try {
            // Look for URL bar or address bar node
            val urlNode = findUrlNode(source)
            if (urlNode != null) {
                val text = urlNode.text?.toString()
                urlNode.recycle()
                if (text != null && (text.startsWith("http://") || text.startsWith("https://") || text.contains("."))) {
                    return text
                }
            }
        } catch (e: Exception) {
            Log.e("BlockerService", "Error extracting URL", e)
        } finally {
            source.recycle()
        }
        return null
    }

    private fun findUrlNode(node: android.view.accessibility.AccessibilityNodeInfo): android.view.accessibility.AccessibilityNodeInfo? {
        // Check if this node might be the URL bar
        if (node.viewIdResourceName?.contains("url") == true ||
            node.viewIdResourceName?.contains("address") == true ||
            node.viewIdResourceName?.contains("mozac_browser_toolbar_url_view") == true) {
            val text = node.text?.toString()
            if (text != null && text.isNotBlank()) {
                return android.view.accessibility.AccessibilityNodeInfo.obtain(node)
            }
        }

        // Search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findUrlNode(child)
            child.recycle()
            if (result != null) return result
        }

        return null
    }

    private fun extractDomain(url: String): String? {
        return try {
            var normalizedUrl = url.trim()
            // Add protocol if missing for proper URL parsing
            if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
                normalizedUrl = "https://$normalizedUrl"
            }
            val uri = java.net.URI(normalizedUrl)
            uri.host?.lowercase()?.removePrefix("www.")
        } catch (e: Exception) {
            Log.e("BlockerService", "Error extracting domain from URL: $url", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun showBlockedNotification(pkg: String, reason: UsageTracker.BlockReason) {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled().not()) {
            Log.w("BlockerService", "Notifications disabled by user")
            return
        }

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            )
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }

        val contentText = when (reason) {
            UsageTracker.BlockReason.ROUTINE_LIMIT -> getString(
                R.string.blocked_by_routine,
                appName
            )

            else -> getString(R.string.reached_limit, appName)
        }

        val notification = NotificationCompat.Builder(this, BLOCKER_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_hourglass_disabled_24)
            .setContentTitle(getString(R.string.app_blocked))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(pkg.hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    private fun showFocusModeNotification(pkg: String) {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled().not()) return
        if (!prefs.getBoolean("focus_reminders", true)) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            )
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }

        val notification = NotificationCompat.Builder(this, BLOCKER_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_hourglass_disabled_24)
            .setContentTitle(getString(R.string.distraction_blocked))
            .setContentText(getString(R.string.you_were_using, appName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(pkg.hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    private fun showWebsiteBlockedNotification(domain: String) {
        if (NotificationManagerCompat.from(this).areNotificationsEnabled().not()) {
            Log.w("BlockerService", "Notifications disabled by user")
            return
        }

        val notification = NotificationCompat.Builder(this, BLOCKER_CHANNEL_ID)
            .setSmallIcon(R.drawable.round_hourglass_disabled_24)
            .setContentTitle(getString(R.string.website_blocked))
            .setContentText(getString(R.string.website_blocked_by_routine, domain))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(domain.hashCode(), notification)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(routineChangeReceiver)
        } catch (e: Exception) {
            Log.e("BlockerService", "Error unregistering receiver", e)
        }
    }
}
