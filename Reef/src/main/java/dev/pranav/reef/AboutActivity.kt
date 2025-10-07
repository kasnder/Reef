package dev.pranav.reef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.pranav.reef.ui.ReefTheme
import dev.pranav.reef.ui.about.AboutScreen
import dev.pranav.reef.util.applyDefaults

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()
        super.onCreate(savedInstanceState)

        setContent {
            ReefTheme {
                AboutScreen(
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}

