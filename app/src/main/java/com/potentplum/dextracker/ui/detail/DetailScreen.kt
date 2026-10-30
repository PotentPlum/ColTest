package com.potentplum.dextracker.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.potentplum.dextracker.data.local.OwnedCardEntity
import com.potentplum.dextracker.data.local.PokemonDetailEntity
import com.potentplum.dextracker.data.remote.PokeApiService
import com.potentplum.dextracker.domain.dexLabel
import com.potentplum.dextracker.ui.components.CardDetailDialog
import com.potentplum.dextracker.ui.components.DexInset
import com.potentplum.dextracker.ui.components.DexScaffold
import com.potentplum.dextracker.ui.components.DexScreen
import com.potentplum.dextracker.ui.components.Pokeball
import com.potentplum.dextracker.ui.components.PokeballLoader
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.components.SilverPlate
import com.potentplum.dextracker.ui.components.TcgCardImage
import com.potentplum.dextracker.ui.components.TypeChip
import com.potentplum.dextracker.ui.theme.Bezel
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.DexYellowDark
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import java.util.Locale

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onAddCard: () -> Unit,
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val owned by viewModel.ownedCards.collectAsStateWithLifecycle()
    DetailContent(
        dex = viewModel.dex,
        name = name,
        detail = detail,
        owned = owned,
        onBack = onBack,
        onAddCard = onAddCard,
        onRetry = viewModel::loadDetail,
        onRemoveCard = viewModel::removeCard,
    )
}

@Composable
fun DetailContent(
    dex: Int,
    name: String,
    detail: DetailState,
    owned: List<OwnedCardEntity>,
    onBack: () -> Unit,
    onAddCard: () -> Unit,
    onRetry: () -> Unit,
    onRemoveCard: (cardId: String) -> Unit,
) {
    var openCard by remember { mutableStateOf<OwnedCardEntity?>(null) }

    val displayName = (detail as? DetailState.Loaded)?.detail?.displayName ?: name

    DexScaffold(
        title = "$displayName ${dex.dexLabel()}".trim(),
        onBack = onBack,
        floatingAction = { AddCardFab(onClick = onAddCard) },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Leave room so the FAB never covers the gallery.
                .padding(bottom = 104.dp),
        ) {
            when (val state = detail) {
                DetailState.Loading -> DexScreen(Modifier.fillMaxWidth().height(180.dp)) {
                    PokeballLoader("Scanning…", Modifier.align(Alignment.Center))
                }
                DetailState.Failed -> DexScreen(Modifier.fillMaxWidth()) {
                    ScreenMessage(
                        title = "No signal",
                        body = "Couldn't load Pokédex data for this Pokémon.",
                        actionLabel = "Retry",
                        onAction = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                is DetailState.Loaded -> {
                    InfoScreen(dex, state.detail)
                    if (state.detail.flavorText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        SilverPlate(Modifier.fillMaxWidth()) {
                            Text(state.detail.flavorText, style = DexText.Screen)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            CollectedCards(owned, onCardClick = { openCard = it })
        }
    }

    openCard?.let { card ->
        CardDetailDialog(
            imageUrl = card.imageLarge,
            placeholderUrl = card.imageSmall,
            title = card.name,
            subtitle = listOfNotNull(card.setName, "#${card.number}", card.releaseDate.take(4), card.rarity)
                .joinToString(" · "),
            onDismiss = { openCard = null },
            destructiveLabel = "Remove from collection",
            onDestructive = {
                onRemoveCard(card.cardId)
                openCard = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoScreen(dex: Int, detail: PokemonDetailEntity) {
    DexScreen(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .viewfinderCorners(),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = PokeApiService.spriteUrl(dex),
                    contentDescription = detail.displayName,
                    filterQuality = FilterQuality.None,
                    // Game sprites have generous transparent padding; scale past it a little.
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.25f, scaleY = 1.25f),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    detail.types.split(',').filter { it.isNotBlank() }.forEach { TypeChip(it) }
                }
                if (detail.genus.isNotBlank()) {
                    Text(detail.genus, style = DexText.ScreenSmall, color = ScreenTextDim)
                }
                Text("HT: ${formatMetres(detail.heightDecimetres)}", style = DexText.Screen)
                Text("WT: ${formatKilos(detail.weightHectograms)}", style = DexText.Screen)
            }
        }
    }
}

private fun formatMetres(dm: Int) = String.format(Locale.US, "%.1f m", dm / 10.0)
private fun formatKilos(hg: Int) = String.format(Locale.US, "%.1f kg", hg / 10.0)

/** Cyan corner brackets around the sprite, like a scanner viewfinder. */
private fun Modifier.viewfinderCorners(): Modifier = drawBehind {
    val len = size.minDimension * 0.18f
    val w = 3.dp.toPx()
    val c = ScreenGlow
    val (r, b) = size.width to size.height
    // top-left
    drawLine(c, Offset(0f, 0f), Offset(len, 0f), w)
    drawLine(c, Offset(0f, 0f), Offset(0f, len), w)
    // top-right
    drawLine(c, Offset(r, 0f), Offset(r - len, 0f), w)
    drawLine(c, Offset(r, 0f), Offset(r, len), w)
    // bottom-left
    drawLine(c, Offset(0f, b), Offset(len, b), w)
    drawLine(c, Offset(0f, b), Offset(0f, b - len), w)
    // bottom-right
    drawLine(c, Offset(r, b), Offset(r - len, b), w)
    drawLine(c, Offset(r, b), Offset(r, b - len), w)
}

@Composable
private fun CollectedCards(cards: List<OwnedCardEntity>, onCardClick: (OwnedCardEntity) -> Unit) {
    DexInset(Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = if (cards.isEmpty()) "COLLECTED CARDS" else "COLLECTED CARDS (${cards.size})",
                style = DexText.Title.copy(fontSize = 17.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            )
            DexScreen(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                contentPadding = 0.dp,
            ) {
                if (cards.isEmpty()) {
                    ScreenMessage(
                        title = "No cards yet",
                        body = "Tap ADD CARD to log the first card you own.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(cards, key = { it.cardId }) { card ->
                            OwnedCard(card, onClick = { onCardClick(card) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnedCard(card: OwnedCardEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(108.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${card.name}, ${card.setName} number ${card.number}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TcgCardImage(
            url = card.imageSmall,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CARD_ASPECT),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            card.setName,
            style = DexText.ScreenSmall,
            color = ScreenText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${card.releaseDate.take(4)} · #${card.number}",
            style = DexText.ScreenSmall.copy(fontSize = 10.sp),
            color = ScreenTextDim,
        )
    }
}

private const val CARD_ASPECT = 0.716f

@Composable
private fun BoxScope.AddCardFab(onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(DexYellow, DexYellow, DexYellowDark)))
            .border(3.dp, Bezel, shape)
            .clickable(onClickLabel = "Add card", role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Pokeball(40.dp)
        Spacer(Modifier.height(4.dp))
        Text("ADD CARD", style = DexText.Label.copy(fontSize = 12.sp), color = Ink)
    }
}
