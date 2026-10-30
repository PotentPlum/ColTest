package com.potentplum.dextracker.ui

import android.net.Uri
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
import com.potentplum.dextracker.ui.sets.SetCardsScreen
import com.potentplum.dextracker.ui.sets.SetCardsViewModel
import com.potentplum.dextracker.ui.sets.SetsScreen
import com.potentplum.dextracker.ui.sets.SetsViewModel
import com.potentplum.dextracker.ui.value.ValueScreen
import com.potentplum.dextracker.ui.value.ValueViewModel
import com.potentplum.dextracker.ui.wanted.WantedScreen
import com.potentplum.dextracker.ui.wanted.WantedViewModel

private object Routes {
    const val GRID = "grid"
    const val VALUE = "value"
    const val SETS = "sets"
    const val WANTED = "wanted"
    const val DETAIL = "detail/{dex}"
    const val ADD_CARD = "add/{dex}"
    const val SET_CARDS = "set/{setId}"
    fun detail(dex: Int) = "detail/$dex"
    fun addCard(dex: Int) = "add/$dex"
    fun setCards(setId: String) = "set/${Uri.encode(setId)}"
}

private val dexArg = listOf(navArgument("dex") { type = NavType.IntType })

@Composable
fun DexNavHost() {
    val nav = rememberNavController()
    val container = (LocalContext.current.applicationContext as DexTrackerApp).container
    val online by container.connectivity.online.collectAsStateWithLifecycle()

    // The top-level tabs: the Pokédex is home, the others sit on top of it so Back returns home.
    val selectTab: (TopTab) -> Unit = { tab ->
        val route = when (tab) {
            TopTab.POKEDEX -> null
            TopTab.SETS -> Routes.SETS
            TopTab.WANTED -> Routes.WANTED
            TopTab.VALUE -> Routes.VALUE
        }
        if (route == null) {
            nav.popBackStack(Routes.GRID, inclusive = false)
        } else {
            nav.navigate(route) {
                popUpTo(Routes.GRID)
                launchSingleTop = true
            }
        }
    }
    // Trainers and Energy are stored under dex 0 and have no Pokédex page.
    val openPokemon: (Int) -> Unit = { dex ->
        if (dex > 0) nav.navigate(Routes.detail(dex)) { launchSingleTop = true }
    }

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
                        it.wantedRepository,
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
        composable(Routes.SETS) {
            SetsScreen(
                viewModel = containerViewModel { SetsViewModel(it.cardRepository, it.connectivity) },
                onSelectTab = selectTab,
                onOpenSet = { setId -> nav.navigate(Routes.setCards(setId)) { launchSingleTop = true } },
            )
        }
        composable(Routes.SET_CARDS, arguments = listOf(navArgument("setId") { type = NavType.StringType })) { entry ->
            val setId = entry.arguments!!.getString("setId")!!
            SetCardsScreen(
                viewModel = containerViewModel(key = "set-$setId") {
                    SetCardsViewModel(
                        setId,
                        it.cardRepository,
                        it.wantedRepository,
                        it.pokedexRepository,
                        it.settingsRepository,
                        it.connectivity,
                    )
                },
                onBack = { nav.popBackStack() },
                onSelectTab = selectTab,
            )
        }
        composable(Routes.WANTED) {
            WantedScreen(
                viewModel = containerViewModel {
                    WantedViewModel(it.wantedRepository, it.cardRepository, it.pokedexRepository, it.settingsRepository, it.connectivity)
                },
                onSelectTab = selectTab,
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
                    AddCardViewModel(
                        dex,
                        it.pokedexRepository,
                        it.cardRepository,
                        it.wantedRepository,
                        it.settingsRepository,
                        it.connectivity,
                    )
                },
                onBack = { nav.popBackStack() },
            )
        }
    }
}
