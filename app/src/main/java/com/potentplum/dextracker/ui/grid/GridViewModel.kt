package com.potentplum.dextracker.ui.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.local.PokemonWithCount
import com.potentplum.dextracker.domain.Generation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GridUiState(
    val generation: Generation = Generation.GEN_1,
    val pokemon: List<PokemonWithCount> = emptyList(),
    val syncing: Boolean = true,
    val syncFailed: Boolean = false,
) {
    val caught: Int get() = pokemon.count { it.cardCount > 0 }
    val total: Int get() = generation.dexRange.count()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GridViewModel(private val repository: PokedexRepository) : ViewModel() {
    private val generation = MutableStateFlow(Generation.GEN_1)
    private val sync = MutableStateFlow(SyncState(syncing = true, failed = false))

    val uiState: StateFlow<GridUiState> = combine(
        // Pair each list with the generation it belongs to so a tab switch never shows
        // the previous generation's Pokémon under the new tab.
        generation.flatMapLatest { gen -> repository.observeGeneration(gen).map { gen to it } },
        sync,
    ) { (gen, pokemon), sync ->
        GridUiState(gen, pokemon, sync.syncing, sync.failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GridUiState())

    init {
        syncSpecies()
    }

    fun selectGeneration(gen: Generation) {
        generation.value = gen
    }

    fun syncSpecies() {
        sync.value = SyncState(syncing = true, failed = false)
        viewModelScope.launch {
            val failed = runCatching { repository.ensureSpeciesCached() }.isFailure
            sync.update { SyncState(syncing = false, failed = failed) }
        }
    }

    private data class SyncState(val syncing: Boolean, val failed: Boolean)
}
