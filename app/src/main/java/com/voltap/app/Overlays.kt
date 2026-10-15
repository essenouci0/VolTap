package com.voltap.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private const val INDIGO = 0xFF4B53E0.toInt()
private const val CORAL = 0xFFE0486B.toInt()
private const val INK = 0xFF12131F.toInt()
private const val SOFT = 0xFFF0F1F8.toInt()

private fun fullScreenParams(touchable: Boolean): WindowManager.LayoutParams {
    var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    val lp = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        flags,
        PixelFormat.TRANSLUCENT,
    )
    lp.gravity = Gravity.TOP or Gravity.START
    if (Build.VERSION.SDK_INT >= 28) {
        lp.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
    return lp
}

/**
 * Faint rings drawn over the app at the saved tap positions.
 * The window ignores touches, so the app underneath keeps working.
 */
class MarkersOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: MarkersView? = null

    fun show(positions: Positions) {
        hide()
        val v = MarkersView(context, positions)
        try {
            wm.addView(v, fullScreenParams(touchable = false))
            view = v
        } catch (_: Exception) {
            view = null
        }
    }

    fun hide() {
        view?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        view = null
    }

    private class MarkersView(context: Context, private val positions: Positions) : View(context) {
        private val density = context.resources.displayMetrics.density
        private val upPaint = paint(INDIGO)
        private val downPaint = paint(CORAL)
        private val loc = IntArray(2)

        private fun paint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            this.color = color
            alpha = 90
        }

        override fun onDraw(canvas: Canvas) {
            getLocationOnScreen(loc)
            val r = 18f * density
            canvas.drawCircle(positions.up.x - loc[0].toFloat(), positions.up.y - loc[1].toFloat(), r, upPaint)
            canvas.drawCircle(positions.down.x - loc[0].toFloat(), positions.down.y - loc[1].toFloat(), r, downPaint)
        }
    }
}

/**
 * Full-screen editor shown over the chosen app. Two rings are dragged onto the buttons
 * that should be tapped; arrow buttons move the selected ring one pixel at a time.
 */
class PlacementOverlay(
    private val context: Context,
    private val profile: Profile,
    private val landscape: Boolean,
    private val onClosed: () -> Unit,
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())
    private val size = dp(76)
    private val rootLoc = IntArray(2)

    private var root: FrameLayout? = null
    private lateinit var upTarget: TargetView
    private lateinit var downTarget: TargetView
    private lateinit var readout: TextView
    private var selected: TargetView? = null

    private fun dp(v: Int) = (v * density).toInt()

    fun show() {
        val frame = FrameLayout(context)
        frame.setBackgroundColor(0x99121320.toInt())
        root = frame

        upTarget = TargetView(context, INDIGO, plus = true)
        downTarget = TargetView(context, CORAL, plus = false)
        listOf(upTarget, downTarget).forEach { target ->
            frame.addView(target, FrameLayout.LayoutParams(size, size))
            target.setOnTouchListener { _, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> select(target)
                    MotionEvent.ACTION_MOVE -> {
                        move(target, e.rawX - rootLoc[0] - size / 2f, e.rawY - rootLoc[1] - size / 2f)
                    }
                }
                true
            }
        }

        frame.addView(
            buildPanel(),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

        try {
            wm.addView(frame, fullScreenParams(touchable = true))
        } catch (_: Exception) {
            root = null
            onClosed()
            return
        }

        frame.post {
            frame.getLocationOnScreen(rootLoc)
            val metrics = context.resources.displayMetrics
            val saved = profile.positions(landscape)
            val up = saved?.up ?: TapPoint((metrics.widthPixels * 0.68f).toInt(), (metrics.heightPixels * 0.30f).toInt())
            val down = saved?.down ?: TapPoint((metrics.widthPixels * 0.32f).toInt(), (metrics.heightPixels * 0.42f).toInt())
            place(upTarget, up)
            place(downTarget, down)
            select(upTarget)
        }
    }

    fun close() {
        handler.removeCallbacksAndMessages(null)
        root?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        root = null
        onClosed()
    }

    // ---- positions -------------------------------------------------------------

    private fun place(target: View, point: TapPoint) {
        move(target, (point.x - rootLoc[0] - size / 2).toFloat(), (point.y - rootLoc[1] - size / 2).toFloat())
    }

    private fun move(target: View, x: Float, y: Float) {
        target.x = x
        target.y = y
        updateReadout()
    }

    private fun centerOnScreen(target: View): TapPoint {
        val loc = IntArray(2)
        target.getLocationOnScreen(loc)
        return TapPoint(loc[0] + size / 2, loc[1] + size / 2)
    }

    private fun select(target: TargetView) {
        selected = target
        upTarget.selectedState = target === upTarget
        downTarget.selectedState = target === downTarget
        updateReadout()
    }

    private fun updateReadout() {
        if (!::readout.isInitialized) return
        val up = centerOnScreen(upTarget)
        val down = centerOnScreen(downTarget)
        val which = if (selected === downTarget) "Volume down" else "Volume up"
        readout.text = "$which selected   ·   Up ${up.x}, ${up.y}   ·   Down ${down.x}, ${down.y}"
    }

    private fun save() {
        val positions = Positions(centerOnScreen(upTarget), centerOnScreen(downTarget))
        VolTapStore.updateProfile(profile.packageName) {
            if (landscape) it.copy(landscape = positions) else it.copy(portrait = positions)
        }
        Toast.makeText(context, "Positions saved", Toast.LENGTH_SHORT).show()
        close()
    }

    // ---- views ---------------------------------------------------------------

    private fun buildPanel(): View {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(dp(32).toFloat(), dp(32).toFloat(), dp(32).toFloat(), dp(32).toFloat(), 0f, 0f, 0f, 0f)
            }
        }

        panel.addView(text("Place your taps", 20f, INK, bold = true))
        panel.addView(text(profile.label + if (landscape) " · landscape" else " · portrait", 13f, 0xFF5F6378.toInt()))
        readout = text("", 12f, 0xFF5F6378.toInt())
        (readout.layoutParams as LinearLayout.LayoutParams).topMargin = dp(10)
        panel.addView(readout)

        val picker = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        picker.addView(pill("Volume up", INDIGO, Color.WHITE) { select(upTarget) }, weighted())
        picker.addView(pill("Volume down", CORAL, Color.WHITE) { select(downTarget) }, weighted())
        panel.addView(picker, topMargin(14))

        val arrows = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        arrows.addView(nudgeButton("◀", -1, 0), arrowParams())
        arrows.addView(nudgeButton("▲", 0, -1), arrowParams())
        arrows.addView(nudgeButton("▼", 0, 1), arrowParams())
        arrows.addView(nudgeButton("▶", 1, 0), arrowParams())
        panel.addView(arrows, topMargin(10))

        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(pill("Cancel", SOFT, INK) { close() }, weighted(1f))
        actions.addView(pill("Lock positions", INDIGO, Color.WHITE) { save() }, weighted(2f))
        panel.addView(actions, topMargin(16))

        return panel
    }

    private fun text(value: String, sp: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value
        textSize = sp
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
    }

    private fun pill(label: String, bg: Int, fg: Int, onClick: () -> Unit) = TextView(context).apply {
        text = label
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setTextColor(fg)
        background = GradientDrawable().apply {
            cornerRadius = dp(28).toFloat()
            setColor(bg)
        }
        minimumHeight = dp(52)
        setOnClickListener { onClick() }
    }

    private fun nudgeButton(label: String, dx: Int, dy: Int): View {
        val button = TextView(context).apply {
            text = label
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(INK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(SOFT)
            }
        }
        val repeater = object : Runnable {
            override fun run() {
                nudge(dx, dy)
                handler.postDelayed(this, 35)
            }
        }
        button.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    nudge(dx, dy)
                    handler.postDelayed(repeater, 380)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(repeater)
            }
            true
        }
        return button
    }

    private fun nudge(dx: Int, dy: Int) {
        val target = selected ?: return
        move(target, target.x + dx, target.y + dy)
    }

    private fun weighted(weight: Float = 1f) = LinearLayout.LayoutParams(0, dp(52), weight).apply {
        marginEnd = dp(8)
    }

    private fun arrowParams() = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
        marginStart = dp(6)
        marginEnd = dp(6)
    }

    private fun topMargin(v: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(v) }

    private inner class TargetView(context: Context, private val tint: Int, private val plus: Boolean) :
        View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            alpha = 120
        }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(6).toFloat()
            color = Color.WHITE
        }
        private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2).toFloat()
            color = Color.WHITE
            alpha = 150
        }
        private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
            strokeWidth = dp(2).toFloat()
            strokeCap = Paint.Cap.ROUND
        }

        var selectedState: Boolean = false
            set(value) {
                field = value
                invalidate()
            }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = width / 2f - dp(14)
            canvas.drawCircle(cx, cy, r, fill)
            canvas.drawCircle(cx, cy, r, ring)
            canvas.drawCircle(cx, cy, dp(5).toFloat(), white)
            if (selectedState) canvas.drawCircle(cx, cy, width / 2f - dp(3), halo)
            val bx = width - dp(13).toFloat()
            val by = dp(13).toFloat()
            canvas.drawCircle(bx, by, dp(11).toFloat(), white)
            val a = dp(5).toFloat()
            canvas.drawLine(bx - a, by, bx + a, by, mark)
            if (plus) canvas.drawLine(bx, by - a, bx, by + a, mark)
        }
    }
}
