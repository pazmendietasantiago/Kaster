package com.spaz.kaster.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spaz.kaster.presentation.screens.home.HomeScreen
import com.spaz.kaster.presentation.screens.player.VideoPlayerScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.spaz.kaster.presentation.screens.player.CastRemoteScreen

@Composable
fun KasterApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "player?uri={uri}&name={name}",
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            VideoPlayerScreen(
                navController = navController,
                videoUri = uri,
                videoName = name
            )
        }
        composable(
            route = "cast_remote",
        ) {
            CastRemoteScreen()
        }
    }
} 