package com.potentplum.dextracker.ui.addcard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.potentplum.dextracker.domain.TcgCard
import com.potentplum.dextracker.ui.components.CardDetailDialog
import com.potentplum.dextracker.ui.components.DexButton
import com.potentplum.dextracker.ui.components.DexButtonStyle
import com.potentplum.dextracker.ui.components.DexInset
import com.potentplum.dextracker.ui.components.DexScaffold
import com.potentplum.dextracker.ui.components.DexScreen
import com.potentplum.dextracker.ui.components.PokeballLoader
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.components.TcgCardImage
import com.potentplum.dextracker.ui.theme.Bezel
import com.potentplum.dextracker.ui.theme.BezelSilver
import com.potentplum.dextracker.ui.theme.BezelSilverDark
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim

private const val CARD_ASPECT = 0.716f

@Composable
fun AddCardScreen(
    viewModel: AddCardViewModel,
    onBack: () -> Unit,
    onAdded: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.added.collect { onAdded() }
    }
    val actions = remember(viewModel) {
        AddCardActions(
            onQueryChange = viewModel::setQuery,
            onSelectSet = viewModel::selectSet,
            onToggleSort = viewModel::toggleSort,
            onSelectCard = viewModel::select,
            onConfirm = viewModel::confirmAdd,
            onRetry = { viewModel.load(forceRefresh = true) },
        )
    }
    AddCardContent(state, actions, onBack)
}

class AddCardActions(
    val onQueryChange: (String) -> Unit,
    val onSelectSet: (setId: String?) -> Unit,
    val onToggleSort: () -> Unit,
    val onSelectCard: (TcgCard?) -> Unit,
    val onConfirm: () -> Unit,
    val onRetry: () -> Unit,
)

@Composable
fun AddCardContent(state: AddCardUiState, actions: AddCardActions, onBack: () -> Unit) {
    var preview by remember { mutableStateOf<TcgCard?>(null) }

    // Back first dismisses the confirmation, then leaves the screen.
    BackHandler(enabled = state.selected != null) { actions.onSelectCard(null) }

    DexScaffold(
        title = if (state.pokemonName.isBlank()) "Add card" else "Add ${state.pokemonName} card",
        onBack = onBack,
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
                            onCardClick = actions.onSelectCard,
                            onCardLongClick = { preview = it },
                        )
                    }
                }
            }
            ConfirmOverlay(
                selected = state.selected,
                ownedIds = state.ownedIds,
                onPreview = { preview = it },
                onCancel = { actions.onSelectCard(null) },
                onConfirm = actions.onConfirm,
                modifier = Modifier.align(Alignment.BottomCenter),
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

/** Slides the confirmation panel in from the bottom while a card is selected. */
@Composable
private fun ConfirmOverlay(
    selected: TcgCard?,
    ownedIds: Set<String>,
    onPreview: (TcgCard) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep showing the last card while the panel animates out.
    var lastSelected by remember { mutableStateOf(selected) }
    if (selected != null) lastSelected = selected
    AnimatedVisibility(
        visible = selected != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        lastSelected?.let { card ->
            ConfirmPanel(
                card = card,
                alreadyOwned = card.id in ownedIds,
                onPreview = { onPreview(card) },
                onCancel = onCancel,
                onConfirm = onConfirm,
            )
        }
    }
}

private fun TcgCard.subtitle() =
    listOfNotNull(setName, "#$number", releaseYear, rarity).joinToString(" · ")

@Composable
private fun Controls(state: AddCardUiState, actions: AddCardActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SearchField(query = state.query, onQueryChange = actions.onQueryChange)
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
            Text(
                text = if (state.cards.size == state.totalValid) {
                    "${state.totalValid} CARDS · ${state.sets.size} SETS · TAP TO ADD, HOLD TO ZOOM"
                } else {
                    "${state.cards.size} OF ${state.totalValid} CARDS · TAP TO ADD, HOLD TO ZOOM"
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
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    DexScreen(Modifier.fillMaxWidth(), contentPadding = 0.dp, showGrid = false, shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = ScreenTextDim)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search set, number, rarity…", style = DexText.Screen, color = ScreenTextDim)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = DexText.Screen,
                    cursorBrush = SolidColor(ScreenGlow),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focus.clearFocus() },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Search cards" },
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    tint = ScreenTextDim,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onQueryChange("") }
                        .padding(6.dp),
                )
            }
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

/** Small dark-screen button used for the filter controls. */
@Composable
private fun ScreenButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(Bezel)
            .padding(3.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Screen, ScreenDeep)))
            .border(1.dp, ScreenLine, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
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
            // Space for the confirmation panel so the last row stays reachable.
            bottom = if (state.selected != null) 200.dp else 12.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.cards, key = { it.id }) { card ->
            ResultCard(
                card = card,
                selected = card.id == state.selected?.id,
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
    selected: Boolean,
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
                onClickLabel = "Select",
                onLongClickLabel = "Zoom",
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics(mergeDescendants = true) {
                this.selected = selected
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
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) DexYellow else ScreenLine,
                    shape = shape,
                )
                .padding(if (selected) 3.dp else 1.dp),
        ) {
            TcgCardImage(url = card.imageSmall, modifier = Modifier.fillMaxSize())
            if (owned) OwnedTag(Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            card.setName,
            style = DexText.ScreenSmall,
            color = if (selected) DexYellow else ScreenText,
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

@Composable
private fun ConfirmPanel(
    card: TcgCard,
    alreadyOwned: Boolean,
    onPreview: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(BezelSilver, BezelSilverDark)))
            .border(3.dp, Bezel, shape)
            .padding(10.dp),
    ) {
        DexScreen(Modifier.fillMaxWidth(), showGrid = false, contentPadding = 12.dp) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TcgCardImage(
                        url = card.imageSmall,
                        contentDescription = "Zoom ${card.name}",
                        modifier = Modifier
                            .width(64.dp)
                            .aspectRatio(CARD_ASPECT)
                            .clickable(onClickLabel = "Zoom", onClick = onPreview),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (alreadyOwned) "ALREADY IN YOUR COLLECTION" else "ADD THIS CARD TO YOUR COLLECTION?",
                            style = DexText.Screen.copy(fontSize = 14.sp),
                            color = ScreenText,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(card.subtitle(), style = DexText.ScreenSmall, color = ScreenTextDim)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DexButton(
                        "Cancel",
                        onClick = onCancel,
                        style = DexButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    DexButton(
                        if (alreadyOwned) "Owned" else "Add to collection",
                        onClick = onConfirm,
                        enabled = !alreadyOwned,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        }
    }
}
