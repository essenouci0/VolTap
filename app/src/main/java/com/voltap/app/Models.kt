package com.voltap.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

data class TapPoint(val x: Int, val y: Int)

data class Positions(val up: TapPoint, val down: TapPoint)

/** Settings of one app (or game). Positions are stored per screen orientation. */
data class Profile(
    val packageName: String,
    val label: String,
    val auto: Boolean = true,
    val repeat: Boolean = false,
    /** 0 = slow, 1 = normal, 2 = fast */
    val repeatSpeed: Int = 1,
    val hideTargets: Boolean = false,
    val lockVolume: Boolean = true,
    val portrait: Positions? = null,
    val landscape: Positions? = null,
) {
    fun positions(landscape: Boolean): Positions? = if (landscape) this.landscape else this.portrait

    val repeatIntervalMs: Long
        get() = when (repeatSpeed) {
            0 -> 250L
            2 -> 60L
            else -> 120L
        }
}

data class AppSettings(
    val masterEnabled: Boolean = true,
    val autoStart: Boolean = true,
    /** 0 = slow, 1 = normal, 2 = fast (length of the injected touch) */
    val tapSpeed: Int = 1,
    val showTargets: Boolean = true,
    val vibrate: Boolean = false,
    val showNotice: Boolean = true,
    val ignoreTransient: Boolean = true,
    /** 0 = light, 1 = dark, 2 = system */
    val theme: Int = 0,
) {
    val tapDurationMs: Long
        get() = when (tapSpeed) {
            0 -> 30L
            2 -> 2L
            else -> 10L
        }
}

/**
 * Single source of truth shared by the UI and the accessibility service
 * (both run in the same process). Backed by SharedPreferences.
 */
object VolTapStore {
    private var prefs: SharedPreferences? = null

    var profiles by mutableStateOf<List<Profile>>(emptyList())
        private set

    var settings by mutableStateOf(AppSettings())
        private set

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("voltap", Context.MODE_PRIVATE)
        load()
    }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        settings = change(settings)
        save()
    }

    fun addProfile(profile: Profile) {
        if (profiles.any { it.packageName == profile.packageName }) return
        profiles = profiles + profile
        save()
    }

    fun updateProfile(packageName: String, change: (Profile) -> Profile) {
        profiles = profiles.map { if (it.packageName == packageName) change(it) else it }
        save()
    }

    fun removeProfile(packageName: String) {
        profiles = profiles.filterNot { it.packageName == packageName }
        save()
    }

    // ---- persistence -------------------------------------------------------

    private fun save() {
        val p = prefs ?: return
        val s = settings
        val settingsJson = JSONObject()
            .put("masterEnabled", s.masterEnabled)
            .put("autoStart", s.autoStart)
            .put("tapSpeed", s.tapSpeed)
            .put("showTargets", s.showTargets)
            .put("vibrate", s.vibrate)
            .put("showNotice", s.showNotice)
            .put("ignoreTransient", s.ignoreTransient)
            .put("theme", s.theme)
        val array = JSONArray()
        profiles.forEach { array.put(profileToJson(it)) }
        p.edit()
            .putString("settings", settingsJson.toString())
            .putString("profiles", array.toString())
            .apply()
    }

    private fun load() {
        val p = prefs ?: return
        runCatching {
            p.getString("settings", null)?.let { raw ->
                val o = JSONObject(raw)
                val d = AppSettings()
                settings = AppSettings(
                    masterEnabled = o.optBoolean("masterEnabled", d.masterEnabled),
                    autoStart = o.optBoolean("autoStart", d.autoStart),
                    tapSpeed = o.optInt("tapSpeed", d.tapSpeed),
                    showTargets = o.optBoolean("showTargets", d.showTargets),
                    vibrate = o.optBoolean("vibrate", d.vibrate),
                    showNotice = o.optBoolean("showNotice", d.showNotice),
                    ignoreTransient = o.optBoolean("ignoreTransient", d.ignoreTransient),
                    theme = o.optInt("theme", d.theme),
                )
            }
        }
        runCatching {
            p.getString("profiles", null)?.let { raw ->
                val array = JSONArray(raw)
                profiles = (0 until array.length()).map { profileFromJson(array.getJSONObject(it)) }
            }
        }
    }

    private fun profileToJson(p: Profile): JSONObject {
        val o = JSONObject()
            .put("pkg", p.packageName)
            .put("label", p.label)
            .put("auto", p.auto)
            .put("repeat", p.repeat)
            .put("repeatSpeed", p.repeatSpeed)
            .put("hideTargets", p.hideTargets)
            .put("lockVolume", p.lockVolume)
        p.portrait?.let { o.put("portrait", positionsToJson(it)) }
        p.landscape?.let { o.put("landscape", positionsToJson(it)) }
        return o
    }

    private fun profileFromJson(o: JSONObject): Profile = Profile(
        packageName = o.getString("pkg"),
        label = o.optString("label", o.getString("pkg")),
        auto = o.optBoolean("auto", true),
        repeat = o.optBoolean("repeat", false),
        repeatSpeed = o.optInt("repeatSpeed", 1),
        hideTargets = o.optBoolean("hideTargets", false),
        lockVolume = o.optBoolean("lockVolume", true),
        portrait = o.optJSONObject("portrait")?.let(::positionsFromJson),
        landscape = o.optJSONObject("landscape")?.let(::positionsFromJson),
    )

    private fun positionsToJson(p: Positions) = JSONObject()
        .put("ux", p.up.x).put("uy", p.up.y)
        .put("dx", p.down.x).put("dy", p.down.y)

    private fun positionsFromJson(o: JSONObject) = Positions(
        up = TapPoint(o.getInt("ux"), o.getInt("uy")),
        down = TapPoint(o.getInt("dx"), o.getInt("dy")),
    )
}
