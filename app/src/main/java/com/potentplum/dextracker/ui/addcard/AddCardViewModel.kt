package com.potentplum.dextracker.ui.addcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.domain.TcgCard
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    OLDEST_FIRST("Oldest first"),
    NEWEST_FIRST("Newest first"),
}

/** A set present in the results, for the "Filter by Set" dropdown. */
data class SetOption(val id: String, val name: String, val year: String, val count: Int)

data class AddCardUiState(
    val pokemonName: String = "",
    val loading: Boolean = true,
    val failed: Boolean = false,
    val totalValid: Int = 0,
    val query: String = "",
    val sets: List<SetOption> = emptyList(),
    val selectedSetId: String? = null,
    val sortOrder: SortOrder = SortOrder.OLDEST_FIRST,
    /** Filtered and sorted results. */
    val cards: List<TcgCard> = emptyList(),
    val ownedIds: Set<String> = emptySet(),
    val selected: TcgCard? = null,
)

class AddCardViewModel(
    private val dex: Int,
    private val pokedex: PokedexRepository,
    private val cards: CardRepository,
) : ViewModel() {

    private data class Controls(
        val query: String = "",
        val setId: String? = null,
        val sort: SortOrder = SortOrder.OLDEST_FIRST,
        val selected: TcgCard? = null,
    )

    private data class Results(
        val loading: Boolean = true,
        val failed: Boolean = false,
        val cards: List<TcgCard> = emptyList(),
    )

    private val name = MutableStateFlow("")
    private val results = MutableStateFlow(Results())
    private val controls = MutableStateFlow(Controls())

    private val _added = Channel<TcgCard>(Channel.BUFFERED)
    /** Emits once a card has been written to the collection. */
    val added: Flow<TcgCard> = _added.receiveAsFlow()

    val uiState: StateFlow<AddCardUiState> = combine(
        name,
        results,
        controls,
        cards.observeOwnedIds(dex),
    ) { name, results, controls, owned ->
        buildState(name, results, controls, owned.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddCardUiState())

    init {
        viewModelScope.launch { name.value = pokedex.cachedName(dex).orEmpty() }
        load()
    }

    /** Auto-query: as soon as the screen opens, fetch every valid card for this Pokémon. */
    fun load(forceRefresh: Boolean = false) {
        results.value = Results(loading = true)
        viewModelScope.launch {
            results.value = runCatching { cards.cardsFor(dex, forceRefresh) }.fold(
                onSuccess = { Results(loading = false, cards = it) },
                onFailure = { Results(loading = false, failed = true) },
            )
        }
    }

    fun setQuery(query: String) = controls.update { it.copy(query = query) }

    fun selectSet(setId: String?) = controls.update { it.copy(setId = setId, selected = null) }

    fun toggleSort() = controls.update {
        it.copy(sort = if (it.sort == SortOrder.OLDEST_FIRST) SortOrder.NEWEST_FIRST else SortOrder.OLDEST_FIRST)
    }

    fun select(card: TcgCard?) = controls.update { it.copy(selected = card) }

    fun confirmAdd() {
        val card = controls.value.selected ?: return
        // Clear the selection first so a double tap can't add (and navigate back) twice.
        controls.update { it.copy(selected = null) }
        viewModelScope.launch {
            cards.addToCollection(dex, card)
            _added.send(card)
        }
    }

    private fun buildState(
        name: String,
        results: Results,
        controls: Controls,
        owned: Set<String>,
    ): AddCardUiState {
        val sets = results.cards
            .groupBy { it.setId }
            .map { (id, cards) ->
                val first = cards.first()
                SetOption(id, first.setName, first.releaseYear, cards.size)
            }
            .sortedWith(compareBy({ it.year }, { it.name }))

        val query = controls.query.trim()
        val filtered = results.cards.filter { card ->
            (controls.setId == null || card.setId == controls.setId) &&
                (query.isEmpty() || card.matches(query))
        }

        val chronological = compareBy<TcgCard>({ it.releaseDate }, { it.setName })
        val ordered = when (controls.sort) {
            SortOrder.OLDEST_FIRST -> filtered.sortedWith(chronological.thenBy { it.numberSortKey() })
            SortOrder.NEWEST_FIRST -> filtered.sortedWith(chronological.reversed().thenBy { it.numberSortKey() })
        }
        return AddCardUiState(
            pokemonName = name,
            loading = results.loading,
            failed = results.failed,
            totalValid = results.cards.size,
            query = controls.query,
            sets = sets,
            selectedSetId = controls.setId,
            sortOrder = controls.sort,
            cards = ordered,
            ownedIds = owned,
            selected = controls.selected,
        )
    }

    private fun TcgCard.matches(query: String): Boolean =
        listOf(name, setName, setSeries, number, rarity.orEmpty(), releaseYear)
            .any { it.contains(query, ignoreCase = true) }

    /** Sorts "4" before "25" and keeps promo numbers like "SWSH020" in a stable spot. */
    private fun TcgCard.numberSortKey(): String =
        number.takeWhile { it.isDigit() }.padStart(5, '0') + number
}
