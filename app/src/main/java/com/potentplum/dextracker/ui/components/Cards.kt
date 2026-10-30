package com.potentplum.dextracker.ui.components

import androidx.compose.foundation.background
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.DexYellow
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenTextDim

/**
 * TCG card art with a Poké Ball placeholder underneath until it loads. Uses AsyncImage rather
 * than SubcomposeAsyncImage, which is slow inside lazy grids.
 */
@Composable
fun TcgCardImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    placeholderUrl: String? = null,
    colorFilter: ColorFilter? = null,
) {
    var state by remember(url) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    Box(modifier.clip(RoundedCornerShape(6.dp))) {
        if (state !is AsyncImagePainter.State.Success) {
            if (placeholderUrl != null) {
                // Show the (usually already cached) small art while the hi-res image loads.
                AsyncImage(placeholderUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                CardPlaceholder(failed = state is AsyncImagePainter.State.Error)
            }
        }
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            onState = { state = it },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CardPlaceholder(failed: Boolean = false) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ScreenLine.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Pokeball(24.dp, dimmed = true)
        if (failed) {
            Text(
                "NO IMAGE",
                style = DexText.ScreenSmall,
                color = ScreenTextDim,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

/** Full-size view of a card with its details and an optional destructive action. */
@Composable
fun CardDetailDialog(
    imageUrl: String,
    placeholderUrl: String?,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    destructiveLabel: String? = null,
    onDestructive: (() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        DexScreen(Modifier.widthIn(max = 420.dp), showGrid = false) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TcgCardImage(
                    url = imageUrl,
                    placeholderUrl = placeholderUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.716f)
                        .background(ScreenDeep),
                )
                Spacer(Modifier.height(12.dp))
                Text(title.uppercase(), style = DexText.ScreenLarge, textAlign = TextAlign.Center)
                Text(subtitle, style = DexText.ScreenSmall, color = ScreenTextDim, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (destructiveLabel != null && onDestructive != null) {
                        DexButton(
                            destructiveLabel,
                            onClick = onDestructive,
                            style = DexButtonStyle.Secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    DexButton("Close", onClick = onDismiss, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Marks a card that's on the wanted list. */
@Composable
fun WantedStar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Ink.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = DexYellow, modifier = Modifier.size(15.dp))
    }
}
