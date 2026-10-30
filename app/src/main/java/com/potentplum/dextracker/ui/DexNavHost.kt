package com.potentplum.dextracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.potentplum.dextracker.DexTrackerApp
import com.potentplum.dextracker.ui.addcard.AddCardScreen
import com.potentplum.dextracker.ui.addcard.AddCardViewModel
import com.potentplum.dextracker.ui.detail.DetailScreen
import com.potentplum.dextracker.ui.detail.DetailViewModel
import com.potentplum.dextracker.ui.grid.GridScreen
import com.potentplum.dextracker.ui.components.LocalOnline
import com.potentplum.dextracker.ui.components.TopTab
import com.potentplum.dextracker.ui.grid.GridViewModel
import com.potentplum.dextracker.ui.value.ValueScreen
import com.potentplum.dextracker.ui.value.ValueViewModel

private object Routes {
    const val GRID = "grid"
    const val VALUE = "value"
    const val DETAIL = "detail/{dex}"
    const val ADD_CARD = "add/{dex}"
    fun detail(dex: Int) = "detail/$dex"
    fun addCard(dex: Int) = "add/$dex"
}

private val dexArg = listOf(navArgument("dex") { type = NavType.IntType })

@Composable
fun DexNavHost() {
    val nav = rememberNavController()
    val container = (LocalContext.current.applicationContext as DexTrackerApp).container
    val online by container.connectivity.online.collectAsStateWithLifecycle()

    // The two top-level tabs: the Pokédex is home, Value sits on top of it so Back returns home.
    val selectTab: (TopTab) -> Unit = { tab ->
        when (tab) {
            TopTab.POKEDEX -> nav.popBackStack(Routes.GRID, inclusive = false)
            TopTab.VALUE -> nav.navigate(Routes.VALUE) {
                popUpTo(Routes.GRID)
                launchSingleTop = true
            }
        }
    }
    val openPokemon: (Int) -> Unit = { dex -> nav.navigate(Routes.detail(dex)) { launchSingleTop = true } }

    CompositionLocalProvider(LocalOnline provides online) {
        DexNavGraph(nav, selectTab, openPokemon)
    }
}

@Composable
private fun DexNavGraph(nav: NavHostController, selectTab: (TopTab) -> Unit, openPokemon: (Int) -> Unit) {
    NavHost(navController = nav, startDestination = Routes.GRID) {
        composable(Routes.GRID) {
            GridScreen(
                viewModel = containerViewModel {
                    GridViewModel(
                        it.pokedexRepository,
                        it.backupRepository,
                        it.settingsRepository,
                        it.connectivity,
                        it.offlineDownloader,
                    )
                },
                onPokemonClick = openPokemon,
                onSelectTab = selectTab,
            )
        }
        composable(Routes.VALUE) {
            ValueScreen(
                viewModel = containerViewModel { ValueViewModel(it.cardRepository, it.connectivity) },
                onSelectTab = selectTab,
                onOpenPokemon = openPokemon,
            )
        }
        composable(Routes.DETAIL, arguments = dexArg) { entry ->
            val dex = entry.arguments!!.getInt("dex")
            DetailScreen(
                viewModel = containerViewModel {
                    DetailViewModel(dex, it.pokedexRepository, it.cardRepository, it.settingsRepository, it.connectivity)
                },
                onBack = { nav.popBackStack() },
                onAddCard = { nav.navigate(Routes.addCard(dex)) { launchSingleTop = true } },
            )
        }
        composable(Routes.ADD_CARD, arguments = dexArg) { entry ->
            val dex = entry.arguments!!.getInt("dex")
            AddCardScreen(
                viewModel = containerViewModel {
                    AddCardViewModel(dex, it.pokedexRepository, it.cardRepository, it.settingsRepository, it.connectivity)
                },
                onBack = { nav.popBackStack() },
            )
        }
    }
}
