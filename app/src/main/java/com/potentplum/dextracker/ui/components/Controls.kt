package com.potentplum.dextracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.potentplum.dextracker.ui.theme.Bezel
import com.potentplum.dextracker.ui.theme.BezelSilverDark
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.DexYellowDark
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.LedRed
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import com.potentplum.dextracker.ui.theme.TabGray
import com.potentplum.dextracker.ui.theme.typeColor

/** A Poké Ball drawn with shapes so it stays crisp at any size. */
@Composable
fun Pokeball(size: Dp, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val top = if (dimmed) Color(0xFF6B6F75) else LedRed
    Canvas(modifier.size(size)) {
        val d = this.size.minDimension
        val stroke = d * 0.08f
        val inset = stroke / 2
        val arcSize = Size(d - stroke, d - stroke)
        drawArc(top, 180f, 180f, useCenter = true, topLeft = Offset(inset, inset), size = arcSize)
        drawArc(Color.White, 0f, 180f, useCenter = true, topLeft = Offset(inset, inset), size = arcSize)
        drawLine(Ink, Offset(0f, d / 2), Offset(d, d / 2), strokeWidth = stroke)
        drawCircle(Ink, radius = d / 2 - inset, style = Stroke(stroke))
        drawCircle(Ink, radius = d * 0.18f)
        drawCircle(Color.White, radius = d * 0.11f)
    }
}

/** Spinning Poké Ball with a status line, shown on a screen while data loads. */
@Composable
fun PokeballLoader(message: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loader")
    val angle by transition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobble",
    )
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Pokeball(44.dp, Modifier.rotate(angle))
        Text(message.uppercase(), style = DexText.ScreenSmall, color = ScreenTextDim, textAlign = TextAlign.Center)
    }
}

/** Empty / error message on a screen, with an optional action button. */
@Composable
fun ScreenMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Pokeball(36.dp, dimmed = true)
        Spacer(Modifier.height(12.dp))
        Text(title.uppercase(), style = DexText.Screen, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(body, style = DexText.ScreenSmall, color = ScreenTextDim, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            DexButton(actionLabel, onClick = onAction)
        }
    }
}

enum class DexButtonStyle { Primary, Secondary }

/** Chunky hardware-style button: yellow for the main action, gray otherwise. */
@Composable
fun DexButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DexButtonStyle = DexButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    val (face, edge) = when {
        !enabled -> TabGray.copy(alpha = 0.5f) to BezelSilverDark
        style == DexButtonStyle.Primary -> DexYellow to DexYellowDark
        else -> TabGray to BezelSilverDark
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .shadow(if (enabled) 3.dp else 0.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(face, face, edge)))
            .border(2.dp, Bezel, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            style = DexText.Label,
            color = Ink.copy(alpha = if (enabled) 1f else 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

/** Generation tab: yellow when selected with a small pointer, gray otherwise. */
@Composable
fun DexTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        if (selected) listOf(DexYellow, DexYellow, DexYellowDark)
                        else listOf(TabGray, TabGray, BezelSilverDark),
                    ),
                )
                .border(2.dp, Bezel, shape)
                .semantics { this.selected = selected }
                .clickable(role = Role.Tab, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text.uppercase(), style = DexText.Label, color = Ink)
        }
        // Pointer under the active tab
        Canvas(Modifier.size(width = 14.dp, height = 7.dp)) {
            if (selected) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                drawPath(path, DexYellow)
            }
        }
    }
}

@Composable
fun TypeChip(type: String, modifier: Modifier = Modifier) {
    val color = typeColor(type)
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .border(2.dp, Bezel, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            type.uppercase(),
            style = DexText.Label.copy(fontSize = 12.sp),
            color = if (color.luminance() > 0.5f) Ink else Color.White,
        )
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
