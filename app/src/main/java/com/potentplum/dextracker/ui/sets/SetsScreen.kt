package com.potentplum.dextracker.ui.sets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.potentplum.dextracker.domain.CardSet
import com.potentplum.dextracker.ui.components.DexInset
import com.potentplum.dextracker.ui.components.DexScaffold
import com.potentplum.dextracker.ui.components.DexScreen
import com.potentplum.dextracker.ui.components.DexSearchField
import com.potentplum.dextracker.ui.components.DexSegmentedControl
import com.potentplum.dextracker.ui.components.DexTabBar
import com.potentplum.dextracker.ui.components.HideKeyboardOnDrag
import com.potentplum.dextracker.ui.components.PokeballLoader
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.components.TopTab
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenText
import com.potentplum.dextracker.ui.theme.ScreenTextDim
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SetsScreen(
    viewModel: SetsViewModel,
    onSelectTab: (TopTab) -> Unit,
    onOpenSet: (setId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SetsContent(
        state = state,
        query = viewModel.query,
        onQueryChange = viewModel::updateQuery,
        onSelectFilter = viewModel::selectFilter,
        onRetry = { viewModel.load(forceRefresh = true) },
        onOpenSet = onOpenSet,
        onSelectTab = onSelectTab,
    )
}

@Composable
fun SetsContent(
    state: SetsUiState,
    onQueryChange: (String) -> Unit,
    onSelectFilter: (SetFilter) -> Unit,
    onRetry: () -> Unit,
    onOpenSet: (setId: String) -> Unit,
    onSelectTab: (TopTab) -> Unit,
    /** The search box text, which runs ahead of [SetsUiState.query] while typing. */
    query: String = state.query,
) {
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var typing by remember { mutableStateOf(false) }
    LaunchedEffect(state.filter, state.query) { listState.scrollToItem(0) }

    DexScaffold(
        title = "Sets",
        compact = scrolled || typing,
        bottomBar = { DexTabBar(TopTab.SETS, onSelectTab) },
    ) {
        DexSearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Find a set: name or code",
            contentDescription = "Search sets",
            onFocusChange = { typing = it },
        )
        Spacer(Modifier.height(8.dp))
        DexSegmentedControl(
            options = SetFilter.entries,
            selected = state.filter,
            label = { it.label },
            onSelect = onSelectFilter,
        )
        if (!state.loading && !state.failed) {
            Text(
                if (state.rows.size == state.totalSets) "${state.totalSets} SETS · NEWEST FIRST · TAP ONE TO QUICK ADD"
                else "${state.rows.size} OF ${state.totalSets} SETS",
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        DexInset(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp),
        ) {
            DexScreen(Modifier.fillMaxSize(), contentPadding = 0.dp) {
                when {
                    state.loading -> PokeballLoader("Loading every set…", Modifier.align(Alignment.Center))
                    state.failed && !state.online -> ScreenMessage(
                        title = "You're offline",
                        body = "The list of sets needs an internet connection. It loads by itself when you reconnect.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.failed -> ScreenMessage(
                        title = "No signal",
                        body = "The card database didn't answer. Check your connection and try again.",
                        actionLabel = "Retry",
                        onAction = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    state.rows.isEmpty() -> ScreenMessage(
                        title = when {
                            state.query.isNotBlank() -> "No matches"
                            state.filter == SetFilter.COMPLETE -> "No complete sets yet"
                            else -> "No sets started yet"
                        },
                        body = if (state.query.isNotBlank()) "No set matches \"${state.query.trim()}\"."
                        else "Sets you add cards from show up here.",
                        actionLabel = "Show all sets",
                        onAction = {
                            onQueryChange("")
                            onSelectFilter(SetFilter.ALL)
                        },
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> SetList(state.rows, listState, onOpenSet)
                }
            }
        }
    }
}

@Composable
private fun SetList(
    rows: List<SetRow>,
    listState: LazyListState,
    onOpenSet: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val hideKeyboardOnDrag = remember(focusManager) { HideKeyboardOnDrag(focusManager) }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(hideKeyboardOnDrag),
    ) {
        rows.forEachIndexed { index, row ->
            // A heading wherever the series changes, e.g. "Mega Evolution", "Scarlet & Violet".
            if (index == 0 || rows[index - 1].set.series != row.set.series) {
                item(key = "series-${row.set.series}-$index") {
                    Text(
                        row.set.series.uppercase(),
                        style = DexText.ScreenSmall.copy(fontSize = 11.sp),
                        color = ScreenGlow,
                        modifier = Modifier.padding(start = 2.dp, top = if (index == 0) 0.dp else 8.dp),
                    )
                }
            }
            item(key = row.set.id) { SetRowItem(row, onClick = { onOpenSet(row.set.id) }) }
        }
    }
}

@Composable
private fun SetRowItem(row: SetRow, onClick: () -> Unit) {
    val set = row.set
    val shape = RoundedCornerShape(10.dp)
    val progress = if (set.total > 0) (row.owned.toFloat() / set.total).coerceIn(0f, 1f) else 0f
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Screen)
            .border(1.dp, if (row.complete) DexYellow else ScreenLine, shape)
            .clickable(role = Role.Button, onClickLabel = "Quick add from ${set.name}", onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${set.name}, ${set.series}, released ${set.monthYear()}, " +
                    "${row.owned} of ${set.total} cards owned" + if (row.complete) ", complete" else ""
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 76.dp, height = 38.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = set.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(set.name, style = DexText.Screen.copy(fontSize = 13.sp), color = ScreenText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(set.code, set.monthYear()).joinToString(" · "),
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = ScreenTextDim,
                maxLines = 1,
            )
            Spacer(Modifier.height(5.dp))
            ProgressBar(progress, if (row.complete) DexYellow else ScreenGlow)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.width(58.dp), horizontalAlignment = Alignment.End) {
            Text("${row.owned}/${set.total}", style = DexText.ScreenSmall, color = ScreenText, textAlign = TextAlign.End, maxLines = 1)
            Text(
                if (row.complete) "★ DONE" else "${(progress * 100).toInt()}%",
                style = DexText.ScreenSmall.copy(fontSize = 10.sp),
                color = if (row.complete) DexYellow else ScreenTextDim,
            )
        }
    }
}

@Composable
internal fun ProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(ScreenDeep),
    ) {
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceAtLeast(0.02f))
                    .background(color),
            )
        }
    }
}

/** "Sep 2026" from the API's "2026/09/16". */
internal fun CardSet.monthYear(): String = runCatching {
    val date = SimpleDateFormat("yyyy/MM/dd", Locale.US).parse(releaseDate)!!
    SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
}.getOrDefault(releaseYear)
