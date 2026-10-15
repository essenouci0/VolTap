package com.voltap.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.voltap.app.Positions
import com.voltap.app.R
import com.voltap.app.TapPoint
import com.voltap.app.VolTapStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val CoralMarker = Color(0xFFE0486B)

/** Real display size in px as (short side, long side), independent of rotation. */
@Suppress("DEPRECATION")
private fun realDisplay(context: Context): Pair<Int, Int> {
    val metrics = DisplayMetrics()
    val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    wm.defaultDisplay.getRealMetrics(metrics)
    return min(metrics.widthPixels, metrics.heightPixels) to max(metrics.widthPixels, metrics.heightPixels)
}

/** Decodes the picked image, shrunk so its longest side stays near 2400 px. */
private fun decodeScreenshot(context: Context, uri: Uri): ImageBitmap? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 2400) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
}.getOrNull()

@Composable
fun ShotScreen(packageName: String, back: () -> Unit) {
    val c = LocalVt.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = VolTapStore.profiles.firstOrNull { it.packageName == packageName }
    if (profile == null) {
        LaunchedEffect(Unit) { back() }
        return
    }

    var uriString by rememberSaveable { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    // Marker positions as fractions of the image (0..1).
    var upX by rememberSaveable { mutableStateOf(0.72f) }
    var upY by rememberSaveable { mutableStateOf(0.70f) }
    var downX by rememberSaveable { mutableStateOf(0.28f) }
    var downY by rememberSaveable { mutableStateOf(0.70f) }
    var selected by rememberSaveable { mutableStateOf(0) }
    var loupe by remember { mutableStateOf(false) }

    val (shortSide, longSide) = remember { realDisplay(context) }
    val landscape = bitmap?.let { it.width > it.height } ?: false
    val realW = if (landscape) longSide else shortSide
    val realH = if (landscape) shortSide else longSide

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uriString = uri.toString()
            failed = false
            // A fresh image restarts from the saved positions (or defaults) in its orientation.
            upX = Float.NaN
        }
    }

    LaunchedEffect(uriString) {
        val raw = uriString ?: return@LaunchedEffect
        loading = true
        val decoded = withContext(Dispatchers.IO) { decodeScreenshot(context, Uri.parse(raw)) }
        bitmap = decoded
        failed = decoded == null
        loading = false
        if (decoded != null && upX.isNaN()) {
            val land = decoded.width > decoded.height
            val w = if (land) longSide else shortSide
            val h = if (land) shortSide else longSide
            val saved = profile.positions(land)
            if (saved != null) {
                upX = saved.up.x / w.toFloat(); upY = saved.up.y / h.toFloat()
                downX = saved.down.x / w.toFloat(); downY = saved.down.y / h.toFloat()
            } else {
                upX = 0.72f; upY = 0.70f; downX = 0.28f; downY = 0.70f
            }
        }
    }

    fun setMarker(index: Int, fx: Float, fy: Float) {
        val x = fx.coerceIn(0f, 1f)
        val y = fy.coerceIn(0f, 1f)
        if (index == 0) { upX = x; upY = y } else { downX = x; downY = y }
    }

    fun nudge(dxPx: Int, dyPx: Int) {
        val fx = (if (selected == 0) upX else downX) + dxPx / realW.toFloat()
        val fy = (if (selected == 0) upY else downY) + dyPx / realH.toFloat()
        setMarker(selected, fx, fy)
    }

    val savedText = stringResource(R.string.toast_saved)

    ScreenColumn(stringResource(R.string.shot_title), back) {
        val bmp = bitmap
        if (bmp == null || upX.isNaN()) {
            IntroCard(loading, failed) { picker.launch("image/*") }
            return@ScreenColumn
        }

        // Which ring is being placed.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MarkerChip(stringResource(R.string.vol_up), c.accent, selected == 0, Modifier.weight(1f)) { selected = 0 }
            MarkerChip(stringResource(R.string.vol_down), CoralMarker, selected == 1, Modifier.weight(1f)) { selected = 1 }
        }
        Spacer(Modifier.height(14.dp))

        val ratio = bmp.width.toFloat() / bmp.height.toFloat()
        var box by remember { mutableStateOf(IntSize.Zero) }
        var dragIndex by remember { mutableStateOf(0) }
        val ups = remember { floatArrayOf(0f, 0f, 0f, 0f) }
        ups[0] = upX; ups[1] = upY; ups[2] = downX; ups[3] = downY

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .heightIn(max = 480.dp)
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, c.border, RoundedCornerShape(16.dp))
                    .onSizeChanged { box = it },
            ) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize(),
                )
                Canvas(
                    Modifier
                        .matchParentSize()
                        .pointerInput(box) {
                            detectTapGestures { p ->
                                if (box.width > 0) setMarker(selected, p.x / box.width, p.y / box.height)
                            }
                        }
                        .pointerInput(box) {
                            detectDragGestures(
                                onDragStart = { p ->
                                    if (box.width > 0) {
                                        val fx = p.x / box.width
                                        val fy = p.y / box.height
                                        // Grab the nearest ring if the finger starts close to one.
                                        val d0 = abs(fx - ups[0]) * box.width + abs(fy - ups[1]) * box.height
                                        val d1 = abs(fx - ups[2]) * box.width + abs(fy - ups[3]) * box.height
                                        val nearest = if (d0 <= d1) 0 else 1
                                        val nearestDist = min(d0, d1)
                                        dragIndex = if (nearestDist < 90.dp.toPx()) nearest else selected
                                        selected = dragIndex
                                        setMarker(dragIndex, fx, fy)
                                        loupe = true
                                    }
                                },
                                onDragEnd = { loupe = false },
                                onDragCancel = { loupe = false },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (box.width > 0) {
                                        setMarker(dragIndex, change.position.x / box.width, change.position.y / box.height)
                                    }
                                },
                            )
                        },
                ) {
                    drawMarker(Offset(upX * size.width, upY * size.height), c.accent, true, selected == 0, density)
                    drawMarker(Offset(downX * size.width, downY * size.height), CoralMarker, false, selected == 1, density)
                    if (loupe) {
                        val fx = if (selected == 0) upX else downX
                        val fy = if (selected == 0) upY else downY
                        drawLoupe(bmp, fx, fy, size.width, density, if (selected == 0) c.accent else CoralMarker)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        val fx = if (selected == 0) upX else downX
        val fy = if (selected == 0) upY else downY
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                VtText(
                    stringResource(R.string.shot_pos, (fx * realW).roundToInt(), (fy * realH).roundToInt()),
                    16,
                )
                VtText(
                    stringResource(if (landscape) R.string.shot_landscape else R.string.shot_portrait),
                    12,
                    FontWeight.Medium,
                    c.sub,
                )
            }
            // Arrows always point the way they move, whatever the language.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                NudgePad(
                    onNudge = { dx, dy -> nudge(dx, dy) },
                    onHold = { loupe = it },
                )
            }
        }

        val screenRatio = realW.toFloat() / realH.toFloat()
        if (abs(ratio - screenRatio) / screenRatio > 0.03f) {
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.warnBg)
                    .padding(16.dp),
            ) { VtText(stringResource(R.string.shot_mismatch), 13, FontWeight.Medium, c.warnText) }
        }

        Spacer(Modifier.height(12.dp))
        VtText(stringResource(R.string.shot_tip), 12, FontWeight.Medium, c.sub, Modifier.padding(horizontal = 4.dp))

        Spacer(Modifier.height(20.dp))
        com.voltap.app.ui.PillButton(stringResource(R.string.shot_save)) {
            val up = TapPoint((upX * realW).roundToInt(), (upY * realH).roundToInt())
            val down = TapPoint((downX * realW).roundToInt(), (downY * realH).roundToInt())
            val positions = Positions(up, down)
            VolTapStore.updateProfile(packageName) {
                if (landscape) it.copy(landscape = positions) else it.copy(portrait = positions)
            }
            Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            back()
        }
        Spacer(Modifier.height(10.dp))
        com.voltap.app.ui.PillButton(stringResource(R.string.shot_change), filled = false) { picker.launch("image/*") }
    }
}

@Composable
private fun IntroCard(loading: Boolean, failed: Boolean, onChoose: () -> Unit) {
    val c = LocalVt.current
    VtCard {
        Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            VtText(stringResource(R.string.shot_intro_title), 18, FontWeight.ExtraBold)
            Step(1, stringResource(R.string.shot_step1))
            Step(2, stringResource(R.string.shot_step2))
            Step(3, stringResource(R.string.shot_step3))
            if (failed) VtText(stringResource(R.string.shot_error), 13, FontWeight.Medium, c.danger)
            if (loading) VtText(stringResource(R.string.shot_loading), 13, FontWeight.Medium, c.sub)
            Spacer(Modifier.height(2.dp))
            PillButton(stringResource(R.string.choose_shot), onClick = { if (!loading) onChoose() })
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    val c = LocalVt.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) { VtText(number.toString(), 13, color = c.accent) }
        Spacer(Modifier.width(12.dp))
        VtText(text, 14, FontWeight.Medium, c.sub, Modifier.weight(1f).padding(top = 3.dp))
    }
}

@Composable
private fun MarkerChip(label: String, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalVt.current
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) c.card else c.segBg)
            .border(2.dp, if (selected) color else Color.Transparent, shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        VtText(label, 13, color = if (selected) c.text else c.sub, align = TextAlign.Center)
    }
}

@Composable
private fun NudgePad(onNudge: (Int, Int) -> Unit, onHold: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NudgeButton("↑", { onNudge(0, -1) }, onHold)
        Row {
            NudgeButton("←", { onNudge(-1, 0) }, onHold)
            Spacer(Modifier.width(4.dp))
            NudgeButton("↓", { onNudge(0, 1) }, onHold)
            Spacer(Modifier.width(4.dp))
            NudgeButton("→", { onNudge(1, 0) }, onHold)
        }
    }
}

/** One press moves 1 px; holding keeps moving, with the magnifier shown. */
@Composable
private fun NudgeButton(glyph: String, step: () -> Unit, onHold: (Boolean) -> Unit) {
    val c = LocalVt.current
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .padding(2.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(c.segBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        step()
                        onHold(true)
                        val job = scope.launch {
                            delay(350)
                            while (true) {
                                step()
                                delay(35)
                            }
                        }
                        tryAwaitRelease()
                        job.cancel()
                        onHold(false)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) { VtText(glyph, 20, color = c.text) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMarker(
    center: Offset,
    color: Color,
    plus: Boolean,
    selected: Boolean,
    density: Float,
) {
    val r = (if (selected) 17f else 14f) * density
    if (selected) drawCircle(color.copy(alpha = 0.22f), r + 8f * density, center)
    drawCircle(Color.White, r + 1.5f * density, center, style = Stroke(width = 4.5f * density))
    drawCircle(color, r, center, style = Stroke(width = 3f * density))
    val arm = r * 0.45f
    val sw = 2.5f * density
    drawLine(color, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), sw)
    if (plus) drawLine(color, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), sw)
}

/** A circular magnifier of the screenshot around the ring, parked on the far side of the image. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLoupe(
    bmp: ImageBitmap,
    fx: Float,
    fy: Float,
    width: Float,
    density: Float,
    color: Color,
) {
    val diameter = 120f * density
    val radius = diameter / 2f
    val margin = 10f * density
    val onLeft = fx > 0.5f
    val cx = if (onLeft) margin + radius else width - margin - radius
    val cy = margin + radius
    val srcSide = (min(bmp.width, bmp.height) * 0.08f).roundToInt().coerceAtLeast(24)
    val sx = (fx * bmp.width).roundToInt() - srcSide / 2
    val sy = (fy * bmp.height).roundToInt() - srcSide / 2
    val scale = diameter / srcSide
    // Only the part of the crop that lies inside the image is drawn, at the same scale.
    val ix0 = max(sx, 0)
    val iy0 = max(sy, 0)
    val ix1 = min(sx + srcSide, bmp.width)
    val iy1 = min(sy + srcSide, bmp.height)
    val path = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx, cy), radius)) }
    drawCircle(Color.White, radius + 2f * density, Offset(cx, cy))
    clipPath(path) {
        drawRect(Color.Black, Offset(cx - radius, cy - radius), androidx.compose.ui.geometry.Size(diameter, diameter))
        if (ix1 > ix0 && iy1 > iy0) {
            drawImage(
                image = bmp,
                srcOffset = IntOffset(ix0, iy0),
                srcSize = IntSize(ix1 - ix0, iy1 - iy0),
                dstOffset = IntOffset(
                    (cx - radius + (ix0 - sx) * scale).roundToInt(),
                    (cy - radius + (iy0 - sy) * scale).roundToInt(),
                ),
                dstSize = IntSize(((ix1 - ix0) * scale).roundToInt(), ((iy1 - iy0) * scale).roundToInt()),
            )
        }
    }
    drawCircle(color, radius, Offset(cx, cy), style = Stroke(width = 3f * density))
    // Crosshair at the exact tap point.
    val arm = 9f * density
    drawLine(color, Offset(cx - arm, cy), Offset(cx + arm, cy), 2f * density)
    drawLine(color, Offset(cx, cy - arm), Offset(cx, cy + arm), 2f * density)
}
