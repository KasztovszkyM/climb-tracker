package bme.prompteng.android.climbtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import bme.prompteng.android.climbtracker.ui.ClimbViewModel
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

@Composable
fun BoulderingApp(viewModel: ClimbViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "tracker") {
        composable("tracker") {
            TrackerScreen(
                viewModel = viewModel,
                onNavigateToTraining = { navController.navigate("training") }
            )
        }
        composable("training") {
            TrainingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}