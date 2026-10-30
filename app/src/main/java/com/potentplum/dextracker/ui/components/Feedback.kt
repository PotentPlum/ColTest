package com.potentplum.dextracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.potentplum.dextracker.ui.theme.Bezel
import com.potentplum.dextracker.ui.theme.BezelSilver
import com.potentplum.dextracker.ui.theme.BezelSilverDark
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import kotlinx.coroutines.delay

/** Snackbars styled as a small Pokédex screen, with the action in button yellow. */
@Composable
fun DexSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data -> DexSnackbar(data) }
}

@Composable
private fun DexSnackbar(data: SnackbarData) {
    DexScreen(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = 0.dp,
        showGrid = false,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                data.visuals.message,
                style = DexText.Screen.copy(fontSize = 13.sp),
                color = ScreenText,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
            )
            data.visuals.actionLabel?.let { label ->
                Text(
                    label.uppercase(),
                    style = DexText.Label.copy(fontSize = 13.sp),
                    color = DexYellow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = data::performAction)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/** A moment worth celebrating: a new Pokémon caught, or a whole generation completed. */
data class Celebration(
    val title: String,
    val message: String,
    /** Distinguishes two celebrations with the same text so each one replays. */
    val id: Long = System.nanoTime(),
)

/**
 * "Gotcha!" banner: a Poké Ball wobbles three times and clicks shut, like a catch in the
 * games. It never blocks the screen and clears itself; tap to dismiss early.
 */
@Composable
fun CelebrationBanner(
    celebration: Celebration?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var last by remember { mutableStateOf(celebration) }
    if (celebration != null) last = celebration

    LaunchedEffect(celebration) {
        if (celebration != null) {
            delay(2_800)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = celebration != null,
        enter = scaleIn(initialScale = 0.6f) + fadeIn(),
        exit = scaleOut(targetScale = 0.9f) + fadeOut(),
        modifier = modifier,
    ) {
        last?.let { shown ->
            val shape = RoundedCornerShape(20.dp)
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .shadow(8.dp, shape)
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(BezelSilver, BezelSilverDark)))
                    .border(3.dp, Bezel, shape)
                    .clickable(onClickLabel = "Dismiss", onClick = onDismiss)
                    .padding(8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                DexScreen(Modifier.fillMaxWidth(), showGrid = false, contentPadding = 14.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        WobblingPokeball(key = shown.id)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f, fill = false)) {
                            Text(shown.title.uppercase(), style = DexText.ScreenLarge, color = DexYellow)
                            Text(
                                shown.message,
                                style = DexText.ScreenSmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                color = ScreenTextDim,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WobblingPokeball(key: Long) {
    val angle = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        repeat(3) {
            angle.animateTo(-22f, tween(110))
            angle.animateTo(22f, tween(180))
            angle.animateTo(0f, tween(110))
            delay(120)
        }
    }
    Box(contentAlignment = Alignment.Center) {
        Pokeball(44.dp, Modifier.rotate(angle.value))
        // Glow ring once the ball has settled
        if (!angle.isRunning) {
            Box(Modifier.size(54.dp).border(2.dp, ScreenGlow.copy(alpha = 0.6f), CircleShape))
        }
    }
}
