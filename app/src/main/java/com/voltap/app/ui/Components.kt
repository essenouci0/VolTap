package com.voltap.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VtText(
    text: String,
    size: Int = 15,
    weight: FontWeight = FontWeight.Bold,
    color: Color = LocalVt.current.text,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        textAlign = align,
    )
}

@Composable
fun ScreenColumn(
    title: String?,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalVt.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (title != null) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(c.card)
                            .border(1.dp, c.border, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { VtText("‹", 26) }
                    Spacer(Modifier.width(14.dp))
                }
                VtText(title, 24, FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(20.dp))
        }
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SectionLabel(text: String) {
    VtText(
        text = text.uppercase(),
        size = 12,
        color = LocalVt.current.sub,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
    )
}

@Composable
fun VtCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalVt.current
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 18.dp),
        content = content,
    )
}

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = LocalVt.current
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (filled) c.accent else c.segBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        VtText(text, 16, color = if (filled) Color.White else c.text)
    }
}

@Composable
fun SwitchRow(
    title: String,
    sub: String?,
    checked: Boolean,
    divider: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val c = LocalVt.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            VtText(title, 15)
            if (sub != null) VtText(sub, 13, FontWeight.Medium, c.sub)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.accent,
                checkedThumbColor = Color.White,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = c.track,
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
    if (divider) HorizontalDivider(color = c.border)
}

@Composable
fun NavRow(title: String, sub: String?, divider: Boolean = true, onClick: () -> Unit) {
    val c = LocalVt.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            VtText(title, 16)
            if (sub != null) VtText(sub, 13, FontWeight.Medium, c.sub)
        }
        VtText("›", 24, FontWeight.Medium, c.sub)
    }
    if (divider) HorizontalDivider(color = c.border)
}

@Composable
fun Segmented(
    title: String?,
    options: List<String>,
    selected: Int,
    disabled: Set<Int> = emptySet(),
    divider: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    val c = LocalVt.current
    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (title != null) VtText(title, 15)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(c.segBg)
                .padding(4.dp),
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selected
                val bg by animateColorAsState(
                    if (isSelected) c.card else Color.Transparent,
                    tween(250),
                    label = "segment",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
                        .clickable(enabled = index !in disabled) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    VtText(
                        label,
                        13,
                        color = when {
                            index in disabled -> c.sub.copy(alpha = 0.5f)
                            isSelected -> c.text
                            else -> c.sub
                        },
                    )
                }
            }
        }
    }
    if (divider) HorizontalDivider(color = c.border)
}
