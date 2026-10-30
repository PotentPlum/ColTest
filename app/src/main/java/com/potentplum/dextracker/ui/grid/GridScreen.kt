package com.potentplum.dextracker.ui.grid

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.potentplum.dextracker.ui.components.DexTab
import com.potentplum.dextracker.ui.components.Pokeball
import com.potentplum.dextracker.ui.components.PokeballLoader
import com.potentplum.dextracker.ui.components.ScreenMessage
import com.potentplum.dextracker.ui.theme.Bezel
import com.potentplum.dextracker.ui.theme.BezelLight
import com.potentplum.dextracker.ui.theme.BezelSilver
import com.potentplum.dextracker.ui.theme.DexText
import com.potentplum.dextracker.ui.theme.DexYellow
import com.potentplum.dextracker.ui.theme.Ink
import com.potentplum.dextracker.ui.theme.MissingTile
import com.potentplum.dextracker.ui.theme.Screen
import com.potentplum.dextracker.ui.theme.ScreenDeep
import com.potentplum.dextracker.ui.theme.ScreenGlow
import com.potentplum.dextracker.ui.theme.ScreenLine
import com.potentplum.dextracker.ui.theme.ScreenTextDim

@Composable
fun GridScreen(viewModel: GridViewModel, onPokemonClick: (Int) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GridContent(
        state = state,
        onSelectGeneration = viewModel::selectGeneration,
        onRetry = viewModel::syncSpecies,
        onPokemonClick = onPokemonClick,
    )
}

@Composable
fun GridContent(
    state: GridUiState,
    onSelectGeneration: (Generation) -> Unit,
    onRetry: () -> Unit,
    onPokemonClick: (Int) -> Unit,
) {
    DexScaffold(title = "Pokédex Tracker") {
        CaughtCounter(state)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Generation.entries.forEach { gen ->
                DexTab(
                    text = gen.label,
                    selected = gen == state.generation,
                    onClick = { onSelectGeneration(gen) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        DexInset(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp),
        ) {
            when {
                state.pokemon.isNotEmpty() -> PokemonGrid(state, onPokemonClick)
                state.syncing -> PokeballLoader("Downloading Pokédex data…", Modifier.align(Alignment.Center))
                else -> DexScreen(Modifier.align(Alignment.Center)) {
                    ScreenMessage(
                        title = "No signal",
                        body = "Couldn't reach PokeAPI. The Pokédex is downloaded once, then works offline.",
                        actionLabel = "Retry",
                        onAction = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun CaughtCounter(state: GridUiState) {
    DexScreen(Modifier.fillMaxWidth(), contentPadding = 10.dp, showGrid = false) {
        Column(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
        ) {
            Text(
                text = "${state.generation.label.uppercase()} – ${state.caught} / ${state.total} CAUGHT",
                style = DexText.ScreenLarge,
                color = ScreenGlow,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(6.dp))
            val progress = if (state.total == 0) 0f else state.caught / state.total.toFloat()
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(ScreenLine),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(ScreenGlow),
                )
            }
        }
    }
}

@Composable
private fun PokemonGrid(state: GridUiState, onPokemonClick: (Int) -> Unit) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(state.generation) { gridState.scrollToItem(0) }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 96.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.pokemon, key = { it.dex }) { pokemon ->
            PokemonTile(pokemon, onClick = { onPokemonClick(pokemon.dex) })
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
            .aspectRatio(1f)
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
                .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 14.dp),
        )
        Text(
            text = pokemon.dex.dexLabel(),
            style = DexText.ScreenSmall.copy(fontSize = 11.sp),
            color = if (collected) ScreenGlow else ScreenTextDim,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp),
        )
        if (collected) {
            Pokeball(
                18.dp,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 6.dp),
            )
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
