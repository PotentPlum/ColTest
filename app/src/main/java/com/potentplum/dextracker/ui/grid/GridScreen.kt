package com.potentplum.dextracker.ui.grid

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.potentplum.dextracker.data.local.PokemonWithCount
import com.potentplum.dextracker.data.remote.PokeApiService
import com.potentplum.dextracker.domain.Generation
import com.potentplum.dextracker.domain.dexLabel
import com.potentplum.dextracker.ui.components.DexInset
import com.potentplum.dextracker.ui.components.DexScaffold
import com.potentplum.dextracker.ui.components.DexScreen
import com.potentplum.dextracker.ui.components.DexSearchField
import com.potentplum.dextracker.ui.components.DexSegmentedControl
import com.potentplum.dextracker.ui.components.DexTab
import com.potentplum.dextracker.ui.components.DexTabBar
import com.potentplum.dextracker.ui.components.PokeballLoader
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.components.ShellIconButton
import com.potentplum.dextracker.ui.components.TopTab
import com.potentplum.dextracker.ui.theme.Bezel
import com.potentplum.dextracker.ui.theme.BezelLight
import com.potentplum.dextracker.ui.theme.BezelSilver
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.LedRed
import com.potentplum.dextracker.ui.theme.MissingTile
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import java.time.LocalDate

@Composable
fun GridScreen(
    viewModel: GridViewModel,
    onPokemonClick: (Int) -> Unit,
    onSelectTab: (TopTab) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> uri?.let(viewModel::importFrom) }

    val actions = remember(viewModel) {
        GridActions(
            onSelectScope = viewModel::selectScope,
            onSelectFilter = viewModel::selectFilter,
            onQueryChange = viewModel::updateQuery,
            onClearFilters = viewModel::clearFilters,
            onRetry = viewModel::syncSpecies,
            onPokemonClick = onPokemonClick,
            onExport = { exportLauncher.launch("pokedex-tracker-backup-${LocalDate.now()}.json") },
            // Some file managers label .json files as plain text or a generic binary.
            onImport = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
            onToggleRegionalForms = viewModel::setIncludeRegionalForms,
            onStartOfflineDownload = viewModel::startOfflineDownload,
            onCancelOfflineDownload = viewModel::cancelOfflineDownload,
            onSelectTab = onSelectTab,
        )
    }
    GridContent(state, actions, snackbar, query = viewModel.query)
}

class GridActions(
    val onSelectScope: (Generation?) -> Unit,
    val onSelectFilter: (CaughtFilter) -> Unit,
    val onQueryChange: (String) -> Unit,
    val onClearFilters: () -> Unit,
    val onRetry: () -> Unit,
    val onPokemonClick: (Int) -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onToggleRegionalForms: (Boolean) -> Unit,
    val onStartOfflineDownload: () -> Unit,
    val onCancelOfflineDownload: () -> Unit,
    val onSelectTab: (TopTab) -> Unit,
)

@Composable
fun GridContent(
    state: GridUiState,
    actions: GridActions,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    /** The search box text, which runs ahead of [GridUiState.query] while typing. */
    query: String = state.query,
) {
    // Hoisted so the position survives a trip to a detail page and back.
    val gridState = rememberLazyGridState()
    val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
    // While typing, the keyboard takes half the screen: fold away everything but the search
    // box and the results. Generation tabs don't apply to a search, so they stay hidden
    // for as long as there's one.
    var typing by remember { mutableStateOf(false) }
    val searching = query.isNotBlank()

    // Only a real change of view resets the scroll. Coming back from a detail page
    // re-runs this effect with the same key, and must keep the user where they were.
    val viewKey = "${state.scope}|${state.filter}|${state.query.trim()}"
    var lastViewKey by rememberSaveable { mutableStateOf(viewKey) }
    LaunchedEffect(viewKey) {
        if (viewKey != lastViewKey) {
            lastViewKey = viewKey
            gridState.scrollToItem(0)
        }
    }

    DexScaffold(
        title = "Pokédex Tracker",
        compact = scrolled || typing,
        actions = { OverflowMenu(state, actions) },
        snackbarHostState = snackbarHostState,
        bottomBar = { DexTabBar(TopTab.POKEDEX, actions.onSelectTab) },
    ) {
        AnimatedVisibility(visible = !typing) {
            Column {
                NationalCounter(state, onCancelDownload = actions.onCancelOfflineDownload)
                Spacer(Modifier.height(8.dp))
            }
        }
        DexSearchField(
            query = query,
            onQueryChange = actions.onQueryChange,
            placeholder = "Find a Pokémon: name or #",
            contentDescription = "Search Pokémon by name or number",
            // Pressing search with a single result opens it straight away.
            onSearch = { state.pokemon.singleOrNull()?.let { actions.onPokemonClick(it.dex) } },
            onFocusChange = { typing = it },
        )
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(visible = !searching && !typing) {
            GenerationTabs(state, actions.onSelectScope)
        }
        DexSegmentedControl(
            options = CaughtFilter.entries,
            selected = state.filter,
            label = { it.label },
            onSelect = actions.onSelectFilter,
        )
        Spacer(Modifier.height(8.dp))
        DexInset(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp),
        ) {
            when {
                state.pokemon.isNotEmpty() -> PokemonGrid(state.pokemon, gridState, actions.onPokemonClick)
                state.searching || state.filter != CaughtFilter.ALL -> EmptyResults(
                    state = state,
                    onClear = actions.onClearFilters,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.syncing -> PokeballLoader("Downloading Pokédex data…", Modifier.align(Alignment.Center))
                else -> DexScreen(Modifier.align(Alignment.Center)) {
                    ScreenMessage(
                        title = "No signal",
                        body = "Couldn't reach PokeAPI. The Pokédex is downloaded once, then works offline.",
                        actionLabel = "Retry",
                        onAction = actions.onRetry,
                    )
                }
            }
        }
    }
}

/** National Dex progress, always visible whatever tab is selected. */
@Composable
private fun NationalCounter(state: GridUiState, onCancelDownload: () -> Unit) {
    val national = state.national
    val percent = "%.1f%%".format(national.fraction * 100)
    DexScreen(Modifier.fillMaxWidth(), contentPadding = 10.dp, showGrid = false) {
        Column(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        "National Dex: ${national.caught} of ${national.total} caught, $percent"
                },
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("NATIONAL DEX", style = DexText.ScreenSmall, color = ScreenTextDim)
                Spacer(Modifier.weight(1f))
                if (!state.online) {
                    Text("● OFFLINE  ", style = DexText.ScreenSmall, color = LedRed)
                }
                Text(percent, style = DexText.ScreenSmall, color = ScreenTextDim)
            }
            Text(
                text = "${national.caught} / ${national.total} CAUGHT",
                style = DexText.ScreenLarge,
                color = ScreenGlow,
            )
            Spacer(Modifier.height(6.dp))
            ProgressBar(national.fraction)
            if (state.download.running) {
                DownloadLine(state.download.done, state.download.total, onCancelDownload)
            }
        }
    }
}

/** Progress of saving the Pokédex for offline use, with a way to stop it. */
@Composable
private fun DownloadLine(done: Int, total: Int, onCancel: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (total == 0) "SAVING FOR OFFLINE…" else "SAVING FOR OFFLINE $done/$total",
            style = DexText.ScreenSmall,
            color = ScreenTextDim,
            modifier = Modifier.weight(1f),
        )
        Text(
            "STOP",
            style = DexText.Label.copy(fontSize = 11.sp),
            color = DexYellow,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClickLabel = "Stop saving for offline", onClick = onCancel)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(ScreenLine),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(CircleShape)
                .background(ScreenGlow),
        )
    }
}

/** "All" plus one tab per generation, each with its own caught count; scrolls sideways. */
@Composable
private fun GenerationTabs(state: GridUiState, onSelect: (Generation?) -> Unit) {
    val tabs = listOf(state.national) + state.generations
    val listState = rememberLazyListState()
    LaunchedEffect(state.scope) {
        val index = tabs.indexOfFirst { it.generation == state.scope }
        if (index >= 0) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(tabs, key = { it.generation?.number ?: 0 }) { progress ->
            DexTab(
                text = progress.label,
                subtitle = "${progress.caught}/${progress.total}" + if (progress.complete) " ✓" else "",
                // While searching, results span every generation, so no tab is active.
                selected = progress.generation == state.scope && !state.searching,
                onClick = { onSelect(progress.generation) },
                modifier = Modifier
                    .width(if (progress.generation == null) 88.dp else 80.dp)
                    .semantics {
                        contentDescription = "${progress.label}, ${progress.caught} of ${progress.total} caught"
                    },
            )
        }
    }
}

@Composable
private fun EmptyResults(state: GridUiState, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val scopeName = if (state.scope == null) "National Dex" else state.scope.label
    DexScreen(modifier) {
        when {
            !state.searching && state.filter == CaughtFilter.MISSING -> ScreenMessage(
                title = "$scopeName complete!",
                body = "Every Pokémon here is registered. Nothing left to catch.",
            )
            !state.searching && state.filter == CaughtFilter.CAUGHT -> ScreenMessage(
                title = "Nothing caught yet",
                body = "Open a Pokémon and add a card to register it.",
                actionLabel = "Show all",
                onAction = onClear,
            )
            else -> ScreenMessage(
                title = "No matches",
                body = "No Pokémon matches \"${state.query.trim()}\"" +
                    if (state.filter != CaughtFilter.ALL) " in ${state.filter.label.lowercase()}." else ".",
                actionLabel = "Clear search",
                onAction = onClear,
            )
        }
    }
}

@Composable
private fun OverflowMenu(state: GridUiState, actions: GridActions) {
    val includeRegionalForms = state.includeRegionalForms
    var expanded by remember { mutableStateOf(false) }
    Box {
        ShellIconButton(Icons.Rounded.MoreVert, contentDescription = "More options", onClick = { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Screen,
        ) {
            MenuItem("Back up collection", Icons.Rounded.FileUpload) {
                expanded = false
                actions.onExport()
            }
            MenuItem("Restore from backup", Icons.Rounded.FileDownload) {
                expanded = false
                actions.onImport()
            }
            HorizontalDivider(color = ScreenLine)
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            if (state.download.running) "Saving for offline…" else "Save Pokédex for offline",
                            style = DexText.ScreenSmall.copy(fontSize = 13.sp),
                            color = ScreenText,
                        )
                        Text(
                            when {
                                state.allSaved -> "All ${Generation.MAX_DEX} pages saved"
                                else -> "${state.savedPages} of ${Generation.MAX_DEX} pages saved · needs a connection"
                            },
                            style = DexText.ScreenSmall.copy(fontSize = 11.sp),
                            color = ScreenTextDim,
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        if (state.allSaved) Icons.Rounded.OfflinePin else Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = ScreenGlow,
                    )
                },
                enabled = state.online && !state.download.running,
                onClick = {
                    expanded = false
                    actions.onStartOfflineDownload()
                },
            )
            HorizontalDivider(color = ScreenLine)
            DropdownMenuItem(
                text = {
                    Column {
                        Text("Count regional forms", style = DexText.ScreenSmall.copy(fontSize = 13.sp), color = ScreenText)
                        Text(
                            "Alolan, Galarian, Hisuian and Paldean cards count toward the base Pokémon",
                            style = DexText.ScreenSmall.copy(fontSize = 11.sp),
                            color = ScreenTextDim,
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        if (includeRegionalForms) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (includeRegionalForms) DexYellow else ScreenTextDim,
                    )
                },
                onClick = { actions.onToggleRegionalForms(!includeRegionalForms) },
                modifier = Modifier.semantics {
                    contentDescription = "Count regional forms, " + if (includeRegionalForms) "on" else "off"
                },
            )
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = DexText.ScreenSmall.copy(fontSize = 13.sp), color = ScreenText) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = ScreenGlow) },
        onClick = onClick,
    )
}

@Composable
private fun PokemonGrid(
    pokemon: List<PokemonWithCount>,
    gridState: LazyGridState,
    onPokemonClick: (Int) -> Unit,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 96.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(pokemon, key = { it.dex }) { entry ->
            PokemonTile(entry, onClick = { onPokemonClick(entry.dex) })
        }
    }
}

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun PokemonTile(pokemon: PokemonWithCount, onClick: () -> Unit) {
    val collected = pokemon.cardCount > 0
    val shape = RoundedCornerShape(12.dp)
    val description = buildString {
        append("${pokemon.name}, number ${pokemon.dex}. ")
        append(
            when (pokemon.cardCount) {
                0 -> "Not collected"
                1 -> "1 card collected"
                else -> "${pokemon.cardCount} cards collected"
            }
        )
    }
    Box(
        modifier = Modifier
            .aspectRatio(0.84f)
            .clip(shape)
            .background(
                if (collected) Brush.verticalGradient(listOf(Screen, ScreenDeep))
                else Brush.verticalGradient(listOf(MissingTile, Bezel)),
            )
            .border(3.dp, if (collected) BezelSilver else BezelLight, shape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        AsyncImage(
            model = PokeApiService.spriteUrl(pokemon.dex),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            colorFilter = if (collected) null else grayscale,
            alpha = if (collected) 1f else 0.45f,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 22.dp),
        )
        Text(
            text = pokemon.dex.dexLabel(),
            style = DexText.ScreenSmall.copy(fontSize = 11.sp),
            color = if (collected) ScreenGlow else ScreenTextDim,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 6.dp),
        )
        Text(
            text = pokemon.name,
            style = DexText.ScreenSmall.copy(fontSize = 11.sp),
            color = if (collected) ScreenText else ScreenTextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
        )
        if (collected) {
            CountBadge(
                count = pokemon.cardCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 5.dp),
            )
        }
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(DexYellow)
            .border(2.dp, Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = DexText.Label.copy(fontSize = if (count > 9) 9.sp else 11.sp),
            color = Ink,
        )
    }
}
