package com.potentplum.dextracker

import android.content.Context
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.local.AppDatabase
import com.potentplum.dextracker.data.remote.Network
import okhttp3.OkHttpClient

/** Manual dependency container; the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val okHttpClient: OkHttpClient = Network.okHttpClient()
    private val database = AppDatabase.build(context)

    val pokedexRepository = PokedexRepository(Network.pokeApi(okHttpClient), database.pokemonDao())
    val cardRepository = CardRepository(Network.tcgApi(okHttpClient), database.ownedCardDao())
}
