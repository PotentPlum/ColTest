package com.potentplum.dextracker.ui.grid

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.ConnectivityMonitor
import com.potentplum.dextracker.data.DownloadProgress
import com.potentplum.dextracker.data.OfflineDownloader
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.SettingsRepository
import com.potentplum.dextracker.data.backup.BackupRepository
import com.potentplum.dextracker.data.backup.InvalidBackupException
import com.potentplum.dextracker.data.local.PokemonWithCount
import com.potentplum.dextracker.domain.DexSearch
import com.potentplum.dextracker.domain.Generation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CaughtFilter(val label: String) {
    ALL("All"),
    MISSING("Missing"),
    CAUGHT("Caught"),
}

/** Caught count for one generation, or for the whole National Dex when [generation] is null. */
data class Progress(val generation: Generation?, val caught: Int, val total: Int) {
    val complete: Boolean get() = total > 0 && caught >= total
    val fraction: Float get() = if (total == 0) 0f else caught / total.toFloat()
    /** "National" rather than "All", so it can't be mistaken for the All / Missing / Caught filter. */
    val label: String get() = generation?.label ?: "National"
}

data class GridUiState(
    /** The selected tab; null is the whole National Dex. */
    val scope: Generation? = null,
    val filter: CaughtFilter = CaughtFilter.ALL,
    val query: String = "",
    /** The Pokémon to show after scope, filter and search are applied. */
    val pokemon: List<PokemonWithCount> = emptyList(),
    val national: Progress = Progress(null, 0, Generation.MAX_DEX),
    val generations: List<Progress> = Generation.entries.map { Progress(it, 0, it.size) },
    val hasSpecies: Boolean = false,
    val syncing: Boolean = true,
    val syncFailed: Boolean = false,
    val includeRegionalForms: Boolean = false,
    val online: Boolean = true,
    /** Pokédex pages saved for offline use. */
    val savedPages: Int = 0,
    val download: DownloadProgress = DownloadProgress(),
) {
    val allSaved: Boolean get() = savedPages >= Generation.MAX_DEX

    /** A search looks across every generation, whatever tab is selected. */
    val searching: Boolean get() = query.isNotBlank()

    val scopeProgress: Progress get() = generations.firstOrNull { it.generation == scope } ?: national
}

class GridViewModel(
    private val repository: PokedexRepository,
    private val backup: BackupRepository,
    private val settings: SettingsRepository,
    connectivity: ConnectivityMonitor,
    private val downloader: OfflineDownloader,
) : ViewModel() {
    private data class Offline(val online: Boolean, val savedPages: Int, val download: DownloadProgress)

    private data class Controls(
        val scope: Generation? = null,
        val filter: CaughtFilter = CaughtFilter.ALL,
    )

    private data class SyncState(val syncing: Boolean, val failed: Boolean)

    private val controls = MutableStateFlow(Controls())

    /**
     * The search box text. Compose state rather than part of [uiState], so the field shows each
     * keystroke immediately; filtering the list follows a moment later.
     */
    var query by mutableStateOf("")
        private set
    private val sync = MutableStateFlow(SyncState(syncing = true, failed = false))

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-off status messages (backup results) for the snackbar. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    private val offline = combine(
        connectivity.online,
        repository.observeCachedDetailCount(),
        downloader.progress,
        ::Offline,
    )

    private val search = combine(controls, snapshotFlow { query }) { controls, query -> controls to query }

    val uiState: StateFlow<GridUiState> = combine(
        repository.observeAll(),
        search,
        sync,
        settings.includeRegionalForms,
        offline,
    ) { all, (controls, query), sync, regional, offline ->
        buildState(all, controls, query, sync, regional).copy(
            online = offline.online,
            savedPages = offline.savedPages,
            download = offline.download,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GridUiState())

    init {
        syncSpecies()
        // Announce each finished offline download once.
        viewModelScope.launch {
            downloader.progress.distinctUntilChangedBy { it.finishedRuns }.drop(1).collect { result ->
                _messages.send(
                    if (result.failed == 0) "Pokédex saved: everything works offline now"
                    else "Saved, but ${result.failed} pages couldn't load. Try again later to finish."
                )
            }
        }
        // A failed first sync (no connection) retries by itself once back online.
        viewModelScope.launch {
            connectivity.online.drop(1).collect { online ->
                if (online && sync.value.failed) syncSpecies()
            }
        }
    }

    fun startOfflineDownload() = downloader.start()

    fun cancelOfflineDownload() = downloader.cancel()

    /** Picking a tab also clears the search, since a search spans every generation. */
    fun selectScope(scope: Generation?) {
        query = ""
        controls.update { it.copy(scope = scope) }
    }

    fun selectFilter(filter: CaughtFilter) = controls.update { it.copy(filter = filter) }

    fun updateQuery(query: String) {
        this.query = query
    }

    fun clearFilters() {
        query = ""
        controls.update { it.copy(filter = CaughtFilter.ALL) }
    }

    fun setIncludeRegionalForms(include: Boolean) = settings.setIncludeRegionalForms(include)

    fun syncSpecies() {
        sync.value = SyncState(syncing = true, failed = false)
        viewModelScope.launch {
            val failed = runCatching { repository.ensureSpeciesCached() }.isFailure
            sync.update { SyncState(syncing = false, failed = failed) }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val message = runCatching { backup.export(uri) }.fold(
                onSuccess = { count -> "Backup saved: ${cards(count)}" },
                onFailure = { "Couldn't save the backup. Try another location." },
            )
            _messages.send(message)
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val message = runCatching { backup.import(uri) }.fold(
                onSuccess = { result ->
                    buildString {
                        append("Restored ${cards(result.imported)}")
                        if (result.alreadyOwned > 0) append(" · ${result.alreadyOwned} already in your collection")
                    }
                },
                onFailure = { error ->
                    if (error is InvalidBackupException) "${error.message}." else "Couldn't read that file."
                },
            )
            _messages.send(message)
        }
    }

    private fun cards(count: Int) = if (count == 1) "1 card" else "$count cards"

    private fun buildState(
        all: List<PokemonWithCount>,
        controls: Controls,
        query: String,
        sync: SyncState,
        includeRegionalForms: Boolean,
    ): GridUiState {
        val caughtByGen = all.filter { it.cardCount > 0 }.groupingBy { it.generation }.eachCount()
        val generations = Generation.entries.map { Progress(it, caughtByGen[it.number] ?: 0, it.size) }
        val searching = query.isNotBlank()
        val visible = all.filter { pokemon ->
            (searching || controls.scope == null || pokemon.generation == controls.scope.number) &&
                when (controls.filter) {
                    CaughtFilter.ALL -> true
                    CaughtFilter.MISSING -> pokemon.cardCount == 0
                    CaughtFilter.CAUGHT -> pokemon.cardCount > 0
                } &&
                DexSearch.matches(query, pokemon.dex, pokemon.name)
        }
        return GridUiState(
            scope = controls.scope,
            filter = controls.filter,
            query = query,
            pokemon = visible,
            national = Progress(null, generations.sumOf { it.caught }, Generation.MAX_DEX),
            generations = generations,
            hasSpecies = all.isNotEmpty(),
            syncing = sync.syncing,
            syncFailed = sync.failed,
            includeRegionalForms = includeRegionalForms,
        )
    }
}
