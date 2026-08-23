package com.remindly.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.remindly.app.ui.screens.ActiveScreen
import com.remindly.app.ui.screens.DoneScreen
import com.remindly.app.ui.screens.SettingsScreen
import com.remindly.app.ui.screens.TaskEditScreen
import com.remindly.app.ui.screens.TodayScreen

/** The four destinations reachable from the bottom bar. */
enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Today", Icons.Default.Home),
    ACTIVE("active", "Active", Icons.AutoMirrored.Filled.List),
    DONE("done", "Done", Icons.Default.CheckCircle),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

const val ROUTE_EDIT = "edit"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindlyRoot(
    viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory),
    openTaskId: Long? = null,
    onOpenTaskHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    var requestClearHistory by remember { mutableStateOf(false) }

    // Deep link from a notification tap: open that task straight away, then clear the flag
    // so a rotation doesn't reopen the editor.
    LaunchedEffect(openTaskId) {
        if (openTaskId != null && openTaskId > 0L) {
            navController.navigate("$ROUTE_EDIT/$openTaskId")
            onOpenTaskHandled()
        }
    }

    val onEditScreen = currentRoute?.startsWith(ROUTE_EDIT) == true

    Scaffold(
        topBar = {
            if (!onEditScreen) {
                TopAppBar(
                    title = { Text(TopLevel.entries.firstOrNull { it.route == currentRoute }?.label ?: "Remindly") },
                    actions = {
                        if (currentRoute == TopLevel.DONE.route) {
                            IconButton(onClick = { requestClearHistory = true }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear history")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!onEditScreen) {
                NavigationBar {
                    TopLevel.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (dest == TopLevel.TODAY && todayTasks.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text("${todayTasks.size}") } }) {
                                        Icon(dest.icon, contentDescription = dest.label)
                                    }
                                } else {
                                    Icon(dest.icon, contentDescription = dest.label)
                                }
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == TopLevel.TODAY.route || currentRoute == TopLevel.ACTIVE.route) {
                FloatingActionButton(onClick = { navController.navigate("$ROUTE_EDIT/0") }) {
                    Icon(Icons.Default.Add, contentDescription = "New reminder")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevel.TODAY.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(TopLevel.TODAY.route) {
                TodayScreen(viewModel, padding) { id -> navController.navigate("$ROUTE_EDIT/$id") }
            }
            composable(TopLevel.ACTIVE.route) {
                ActiveScreen(viewModel, padding) { id -> navController.navigate("$ROUTE_EDIT/$id") }
            }
            composable(TopLevel.DONE.route) {
                DoneScreen(
                    viewModel = viewModel,
                    contentPadding = padding,
                    onOpenTask = { id -> navController.navigate("$ROUTE_EDIT/$id") },
                    showClearDialog = requestClearHistory,
                    onDismissClearDialog = { requestClearHistory = false }
                )
            }
            composable(TopLevel.SETTINGS.route) {
                SettingsScreen(viewModel, padding)
            }
            composable(
                route = "$ROUTE_EDIT/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) { entry ->
                TaskEditScreen(
                    viewModel = viewModel,
                    taskId = entry.arguments?.getLong("taskId") ?: 0L,
                    contentPadding = padding,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
