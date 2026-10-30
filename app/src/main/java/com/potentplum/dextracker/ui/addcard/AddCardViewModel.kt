package com.potentplum.dextracker.ui.addcard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.ConnectivityMonitor
import com.potentplum.dextracker.data.CopyKey
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.SettingsRepository
import com.potentplum.dextracker.data.local.CardWithCopies
import com.potentplum.dextracker.data.toEntity
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.domain.Generation
import com.potentplum.dextracker.domain.TcgCard
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
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
    /** Owned cards of this Pokémon, by card id. */
    val owned: Map<String, CardWithCopies> = emptyMap(),
    /** The card whose copies sheet is open. */
    val sheetCard: TcgCard? = null,
    val condition: CardCondition = CardCondition.NEAR_MINT,
    val language: CardLanguage = CardLanguage.ENGLISH,
    val online: Boolean = true,
) {
    val ownedIds: Set<String> get() = owned.keys
}

/** A copy added from this screen, kept so the snackbar's Undo can take back exactly it. */
data class CardAdded(
    val card: TcgCard,
    val key: CopyKey,
    val pokemonName: String,
    val newCatch: Boolean,
    val completedGeneration: Generation?,
)

class AddCardViewModel(
    private val dex: Int,
    private val pokedex: PokedexRepository,
    private val cards: CardRepository,
    private val settings: SettingsRepository,
    connectivity: ConnectivityMonitor,
) : ViewModel() {

    private data class Controls(
        val setId: String? = null,
        val sort: SortOrder = SortOrder.OLDEST_FIRST,
        val sheetCardId: String? = null,
    )

    private data class Environment(
        val includeRegionalForms: Boolean,
        val online: Boolean,
        val condition: CardCondition,
        val language: CardLanguage,
    )

    private data class Results(
        val loading: Boolean = true,
        val failed: Boolean = false,
        val cards: List<TcgCard> = emptyList(),
    )

    private val name = MutableStateFlow("")
    private val results = MutableStateFlow(Results())
    private val controls = MutableStateFlow(Controls())

    /**
     * The search box text. Compose state rather than part of [uiState], so the field shows each
     * keystroke immediately; the results follow a moment later.
     */
    var query by mutableStateOf("")
        private set

    /** Serialises collection writes so rapid taps apply in order against the real state. */
    private val writeLock = Mutex()

    private val _added = Channel<CardAdded>(Channel.BUFFERED)
    /** Emits each copy once it's written, for the snackbar and celebrations. */
    val added: Flow<CardAdded> = _added.receiveAsFlow()

    private val environment = combine(
        settings.includeRegionalForms,
        connectivity.online,
        settings.defaultCondition,
        settings.defaultLanguage,
        ::Environment,
    )

    private val search = combine(controls, snapshotFlow { query }) { controls, query -> controls to query }

    val uiState: StateFlow<AddCardUiState> = combine(
        name,
        results,
        search,
        cards.observeOwned(dex),
        environment,
    ) { name, results, (controls, query), owned, environment ->
        buildState(name, results, controls, query, owned.associateBy { it.card.cardId }, environment)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddCardUiState())

    init {
        viewModelScope.launch { name.value = pokedex.cachedName(dex).orEmpty() }
        load()
        // Search again by itself once the connection comes back.
        viewModelScope.launch {
            connectivity.online.drop(1).filter { it }.collect {
                if (results.value.failed) load(forceRefresh = true)
            }
        }
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

    fun updateQuery(query: String) {
        this.query = query
    }

    fun selectSet(setId: String?) = controls.update { it.copy(setId = setId) }

    fun toggleSort() = controls.update {
        it.copy(sort = if (it.sort == SortOrder.OLDEST_FIRST) SortOrder.NEWEST_FIRST else SortOrder.OLDEST_FIRST)
    }

    fun openSheet(card: TcgCard) = controls.update { it.copy(sheetCardId = card.id) }

    fun closeSheet() = controls.update { it.copy(sheetCardId = null) }

    fun addCopy(card: TcgCard, variant: CardVariant) {
        closeSheet()
        val key = CopyKey(card.id, variant, settings.defaultCondition.value, settings.defaultLanguage.value)
        viewModelScope.launch {
            val result = writeLock.withLock { cards.addCopy(card.toEntity(dex), key) }
            _added.send(CardAdded(card, key, name.value, result.newCatch, result.completedGeneration))
        }
    }

    fun undo(added: CardAdded) {
        viewModelScope.launch { writeLock.withLock { cards.removeOneCopy(added.key) } }
    }

    fun changeQuantity(copyId: Long, delta: Int) {
        viewModelScope.launch { writeLock.withLock { cards.changeQuantity(copyId, delta) } }
    }

    fun setDefaultCondition(condition: CardCondition) = settings.setDefaultCondition(condition)

    fun setDefaultLanguage(language: CardLanguage) = settings.setDefaultLanguage(language)

    private fun buildState(
        name: String,
        results: Results,
        controls: Controls,
        query: String,
        owned: Map<String, CardWithCopies>,
        environment: Environment,
    ): AddCardUiState {
        // Regional forms stay visible when owned, so a card added under the other setting
        // can still be found and removed here.
        val valid = results.cards.filter { environment.includeRegionalForms || !it.regionalForm || it.id in owned }
        val sets = valid
            .groupBy { it.setId }
            .map { (id, cards) ->
                val first = cards.first()
                SetOption(id, first.setName, first.releaseYear, cards.size)
            }
            .sortedWith(compareBy({ it.year }, { it.name }))

        val needle = query.trim()
        val filtered = valid.filter { card ->
            (controls.setId == null || card.setId == controls.setId) &&
                (needle.isEmpty() || card.matches(needle))
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
            query = query,
            sets = sets,
            selectedSetId = controls.setId,
            sortOrder = controls.sort,
            cards = ordered,
            owned = owned,
            sheetCard = controls.sheetCardId?.let { id -> results.cards.firstOrNull { it.id == id } },
            condition = environment.condition,
            language = environment.language,
            online = environment.online,
        )
    }

    private fun TcgCard.matches(query: String): Boolean =
        listOf(name, setName, setSeries, number, rarity.orEmpty(), releaseYear)
            .any { it.contains(query, ignoreCase = true) }

    /** Sorts "4" before "25" and keeps promo numbers like "SWSH020" in a stable spot. */
    private fun TcgCard.numberSortKey(): String =
        number.takeWhile { it.isDigit() }.padStart(5, '0') + number
}
