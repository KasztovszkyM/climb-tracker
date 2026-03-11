package bme.prompteng.android.climbtracker.ui

import kotlin.collections.isNotEmpty
import kotlin.collections.mapIndexed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import bme.prompteng.android.climbtracker.model.ClimbGrade

@Composable
fun TrackerScreen(viewModel: ClimbViewModel, onNavigateToTraining: () -> Unit) {
    val climbs by viewModel.climbs.collectAsState()
    val averageGrade by viewModel.averageGrade.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Track Your Climb", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // 6 Grade Buttons Grid
        val grades = ClimbGrade.entries.toTypedArray()
        for (i in grades.indices step 2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GradeButton(grades[i]) { viewModel.addClimb(grades[i]) }
                if (i + 1 < grades.size) {
                    GradeButton(grades[i + 1]) { viewModel.addClimb(grades[i + 1]) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.undoLastClimb() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
            Text("Undo Last Climb")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Progression (Average: ${String.format("%.2f", averageGrade)})")

        // Custom Graph Chart
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color(0xFFFAFAFA))) {
            val width = size.width
            val height = size.height
            val maxGradeValue = 5f

            // Draw Average Line
            val avgY = height - ((averageGrade / maxGradeValue) * height)
            drawLine(
                color = Color.Red,
                start = Offset(0f, avgY),
                end = Offset(width, avgY),
                strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Draw Progression Line
            if (climbs.isNotEmpty()) {
                val pointSpacing = if (climbs.size > 1) width / (climbs.size - 1) else width
                val points = climbs.mapIndexed { index, climb ->
                    val x = index * pointSpacing
                    val y = height - ((climb.gradeValue / maxGradeValue) * height)
                    Offset(x, y)
                }

                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = Color.Blue,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 6f
                    )
                }

                points.forEach { point ->
                    drawCircle(color = Color.DarkGray, radius = 8f, center = point)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onNavigateToTraining, modifier = Modifier.fillMaxWidth()) {
            Text("Get Training Plan")
        }
    }
}

@Composable
fun GradeButton(grade: ClimbGrade, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = grade.color),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(150.dp).height(60.dp)
    ) {
        Text(grade.label, color = grade.textColor)
    }
}
