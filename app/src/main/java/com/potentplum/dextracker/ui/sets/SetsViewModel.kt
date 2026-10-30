package com.potentplum.dextracker.ui.sets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.ConnectivityMonitor
import com.potentplum.dextracker.domain.CardSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SetFilter(val label: String) {
    ALL("All"),
    STARTED("Started"),
    COMPLETE("Complete"),
}

/** A set with how many of its cards are owned. */
data class SetRow(val set: CardSet, val owned: Int) {
    val complete: Boolean get() = set.total > 0 && owned >= set.total
}

data class SetsUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val online: Boolean = true,
    /** Filtered, newest first. */
    val rows: List<SetRow> = emptyList(),
    val totalSets: Int = 0,
    val filter: SetFilter = SetFilter.ALL,
    val query: String = "",
)

class SetsViewModel(
    private val cards: CardRepository,
    connectivity: ConnectivityMonitor,
) : ViewModel() {
    private data class Results(val loading: Boolean = true, val failed: Boolean = false, val sets: List<CardSet> = emptyList())

    private val results = MutableStateFlow(Results())
    private val filter = MutableStateFlow(SetFilter.ALL)

    /** The search box text; Compose state so the field never drops a keystroke. */
    var query by mutableStateOf("")
        private set

    private val controls = combine(filter, snapshotFlow { query }) { filter, query -> filter to query }

    val uiState: StateFlow<SetsUiState> = combine(
        results,
        controls,
        cards.observeOwnedCountsBySet(),
        connectivity.online,
    ) { results, (filter, query), owned, online ->
        val rows = results.sets.map { SetRow(it, owned[it.id] ?: 0) }
        val needle = query.trim()
        SetsUiState(
            loading = results.loading,
            failed = results.failed,
            online = online,
            rows = rows.filter { row ->
                (needle.isEmpty() || row.set.matches(needle)) &&
                    when (filter) {
                        SetFilter.ALL -> true
                        SetFilter.STARTED -> row.owned > 0 && !row.complete
                        SetFilter.COMPLETE -> row.complete
                    }
            },
            totalSets = rows.size,
            filter = filter,
            query = query,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetsUiState())

    init {
        load()
        viewModelScope.launch {
            connectivity.online.drop(1).filter { it }.collect {
                if (results.value.failed) load(forceRefresh = true)
            }
        }
    }

    fun load(forceRefresh: Boolean = false) {
        results.value = Results(loading = true)
        viewModelScope.launch {
            results.value = runCatching { cards.sets(forceRefresh) }.fold(
                onSuccess = { Results(loading = false, sets = it) },
                onFailure = { Results(loading = false, failed = true) },
            )
        }
    }

    fun updateQuery(query: String) {
        this.query = query
    }

    fun selectFilter(filter: SetFilter) {
        this.filter.value = filter
    }
}
