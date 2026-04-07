package bme.prompteng.android.climbtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import bme.prompteng.android.climbtracker.ui.AiScreen
import bme.prompteng.android.climbtracker.ui.ClimbViewModel
import bme.prompteng.android.climbtracker.ui.ProfileScreen
import bme.prompteng.android.climbtracker.ui.TrackerScreen
import bme.prompteng.android.climbtracker.ui.TrainingScreen

class MainActivity : ComponentActivity() {
    private val viewModel: ClimbViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    BoulderingApp(viewModel)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Tracker : Screen("tracker", Icons.Filled.Place, "Climbs")
    object Ai : Screen("ai", Icons.Filled.Lightbulb, "AI")
    object Workout : Screen("workout", Icons.Filled.FitnessCenter, "Workout")
    object Profile : Screen("profile", Icons.Filled.AccountCircle, "Profile")
}

@Composable
fun BoulderingApp(viewModel: ClimbViewModel) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Tracker, Screen.Ai, Screen.Workout, Screen.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                restoreState = true
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Tracker.route, Modifier.padding(innerPadding)) {
            composable(Screen.Tracker.route) {
                TrackerScreen(
                    viewModel = viewModel
                )
            }
            composable(Screen.Ai.route) {
                AiScreen()
            }
            composable(Screen.Workout.route) {
                TrainingScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
