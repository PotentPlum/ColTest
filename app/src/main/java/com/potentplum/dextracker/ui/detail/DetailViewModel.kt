package com.potentplum.dextracker.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.local.OwnedCardEntity
import com.potentplum.dextracker.data.local.PokemonDetailEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {
    private val _name = MutableStateFlow("")
    /** Name from the cached list, available immediately even before detail loads. */
    val name: StateFlow<String> = _name.asStateFlow()

    private val _detail = MutableStateFlow<DetailState>(DetailState.Loading)
    val detail: StateFlow<DetailState> = _detail.asStateFlow()

    val ownedCards: StateFlow<List<OwnedCardEntity>> = cards.observeOwned(dex)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _name.value = pokedex.cachedName(dex).orEmpty() }
        loadDetail()
    }

    fun loadDetail() {
        _detail.value = DetailState.Loading
        viewModelScope.launch {
            _detail.value = runCatching { pokedex.detail(dex) }
                .fold(onSuccess = { DetailState.Loaded(it) }, onFailure = { DetailState.Failed })
        }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch { cards.removeFromCollection(cardId) }
    }
}
