package com.potentplum.dextracker.ui.sets

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
import com.potentplum.dextracker.data.toEntity
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardSet
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.domain.Generation
import com.potentplum.dextracker.domain.SetCard
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

enum class OwnedFilter(val label: String) {
    ALL("All"),
    MISSING("Missing"),
    OWNED("Owned"),
}

/** Why the quick chooser is open: a tap or − that couldn't tell which copy was meant. */
sealed interface QuickChoice {
    val cardId: String

    /** The card has more versions than a normal and a holo, so ask which one to add. */
    data class AddVersion(override val cardId: String) : QuickChoice

    /** More than one line of copies is owned, so ask which one loses a copy. */
    data class RemoveCopy(override val cardId: String) : QuickChoice
}

data class QuickAddUiState(
    val set: CardSet? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val online: Boolean = true,
    /** Every card in the set, in number order. */
    val allCards: List<SetCard> = emptyList(),
    /** [allCards] after the owned filter. */
    val cards: List<SetCard> = emptyList(),
    val filter: OwnedFilter = OwnedFilter.ALL,
    /** Owned cards of this set, by card id. */
    val owned: Map<String, CardWithCopies> = emptyMap(),
    val wantedIds: Set<String> = emptySet(),
    val condition: CardCondition = CardCondition.NEAR_MINT,
    val language: CardLanguage = CardLanguage.ENGLISH,
    /** The card whose copies sheet is open. */
    val sheetCard: SetCard? = null,
    val choice: QuickChoice? = null,
) {
    val ownedCount: Int get() = allCards.count { it.card.id in owned }
    val totalCount: Int get() = allCards.size.takeIf { it > 0 } ?: set?.total ?: 0

    fun copiesOf(card: SetCard): Int = owned[card.card.id]?.copies?.sumOf { it.quantity } ?: 0

    fun cardById(id: String): SetCard? = allCards.firstOrNull { it.card.id == id }
}

/** A copy added from Quick add, kept so the snackbar's Undo can take back exactly it. */
data class QuickAdded(
    val card: SetCard,
    val key: CopyKey,
    val pokemonName: String,
    val newCatch: Boolean,
    val completedGeneration: Generation?,
    val unwanted: WantedCardEntity?,
)

class SetCardsViewModel(
    private val setId: String,
    private val cards: CardRepository,
    private val wanted: WantedRepository,
    private val pokedex: PokedexRepository,
    private val settings: SettingsRepository,
    connectivity: ConnectivityMonitor,
) : ViewModel() {
    private data class Results(
        val loading: Boolean = true,
        val failed: Boolean = false,
        val set: CardSet? = null,
        val cards: List<SetCard> = emptyList(),
    )

    private data class Controls(
        val filter: OwnedFilter = OwnedFilter.ALL,
        val sheetCardId: String? = null,
        val choice: QuickChoice? = null,
    )

    private val results = MutableStateFlow(Results())
    private val controls = MutableStateFlow(Controls())

    /** Serialises collection writes so rapid taps apply in order against the real state. */
    private val writeLock = Mutex()

    private val _added = Channel<QuickAdded>(Channel.BUFFERED)
    /** Emits each copy once it's written, for the snackbar and celebrations. */
    val added: Flow<QuickAdded> = _added.receiveAsFlow()

    private data class Environment(
        val condition: CardCondition,
        val language: CardLanguage,
        val includeRegionalForms: Boolean,
    )

    private val environment = combine(
        settings.defaultCondition,
        settings.defaultLanguage,
        settings.includeRegionalForms,
        ::Environment,
    )

    val uiState: StateFlow<QuickAddUiState> = combine(
        results,
        controls,
        combine(cards.observeOwnedInSet(setId), wanted.observeIds()) { owned, wantedIds -> owned to wantedIds },
        environment,
        connectivity.online,
    ) { results, controls, (ownedList, wantedIds), environment, online ->
        val owned = ownedList.associateBy { it.card.cardId }
        // With regional forms switched off, an Alolan Vulpix is collected but doesn't catch Vulpix.
        val cards = if (environment.includeRegionalForms) results.cards
        else results.cards.map { if (it.card.regionalForm) it.copy(dex = 0) else it }
        QuickAddUiState(
            set = results.set,
            loading = results.loading,
            failed = results.failed,
            online = online,
            allCards = cards,
            cards = cards.filter { card ->
                when (controls.filter) {
                    OwnedFilter.ALL -> true
                    OwnedFilter.MISSING -> card.card.id !in owned
                    OwnedFilter.OWNED -> card.card.id in owned
                }
            },
            filter = controls.filter,
            owned = owned,
            wantedIds = wantedIds,
            condition = environment.condition,
            language = environment.language,
            sheetCard = controls.sheetCardId?.let { id -> cards.firstOrNull { it.card.id == id } },
            choice = controls.choice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickAddUiState())

    init {
        load()
        viewModelScope.launch {
            connectivity.online.drop(1).filter { it }.collect {
                if (results.value.failed) load(forceRefresh = true)
            }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        results.update { it.copy(loading = true, failed = false) }
        viewModelScope.launch {
            results.value = runCatching {
                val set = cards.sets().firstOrNull { it.id == setId }
                Results(loading = false, set = set, cards = cards.cardsInSet(setId, forceRefresh))
            }.getOrElse { Results(loading = false, failed = true, set = results.value.set) }
        }
    }

    fun selectFilter(filter: OwnedFilter) = controls.update { it.copy(filter = filter) }

    /** One tap: add a copy, or ask which version when that isn't obvious. */
    fun tap(card: SetCard) {
        val variant = CardVariant.quickAddFor(card.card.variants)
        if (variant == null) controls.update { it.copy(choice = QuickChoice.AddVersion(card.card.id)) }
        else add(card, variant)
    }

    /** The − button: one copy fewer, asking which line when more than one is owned. */
    fun minus(card: SetCard) {
        val lines = uiState.value.owned[card.card.id]?.copies.orEmpty()
        when (lines.size) {
            0 -> Unit
            1 -> changeQuantity(lines.single().id, -1)
            else -> controls.update { it.copy(choice = QuickChoice.RemoveCopy(card.card.id)) }
        }
    }

    fun closeChoice() = controls.update { it.copy(choice = null) }

    fun openSheet(card: SetCard) = controls.update { it.copy(sheetCardId = card.card.id) }

    fun closeSheet() = controls.update { it.copy(sheetCardId = null) }

    fun add(card: SetCard, variant: CardVariant) {
        val key = CopyKey(card.card.id, variant, settings.defaultCondition.value, settings.defaultLanguage.value)
        viewModelScope.launch {
            val result = writeLock.withLock { cards.addCopy(card.card.toEntity(card.dex), key) }
            val name = if (card.countsTowardDex) pokedex.cachedName(card.dex).orEmpty() else ""
            _added.send(QuickAdded(card, key, name, result.newCatch, result.completedGeneration, result.unwanted))
        }
    }

    fun undo(added: QuickAdded) {
        viewModelScope.launch {
            writeLock.withLock {
                cards.removeOneCopy(added.key)
                added.unwanted?.let { wanted.restore(it) }
            }
        }
    }

    fun changeQuantity(copyId: Long, delta: Int) {
        viewModelScope.launch { writeLock.withLock { cards.changeQuantity(copyId, delta) } }
    }

    fun editCopy(copyId: Long, variant: CardVariant, condition: CardCondition, language: CardLanguage) {
        viewModelScope.launch { writeLock.withLock { cards.editCopy(copyId, variant, condition, language) } }
    }

    fun toggleWanted(card: SetCard) {
        viewModelScope.launch { wanted.toggle(card.card, card.dex) }
    }

    fun setDefaultCondition(condition: CardCondition) = settings.setDefaultCondition(condition)

    fun setDefaultLanguage(language: CardLanguage) = settings.setDefaultLanguage(language)
}
