package com.app.pro.mayurone.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.pro.mayurone.ui.navigation.NavGraph
import com.app.pro.mayurone.ui.navigation.Screen
import com.app.pro.mayurone.ui.viewmodel.MayurOneViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MayurOneApp(viewModel: MayurOneViewModel) {
    val navController = rememberNavController()
    val activeTenant by viewModel.activeTenant.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        NavigationItem("Estimates", Screen.Estimates.route, Icons.Default.ReceiptLong),
        NavigationItem("Customers", Screen.Customers.route, Icons.Default.People),
        NavigationItem("Settings", Screen.Settings.route, Icons.Default.Settings)
    )

    val topLevelRoutes = listOf(
        Screen.Estimates.route,
        Screen.Customers.route,
        Screen.Settings.route
    )
    val isTopLevelDestination = currentRoute in topLevelRoutes

    Scaffold(
        topBar = {
            if (isTopLevelDestination) {
                TopAppBar(
                    title = {
                        Text(
                            text = activeTenant?.businessName ?: "MayurOne SaaS",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        val syncIcon = if (pendingSyncCount > 0) Icons.Default.Sync else Icons.Default.CloudDone
                        Icon(
                            imageVector = syncIcon,
                            contentDescription = "Sync Status",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        )
    }
}

private data class NavigationItem(
    val label: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
