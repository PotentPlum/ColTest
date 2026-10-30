package com.potentplum.dextracker.ui.addcard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.potentplum.dextracker.domain.TcgCard
import com.potentplum.dextracker.ui.components.CardDetailDialog
import com.potentplum.dextracker.ui.components.Celebration
import com.potentplum.dextracker.ui.components.CelebrationBanner
import com.potentplum.dextracker.ui.components.DexInset
import com.potentplum.dextracker.ui.components.DexScaffold
import com.potentplum.dextracker.ui.components.DexScreen
import com.potentplum.dextracker.ui.components.DexSearchField
import com.potentplum.dextracker.ui.components.PokeballLoader
import com.potentplum.dextracker.ui.components.ScreenButton
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.components.TcgCardImage
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import kotlinx.coroutines.launch

private const val CARD_ASPECT = 0.716f

@Composable
fun AddCardScreen(
    viewModel: AddCardViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var celebration by remember { mutableStateOf<Celebration?>(null) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.changes.collect { change ->
            haptics.performHapticFeedback(
                if (change is CollectionChange.Added && change.newCatch) HapticFeedbackType.LongPress
                else HapticFeedbackType.TextHandleMove,
            )
            if (change is CollectionChange.Added && change.newCatch) celebration = change.toCelebration()
            // Each change gets its own Undo, so replace any snackbar still showing.
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
        AddCardActions(
            onQueryChange = viewModel::setQuery,
            onSelectSet = viewModel::selectSet,
            onToggleSort = viewModel::toggleSort,
            onToggleCard = viewModel::toggle,
            onRetry = { viewModel.load(forceRefresh = true) },
        )
    }
    AddCardContent(
        state = state,
        actions = actions,
        onBack = onBack,
        snackbarHostState = snackbar,
        celebration = celebration,
        onCelebrationDone = { celebration = null },
    )
}

private fun CollectionChange.message(): String {
    val label = "${card.setName} #${card.number}"
    return when (this) {
        is CollectionChange.Added -> "Added $label"
        is CollectionChange.Removed -> "Removed $label"
    }
}

private fun CollectionChange.Added.toCelebration(): Celebration {
    val who = pokemonName.ifBlank { card.name }
    return when (val gen = completedGeneration) {
        null -> Celebration("Gotcha!", "$who was registered in your Pokédex.")
        else -> Celebration("${gen.label} complete!", "$who was the last one. All ${gen.size} are registered!")
    }
}

class AddCardActions(
    val onQueryChange: (String) -> Unit,
    val onSelectSet: (setId: String?) -> Unit,
    val onToggleSort: () -> Unit,
    val onToggleCard: (TcgCard) -> Unit,
    val onRetry: () -> Unit,
)

@Composable
fun AddCardContent(
    state: AddCardUiState,
    actions: AddCardActions,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    celebration: Celebration? = null,
    onCelebrationDone: () -> Unit = {},
) {
    var preview by remember { mutableStateOf<TcgCard?>(null) }

    DexScaffold(
        title = if (state.pokemonName.isBlank()) "Add card" else "Add ${state.pokemonName} card",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) {
        Controls(state, actions)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp),
        ) {
            DexInset(Modifier.fillMaxSize()) {
                DexScreen(Modifier.fillMaxSize(), contentPadding = 0.dp) {
                    when {
                        state.loading -> PokeballLoader(
                            "Searching every ${state.pokemonName.ifBlank { "card" }} ever printed…",
                            Modifier.align(Alignment.Center),
                        )
                        state.failed -> ScreenMessage(
                            title = "No signal",
                            body = "The card database didn't answer. Check your connection and try again.",
                            actionLabel = "Retry",
                            onAction = actions.onRetry,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.totalValid == 0 -> ScreenMessage(
                            title = "No cards found",
                            body = "No valid cards are printed for this Pokémon yet.",
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.cards.isEmpty() -> ScreenMessage(
                            title = "No matches",
                            body = "Nothing matches your search or set filter.",
                            actionLabel = "Clear filters",
                            onAction = {
                                actions.onQueryChange("")
                                actions.onSelectSet(null)
                            },
                            modifier = Modifier.align(Alignment.Center),
                        )
                        else -> ResultsGrid(
                            state = state,
                            onCardClick = actions.onToggleCard,
                            onCardLongClick = { preview = it },
                        )
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

private fun TcgCard.subtitle() =
    listOfNotNull(setName, "#$number", releaseYear, rarity).joinToString(" · ")

@Composable
private fun Controls(state: AddCardUiState, actions: AddCardActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DexSearchField(
            query = state.query,
            onQueryChange = actions.onQueryChange,
            placeholder = "Search set, number, rarity…",
            contentDescription = "Search cards",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SetDropdown(
                sets = state.sets,
                selectedSetId = state.selectedSetId,
                totalCount = state.totalValid,
                onSelect = actions.onSelectSet,
                modifier = Modifier.weight(1f),
            )
            ScreenButton(
                onClick = actions.onToggleSort,
                modifier = Modifier.semantics {
                    contentDescription = "Sort by year, currently ${state.sortOrder.label}"
                },
            ) {
                Icon(Icons.Rounded.SwapVert, contentDescription = null, tint = ScreenGlow, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (state.sortOrder == SortOrder.OLDEST_FIRST) "OLDEST" else "NEWEST",
                    style = DexText.ScreenSmall.copy(fontSize = 12.sp),
                )
            }
        }
        if (!state.loading && !state.failed && state.totalValid > 0) {
            val ownedHere = state.cards.count { it.id in state.ownedIds }
            Text(
                text = buildString {
                    if (state.cards.size == state.totalValid) append("${state.totalValid} CARDS")
                    else append("${state.cards.size} OF ${state.totalValid}")
                    append(" · $ownedHere OWNED · TAP TO ADD, HOLD TO ZOOM")
                },
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SetDropdown(
    sets: List<SetOption>,
    selectedSetId: String?,
    totalCount: Int,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = sets.firstOrNull { it.id == selectedSetId }
    Box(modifier) {
        ScreenButton(
            onClick = { expanded = true },
            enabled = sets.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Filter by set, ${selected?.name ?: "all sets"}" },
        ) {
            Text(
                text = selected?.let { "${it.name} (${it.year})" } ?: "ALL SETS",
                style = DexText.ScreenSmall.copy(fontSize = 12.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = ScreenGlow)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Screen,
            modifier = Modifier.heightIn(max = 420.dp),
        ) {
            SetMenuItem("All sets", "$totalCount", selectedSetId == null) {
                onSelect(null)
                expanded = false
            }
            sets.forEach { set ->
                SetMenuItem("${set.name} · ${set.year}", "${set.count}", set.id == selectedSetId) {
                    onSelect(set.id)
                    expanded = false
                }
            }
        }
    }
}

@Composable
private fun SetMenuItem(label: String, count: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = DexText.ScreenSmall.copy(fontSize = 13.sp),
                    color = if (selected) DexYellow else ScreenText,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(12.dp))
                Text(count, style = DexText.ScreenSmall, color = ScreenTextDim)
            }
        },
        leadingIcon = {
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = DexYellow)
        },
        onClick = onClick,
    )
}

@Composable
private fun ResultsGrid(
    state: AddCardUiState,
    onCardClick: (TcgCard) -> Unit,
    onCardLongClick: (TcgCard) -> Unit,
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(state.selectedSetId, state.sortOrder) { gridState.scrollToItem(0) }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 4.dp,
            // Space for the Undo snackbar so the last row stays reachable.
            bottom = 80.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.cards, key = { it.id }) { card ->
            ResultCard(
                card = card,
                owned = card.id in state.ownedIds,
                onClick = { onCardClick(card) },
                onLongClick = { onCardLongClick(card) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultCard(
    card: TcgCard,
    owned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = if (owned) "Remove from collection" else "Add to collection",
                onLongClickLabel = "Zoom",
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics(mergeDescendants = true) {
                this.selected = owned
                contentDescription = buildString {
                    append("${card.name}, ${card.subtitle()}")
                    if (owned) append(", already in collection")
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(CARD_ASPECT)
                .clip(shape)
                .border(
                    width = if (owned) 3.dp else 1.dp,
                    color = if (owned) DexYellow else ScreenLine,
                    shape = shape,
                )
                .padding(if (owned) 3.dp else 1.dp),
        ) {
            TcgCardImage(url = card.imageSmall, modifier = Modifier.fillMaxSize())
            AnimatedOwnedTag(
                visible = owned,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            card.setName,
            style = DexText.ScreenSmall,
            color = if (owned) DexYellow else ScreenText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            "${card.releaseYear} · #${card.number}",
            style = DexText.ScreenSmall.copy(fontSize = 10.sp),
            color = ScreenTextDim,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/** Pops the OWNED tag in when a card is added, so the tap visibly lands. */
@Composable
private fun AnimatedOwnedTag(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.4f) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier,
    ) {
        OwnedTag()
    }
}

@Composable
private fun OwnedTag(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .clip(shape)
            .background(DexYellow)
            .border(1.5.dp, Ink, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(2.dp))
        Text("OWNED", style = DexText.Label.copy(fontSize = 10.sp), color = Ink)
    }
}

