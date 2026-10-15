package com.voltap.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat


import android.os.Build


import android.view.Gravity

import android.view.View
import android.view.WindowManager





private const val INDIGO = 0xFF4B53E0.toInt()
private const val CORAL = 0xFFE0486B.toInt()



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
