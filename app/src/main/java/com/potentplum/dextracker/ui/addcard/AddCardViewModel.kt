package com.potentplum.dextracker.ui.addcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.SettingsRepository
import com.potentplum.dextracker.data.local.OwnedCardEntity
import com.potentplum.dextracker.domain.Generation
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
)

/** A change made from this screen, kept so the snackbar's Undo can reverse exactly it. */
sealed interface CollectionChange {
    val card: TcgCard

    data class Added(
        override val card: TcgCard,
        val pokemonName: String,
        val newCatch: Boolean,
        val completedGeneration: Generation?,
    ) : CollectionChange

    data class Removed(override val card: TcgCard, val stored: OwnedCardEntity) : CollectionChange
}

class AddCardViewModel(
    private val dex: Int,
    private val pokedex: PokedexRepository,
    private val cards: CardRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private data class Controls(
        val query: String = "",
        val setId: String? = null,
        val sort: SortOrder = SortOrder.OLDEST_FIRST,
    )

    private data class Results(
        val loading: Boolean = true,
        val failed: Boolean = false,
        val cards: List<TcgCard> = emptyList(),
    )

    private val name = MutableStateFlow("")
    private val results = MutableStateFlow(Results())
    private val controls = MutableStateFlow(Controls())

    /** Serialises collection writes so rapid taps apply in order against the real state. */
    private val writeLock = Mutex()

    private val _changes = Channel<CollectionChange>(Channel.BUFFERED)
    /** Emits each add or removal once it's written, for the snackbar and celebrations. */
    val changes: Flow<CollectionChange> = _changes.receiveAsFlow()

    val uiState: StateFlow<AddCardUiState> = combine(
        name,
        results,
        controls,
        cards.observeOwnedIds(dex),
        settings.includeRegionalForms,
    ) { name, results, controls, owned, regional ->
        buildState(name, results, controls, owned.toSet(), regional)
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

    fun selectSet(setId: String?) = controls.update { it.copy(setId = setId) }

    fun toggleSort() = controls.update {
        it.copy(sort = if (it.sort == SortOrder.OLDEST_FIRST) SortOrder.NEWEST_FIRST else SortOrder.OLDEST_FIRST)
    }

    /** One tap adds a card, or removes it if it's already owned. Either can be undone. */
    fun toggle(card: TcgCard) {
        viewModelScope.launch {
            val change = writeLock.withLock {
                if (cards.isOwned(card.id)) {
                    cards.removeFromCollection(card.id)?.let { CollectionChange.Removed(card, it) }
                } else {
                    val result = cards.addToCollection(dex, card)
                    CollectionChange.Added(card, name.value, result.newCatch, result.completedGeneration)
                }
            }
            change?.let { _changes.send(it) }
        }
    }

    fun undo(change: CollectionChange) {
        viewModelScope.launch {
            writeLock.withLock {
                when (change) {
                    is CollectionChange.Added -> cards.removeFromCollection(change.card.id)
                    is CollectionChange.Removed -> cards.restore(change.stored)
                }
            }
        }
    }

    private fun buildState(
        name: String,
        results: Results,
        controls: Controls,
        owned: Set<String>,
        includeRegionalForms: Boolean,
    ): AddCardUiState {
        // Regional forms stay visible when owned, so a card added under the other setting
        // can still be found and removed here.
        val valid = results.cards.filter { includeRegionalForms || !it.regionalForm || it.id in owned }
        val sets = valid
            .groupBy { it.setId }
            .map { (id, cards) ->
                val first = cards.first()
                SetOption(id, first.setName, first.releaseYear, cards.size)
            }
            .sortedWith(compareBy({ it.year }, { it.name }))

        val query = controls.query.trim()
        val filtered = valid.filter { card ->
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
            totalValid = valid.size,
            query = controls.query,
            sets = sets,
            selectedSetId = controls.setId,
            sortOrder = controls.sort,
            cards = ordered,
            ownedIds = owned,
        )
    }

    private fun TcgCard.matches(query: String): Boolean =
        listOf(name, setName, setSeries, number, rarity.orEmpty(), releaseYear)
            .any { it.contains(query, ignoreCase = true) }

    /** Sorts "4" before "25" and keeps promo numbers like "SWSH020" in a stable spot. */
    private fun TcgCard.numberSortKey(): String =
        number.takeWhile { it.isDigit() }.padStart(5, '0') + number
}
