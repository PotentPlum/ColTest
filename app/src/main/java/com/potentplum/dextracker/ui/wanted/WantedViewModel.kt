package com.potentplum.dextracker.ui.wanted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.ConnectivityMonitor
import com.potentplum.dextracker.data.CopyKey
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.SettingsRepository
import com.potentplum.dextracker.data.WantedRepository
import com.potentplum.dextracker.data.local.CardWithCopies
import com.potentplum.dextracker.data.local.WantedCardEntity
import com.potentplum.dextracker.data.price
import com.potentplum.dextracker.data.toCardEntity
import com.potentplum.dextracker.data.variantList
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardNumberOrder
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.domain.Generation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class WantedFilter(val label: String) {
    ALL("All"),
    NEW_CATCH("New catch"),
}

enum class WantedSort(val label: String) {
    PRICE("Price"),
    DEX("Dex #"),
    SET("Set"),
    ADDED("Added"),
}

/** A wanted card; [newCatch] when getting it would register a Pokémon that isn't caught yet. */
data class WantedEntry(val card: WantedCardEntity, val newCatch: Boolean) {
    val price: Double? get() = card.price
}

data class WantedUiState(
    val loading: Boolean = true,
    /** Filtered and sorted. */
    val entries: List<WantedEntry> = emptyList(),
    val totalCount: Int = 0,
    val newCatchCount: Int = 0,
    /** Cardmarket trend of one copy of every wanted card. */
    val totalPrice: Double = 0.0,
    val filter: WantedFilter = WantedFilter.ALL,
    val sort: WantedSort = WantedSort.PRICE,
    val owned: Map<String, CardWithCopies> = emptyMap(),
    val condition: CardCondition = CardCondition.NEAR_MINT,
    val language: CardLanguage = CardLanguage.ENGLISH,
    val sheetCard: WantedCardEntity? = null,
    /** Got it on a card with more versions than a normal and a holo: which one? */
    val choosingFor: WantedCardEntity? = null,
    val online: Boolean = true,
)

/** Something the snackbar offers to undo. */
sealed interface WantedChange {
    val card: WantedCardEntity

    data class GotIt(
        override val card: WantedCardEntity,
        val key: CopyKey,
        val pokemonName: String,
        val newCatch: Boolean,
        val completedGeneration: Generation?,
    ) : WantedChange

    data class Removed(override val card: WantedCardEntity) : WantedChange
}

class WantedViewModel(
    private val wanted: WantedRepository,
    private val cards: CardRepository,
    private val pokedex: PokedexRepository,
    private val settings: SettingsRepository,
    connectivity: ConnectivityMonitor,
) : ViewModel() {
    private data class Controls(
        val filter: WantedFilter = WantedFilter.ALL,
        val sort: WantedSort = WantedSort.PRICE,
        val sheetCardId: String? = null,
        val choosingForId: String? = null,
    )

    private val controls = MutableStateFlow(Controls())
    private val writeLock = Mutex()

    private val _changes = Channel<WantedChange>(Channel.BUFFERED)
    /** Emits each change once it's written, for the snackbar and celebrations. */
    val changes: Flow<WantedChange> = _changes.receiveAsFlow()

    private val collection = combine(cards.observeAllOwned(), cards.observeCaughtDexes()) { owned, caught -> owned to caught }
    private val environment = combine(settings.defaultCondition, settings.defaultLanguage, connectivity.online) { c, l, o ->
        Triple(c, l, o)
    }

    val uiState: StateFlow<WantedUiState> = combine(
        wanted.observeAll(),
        collection,
        controls,
        environment,
    ) { list, (ownedList, caught), controls, (condition, language, online) ->
        val all = list.map { WantedEntry(it, newCatch = it.dex > 0 && it.dex !in caught) }
        val shown = all
            .filter { controls.filter == WantedFilter.ALL || it.newCatch }
            .sortedWith(comparatorFor(controls.sort))
        WantedUiState(
            loading = false,
            entries = shown,
            totalCount = all.size,
            newCatchCount = all.count { it.newCatch },
            totalPrice = all.sumOf { it.price ?: 0.0 },
            filter = controls.filter,
            sort = controls.sort,
            owned = ownedList.associateBy { it.card.cardId },
            condition = condition,
            language = language,
            sheetCard = controls.sheetCardId?.let { id -> list.firstOrNull { it.cardId == id } },
            choosingFor = controls.choosingForId?.let { id -> list.firstOrNull { it.cardId == id } },
            online = online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WantedUiState())

    init {
        // Prices move slowly; refresh on open only when they're missing or a day old.
        viewModelScope.launch {
            val list = wanted.observeAll().first()
            val oldest = list.minOfOrNull { it.pricesUpdatedAt ?: 0L } ?: return@launch
            if (System.currentTimeMillis() - oldest > STALE_AFTER_MS && connectivity.online.value) {
                runCatching { wanted.refreshPrices() }
            }
        }
    }

    fun selectFilter(filter: WantedFilter) = controls.update { it.copy(filter = filter) }

    /** Steps through the sort orders. */
    fun nextSort() = controls.update {
        it.copy(sort = WantedSort.entries[(it.sort.ordinal + 1) % WantedSort.entries.size])
    }

    fun openSheet(card: WantedCardEntity) = controls.update { it.copy(sheetCardId = card.cardId) }

    fun closeSheet() = controls.update { it.copy(sheetCardId = null) }

    fun closeChooser() = controls.update { it.copy(choosingForId = null) }

    /** Adds a copy as "Adding as" says, which takes the card off the list; asks for the version if needed. */
    fun gotIt(card: WantedCardEntity) {
        val variant = CardVariant.quickAddFor(card.variantList)
        if (variant == null) controls.update { it.copy(choosingForId = card.cardId) } else add(card, variant)
    }

    fun add(card: WantedCardEntity, variant: CardVariant) {
        val key = CopyKey(card.cardId, variant, settings.defaultCondition.value, settings.defaultLanguage.value)
        viewModelScope.launch {
            val result = writeLock.withLock { cards.addCopy(card.toCardEntity(), key) }
            val name = if (card.dex > 0) pokedex.cachedName(card.dex).orEmpty() else ""
            _changes.send(WantedChange.GotIt(result.unwanted ?: card, key, name, result.newCatch, result.completedGeneration))
        }
    }

    fun remove(card: WantedCardEntity) {
        viewModelScope.launch {
            val removed = writeLock.withLock { wanted.remove(card.cardId) } ?: return@launch
            _changes.send(WantedChange.Removed(removed))
        }
    }

    fun undo(change: WantedChange) {
        viewModelScope.launch {
            writeLock.withLock {
                if (change is WantedChange.GotIt) cards.removeOneCopy(change.key)
                wanted.restore(change.card)
            }
        }
    }

    fun changeQuantity(copyId: Long, delta: Int) {
        viewModelScope.launch { writeLock.withLock { cards.changeQuantity(copyId, delta) } }
    }

    fun editCopy(copyId: Long, variant: CardVariant, condition: CardCondition, language: CardLanguage) {
        viewModelScope.launch { writeLock.withLock { cards.editCopy(copyId, variant, condition, language) } }
    }

    fun setDefaultCondition(condition: CardCondition) = settings.setDefaultCondition(condition)

    fun setDefaultLanguage(language: CardLanguage) = settings.setDefaultLanguage(language)

    private fun comparatorFor(sort: WantedSort): Comparator<WantedEntry> = when (sort) {
        WantedSort.PRICE -> compareByDescending<WantedEntry> { it.price ?: -1.0 }.thenBy { it.card.name }
        WantedSort.DEX -> compareBy<WantedEntry> { it.card.dex.takeIf { dex -> dex > 0 } ?: Int.MAX_VALUE }
            .thenBy { it.card.releaseDate }
        WantedSort.SET -> compareByDescending<WantedEntry> { it.card.releaseDate }
            .thenBy { it.card.setName }
            .thenBy(CardNumberOrder) { it.card.number }
        WantedSort.ADDED -> compareByDescending { it.card.addedAt }
    }

    private companion object {
        const val STALE_AFTER_MS = 24L * 60 * 60 * 1000
    }
}
