package com.dayanand.wordscapes.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dayanand.wordscapes.data.repository.LevelRepositoryImpl
import com.dayanand.wordscapes.data.repository.UserPreferencesRepositoryImpl
import com.dayanand.wordscapes.data.source.LevelDataSource
import com.dayanand.wordscapes.presentation.gameplay.GameScreen
import com.dayanand.wordscapes.presentation.gameplay.GameViewModel
import com.dayanand.wordscapes.presentation.home.HomeScreen
import com.dayanand.wordscapes.presentation.home.HomeViewModel
import com.dayanand.wordscapes.presentation.levelselect.LevelSelectScreen
import com.dayanand.wordscapes.presentation.levelselect.LevelSelectViewModel
import com.dayanand.wordscapes.presentation.pause.PauseScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext

    // Repositories
    val levelRepository = remember {
        LevelRepositoryImpl(LevelDataSource(context))
    }
    val userPreferencesRepository = remember {
        UserPreferencesRepositoryImpl(context)
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // HOME
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel {
                HomeViewModel(userPreferencesRepository)
            }
            HomeScreen(
                viewModel = viewModel,
                onPlayClick = { levelId ->
                    navController.navigate(Routes.gameplay(levelId))
                },
                onLevelSelectClick = {
                    navController.navigate(Routes.LEVEL_SELECT) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // LEVEL SELECT
        composable(Routes.LEVEL_SELECT) {
            val viewModel: LevelSelectViewModel = viewModel {
                LevelSelectViewModel(levelRepository, userPreferencesRepository)
            }
            LevelSelectScreen(
                viewModel = viewModel,
                onLevelClick = { levelId ->
                    navController.navigate(Routes.gameplay(levelId))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // GAMEPLAY
        composable(
            route = Routes.GAMEPLAY,
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt("levelId") ?: 1
            val viewModel: GameViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(
                        modelClass: Class<T>,
                        extras: CreationExtras
                    ): T {
                        val savedStateHandle = extras.createSavedStateHandle()
                        @Suppress("UNCHECKED_CAST")
                        return GameViewModel(
                            savedStateHandle = savedStateHandle,
                            levelRepository = levelRepository,
                            userPreferencesRepository = userPreferencesRepository
                        ) as T
                    }
                }
            )

            GameScreen(
                viewModel = viewModel,
                levelId = levelId,
                onPauseClick = {
                    navController.navigate(Routes.pause(levelId)) {
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    if (!navController.popBackStack(Routes.LEVEL_SELECT, false)) {
                        navController.popBackStack()
                    }
                },
                onNextLevelClick = { nextId ->
                    navController.navigate(Routes.gameplay(nextId)) {
                        popUpTo(Routes.GAMEPLAY) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onLevelSelectClick = {
                    if (!navController.popBackStack(Routes.LEVEL_SELECT, false)) {
                        navController.navigate(Routes.LEVEL_SELECT) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                }
            )
        }

        // PAUSE
        composable(
            route = Routes.PAUSE,
            arguments = listOf(navArgument("levelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt("levelId") ?: 1

            PauseScreen(
                levelId = levelId,
                onResumeClick = {
                    navController.popBackStack()
                },
                onLevelSelectClick = {
                    if (!navController.popBackStack(Routes.LEVEL_SELECT, false)) {
                        navController.navigate(Routes.LEVEL_SELECT) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                },
                onHomeClick = {
                    navController.popBackStack(Routes.HOME, false)
                }
            )
        }
    }
}
