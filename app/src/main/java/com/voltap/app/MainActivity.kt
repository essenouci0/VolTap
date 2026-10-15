package com.voltap.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.voltap.app.ui.ShotScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.voltap.app.ui.EditorScreen
import com.voltap.app.ui.HomeScreen
import com.voltap.app.ui.ProfilesScreen
import com.voltap.app.ui.SettingsScreen
import com.voltap.app.ui.SplashScreen
import com.voltap.app.ui.VolTapTheme

class MainActivity : ComponentActivity() {

    private var serviceEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VolTapStore.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val language = VolTapStore.settings.language
            val baseConfig = LocalConfiguration.current
            val localized = remember(language, baseConfig) { Lang.localized(this@MainActivity, language) }
            val config = localized.resources.configuration
            val direction = if (TextUtils.getLayoutDirectionFromLocale(config.locales[0]) == View.LAYOUT_DIRECTION_RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides config,
                LocalLayoutDirection provides direction,
            ) {
                VolTapTheme(VolTapStore.settings.theme) {
                    VolTapApp(
                        serviceEnabled = serviceEnabled,
                        onOpenAccessibility = ::openAccessibilitySettings,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = VolTapService.isEnabled(this)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}

@Composable
private fun VolTapApp(
    serviceEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
) {
    var route by rememberSaveable { mutableStateOf("splash") }

    BackHandler(enabled = route != "home" && route != "splash") {
        route = when {
            route.startsWith("editor:") -> "profiles"
            route.startsWith("shot:") -> "editor:" + route.removePrefix("shot:")
            else -> "home"
        }
    }

    Crossfade(targetState = route, animationSpec = tween(250), label = "route") { current ->
        when {
            current == "splash" -> SplashScreen { route = "home" }
            current == "home" -> HomeScreen(serviceEnabled, onOpenAccessibility) { route = it }
            current == "profiles" -> ProfilesScreen(go = { route = it }, back = { route = "home" })
            current == "settings" -> SettingsScreen(serviceEnabled, onOpenAccessibility) { route = "home" }
            current.startsWith("editor:") -> EditorScreen(
                packageName = current.removePrefix("editor:"),
                onPlace = { route = "shot:$it" },
                back = { route = "profiles" },
            )
            current.startsWith("shot:") -> ShotScreen(
                packageName = current.removePrefix("shot:"),
                back = { route = "editor:" + current.removePrefix("shot:") },
            )
            else -> HomeScreen(serviceEnabled, onOpenAccessibility) { route = it }
        }
    }
}
