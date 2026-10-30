package com.potentplum.dextracker

import android.content.Context
import com.potentplum.dextracker.data.CardRepository
import com.potentplum.dextracker.data.ConnectivityMonitor
import com.potentplum.dextracker.data.OfflineDownloader
import com.potentplum.dextracker.data.PokedexRepository
import com.potentplum.dextracker.data.SettingsRepository
import com.potentplum.dextracker.data.backup.BackupRepository
import com.potentplum.dextracker.data.local.AppDatabase
import com.potentplum.dextracker.data.remote.Network
import okhttp3.OkHttpClient

/** Manual dependency container; the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val okHttpClient: OkHttpClient = Network.okHttpClient()
    private val database = AppDatabase.build(context)

    val pokedexRepository = PokedexRepository(Network.pokeApi(okHttpClient), database.pokemonDao())
    val cardRepository = CardRepository(Network.tcgApi(okHttpClient), database)
    val settingsRepository = SettingsRepository(context)
    val backupRepository = BackupRepository(context, database, settingsRepository)
    val connectivity = ConnectivityMonitor(context)
    val offlineDownloader = OfflineDownloader(context, pokedexRepository, cardRepository)
}
