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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.voltap.app.Positions
import com.voltap.app.R
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
            BrandName(52, Color.White, Color(0xFFC4C8FF))
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
        ) { VtText(stringResource(R.string.get_started), 16, color = Color(0xFF3B43C7)) }
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
            BrandName(24, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(c.card)
                    .border(1.dp, c.border, RoundedCornerShape(22.dp))
                    .clickable { go("settings") }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { VtText(stringResource(R.string.settings), 14) }
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
                VtText(stringResource(if (on) R.string.status_active else R.string.status_paused), 12, color = Color.White)
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
                stringResource(R.string.vol_up),
                13,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 96.dp, end = 24.dp),
            )
            VtText(
                stringResource(R.string.vol_down),
                13,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 96.dp, start = 24.dp),
            )
            VtText(
                stringResource(R.string.hero_hint),
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
                stringResource(if (on) R.string.keys_mapped else R.string.keys_normal),
                20,
                align = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            VtText(
                stringResource(if (on) R.string.keys_mapped_sub else R.string.keys_normal_sub),
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
                    VtText(stringResource(R.string.acc_off_title), 16)
                    VtText(
                        stringResource(R.string.acc_off_body),
                        13,
                        FontWeight.Medium,
                        c.sub,
                    )
                    Spacer(Modifier.height(6.dp))
                    PillButton(stringResource(R.string.acc_open), onClick = onOpenAccessibility)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        VtCard {
            NavRow(
                stringResource(R.string.app_profiles),
                stringResource(
                    R.string.profiles_sub,
                    LocalContext.current.resources.getQuantityString(
                        R.plurals.apps_count,
                        VolTapStore.profiles.size,
                        VolTapStore.profiles.size,
                    ),
                ),
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

    ScreenColumn(stringResource(R.string.app_profiles), back) {
        VtCard {
            SwitchRow(
                stringResource(R.string.auto_start),
                stringResource(if (settings.autoStart) R.string.auto_start_on else R.string.auto_start_off),
                settings.autoStart,
                divider = false,
            ) { value -> VolTapStore.updateSettings { it.copy(autoStart = value) } }
        }

        SectionLabel(stringResource(R.string.your_apps, profiles.size))
        if (profiles.isEmpty()) {
            VtText(
                stringResource(R.string.no_apps),
                14,
                FontWeight.Medium,
                c.sub,
                Modifier.padding(horizontal = 4.dp),
            )
        } else {
            VtCard {
                profiles.forEachIndexed { index, profile ->
                    val setText = stringResource(
                        when {
                            profile.portrait != null && profile.landscape != null -> R.string.sub_both_set
                            profile.portrait != null -> R.string.sub_portrait_set
                            profile.landscape != null -> R.string.sub_landscape_set
                            else -> R.string.positions_not_set
                        },
                    )
                    val repeatText = stringResource(R.string.hold_to_repeat)
                    val offText = stringResource(R.string.off)
                    val sub = buildString {
                        append(setText)
                        if (profile.repeat) append(" · ").append(repeatText)
                        if (!profile.auto) append(" · ").append(offText)
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
        ) { VtText("+  " + stringResource(R.string.add_app), 15, color = c.accent) }
        Spacer(Modifier.height(14.dp))
        VtText(
            stringResource(R.string.profiles_footer),
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
            confirmButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.choose_app)) },
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

@Composable
private fun describe(positions: Positions?): String =
    if (positions == null) stringResource(R.string.not_set)
    else stringResource(
        R.string.pos_summary,
        positions.up.x, positions.up.y, positions.down.x, positions.down.y,
    )

@Composable
fun EditorScreen(
    packageName: String,
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
                stringResource(R.string.start_auto),
                stringResource(R.string.start_auto_sub),
                profile.auto,
                divider = false,
            ) { value -> change { it.copy(auto = value) } }
        }

        SectionLabel(stringResource(R.string.tap_positions))
        VtCard {
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VtText(stringResource(R.string.portrait), 13, color = c.sub)
                VtText(describe(profile.portrait), 14)
                HorizontalDivider(color = c.border)
                VtText(stringResource(R.string.landscape), 13, color = c.sub)
                VtText(describe(profile.landscape), 14)
                VtText(
                    stringResource(R.string.shot_hint),
                    12,
                    FontWeight.Medium,
                    c.sub,
                )
                PillButton(
                    stringResource(if (profile.portrait != null || profile.landscape != null) R.string.change_positions else R.string.choose_shot),
                    onClick = { onPlace(packageName) },
                )
            }
        }

        SectionLabel(stringResource(R.string.in_this_app))
        VtCard {
            SwitchRow(
                stringResource(R.string.hold_to_repeat),
                stringResource(R.string.repeat_sub),
                profile.repeat,
            ) { value -> change { it.copy(repeat = value) } }
            if (profile.repeat) {
                Segmented(
                    stringResource(R.string.repeat_speed),
                    listOf(stringResource(R.string.slow), stringResource(R.string.normal), stringResource(R.string.fast)),
                    profile.repeatSpeed,
                ) { index -> change { it.copy(repeatSpeed = index) } }
            }
            SwitchRow(
                stringResource(R.string.hide_targets),
                stringResource(R.string.hide_targets_sub),
                profile.hideTargets,
            ) { value -> change { it.copy(hideTargets = value) } }
            SwitchRow(
                stringResource(R.string.lock_volume),
                stringResource(R.string.lock_volume_sub),
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
                VtText(stringResource(R.string.repeat_warn_title), 14, color = c.warnText)
                VtText(
                    stringResource(R.string.repeat_warn_body),
                    13,
                    FontWeight.Medium,
                    c.warnText,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PillButton(stringResource(R.string.done), onClick = back)
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
        ) { VtText(stringResource(R.string.remove_app), 14, color = c.danger) }
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
    val testingText = stringResource(R.string.testing_dots)
    val failedText = stringResource(R.string.test_failed)
    val serviceFirst = stringResource(R.string.toast_service_first)
    val resources = LocalContext.current.resources
    var running by remember { mutableStateOf(false) }

    ScreenColumn(stringResource(R.string.settings), back) {
        SectionLabel(stringResource(R.string.service))
        VtCard {
            Row(
                Modifier.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    VtText(stringResource(R.string.accessibility_service), 15)
                    VtText(stringResource(R.string.needed_sub), 13, FontWeight.Medium, c.sub)
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (serviceEnabled) Color(0xFFE6F6EF) else Color(0xFFFDE9EE))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    VtText(
                        stringResource(if (serviceEnabled) R.string.connected else R.string.off),
                        12,
                        color = if (serviceEnabled) Color(0xFF137A51) else Color(0xFFA8324F),
                    )
                }
            }
            HorizontalDivider(color = c.border)
            NavRow(stringResource(R.string.open_system_acc), null, divider = false, onClick = onOpenAccessibility)
        }

        SectionLabel(stringResource(R.string.input_engine))
        VtCard {
            Segmented(
                stringResource(R.string.input_engine),
                listOf(stringResource(R.string.standard), stringResource(R.string.fast_soon)),
                0,
                disabled = setOf(1),
            ) {}
            Row(
                Modifier.padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    VtText(stringResource(R.string.latency_test), 15)
                    VtText(
                        latency ?: stringResource(R.string.latency_sub),
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
                                Toast.makeText(context, serviceFirst, Toast.LENGTH_LONG).show()
                            } else {
                                running = true
                                latency = testingText
                                service.measureLatency { ms ->
                                    running = false
                                    latency = if (ms == null) failedText else resources.getString(R.string.latency_result, "%.1f".format(ms))
                                }
                            }
                        }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) { VtText(stringResource(if (running) R.string.testing else R.string.run_test), 14, color = Color.White) }
            }
        }

        SectionLabel(stringResource(R.string.behavior))
        VtCard {
            Segmented(stringResource(R.string.tap_speed), listOf(stringResource(R.string.slow), stringResource(R.string.normal), stringResource(R.string.fast)), s.tapSpeed) { index ->
                VolTapStore.updateSettings { it.copy(tapSpeed = index) }
            }
            SwitchRow(stringResource(R.string.show_targets), stringResource(R.string.show_targets_sub), s.showTargets) { value ->
                VolTapStore.updateSettings { it.copy(showTargets = value) }
            }
            SwitchRow(stringResource(R.string.vibrate), stringResource(R.string.vibrate_sub), s.vibrate, divider = false) { value ->
                VolTapStore.updateSettings { it.copy(vibrate = value) }
            }
        }

        SectionLabel(stringResource(R.string.auto_start))
        VtCard {
            SwitchRow(stringResource(R.string.show_notice), stringResource(R.string.show_notice_sub), s.showNotice) { value ->
                VolTapStore.updateSettings { it.copy(showNotice = value) }
            }
            SwitchRow(
                stringResource(R.string.ignore_transient),
                stringResource(R.string.ignore_transient_sub),
                s.ignoreTransient,
                divider = false,
            ) { value -> VolTapStore.updateSettings { it.copy(ignoreTransient = value) } }
        }

        SectionLabel(stringResource(R.string.appearance))
        VtCard {
            Segmented(
                stringResource(R.string.theme),
                listOf(stringResource(R.string.light), stringResource(R.string.dark), stringResource(R.string.system)),
                s.theme,
            ) { index -> VolTapStore.updateSettings { it.copy(theme = index) } }
            Segmented(
                stringResource(R.string.language),
                listOf(stringResource(R.string.system), stringResource(R.string.lang_english), stringResource(R.string.lang_arabic)),
                s.language,
                divider = false,
            ) { index -> VolTapStore.updateSettings { it.copy(language = index) } }
        }
    }
}
