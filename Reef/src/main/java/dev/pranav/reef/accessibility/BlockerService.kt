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

    // Caching for website URL checks to avoid performance issues
    private var lastCheckedUrl: String? = null
    private var lastUrlCheckTime: Long = 0
    private val URL_CHECK_THROTTLE_MS = 500L // Throttle URL checks to max 2 per second

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
            Log.d("BlockerService", "[DEBUG] Firefox package detected: $pkg, eventType=${event.eventType}, contentChangeTypes=${event.contentChangeTypes}")

            // Throttle URL checks to avoid performance issues
            val currentTime = System.currentTimeMillis()
            val shouldCheckUrl = currentTime - lastUrlCheckTime >= URL_CHECK_THROTTLE_MS

            if (!shouldCheckUrl) {
                Log.d("BlockerService", "[DEBUG] URL check throttled (${currentTime - lastUrlCheckTime}ms since last check)")
            } else {
                lastUrlCheckTime = currentTime
                val url = extractUrlFromEvent(event)
                Log.d("BlockerService", "[DEBUG] Extracted URL from event: '$url'")
                if (url == null) {
                    Log.d("BlockerService", "[DEBUG] URL extraction returned null - could not find URL bar node or text")
                } else if (url == lastCheckedUrl) {
                    Log.d("BlockerService", "[DEBUG] URL unchanged from last check, skipping")
                } else {
                    lastCheckedUrl = url
                    val domain = extractDomain(url)
                    Log.d("BlockerService", "[DEBUG] Extracted domain: '$domain' from URL: '$url'")
                    if (domain == null) {
                        Log.d("BlockerService", "[DEBUG] Domain extraction failed for URL: '$url'")
                    } else {
                        val isBlocked = Routines.isWebsiteBlocked(domain)
                        Log.d("BlockerService", "[DEBUG] isWebsiteBlocked('$domain') = $isBlocked")
                        val sessions = Routines.getActiveSessions()
                        Log.d("BlockerService", "[DEBUG] Active sessions: ${sessions.size}")
                        sessions.forEach { session ->
                            Log.d("BlockerService", "[DEBUG]   Session ${session.routineId}: websiteLimits=${session.websiteLimits}")
                        }
                        if (isBlocked) {
                            Log.d("BlockerService", "Blocking website $domain in Firefox")
                            performGlobalAction(GLOBAL_ACTION_HOME)
                            showWebsiteBlockedNotification(domain)
                            return
                        }
                    }
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
        // Use rootInActiveWindow instead of event.source because the URL bar is in the
        // toolbar (a sibling of the WebView), not inside the WebView where content changes fire.
        val root = rootInActiveWindow
        if (root == null) {
            Log.d("BlockerService", "[DEBUG] extractUrlFromEvent: rootInActiveWindow is null")
            return null
        }

        try {
            val pkg = event.packageName?.toString() ?: return null

            // Use efficient direct viewId lookup for known Firefox URL bar IDs
            val knownUrlViewIds = listOf(
                "$pkg:id/mozac_browser_toolbar_url_view",
                "$pkg:id/url_bar_title",
                "$pkg:id/display_url"
            )

            for (viewId in knownUrlViewIds) {
                val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
                if (nodes.isNullOrEmpty()) continue

                for (node in nodes) {
                    try {
                        val text = node.text?.toString()
                        Log.d("BlockerService", "[DEBUG] extractUrlFromEvent: found node viewId='$viewId', text='$text'")
                        if (text != null && isValidUrlFormat(text)) {
                            return text
                        }
                    } finally {
                        node.recycle()
                    }
                }
            }

            Log.d("BlockerService", "[DEBUG] extractUrlFromEvent: no URL node found via direct viewId lookup")
        } catch (e: Exception) {
            Log.e("BlockerService", "Error extracting URL", e)
        } finally {
            root.recycle()
        }
        return null
    }

    private fun isValidUrlFormat(text: String): Boolean {
        // Check for valid URL format: should start with http(s):// or look like a domain
        val trimmed = text.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return true
        }
        // Check if it looks like a domain (word.word pattern, no spaces)
        if (trimmed.contains(" ") || trimmed.length < 4) {
            return false
        }
        // Simple domain pattern: contains a dot and no invalid characters
        val domainPattern = Regex("^[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}(/.*)?$")
        return domainPattern.matches(trimmed)
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
