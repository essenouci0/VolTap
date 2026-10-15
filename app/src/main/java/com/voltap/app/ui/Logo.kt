package com.voltap.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.launch

/**
 * The VolTap mark: a volume-rocker capsule with plus and minus marks and a tap ring in the middle.
 * Drawn in a 136 x 304 design space; give it a modifier with that aspect ratio.
 * With [animate] the capsule scales in, the marks fade in, the ring is drawn and the dot pops.
 */
@Composable
fun LogoCapsule(
    modifier: Modifier,
    capsule: Color = Color.White,
    marks: Color,
    ring: Color,
    animate: Boolean = false,
) {
    val capsuleScale = remember { Animatable(if (animate) 0.8f else 1f) }
    val capsuleAlpha = remember { Animatable(if (animate) 0f else 1f) }
    val marksAlpha = remember { Animatable(if (animate) 0f else 1f) }
    val ringSweep = remember { Animatable(if (animate) 0f else 1f) }
    val dotScale = remember { Animatable(if (animate) 0f else 1f) }

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        launch { capsuleScale.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
        launch { capsuleAlpha.animateTo(1f, tween(500)) }
        launch { marksAlpha.animateTo(1f, tween(500, delayMillis = 550)) }
        launch { ringSweep.animateTo(1f, tween(900, delayMillis = 500, easing = FastOutSlowInEasing)) }
        launch {
            dotScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
        }
    }

    Canvas(modifier) {
        val s = size.width / 136f
        scale(capsuleScale.value, pivot = center) {
            drawRoundRect(
                color = capsule.copy(alpha = capsuleAlpha.value),
                topLeft = Offset.Zero,
                size = Size(136f * s, 304f * s),
                cornerRadius = CornerRadius(68f * s),
            )
            val markStroke = Stroke(width = 12f * s, cap = StrokeCap.Round)
            val markColor = marks.copy(alpha = marksAlpha.value)
            // plus
            drawLine(markColor, Offset(54f * s, 62f * s), Offset(82f * s, 62f * s), markStroke.width, StrokeCap.Round)
            drawLine(markColor, Offset(68f * s, 48f * s), Offset(68f * s, 76f * s), markStroke.width, StrokeCap.Round)
            // minus
            drawLine(markColor, Offset(54f * s, 242f * s), Offset(82f * s, 242f * s), markStroke.width, StrokeCap.Round)
        }
        val cx = 68f * s
        val cy = 152f * s
        val r = 40f * s
        drawArc(
            color = ring,
            startAngle = -90f,
            sweepAngle = 360f * ringSweep.value,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = 12f * s),
        )
        drawCircle(ring, radius = 11f * s * dotScale.value, center = Offset(cx, cy))
    }
}
