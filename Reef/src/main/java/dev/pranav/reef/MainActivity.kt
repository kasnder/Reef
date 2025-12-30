package dev.pranav.reef

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.pranav.reef.intro.AppIntroActivity
import dev.pranav.reef.ui.ReefTheme
import dev.pranav.reef.util.*

class MainActivity: ComponentActivity() {
    private var pendingFocusModeStart = false
    private var hasCheckedPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyDefaults()
        addExceptions()

        setContent {
            ReefTheme {
                MainScreen(
                    onNavigateToTimer = {
                        startActivity(Intent(this, TimerActivity::class.java))
                    },
                    onNavigateToUsage = {
                        startActivity(Intent(this, AppUsageActivity::class.java))
                    },
                    onNavigateToRoutines = {
                        startActivity(Intent(this, RoutinesActivity::class.java))
                    },
                    onNavigateToWhitelist = {
                        startActivity(Intent(this, WhitelistActivity::class.java))
                    },
                    onNavigateToSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onNavigateToIntro = {
                        startActivity(Intent(this, AppIntroActivity::class.java))
                    },
                    onRequestAccessibility = {
                        pendingFocusModeStart = true
                        showAccessibilityDialog()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!hasCheckedPermissions && !prefs.getBoolean("first_run", true)) {
            hasCheckedPermissions = true
            checkAndRequestMissingPermissions()
        }

        if (pendingFocusModeStart && isAccessibilityServiceEnabledForBlocker()) {
            pendingFocusModeStart = false
            startActivity(Intent(this, TimerActivity::class.java))
        }
    }

    private fun addExceptions() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)

        packageManager.queryIntentActivities(intent, 0).forEach {
            val packageName = it.activityInfo.packageName
            if (!Whitelist.isWhitelisted(packageName)) {
                Whitelist.whitelist(packageName)
            }
        }
    }
}
