package com.potentplum.dextracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.potentplum.dextracker.ui.addcard.AddCardScreen
import com.potentplum.dextracker.ui.addcard.AddCardViewModel
import com.potentplum.dextracker.ui.detail.DetailScreen
import com.potentplum.dextracker.ui.detail.DetailViewModel
import com.potentplum.dextracker.ui.grid.GridScreen
import com.potentplum.dextracker.ui.grid.GridViewModel

private object Routes {
    const val GRID = "grid"
    const val DETAIL = "detail/{dex}"
    const val ADD_CARD = "add/{dex}"
    fun detail(dex: Int) = "detail/$dex"
    fun addCard(dex: Int) = "add/$dex"
}

private val dexArg = listOf(navArgument("dex") { type = NavType.IntType })

@Composable
fun DexNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.GRID) {
        composable(Routes.GRID) {
            GridScreen(
                viewModel = containerViewModel { GridViewModel(it.pokedexRepository) },
                onPokemonClick = { dex -> nav.navigate(Routes.detail(dex)) { launchSingleTop = true } },
            )
        }
        composable(Routes.DETAIL, arguments = dexArg) { entry ->
            val dex = entry.arguments!!.getInt("dex")
            DetailScreen(
                viewModel = containerViewModel { DetailViewModel(dex, it.pokedexRepository, it.cardRepository) },
                onBack = { nav.popBackStack() },
                onAddCard = { nav.navigate(Routes.addCard(dex)) { launchSingleTop = true } },
            )
        }
        composable(Routes.ADD_CARD, arguments = dexArg) { entry ->
            val dex = entry.arguments!!.getInt("dex")
            AddCardScreen(
                viewModel = containerViewModel { AddCardViewModel(dex, it.pokedexRepository, it.cardRepository) },
                onBack = { nav.popBackStack() },
                onAdded = { nav.popBackStack() },
            )
        }
    }
}
