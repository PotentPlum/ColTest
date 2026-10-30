package com.potentplum.dextracker.ui.wanted

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.potentplum.dextracker.data.local.WantedCardEntity
import com.potentplum.dextracker.data.variantList
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardVariant
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
import com.potentplum.dextracker.ui.components.QuickChooser
import com.potentplum.dextracker.ui.components.ScreenButton
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.components.SheetCard
import com.potentplum.dextracker.ui.components.TcgCardImage
import com.potentplum.dextracker.ui.components.TopTab
import com.potentplum.dextracker.ui.components.copyLabel
import com.potentplum.dextracker.ui.components.euro
import com.potentplum.dextracker.ui.theme.DexRedDark
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.DexYellowDark
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import kotlinx.coroutines.launch

@Composable
fun WantedScreen(
    viewModel: WantedViewModel,
    onSelectTab: (TopTab) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var celebration by remember { mutableStateOf<Celebration?>(null) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.changes.collect { change ->
            if (change is WantedChange.GotIt) {
                haptics.performHapticFeedback(
                    if (change.newCatch) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                )
                if (change.newCatch) celebration = change.toCelebration()
            }
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
        WantedActions(
            onSelectFilter = viewModel::selectFilter,
            onNextSort = viewModel::nextSort,
            onOpen = viewModel::openSheet,
            onGotIt = viewModel::gotIt,
            onRemove = viewModel::remove,
            onAdd = viewModel::add,
            onChangeQuantity = viewModel::changeQuantity,
            onEditCopy = viewModel::editCopy,
            onCloseSheet = viewModel::closeSheet,
            onCloseChooser = viewModel::closeChooser,
            onConditionChange = viewModel::setDefaultCondition,
            onLanguageChange = viewModel::setDefaultLanguage,
        )
    }
    WantedContent(
        state = state,
        actions = actions,
        onSelectTab = onSelectTab,
        snackbarHostState = snackbar,
        celebration = celebration,
        onCelebrationDone = { celebration = null },
    )
}

private fun WantedChange.message(): String = when (this) {
    is WantedChange.GotIt -> "Got ${card.name} #${card.number} · ${copyLabel(key.condition, key.language)}"
    is WantedChange.Removed -> "${card.name} #${card.number} is off your wanted list"
}

private fun WantedChange.GotIt.toCelebration(): Celebration {
    val who = pokemonName.ifBlank { card.name }
    return when (val gen = completedGeneration) {
        null -> Celebration("Gotcha!", "$who was registered in your Pokédex.")
        else -> Celebration("${gen.label} complete!", "$who was the last one. All ${gen.size} are registered!")
    }
}

class WantedActions(
    val onSelectFilter: (WantedFilter) -> Unit,
    val onNextSort: () -> Unit,
    val onOpen: (WantedCardEntity) -> Unit,
    val onGotIt: (WantedCardEntity) -> Unit,
    val onRemove: (WantedCardEntity) -> Unit,
    val onAdd: (WantedCardEntity, CardVariant) -> Unit,
    val onChangeQuantity: (copyId: Long, delta: Int) -> Unit,
    val onEditCopy: (copyId: Long, CardVariant, CardCondition, CardLanguage) -> Unit,
    val onCloseSheet: () -> Unit,
    val onCloseChooser: () -> Unit,
    val onConditionChange: (CardCondition) -> Unit,
    val onLanguageChange: (CardLanguage) -> Unit,
)

@Composable
fun WantedContent(
    state: WantedUiState,
    actions: WantedActions,
    onSelectTab: (TopTab) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    celebration: Celebration? = null,
    onCelebrationDone: () -> Unit = {},
) {
    var preview by remember { mutableStateOf<WantedCardEntity?>(null) }

    DexScaffold(
        title = "Wanted",
        snackbarHostState = snackbarHostState,
        bottomBar = { DexTabBar(TopTab.WANTED, onSelectTab) },
    ) {
        SummaryPanel(state)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DexSegmentedControl(
                options = WantedFilter.entries,
                selected = state.filter,
                label = { if (it == WantedFilter.NEW_CATCH) "${it.label} (${state.newCatchCount})" else it.label },
                onSelect = actions.onSelectFilter,
                modifier = Modifier.weight(1f),
            )
            ScreenButton(
                onClick = actions.onNextSort,
                modifier = Modifier.semantics { contentDescription = "Sort by ${state.sort.label}, tap to change" },
            ) {
                Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = ScreenGlow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(state.sort.label.uppercase(), style = DexText.ScreenSmall.copy(fontSize = 12.sp))
            }
        }
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
                        state.loading -> Unit
                        state.totalCount == 0 -> ScreenMessage(
                            title = "Nothing wanted yet",
                            body = "Hold a card in Sets, or open one in a Pokémon's card list, and tap " +
                                "\"Want this card\". It shows up here with its price.",
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.entries.isEmpty() -> ScreenMessage(
                            title = "No new catches",
                            body = "Every wanted card is of a Pokémon you've already caught.",
                            actionLabel = "Show all",
                            onAction = { actions.onSelectFilter(WantedFilter.ALL) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                        else -> WantedList(state, actions)
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
            copies = state.owned[card.cardId]?.copies.orEmpty(),
            condition = state.condition,
            language = state.language,
            onConditionChange = actions.onConditionChange,
            onLanguageChange = actions.onLanguageChange,
            onAdd = { variant -> actions.onAdd(card, variant) },
            onChangeQuantity = actions.onChangeQuantity,
            onEditCopy = actions.onEditCopy,
            onZoom = { preview = card },
            onDismiss = actions.onCloseSheet,
            wanted = true,
            onToggleWanted = {
                actions.onCloseSheet()
                actions.onRemove(card)
            },
        )
    }

    state.choosingFor?.let { card ->
        QuickChooser(
            title = card.name,
            subtitle = "#${card.number} · Which version did you get? Adds a ${copyLabel(state.condition, state.language)} copy.",
            options = CardVariant.optionsFor(card.variantList).map { variant ->
                ChooserOption(label = variant.label, icon = Icons.Rounded.Add, onClick = { actions.onAdd(card, variant) })
            },
            onDismiss = actions.onCloseChooser,
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

private fun WantedCardEntity.subtitle() =
    listOfNotNull(setName, "#$number", releaseDate.take(4), rarity).joinToString(" · ")

private fun WantedCardEntity.toSheetCard() = SheetCard(
    cardId = cardId,
    name = name,
    subtitle = subtitle(),
    imageSmall = imageSmall,
    imageLarge = imageLarge,
    variantOptions = CardVariant.optionsFor(variantList),
)

@Composable
private fun SummaryPanel(state: WantedUiState) {
    DexScreen(Modifier.fillMaxWidth(), showGrid = false, contentPadding = 12.dp) {
        Row(
            Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text("WANTED CARDS", style = DexText.ScreenSmall, color = ScreenTextDim)
                Text(
                    if (state.totalCount == 1) "1 CARD" else "${state.totalCount} CARDS",
                    style = DexText.ScreenLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
                    color = ScreenGlow,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("TOTAL TREND", style = DexText.ScreenSmall, color = ScreenTextDim)
                Text(
                    euro(state.totalPrice),
                    style = DexText.ScreenLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
                    color = DexYellow,
                )
            }
        }
    }
}

@Composable
private fun WantedList(state: WantedUiState, actions: WantedActions) {
    // New catches first, since registering every Pokémon is the point of the app.
    val grouped = state.filter == WantedFilter.ALL && state.newCatchCount in 1 until state.totalCount
    val (fresh, rest) = if (grouped) state.entries.partition { it.newCatch } else state.entries to emptyList()
    LazyColumn(
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (grouped) item(key = "head-new") { GroupHeading("New Pokémon for your Dex", fresh.size) }
        items(fresh, key = { it.card.cardId }) { entry -> SwipeableRow(entry, actions) }
        if (grouped) item(key = "head-rest") { GroupHeading("More cards", rest.size, Modifier.padding(top = 8.dp)) }
        items(rest, key = { it.card.cardId }) { entry -> SwipeableRow(entry, actions) }
        item(key = "hint") {
            Text(
                "SWIPE A CARD AWAY TO REMOVE IT · TAP FOR DETAILS",
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = ScreenTextDim,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 70.dp),
            )
        }
    }
}

@Composable
private fun GroupHeading(text: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 2.dp)) {
        Text(text.uppercase(), style = DexText.ScreenSmall, color = ScreenGlow, modifier = Modifier.weight(1f))
        Text("$count", style = DexText.ScreenSmall, color = ScreenTextDim)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRow(entry: WantedEntry, actions: WantedActions) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) actions.onRemove(entry.card)
            value != SwipeToDismissBoxValue.Settled
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DexRedDark)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = ScreenText)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = ScreenText)
            }
        },
    ) {
        WantedRow(entry, actions)
    }
}

@Composable
private fun WantedRow(entry: WantedEntry, actions: WantedActions) {
    val card = entry.card
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Screen)
            .border(1.dp, ScreenLine, shape)
            .clickable(onClickLabel = "Details", onClick = { actions.onOpen(card) })
            .padding(start = 7.dp, end = 8.dp, top = 7.dp, bottom = 7.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("${card.name}, ${card.setName} number ${card.number}")
                    entry.price?.let { append(", ${euro(it)}") }
                    if (entry.newCatch) append(", new catch")
                }
                customActions = listOf(
                    CustomAccessibilityAction("Got it") { actions.onGotIt(card); true },
                    CustomAccessibilityAction("Remove from wanted list") { actions.onRemove(card); true },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TcgCardImage(
            url = card.imageSmall,
            modifier = Modifier
                .width(46.dp)
                .aspectRatio(0.716f),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    card.name,
                    style = DexText.Screen,
                    color = ScreenText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (entry.newCatch) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "NEW CATCH",
                        style = DexText.Label.copy(fontSize = 9.sp),
                        color = Ink,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DexYellow)
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                listOfNotNull("${card.setName} · #${card.number}", card.rarity).joinToString(" · "),
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = ScreenTextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.price?.let { euro(it) } ?: "No price yet",
                style = DexText.ScreenSmall,
                color = if (entry.price != null) ScreenGlow else ScreenTextDim,
            )
        }
        Spacer(Modifier.width(8.dp))
        GotItButton(onClick = { actions.onGotIt(card) })
    }
}

@Composable
private fun GotItButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .height(36.dp)
            .clip(shape)
            .background(DexYellow)
            .border(1.dp, DexYellowDark, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("GOT IT", style = DexText.Label.copy(fontSize = 11.sp), color = Ink)
    }
}
