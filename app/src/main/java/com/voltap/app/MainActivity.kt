package com.voltap.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
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
            VolTapTheme(VolTapStore.settings.theme) {
                VolTapApp(
                    serviceEnabled = serviceEnabled,
                    onOpenAccessibility = ::openAccessibilitySettings,
                    onPlace = ::startPlacement,
                )
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

    /** Opens the app and asks the service to show the draggable targets over it. */
    private fun startPlacement(packageName: String) {
        val service = VolTapService.instance
        if (service == null) {
            Toast.makeText(this, "Turn on the VolTap accessibility service first", Toast.LENGTH_LONG).show()
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) {
            Toast.makeText(this, "This app can't be opened", Toast.LENGTH_LONG).show()
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launch)
        service.startPlacement(packageName)
    }
}

@Composable
private fun VolTapApp(
    serviceEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onPlace: (String) -> Unit,
) {
    var route by rememberSaveable { mutableStateOf("splash") }

    BackHandler(enabled = route != "home" && route != "splash") {
        route = when {
            route.startsWith("editor:") -> "profiles"
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
                serviceEnabled = serviceEnabled,
                onPlace = onPlace,
                back = { route = "profiles" },
            )
            else -> HomeScreen(serviceEnabled, onOpenAccessibility) { route = it }
        }
    }
}
