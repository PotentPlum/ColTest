package com.potentplum.dextracker.ui.sets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.potentplum.dextracker.data.local.OwnedCopyEntity
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.domain.SetCard
import com.potentplum.dextracker.domain.TcgCard
import com.potentplum.dextracker.ui.components.AddingAsSheet
import com.potentplum.dextracker.ui.components.CardDetailDialog
import com.potentplum.dextracker.ui.components.Celebration
import com.potentplum.dextracker.ui.components.CelebrationBanner
import com.potentplum.dextracker.ui.components.ChooserOption
import com.potentplum.dextracker.ui.components.CopiesSheet
import com.potentplum.dextracker.ui.components.DexInset
import com.potentplum.dextracker.ui.components.DexScaffold
import com.potentplum.dextracker.ui.components.DexScreen
import com.potentplum.dextracker.ui.components.DexSegmentedControl
import com.potentplum.dextracker.ui.components.DexTabBar
import com.potentplum.dextracker.ui.components.PokeballLoader
import com.potentplum.dextracker.ui.components.QuickChooser
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.components.SheetCard
import com.potentplum.dextracker.ui.components.TcgCardImage
import com.potentplum.dextracker.ui.components.TopTab
import com.potentplum.dextracker.ui.components.WantedStar
import com.potentplum.dextracker.ui.components.copyLabel
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import com.potentplum.dextracker.ui.theme.TabGray
import kotlinx.coroutines.launch

private const val CARD_ASPECT = 0.716f

/** Missing cards are shown in black and white, like an empty binder pocket. */
private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun SetCardsScreen(
    viewModel: SetCardsViewModel,
    onBack: () -> Unit,
    onSelectTab: (TopTab) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var celebration by remember { mutableStateOf<Celebration?>(null) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.added.collect { change ->
            haptics.performHapticFeedback(
                if (change.newCatch) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
            )
            if (change.newCatch) celebration = change.toCelebration()
            // Each add gets its own Undo, so replace any snackbar still showing.
            snackbar.currentSnackbarData?.dismiss()
            launch {
                val result = snackbar.showSnackbar(
                    message = change.message(),
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.undo(change)
            }
        }
    }
    val actions = remember(viewModel) {
        QuickAddActions(
            onSelectFilter = viewModel::selectFilter,
            onTap = viewModel::tap,
            onHold = viewModel::openSheet,
            onMinus = viewModel::minus,
            onRetry = { viewModel.load(forceRefresh = true) },
            onAdd = viewModel::add,
            onChangeQuantity = viewModel::changeQuantity,
            onEditCopy = viewModel::editCopy,
            onToggleWanted = viewModel::toggleWanted,
            onCloseSheet = viewModel::closeSheet,
            onCloseChoice = viewModel::closeChoice,
            onConditionChange = viewModel::setDefaultCondition,
            onLanguageChange = viewModel::setDefaultLanguage,
        )
    }
    QuickAddContent(
        state = state,
        actions = actions,
        onBack = onBack,
        onSelectTab = onSelectTab,
        snackbarHostState = snackbar,
        celebration = celebration,
        onCelebrationDone = { celebration = null },
    )
}

private fun QuickAdded.message(): String {
    val details = listOfNotNull(
        key.variant.takeIf { card.card.variants.size > 1 }?.label,
        key.condition.label,
        key.language.label,
    ).joinToString(" · ")
    val wantedNote = if (unwanted != null) " · off your wanted list" else ""
    return "+1 ${card.card.name} #${card.card.number} · $details$wantedNote"
}

private fun QuickAdded.toCelebration(): Celebration {
    val who = pokemonName.ifBlank { card.card.name }
    return when (val gen = completedGeneration) {
        null -> Celebration("Gotcha!", "$who was registered in your Pokédex.")
        else -> Celebration("${gen.label} complete!", "$who was the last one. All ${gen.size} are registered!")
    }
}

class QuickAddActions(
    val onSelectFilter: (OwnedFilter) -> Unit,
    val onTap: (SetCard) -> Unit,
    val onHold: (SetCard) -> Unit,
    val onMinus: (SetCard) -> Unit,
    val onRetry: () -> Unit,
    val onAdd: (SetCard, CardVariant) -> Unit,
    val onChangeQuantity: (copyId: Long, delta: Int) -> Unit,
    val onEditCopy: (copyId: Long, CardVariant, CardCondition, CardLanguage) -> Unit,
    val onToggleWanted: (SetCard) -> Unit,
    val onCloseSheet: () -> Unit,
    val onCloseChoice: () -> Unit,
    val onConditionChange: (CardCondition) -> Unit,
    val onLanguageChange: (CardLanguage) -> Unit,
)

@Composable
fun QuickAddContent(
    state: QuickAddUiState,
    actions: QuickAddActions,
    onBack: () -> Unit,
    onSelectTab: (TopTab) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    celebration: Celebration? = null,
    onCelebrationDone: () -> Unit = {},
) {
    var preview by remember { mutableStateOf<TcgCard?>(null) }
    var editingDefaults by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
    LaunchedEffect(state.filter) { gridState.scrollToItem(0) }

    DexScaffold(
        title = state.set?.name ?: "Quick add",
        onBack = onBack,
        compact = scrolled,
        snackbarHostState = snackbarHostState,
        bottomBar = { DexTabBar(TopTab.SETS, onSelectTab) },
    ) {
        StatusPanel(state, onEditDefaults = { editingDefaults = true })
        Spacer(Modifier.height(8.dp))
        DexSegmentedControl(
            options = OwnedFilter.entries,
            selected = state.filter,
            label = { it.label },
            onSelect = actions.onSelectFilter,
        )
        Text(
            "ORDERED BY NUMBER · TAP TO ADD · HOLD FOR DETAILS",
            style = DexText.ScreenSmall.copy(fontSize = 10.sp),
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
        ) {
            DexInset(Modifier.fillMaxSize()) {
                DexScreen(Modifier.fillMaxSize(), contentPadding = 0.dp) {
                    when {
                        state.loading -> PokeballLoader(
                            "Opening ${state.set?.name ?: "the set"}…",
                            Modifier.align(Alignment.Center),
                        )
                        state.failed && !state.online -> ScreenMessage(
                            title = "You're offline",
                            body = "Quick add needs an internet connection to list the set. " +
                                "It loads by itself when you reconnect.",
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.failed -> ScreenMessage(
                            title = "No signal",
                            body = "The card database didn't answer. Check your connection and try again.",
                            actionLabel = "Retry",
                            onAction = actions.onRetry,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.allCards.isEmpty() -> ScreenMessage(
                            title = "No cards listed",
                            body = "The card database doesn't list any cards for this set yet.",
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.cards.isEmpty() -> ScreenMessage(
                            title = if (state.filter == OwnedFilter.OWNED) "None owned yet" else "Set complete!",
                            body = if (state.filter == OwnedFilter.OWNED) "Tap a card to add it to your collection."
                            else "You own every card in this set.",
                            actionLabel = "Show all cards",
                            onAction = { actions.onSelectFilter(OwnedFilter.ALL) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                        else -> LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.cards, key = { it.card.id }) { card ->
                                QuickCard(
                                    card = card,
                                    copies = state.copiesOf(card),
                                    wanted = card.card.id in state.wantedIds,
                                    onTap = { actions.onTap(card) },
                                    onHold = { actions.onHold(card) },
                                    onMinus = { actions.onMinus(card) },
                                )
                            }
                        }
                    }
                }
            }
            CelebrationBanner(
                celebration = celebration,
                onDismiss = onCelebrationDone,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
            )
        }
    }

    state.sheetCard?.let { card ->
        CopiesSheet(
            card = card.toSheetCard(),
            copies = state.owned[card.card.id]?.copies.orEmpty(),
            condition = state.condition,
            language = state.language,
            onConditionChange = actions.onConditionChange,
            onLanguageChange = actions.onLanguageChange,
            onAdd = { variant -> actions.onAdd(card, variant) },
            onChangeQuantity = actions.onChangeQuantity,
            onEditCopy = actions.onEditCopy,
            onZoom = { preview = card.card },
            onDismiss = actions.onCloseSheet,
            wanted = card.card.id in state.wantedIds,
            onToggleWanted = { actions.onToggleWanted(card) },
        )
    }

    state.choice?.let { choice ->
        val card = state.cardById(choice.cardId) ?: return@let
        val copies = state.owned[card.card.id]?.copies.orEmpty()
        when (choice) {
            is QuickChoice.AddVersion -> QuickChooser(
                title = card.card.name,
                subtitle = "#${card.card.number} · Which version? Adds a ${copyLabel(state.condition, state.language)} copy.",
                options = CardVariant.optionsFor(card.card.variants).map { variant ->
                    val owned = copies.filter { it.variant == variant.key }.sumOf { it.quantity }
                    ChooserOption(
                        label = variant.label,
                        detail = if (owned > 0) "OWN $owned" else null,
                        icon = Icons.Rounded.Add,
                        onClick = { actions.onAdd(card, variant) },
                    )
                },
                onDismiss = actions.onCloseChoice,
            )
            is QuickChoice.RemoveCopy -> QuickChooser(
                title = card.card.name,
                subtitle = "#${card.card.number} · Which copy should go?",
                options = copies.sortedWith(copyOrder).map { copy ->
                    ChooserOption(
                        label = copy.describe(),
                        detail = "×${copy.quantity}",
                        icon = Icons.Rounded.Remove,
                        onClick = { actions.onChangeQuantity(copy.id, -1) },
                    )
                },
                onDismiss = actions.onCloseChoice,
            )
        }
    }

    if (editingDefaults) {
        AddingAsSheet(
            condition = state.condition,
            language = state.language,
            onConditionChange = actions.onConditionChange,
            onLanguageChange = actions.onLanguageChange,
            onDismiss = { editingDefaults = false },
        )
    }

    preview?.let { card ->
        CardDetailDialog(
            imageUrl = card.imageLarge,
            placeholderUrl = card.imageSmall,
            title = card.name,
            subtitle = card.subtitle(),
            onDismiss = { preview = null },
        )
    }
}

private val copyOrder = compareBy<OwnedCopyEntity>({ CardVariant.fromKey(it.variant)?.ordinal }, { it.condition }, { it.language })

private fun OwnedCopyEntity.describe(): String {
    val variant = CardVariant.fromKey(variant) ?: CardVariant.UNSPECIFIED
    return "${variant.label} · ${copyLabel(CardCondition.fromCode(condition), CardLanguage.fromCode(language))}"
}

private fun TcgCard.subtitle() = listOfNotNull(setName, "#$number", releaseYear, rarity).joinToString(" · ")

private fun SetCard.toSheetCard() = SheetCard(
    cardId = card.id,
    name = card.name,
    subtitle = card.subtitle() + if (countsTowardDex) "" else " · Doesn't count toward the Pokédex",
    imageSmall = card.imageSmall,
    imageLarge = card.imageLarge,
    variantOptions = CardVariant.optionsFor(card.variants),
)

/** Progress through the set, and what condition and language a tap adds. */
@Composable
private fun StatusPanel(state: QuickAddUiState, onEditDefaults: () -> Unit) {
    val total = state.totalCount
    val owned = state.ownedCount
    val progress = if (total > 0) owned.toFloat() / total else 0f
    DexScreen(Modifier.fillMaxWidth(), showGrid = false, contentPadding = 0.dp) {
        Column {
            Column(
                Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                    .semantics(mergeDescendants = true) {},
            ) {
                Row {
                    Text(
                        "QUICK ADD" + (state.set?.code?.let { " · $it" } ?: ""),
                        style = DexText.ScreenSmall,
                        color = ScreenTextDim,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${(progress * 100).toInt()}%", style = DexText.ScreenSmall, color = ScreenTextDim)
                }
                Text(
                    "$owned / $total OWNED",
                    style = DexText.ScreenLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
                    color = ScreenGlow,
                )
                Spacer(Modifier.height(6.dp))
                ProgressBar(progress, if (total in 1..owned) DexYellow else ScreenGlow)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(ScreenDeep)
                    .clickable(onClickLabel = "Change condition and language", role = Role.Button, onClick = onEditDefaults)
                    .padding(start = 12.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ADDING AS", style = DexText.ScreenSmall.copy(fontSize = 10.sp), color = ScreenTextDim)
                Spacer(Modifier.width(8.dp))
                Text(
                    copyLabel(state.condition, state.language),
                    style = DexText.ScreenSmall.copy(fontSize = 12.sp),
                    color = ScreenText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("EDIT", style = DexText.Label.copy(fontSize = 12.sp), color = DexYellow)
            }
        }
    }
}

/**
 * One card of the set. A tap adds a copy; once owned, a − n + stepper sits underneath. Missing
 * cards are greyed out; Trainers and Energy work the same but don't count toward the Pokédex.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickCard(
    card: SetCard,
    copies: Int,
    wanted: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit,
    onMinus: () -> Unit,
) {
    val owned = copies > 0
    val shape = RoundedCornerShape(8.dp)
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(CARD_ASPECT)
                .clip(shape)
                .border(if (owned) 3.dp else 1.dp, if (owned) DexYellow else ScreenLine, shape)
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = "Add one",
                    onLongClickLabel = "Details",
                    onClick = onTap,
                    onLongClick = onHold,
                )
                .semantics(mergeDescendants = true) {
                    selected = owned
                    contentDescription = buildString {
                        append("${card.card.name}, number ${card.card.number}")
                        if (owned) append(", $copies in collection") else append(", missing")
                        if (wanted) append(", on your wanted list")
                        if (!card.countsTowardDex) append(", doesn't count toward the Pokédex")
                    }
                }
                .padding(if (owned) 3.dp else 1.dp),
        ) {
            TcgCardImage(
                url = card.card.imageSmall,
                colorFilter = if (owned) null else Grayscale,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (owned) 1f else 0.55f),
            )
            Text(
                "#${card.card.number}",
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = ScreenText,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Screen.copy(alpha = 0.88f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
            if (owned) {
                CountBadge(copies, Modifier.align(Alignment.TopEnd).padding(4.dp))
            }
            if (wanted) {
                WantedStar(Modifier.align(Alignment.BottomEnd).padding(4.dp))
            }
            PlusOnePop(copies, Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(5.dp))
        if (owned) {
            Stepper(copies, onMinus = onMinus, onPlus = onTap)
        } else {
            Text(
                card.card.name,
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = ScreenTextDim,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun CountBadge(copies: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(DexYellow)
            .border(1.5.dp, Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$copies", style = DexText.Label.copy(fontSize = 11.sp), color = Ink)
    }
}

@Composable
private fun Stepper(copies: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Screen)
            .border(1.dp, ScreenLine, RoundedCornerShape(16.dp))
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Rounded.Remove, "One fewer", TabGray, onMinus)
        Text(
            "$copies",
            style = DexText.Screen,
            color = ScreenText,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepButton(Icons.Rounded.Add, "One more", DexYellow, onPlus)
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Ink, modifier = Modifier.size(18.dp))
    }
}

/** A "+1" that floats up and fades whenever the count goes up, so every tap visibly lands. */
@Composable
private fun PlusOnePop(copies: Int, modifier: Modifier = Modifier) {
    var previous by remember { mutableIntStateOf(copies) }
    val pop = remember { Animatable(0f) }
    LaunchedEffect(copies) {
        if (copies > previous) {
            pop.snapTo(1f)
            previous = copies
            pop.animateTo(0f, tween(700))
        } else {
            previous = copies
        }
    }
    if (pop.value > 0f) {
        Text(
            "+1",
            style = DexText.Label.copy(fontSize = 18.sp),
            color = Ink,
            modifier = modifier
                .graphicsLayer {
                    alpha = pop.value
                    translationY = -(1f - pop.value) * 48f
                }
                .clip(RoundedCornerShape(12.dp))
                .background(DexYellow)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}
