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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import com.potentplum.dextracker.data.local.CardEntity
import com.potentplum.dextracker.data.local.CardWithCopies
import com.potentplum.dextracker.data.variantList
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.ui.components.CopiesSheet
import com.potentplum.dextracker.ui.components.SheetCard
import com.potentplum.dextracker.ui.theme.ScreenDeep
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
import com.potentplum.dextracker.data.local.PokemonDetailEntity
import com.potentplum.dextracker.data.remote.PokeApiService
import com.potentplum.dextracker.domain.dexLabel
import com.potentplum.dextracker.ui.components.CardDetailDialog
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
    val online by viewModel.online.collectAsStateWithLifecycle()
    val condition by viewModel.defaultCondition.collectAsStateWithLifecycle()
    val language by viewModel.defaultLanguage.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        DetailActions(
            onBack = onBack,
            onAddCard = onAddCard,
            onRetry = viewModel::loadDetail,
            onAddCopy = viewModel::addCopy,
            onChangeQuantity = viewModel::changeQuantity,
            onConditionChange = viewModel::setDefaultCondition,
            onLanguageChange = viewModel::setDefaultLanguage,
        )
    }
    DetailContent(
        dex = viewModel.dex,
        name = name,
        detail = detail,
        owned = owned,
        online = online,
        condition = condition,
        language = language,
        actions = actions,
    )
}

class DetailActions(
    val onBack: () -> Unit,
    val onAddCard: () -> Unit,
    val onRetry: () -> Unit,
    val onAddCopy: (CardWithCopies, CardVariant) -> Unit,
    val onChangeQuantity: (copyId: Long, delta: Int) -> Unit,
    val onConditionChange: (CardCondition) -> Unit,
    val onLanguageChange: (CardLanguage) -> Unit,
)

@Composable
fun DetailContent(
    dex: Int,
    name: String,
    detail: DetailState,
    owned: List<CardWithCopies>,
    online: Boolean,
    condition: CardCondition,
    language: CardLanguage,
    actions: DetailActions,
) {
    // Track the open card by id so the sheet always shows the latest copies.
    var openCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var zoomCard by remember { mutableStateOf<CardEntity?>(null) }

    val displayName = (detail as? DetailState.Loaded)?.detail?.displayName ?: name

    DexScaffold(
        title = "$displayName ${dex.dexLabel()}".trim(),
        onBack = actions.onBack,
        floatingAction = { AddCardFab(onClick = actions.onAddCard) },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            // Leave room so the FAB never covers the last row.
            contentPadding = PaddingValues(bottom = 112.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            fullWidth("info") {
                Column {
                    when (val state = detail) {
                        DetailState.Loading -> DexScreen(Modifier.fillMaxWidth().height(180.dp)) {
                            PokeballLoader("Scanning…", Modifier.align(Alignment.Center))
                        }
                        DetailState.Failed -> DexScreen(Modifier.fillMaxWidth()) {
                            if (online) {
                                ScreenMessage(
                                    title = "No signal",
                                    body = "Couldn't load Pokédex data for this Pokémon.",
                                    actionLabel = "Retry",
                                    onAction = actions.onRetry,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            } else {
                                ScreenMessage(
                                    title = "Offline",
                                    body = "This page isn't saved yet. It loads as soon as you're back online. " +
                                        "To keep every page offline, use ⋮ → Save Pokédex for offline on the main screen.",
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
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
                    CollectedHeader(owned)
                }
            }
            if (owned.isEmpty()) {
                fullWidth("empty") {
                    DexScreen(Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
                        ScreenMessage(
                            title = "No cards yet",
                            body = "Tap ADD CARD to log the first card you own.",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            } else {
                items(owned, key = { it.card.cardId }) { card ->
                    OwnedCard(card, onClick = { openCardId = card.card.cardId })
                }
            }
        }
    }

    val open = owned.firstOrNull { it.card.cardId == openCardId }
    if (open != null) {
        CopiesSheet(
            card = open.card.toSheetCard(),
            copies = open.copies,
            condition = condition,
            language = language,
            onConditionChange = actions.onConditionChange,
            onLanguageChange = actions.onLanguageChange,
            onAdd = { variant -> actions.onAddCopy(open, variant) },
            onChangeQuantity = actions.onChangeQuantity,
            onZoom = { zoomCard = open.card },
            onDismiss = { openCardId = null },
        )
    }

    zoomCard?.let { card ->
        CardDetailDialog(
            imageUrl = card.imageLarge,
            placeholderUrl = card.imageSmall,
            title = card.name,
            subtitle = card.subtitle(),
            onDismiss = { zoomCard = null },
        )
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private fun CardEntity.subtitle() =
    listOfNotNull(setName, "#$number", releaseDate.take(4), rarity).joinToString(" · ")

private fun CardEntity.toSheetCard() = SheetCard(
    cardId = cardId,
    name = name,
    subtitle = subtitle(),
    imageSmall = imageSmall,
    imageLarge = imageLarge,
    variantOptions = CardVariant.optionsFor(variantList),
)

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
private fun CollectedHeader(owned: List<CardWithCopies>) {
    val copies = owned.sumOf { card -> card.copies.sumOf { it.quantity } }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = if (owned.isEmpty()) "COLLECTED CARDS" else "COLLECTED CARDS (${owned.size})",
            style = DexText.Title.copy(fontSize = 17.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (owned.isNotEmpty()) {
            Text(
                text = (if (copies != owned.size) "$copies COPIES · " else "") + "TAP A CARD TO EDIT OR REMOVE",
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OwnedCard(entry: CardWithCopies, onClick: () -> Unit) {
    val card = entry.card
    val quantity = entry.copies.sumOf { it.quantity }
    val variants = entry.copies
        .mapNotNull { CardVariant.fromKey(it.variant)?.takeIf { v -> v != CardVariant.UNSPECIFIED } }
        .distinct()
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ScreenDeep)
            .clickable(onClickLabel = "Edit copies", role = Role.Button, onClick = onClick)
            .padding(6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${card.name}, ${card.setName} number ${card.number}, " +
                    if (quantity == 1) "1 copy" else "$quantity copies"
            },
    ) {
        Box {
            TcgCardImage(
                url = card.imageSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CARD_ASPECT),
            )
            if (quantity > 1) {
                Text(
                    "×$quantity",
                    style = DexText.Label.copy(fontSize = 11.sp),
                    color = Ink,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DexYellow)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(card.setName, style = DexText.ScreenSmall, color = ScreenText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            when (variants.size) {
                0 -> "${card.releaseDate.take(4)} · #${card.number}"
                1 -> "#${card.number} · ${variants.single().label}"
                else -> "#${card.number} · ${variants.size} variants"
            },
            style = DexText.ScreenSmall.copy(fontSize = 10.sp),
            color = ScreenTextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
