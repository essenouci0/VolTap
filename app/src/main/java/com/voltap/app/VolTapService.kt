package com.voltap.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

/**
 * Turns the volume keys into taps while a profiled app is in the foreground.
 *
 * - Foreground app is tracked from window-state events (no polling).
 * - Volume keys are received through onKeyEvent and answered with a tap on press.
 * - Gestures are prepared once per position and reused, so a key press costs one dispatchGesture call.
 */
class VolTapService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: VolTapService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val me = ComponentName(context, VolTapService::class.java)
            return enabled.split(':').any {
                it.equals(me.flattenToString(), ignoreCase = true) ||
                    it.equals(me.flattenToShortString(), ignoreCase = true)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val gestureCache = HashMap<Triple<Int, Int, Long>, GestureDescription>()

    private var currentPackage: String = ""
    private var active: Profile? = null
    private var placement: PlacementOverlay? = null
    private var markers: MarkersOverlay? = null

    private var repeatPoint: TapPoint? = null
    private var repeatIntervalMs: Long = 120L

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val point = repeatPoint ?: return
            tap(point)
            handler.postDelayed(this, repeatIntervalMs)
        }
    }

    // ---- lifecycle ---------------------------------------------------------

    override fun onServiceConnected() {
        super.onServiceConnected()
        VolTapStore.init(applicationContext)
        instance = this
        markers = MarkersOverlay(this)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    override fun onInterrupt() {
        stopRepeat()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        gestureCache.clear()
        refreshMarkers()
    }

    private fun shutdown() {
        stopRepeat()
        placement?.close()
        placement = null
        markers?.hide()
        markers = null
        active = null
        instance = null
    }

    // ---- foreground app tracking --------------------------------------------

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (placement != null) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString().orEmpty()

        // Our own overlay windows must not count as "leaving the app".
        if (pkg == packageName && !cls.endsWith("MainActivity")) return

        if (VolTapStore.settings.ignoreTransient && isTransient(pkg, cls)) return

        if (pkg != currentPackage) {
            currentPackage = pkg
            updateActivation()
        }
    }

    private fun isTransient(pkg: String, cls: String): Boolean {
        if (pkg == "com.android.systemui") return true
        if (cls.startsWith("android.inputmethodservice")) return true
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }

    private fun resolveActive(): Profile? {
        val s = VolTapStore.settings
        if (!s.masterEnabled || !s.autoStart) return null
        return VolTapStore.profiles.firstOrNull { it.packageName == currentPackage && it.auto }
    }

    private fun updateActivation() {
        val next = resolveActive()
        val changed = next?.packageName != active?.packageName
        active = next
        stopRepeat()
        refreshMarkers()
        if (changed && next != null && VolTapStore.settings.showNotice) {
            Toast.makeText(this, "VolTap active · ${next.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshMarkers() {
        val profile = resolveActive()
        val positions = profile?.positions(isLandscape())
        if (profile != null && positions != null && VolTapStore.settings.showTargets && !profile.hideTargets) {
            markers?.show(positions)
        } else {
            markers?.hide()
        }
    }

    // ---- volume keys ----------------------------------------------------------

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        if (placement != null) return false

        val profile = resolveActive() ?: return false
        val positions = profile.positions(isLandscape()) ?: return false
        val point = if (code == KeyEvent.KEYCODE_VOLUME_UP) positions.up else positions.down

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    tap(point)
                    if (profile.repeat) startRepeat(point, profile.repeatIntervalMs)
                }
            }
            KeyEvent.ACTION_UP -> stopRepeat()
        }
        // true = the key is consumed, so the system volume does not change.
        return profile.lockVolume
    }

    private fun tap(point: TapPoint) {
        val duration = VolTapStore.settings.tapDurationMs
        val gesture = gestureCache.getOrPut(Triple(point.x, point.y, duration)) {
            val path = Path().apply { moveTo(point.x.toFloat(), point.y.toFloat()) }
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
        }
        dispatchGesture(gesture, null, null)
        if (VolTapStore.settings.vibrate) {
            val vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun startRepeat(point: TapPoint, intervalMs: Long) {
        stopRepeat()
        repeatPoint = point
        repeatIntervalMs = intervalMs
        handler.postDelayed(repeatRunnable, intervalMs)
    }

    private fun stopRepeat() {
        handler.removeCallbacks(repeatRunnable)
        repeatPoint = null
    }

    // ---- placing the targets ---------------------------------------------------

    /** Shows the draggable targets over the app that was just opened. */
    fun startPlacement(packageName: String, delayMs: Long = 900L) {
        handler.postDelayed({
            val profile = VolTapStore.profiles.firstOrNull { it.packageName == packageName }
                ?: return@postDelayed
            placement?.close()
            stopRepeat()
            markers?.hide()
            active = null
            placement = PlacementOverlay(this, profile, isLandscape()) {
                placement = null
                updateActivation()
            }.also { it.show() }
        }, delayMs)
    }

    // ---- latency test ------------------------------------------------------------

    /**
     * Sends [rounds] taps at the top-left corner and reports the median time (ms)
     * between dispatchGesture and the system confirming the gesture finished.
     */
    fun measureLatency(rounds: Int = 10, onDone: (Float?) -> Unit) {
        val times = ArrayList<Float>()
        fun next(i: Int) {
            if (i >= rounds) {
                onDone(if (times.isEmpty()) null else times.sorted()[times.size / 2])
                return
            }
            val path = Path().apply { moveTo(2f, 2f) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 1))
                .build()
            val start = System.nanoTime()
            val accepted = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        times.add((System.nanoTime() - start) / 1_000_000f)
                        next(i + 1)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        next(i + 1)
                    }
                },
                handler,
            )
            if (!accepted) onDone(null)
        }
        next(0)
    }

    // ---- helpers -----------------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun isLandscape(): Boolean {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return metrics.widthPixels > metrics.heightPixels
    }
}
