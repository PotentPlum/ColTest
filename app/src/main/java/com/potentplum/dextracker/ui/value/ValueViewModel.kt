package com.potentplum.dextracker.ui.value

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.ConnectivityMonitor
import com.potentplum.dextracker.data.local.CardWithCopies
import com.potentplum.dextracker.data.prices
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardVariant
import com.potentplum.dextracker.domain.CollectionValuation
import com.potentplum.dextracker.domain.Valuation
import com.potentplum.dextracker.domain.ValuedCopy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ValueUiState(
    val loading: Boolean = true,
    val valuation: CollectionValuation = CollectionValuation(0.0, 0, 0, emptyList()),
    /** Oldest price fetch among owned cards; null when nothing has been priced yet. */
    val pricesUpdatedAt: Long? = null,
    val refreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val online: Boolean = true,
)

class ValueViewModel(
    private val cards: CardRepository,
    connectivity: ConnectivityMonitor,
) : ViewModel() {
    private data class RefreshState(val refreshing: Boolean = false, val failed: Boolean = false)

    private val refresh = MutableStateFlow(RefreshState())

    val uiState: StateFlow<ValueUiState> = combine(
        cards.observeAllOwned(),
        refresh,
        connectivity.online,
    ) { owned, refresh, online ->
        ValueUiState(
            loading = false,
            valuation = Valuation.of(owned.flatMap { it.valuedCopies() }),
            pricesUpdatedAt = owned.mapNotNull { it.card.pricesUpdatedAt }.minOrNull(),
            refreshing = refresh.refreshing,
            refreshFailed = refresh.failed,
            online = online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ValueUiState())

    init {
        // Prices move slowly; refresh on open only when they're missing or a day old.
        viewModelScope.launch {
            val owned = cards.observeAllOwned().first()
            val oldest = owned.minOfOrNull { it.card.pricesUpdatedAt ?: 0L } ?: return@launch
            if (System.currentTimeMillis() - oldest > STALE_AFTER_MS && connectivity.online.value) refreshPrices()
        }
    }

    fun refreshPrices() {
        if (refresh.value.refreshing) return
        refresh.value = RefreshState(refreshing = true)
        viewModelScope.launch {
            val failed = runCatching { cards.refreshPrices() }.isFailure
            refresh.value = RefreshState(refreshing = false, failed = failed)
        }
    }

    private fun CardWithCopies.valuedCopies(): List<ValuedCopy> = copies.map { copy ->
        val variant = CardVariant.fromKey(copy.variant) ?: CardVariant.UNSPECIFIED
        ValuedCopy(
            cardId = card.cardId,
            dex = card.dex,
            name = card.name,
            setName = card.setName,
            number = card.number,
            imageSmall = card.imageSmall,
            variant = variant,
            condition = CardCondition.fromCode(copy.condition),
            language = CardLanguage.fromCode(copy.language),
            quantity = copy.quantity,
            unitValue = card.prices.unitValue(variant),
        )
    }

    private companion object {
        const val STALE_AFTER_MS = 24L * 60 * 60 * 1000
    }
}
