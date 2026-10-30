package com.potentplum.dextracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.ConnectivityMonitor
import com.potentplum.dextracker.data.CopyKey
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.SettingsRepository
import com.potentplum.dextracker.data.local.CardWithCopies
import com.potentplum.dextracker.data.local.PokemonDetailEntity
import com.potentplum.dextracker.domain.CardCondition
import com.potentplum.dextracker.domain.CardLanguage
import com.potentplum.dextracker.domain.CardVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data class Loaded(val detail: PokemonDetailEntity) : DetailState
    data object Failed : DetailState
}

class DetailViewModel(
    val dex: Int,
    private val pokedex: PokedexRepository,
    private val cards: CardRepository,
    private val settings: SettingsRepository,
    connectivity: ConnectivityMonitor,
) : ViewModel() {
    private val _name = MutableStateFlow("")
    /** Name from the cached list, available immediately even before detail loads. */
    val name: StateFlow<String> = _name.asStateFlow()

    private val _detail = MutableStateFlow<DetailState>(DetailState.Loading)
    val detail: StateFlow<DetailState> = _detail.asStateFlow()

    val ownedCards: StateFlow<List<CardWithCopies>> = cards.observeOwned(dex)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val online: StateFlow<Boolean> = connectivity.online
    val defaultCondition: StateFlow<CardCondition> = settings.defaultCondition
    val defaultLanguage: StateFlow<CardLanguage> = settings.defaultLanguage

    init {
        viewModelScope.launch { _name.value = pokedex.cachedName(dex).orEmpty() }
        loadDetail()
        // Retry by itself once the connection comes back.
        viewModelScope.launch {
            connectivity.online.drop(1).filter { it }.collect {
                if (_detail.value == DetailState.Failed) loadDetail()
            }
        }
    }

    fun loadDetail() {
        _detail.value = DetailState.Loading
        viewModelScope.launch {
            _detail.value = runCatching { pokedex.detail(dex) }
                .fold(onSuccess = { DetailState.Loaded(it) }, onFailure = { DetailState.Failed })
        }
    }

    fun addCopy(card: CardWithCopies, variant: CardVariant) {
        val key = CopyKey(card.card.cardId, variant, settings.defaultCondition.value, settings.defaultLanguage.value)
        viewModelScope.launch { cards.addCopy(card.card, key) }
    }

    fun changeQuantity(copyId: Long, delta: Int) {
        viewModelScope.launch { cards.changeQuantity(copyId, delta) }
    }

    fun editCopy(copyId: Long, variant: CardVariant, condition: CardCondition, language: CardLanguage) {
        viewModelScope.launch { cards.editCopy(copyId, variant, condition, language) }
    }

    fun setDefaultCondition(condition: CardCondition) = settings.setDefaultCondition(condition)

    fun setDefaultLanguage(language: CardLanguage) = settings.setDefaultLanguage(language)
}
