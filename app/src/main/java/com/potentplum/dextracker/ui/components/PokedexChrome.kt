package com.potentplum.dextracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.potentplum.dextracker.ui.theme.Bezel
import com.potentplum.dextracker.ui.theme.BezelLight
import com.potentplum.dextracker.ui.theme.BezelSilver
import com.potentplum.dextracker.ui.theme.BezelSilverDark
import com.potentplum.dextracker.ui.theme.DexRed
import com.potentplum.dextracker.ui.theme.DexRedDark
import com.potentplum.dextracker.ui.theme.DexRedDeep
import com.potentplum.dextracker.ui.theme.DexRedLight
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.LedGreen
import com.potentplum.dextracker.ui.theme.LedRed
import com.potentplum.dextracker.ui.theme.LedYellow
import com.potentplum.dextracker.ui.theme.LensBlue
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenLine

/** Whether the device is online; drives the hardware strip's connection LED. */
val LocalOnline = compositionLocalOf { true }

/** The app's top-level sections, switched with [DexTabBar]. */
enum class TopTab(val label: String) {
    POKEDEX("Pokédex"),
    VALUE("Value"),
}

/**
 * A deliberately quiet tab strip along the bottom of the shell: small text, with only a thin
 * yellow line marking the current tab. Collecting Pokémon stays the focus.
 */
@Composable
fun DexTabBar(selected: TopTab, onSelect: (TopTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        TopTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                Modifier
                    .width(112.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Tab) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 24.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (isSelected) DexYellow else Color.Transparent),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    tab.label.uppercase(),
                    style = DexText.Label.copy(fontSize = 11.sp, letterSpacing = 1.5.sp),
                    color = Color.White.copy(alpha = if (isSelected) 0.95f else 0.55f),
                )
            }
        }
    }
}

/**
 * The red Pokédex body every screen sits in: hardware strip with the lens and LEDs,
 * a title plate, then the screen content. [floatingAction] is drawn bottom-end.
 *
 * When [compact] is true (e.g. while a list is scrolled) the hardware strip folds away and
 * a small lens moves into the title plate, giving the content more room. [actions] sit at
 * the end of the title plate.
 */
@Composable
fun DexScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    compact: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    floatingAction: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DexRedLight, DexRed, DexRedDark)))
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !compact,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                HardwareStrip()
            }
            TitlePlate(title = title, onBack = onBack, showLens = compact && onBack == null, actions = actions)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                content = content,
            )
            bottomBar?.invoke()
        }
        floatingAction?.invoke(this)
        if (snackbarHostState != null) {
            DexSnackbarHost(
                snackbarHostState,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = if (bottomBar != null) 64.dp else 20.dp),
            )
        }
    }
}

@Composable
private fun HardwareStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Lens(size = 46.dp)
        Spacer(Modifier.width(10.dp))
        Led(LensBlue, 12.dp)
        Spacer(Modifier.width(10.dp))
        // Speaker grille
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .size(width = 3.dp, height = 12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DexRedDeep),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        // The green LED doubles as a connection light: it goes dark while offline.
        val online = LocalOnline.current
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.semantics { contentDescription = if (online) "Online" else "Offline" },
        ) {
            Led(if (online) LedGreen else BezelLight, 9.dp)
            Led(LedYellow, 9.dp)
            Led(LedRed, 9.dp)
        }
    }
}

/** The big blue camera lens in the top-left corner of the device. */
@Composable
fun Lens(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, Bezel, CircleShape)
            .padding(size * 0.12f)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF9AD3FF), LensBlue, Color(0xFF0D3C78)),
                    center = Offset.Unspecified,
                ),
            )
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = this.size.minDimension * 0.16f,
                    center = Offset(this.size.width * 0.34f, this.size.height * 0.32f),
                )
            },
    )
}

@Composable
private fun Led(color: Color, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), color, color.copy(alpha = 0.8f))))
            .border(1.dp, Bezel, CircleShape),
    )
}

@Composable
private fun TitlePlate(
    title: String,
    onBack: (() -> Unit)?,
    showLens: Boolean,
    actions: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            ShellIconButton(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onBack)
        } else if (showLens) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Lens(30.dp) }
        } else {
            Spacer(Modifier.width(40.dp))
        }
        Text(
            text = title.uppercase(),
            style = DexText.Title,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .semantics { heading() },
        )
        if (actions != null) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { actions() }
        } else {
            Spacer(Modifier.width(40.dp))
        }
    }
}

/** Round button in the red shell, matching the back button; used for title plate actions. */
@Composable
fun ShellIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DexRedDark)
            .border(2.dp, DexRedDeep, CircleShape)
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White)
    }
}

/**
 * A dark teal display set into a black bezel, with a faint grid like an old LCD.
 */
@Composable
fun DexScreen(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: Dp = 12.dp,
    showGrid: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(3.dp, shape)
            .clip(shape)
            .background(Bezel)
            .padding(4.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Screen, ScreenDeep)))
            .then(if (showGrid) Modifier.screenGrid() else Modifier)
            .border(1.dp, ScreenLine, shape)
            .padding(contentPadding),
        content = content,
    )
}

private fun Modifier.screenGrid(step: Dp = 18.dp): Modifier = drawBehind {
    val px = step.toPx()
    val line = ScreenLine.copy(alpha = 0.35f)
    var x = px
    while (x < size.width) {
        drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += px
    }
    var y = px
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += px
    }
}

/** A recessed area of the red shell, used to group the grid and galleries. */
@Composable
fun DexInset(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(DexRedDeep, DexRedDark)))
            .border(2.dp, DexRedDeep, shape)
            .padding(8.dp),
        content = content,
    )
}

/** A silver speaker-style plate with a status LED, used for flavour text. */
@Composable
fun SilverPlate(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .shadow(3.dp, shape)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(BezelSilver, BezelSilverDark)))
                .padding(8.dp),
        ) {
            DexScreen(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                showGrid = false,
                content = content,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(6.dp))
                .background(BezelLight)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier
                    .size(width = 18.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(LedGreen),
            )
        }
    }
}
