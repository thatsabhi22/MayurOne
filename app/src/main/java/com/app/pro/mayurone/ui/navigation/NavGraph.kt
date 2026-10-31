package com.app.pro.mayurone.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.app.pro.mayurone.ui.screens.customers.CustomersScreen
import com.app.pro.mayurone.ui.screens.estimates.EstimateDetailScreen
import com.app.pro.mayurone.ui.screens.estimates.EstimateFormScreen
import com.app.pro.mayurone.ui.screens.estimates.EstimatesScreen
import com.app.pro.mayurone.ui.screens.settings.SettingsScreen
import com.app.pro.mayurone.ui.viewmodel.MayurOneViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MayurOneViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Estimates.route,
        modifier = modifier
    ) {
        composable(Screen.Estimates.route) {
            EstimatesScreen(
                viewModel = viewModel,
                onNavigateToCreate = {
                    navController.navigate(Screen.EstimateForm.createRoute(null))
                },
                onNavigateToDetail = { estimateId ->
                    navController.navigate(Screen.EstimateDetail.createRoute(estimateId))
                }
            )
        }

        composable(
            route = Screen.EstimateForm.route,
            arguments = listOf(
                navArgument("estimateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val estimateId = backStackEntry.arguments?.getString("estimateId")
            EstimateFormScreen(
                viewModel = viewModel,
                existingEstimateId = estimateId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EstimateDetail.route,
            arguments = listOf(
                navArgument("estimateId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val estimateId = backStackEntry.arguments?.getString("estimateId") ?: ""
            EstimateDetailScreen(
                viewModel = viewModel,
                estimateId = estimateId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNewRevision = { id ->
                    navController.navigate(Screen.EstimateForm.createRoute(id))
                }
            )
        }

        composable(Screen.Customers.route) {
            CustomersScreen(viewModel = viewModel)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel)
        }
    }
}
