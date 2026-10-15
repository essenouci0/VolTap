package com.voltap.app.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltap.app.Positions
import com.voltap.app.Profile
import com.voltap.app.VolTapService
import com.voltap.app.VolTapStore

// ---------------------------------------------------------------------------------------------
// Splash
// ---------------------------------------------------------------------------------------------

@Composable
fun SplashScreen(onStart: () -> Unit) {
    val c = LocalVt.current
    Box(
        Modifier
            .fillMaxSize()
            .background(c.accent)
            .systemBarsPadding()
            .padding(20.dp),
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoCapsule(
                modifier = Modifier
                    .width(120.dp)
                    .height(268.dp),
                marks = Color(0xFF6C74EE),
                ring = c.accent,
                animate = true,
            )
            Spacer(Modifier.height(28.dp))
            Row {
                VtText("Vol", 52, FontWeight.ExtraBold, Color.White)
                VtText("Tap", 52, FontWeight.ExtraBold, Color(0xFFC4C8FF))
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Color.White)
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center,
        ) { VtText("Get started", 16, color = Color(0xFF3B43C7)) }
    }
}

// ---------------------------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------------------------

@Composable
fun HomeScreen(serviceEnabled: Boolean, onOpenAccessibility: () -> Unit, go: (String) -> Unit) {
    val c = LocalVt.current
    val settings = VolTapStore.settings
    val on = settings.masterEnabled
    val heroBg by animateColorAsState(if (on) c.accent else c.paused, tween(500), label = "hero")
    val ringColor by animateColorAsState(if (on) c.accent else Color(0xFF8A8FA8), tween(400), label = "ring")

    ScreenColumn(null, null) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoCapsule(
                modifier = Modifier
                    .width(16.dp)
                    .height(36.dp),
                capsule = c.accent,
                marks = Color.White.copy(alpha = 0.6f),
                ring = Color.White,
            )
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f)) {
                VtText("Vol", 24, FontWeight.ExtraBold)
                VtText("Tap", 24, FontWeight.ExtraBold, c.accent)
            }
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.card)
                    .border(1.dp, c.border, RoundedCornerShape(22.dp))
                    .clickable { go("settings") }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { VtText("Settings", 14) }
        }
        Spacer(Modifier.height(20.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(380.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(heroBg),
        ) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (on) Color(0xFF7CF0B6) else Color(0xFFD9DBF2)),
                )
                Spacer(Modifier.width(8.dp))
                VtText(if (on) "Active" else "Paused", 12, color = Color.White)
            }
            LogoCapsule(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(100.dp)
                    .height(224.dp),
                marks = Color(0xFF6C74EE),
                ring = ringColor,
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(76.dp)
                    .clip(CircleShape)
                    .clickable {
                        VolTapStore.updateSettings { it.copy(masterEnabled = !it.masterEnabled) }
                    },
            )
            VtText(
                "Volume up",
                13,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 96.dp, end = 24.dp),
            )
            VtText(
                "Volume down",
                13,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 96.dp, start = 24.dp),
            )
            VtText(
                "Tap the ring to turn VolTap on or off",
                12,
                FontWeight.Medium,
                Color.White.copy(alpha = 0.85f),
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
            )
        }

        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            VtText(
                if (on) "Volume keys are mapped" else "Volume keys work normally",
                20,
                align = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            VtText(
                if (on) "They tap while a saved app is open" else "Mappings are paused",
                14,
                FontWeight.Medium,
                c.sub,
                align = TextAlign.Center,
            )
        }

        if (!serviceEnabled) {
            Spacer(Modifier.height(20.dp))
            VtCard(Modifier.padding(0.dp)) {
                Column(Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VtText("Accessibility service is off", 16)
                    VtText(
                        "VolTap needs it to see the volume keys and to tap. Open Accessibility settings, choose VolTap and switch it on.",
                        13,
                        FontWeight.Medium,
                        c.sub,
                    )
                    Spacer(Modifier.height(6.dp))
                    PillButton("Open Accessibility settings", onClick = onOpenAccessibility)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        VtCard {
            NavRow(
                "App profiles",
                "${VolTapStore.profiles.size} apps · starts automatically",
                divider = false,
            ) { go("profiles") }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// App profiles
// ---------------------------------------------------------------------------------------------

private data class InstalledApp(val packageName: String, val label: String)

private fun launchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun ProfilesScreen(go: (String) -> Unit, back: () -> Unit) {
    val c = LocalVt.current
    val context = LocalContext.current
    val settings = VolTapStore.settings
    val profiles = VolTapStore.profiles
    var picking by remember { mutableStateOf(false) }

    ScreenColumn("App profiles", back) {
        VtCard {
            SwitchRow(
                "Auto-start",
                if (settings.autoStart) "Starts when you open a saved app" else "Turn on to start with your apps",
                settings.autoStart,
                divider = false,
            ) { value -> VolTapStore.updateSettings { it.copy(autoStart = value) } }
        }

        SectionLabel("Your apps · ${profiles.size}")
        if (profiles.isEmpty()) {
            VtText(
                "No apps yet. Add the game or app you want to control with the volume keys.",
                14,
                FontWeight.Medium,
                c.sub,
                Modifier.padding(horizontal = 4.dp),
            )
        } else {
            VtCard {
                profiles.forEachIndexed { index, profile ->
                    val saved = listOfNotNull(
                        if (profile.portrait != null) "Portrait" else null,
                        if (profile.landscape != null) "Landscape" else null,
                    )
                    val sub = buildString {
                        append(if (saved.isEmpty()) "Positions not set" else saved.joinToString(" + ") + " set")
                        if (profile.repeat) append(" · Hold to repeat")
                        if (!profile.auto) append(" · Off")
                    }
                    NavRow(profile.label, sub, divider = index != profiles.lastIndex) {
                        go("editor:" + profile.packageName)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .border(2.dp, c.accent.copy(alpha = 0.45f), RoundedCornerShape(30.dp))
                .clickable { picking = true },
            contentAlignment = Alignment.Center,
        ) { VtText("+  Add an app", 15, color = c.accent) }
        Spacer(Modifier.height(14.dp))
        VtText(
            "VolTap starts and stops with each app by itself.",
            12,
            FontWeight.SemiBold,
            c.sub,
            Modifier.fillMaxWidth(),
            TextAlign.Center,
        )
    }

    if (picking) {
        val apps = remember {
            val added = VolTapStore.profiles.map { it.packageName }.toSet()
            launchableApps(context).filter { it.packageName !in added }
        }
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
            title = { Text("Choose an app") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(apps) { app ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    VolTapStore.addProfile(Profile(app.packageName, app.label))
                                    picking = false
                                    go("editor:" + app.packageName)
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(app.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(app.packageName, fontSize = 12.sp)
                        }
                        HorizontalDivider()
                    }
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// One app's profile
// ---------------------------------------------------------------------------------------------

private fun describe(positions: Positions?): String =
    if (positions == null) "Not set"
    else "Up ${positions.up.x}, ${positions.up.y}  ·  Down ${positions.down.x}, ${positions.down.y}"

@Composable
fun EditorScreen(
    packageName: String,
    serviceEnabled: Boolean,
    onPlace: (String) -> Unit,
    back: () -> Unit,
) {
    val c = LocalVt.current
    val profile = VolTapStore.profiles.firstOrNull { it.packageName == packageName }
    if (profile == null) {
        LaunchedEffect(Unit) { back() }
        return
    }
    fun change(block: (Profile) -> Profile) = VolTapStore.updateProfile(packageName, block)

    ScreenColumn(profile.label, back) {
        VtCard {
            SwitchRow(
                "Start automatically",
                "Turns on when this app opens",
                profile.auto,
                divider = false,
            ) { value -> change { it.copy(auto = value) } }
        }

        SectionLabel("Tap positions")
        VtCard {
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VtText("Portrait", 13, color = c.sub)
                VtText(describe(profile.portrait), 14)
                HorizontalDivider(color = c.border)
                VtText("Landscape", 13, color = c.sub)
                VtText(describe(profile.landscape), 14)
                VtText(
                    "VolTap opens the app and shows two rings. Drag them onto the buttons you want tapped. Rotate your phone first to set landscape positions.",
                    12,
                    FontWeight.Medium,
                    c.sub,
                )
                PillButton(
                    if (serviceEnabled) "Set tap positions" else "Turn on the service first",
                    onClick = { onPlace(packageName) },
                )
            }
        }

        SectionLabel("In this app")
        VtCard {
            SwitchRow(
                "Hold to repeat",
                "Keep tapping while the key is held",
                profile.repeat,
            ) { value -> change { it.copy(repeat = value) } }
            if (profile.repeat) {
                Segmented(
                    "Repeat speed",
                    listOf("Slow", "Normal", "Fast"),
                    profile.repeatSpeed,
                ) { index -> change { it.copy(repeatSpeed = index) } }
            }
            SwitchRow(
                "Hide targets",
                "Nothing drawn over this app",
                profile.hideTargets,
            ) { value -> change { it.copy(hideTargets = value) } }
            SwitchRow(
                "Lock volume level",
                "Keys never change the sound",
                profile.lockVolume,
                divider = false,
            ) { value -> change { it.copy(lockVolume = value) } }
        }

        if (profile.repeat) {
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.warnBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                VtText("Use repeat with care", 14, color = c.warnText)
                VtText(
                    "Some online games block automated taps. Repeating taps can put your account at risk.",
                    13,
                    FontWeight.Medium,
                    c.warnText,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PillButton("Done", onClick = back)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable {
                    VolTapStore.removeProfile(packageName)
                    back()
                },
            contentAlignment = Alignment.Center,
        ) { VtText("Remove this app", 14, color = c.danger) }
    }
}

// ---------------------------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------------------------

@Composable
fun SettingsScreen(serviceEnabled: Boolean, onOpenAccessibility: () -> Unit, back: () -> Unit) {
    val c = LocalVt.current
    val context = LocalContext.current
    val s = VolTapStore.settings
    var latency by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }

    ScreenColumn("Settings", back) {
        SectionLabel("Service")
        VtCard {
            Row(
                Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    VtText("Accessibility service", 15)
                    VtText("Needed to see the keys and tap", 13, FontWeight.Medium, c.sub)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (serviceEnabled) Color(0xFFE6F6EF) else Color(0xFFFDE9EE))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    VtText(
                        if (serviceEnabled) "Connected" else "Off",
                        12,
                        color = if (serviceEnabled) Color(0xFF137A51) else Color(0xFFA8324F),
                    )
                }
            }
            HorizontalDivider(color = c.border)
            NavRow("Open system Accessibility settings", null, divider = false, onClick = onOpenAccessibility)
        }

        SectionLabel("Input engine")
        VtCard {
            Segmented(
                "Input engine",
                listOf("Standard", "Fast (Shizuku, soon)"),
                0,
                disabled = setOf(1),
            ) {}
            Row(
                Modifier.padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    VtText("Latency test", 15)
                    VtText(
                        latency ?: "Key-to-tap time on this phone",
                        13,
                        FontWeight.Medium,
                        c.sub,
                    )
                }
                Box(
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.accent)
                        .clickable(enabled = !running) {
                            val service = VolTapService.instance
                            if (service == null) {
                                Toast.makeText(context, "Turn on the accessibility service first", Toast.LENGTH_LONG).show()
                            } else {
                                running = true
                                latency = "Testing…"
                                service.measureLatency { ms ->
                                    running = false
                                    latency = if (ms == null) "Test failed" else "About ${"%.1f".format(ms)} ms (median of 10)"
                                }
                            }
                        }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) { VtText(if (running) "Testing" else "Run test", 14, color = Color.White) }
            }
        }

        SectionLabel("Behavior")
        VtCard {
            Segmented("Tap speed", listOf("Slow", "Normal", "Fast"), s.tapSpeed) { index ->
                VolTapStore.updateSettings { it.copy(tapSpeed = index) }
            }
            SwitchRow("Show targets", "Faint rings over other apps", s.showTargets) { value ->
                VolTapStore.updateSettings { it.copy(showTargets = value) }
            }
            SwitchRow("Vibrate on tap", "A short pulse after each tap", s.vibrate, divider = false) { value ->
                VolTapStore.updateSettings { it.copy(vibrate = value) }
            }
        }

        SectionLabel("Auto-start")
        VtCard {
            SwitchRow("Show start notice", "A brief note when an app profile starts", s.showNotice) { value ->
                VolTapStore.updateSettings { it.copy(showNotice = value) }
            }
            SwitchRow(
                "Ignore keyboard and shade",
                "They don't count as leaving the app",
                s.ignoreTransient,
                divider = false,
            ) { value -> VolTapStore.updateSettings { it.copy(ignoreTransient = value) } }
        }

        SectionLabel("Appearance")
        VtCard {
            Segmented("Theme", listOf("Light", "Dark", "System"), s.theme, divider = false) { index ->
                VolTapStore.updateSettings { it.copy(theme = index) }
            }
        }
    }
}
