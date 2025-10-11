package dev.pranav.reef.util

import android.content.SharedPreferences

const val CHANNEL_ID = "content_blocker"
lateinit var prefs: SharedPreferences

val isPrefsInitialized: Boolean
    get() = ::prefs.isInitialized
