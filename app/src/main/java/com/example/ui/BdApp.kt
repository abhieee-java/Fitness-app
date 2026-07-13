package com.example.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Chat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun BdApp(viewModel: FitnessViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    AppTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { BdBottomNav(navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(220))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(220))
                }
            ) {
                composable("home") {
                    HomeScreen(viewModel = viewModel, onNavigateToWorkout = { day ->
                        navController.navigate("workout/$day")
                    })
                }
                composable("workout/{day}") { backStackEntry ->
                    val day = backStackEntry.arguments?.getString("day") ?: "Mon"
                    WorkoutScreen(viewModel = viewModel, dayId = day)
                }
                composable("progress") {
                    ProgressScreen(viewModel = viewModel)
                }
                composable("judge") {
                    JudgeScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun BdBottomNav(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("home", "Home", Icons.Default.Home),
        BottomNavItem("workout/Mon", "Workout", Icons.Default.FitnessCenter),
        BottomNavItem("progress", "Progress", Icons.Default.Timeline),
        BottomNavItem("judge", "Judge", Icons.Default.Chat)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        items.forEach { item ->
            val isSelected = currentRoute?.startsWith(item.route.split("/")[0]) == true
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)
