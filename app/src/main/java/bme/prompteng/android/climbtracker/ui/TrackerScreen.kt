package bme.prompteng.android.climbtracker.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bme.prompteng.android.climbtracker.data.ClimbEntity
import bme.prompteng.android.climbtracker.model.ClimbGrade
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(viewModel: ClimbViewModel) {
    val climbs by viewModel.climbs.collectAsState()
    val averageGrade by viewModel.averageGrade.collectAsState()
    val filterDate by viewModel.filterDate.collectAsState()
    val currentQuote by viewModel.currentQuote.collectAsState()
    val scrollState = rememberScrollState()

    var showHistoryDialog by remember { mutableStateOf(false) }

    if (showHistoryDialog) {
        HistoryDialog(
            climbs = climbs,
            onDelete = { viewModel.deleteClimb(it) },
            onDismiss = { showHistoryDialog = false }
        )
    }
    
    // Refresh quote when screen becomes visible
    LaunchedEffect(Unit) {
        viewModel.refreshQuote()
    }

    var selectedType by remember { mutableStateOf("Static") }
    val climbTypes = listOf("Slab", "Overhang", "Dynamic", "Static")

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setFilterDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullHeight = this.maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section 1: Logging Controls (Unified Full Screen experience)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = fullHeight)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Track Your Climb", style = MaterialTheme.typography.headlineMedium)
                
                // Motivational Quote Line
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable { viewModel.refreshQuote() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = currentQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Climb Type Selection Row
                Text("Select Type:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    climbTypes.forEach { type ->
                        val isSelected = selectedType == type
                        OutlinedButton(
                            onClick = { selectedType = type },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(32.dp).padding(horizontal = 2.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder
                        ) {
                            Text(type, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Grade Logging Buttons
                val grades = ClimbGrade.entries.toTypedArray()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    for (i in grades.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            GradeButton(grades[i]) { viewModel.addClimb(grades[i], selectedType) }
                            if (i + 1 < grades.size) {
                                GradeButton(grades[i + 1]) { viewModel.addClimb(grades[i + 1], selectedType) }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.undoLastClimb() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.width(150.dp).height(40.dp)
                    ) {
                        Text("Undo Last")
                    }

                    Button(
                        onClick = { showHistoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.width(150.dp).height(40.dp)
                    ) {
                        Text("History")
                    }
                }

                // This Spacer pushes the following text to the bottom of the screen
                Text("Scroll down for statistics ↓", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Section 2: Statistics and Charts
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                // Date Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (filterDate == null) "All Time" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(filterDate!!)),
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (filterDate != null) {
                        IconButton(onClick = { viewModel.setFilterDate(null) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Filter", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Climbing Progression",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Current Average: ${String.format("%.2f", averageGrade)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Main Custom Column Chart
                ClimbChart(climbs = climbs, averageGrade = averageGrade, title = "All Styles Progression")
                
                Spacer(modifier = Modifier.height(32.dp))

                // Grade Distribution Chart
                GradeDistributionChart(climbs = climbs)

                Spacer(modifier = Modifier.height(32.dp))
                Text("Breakdown by Style", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // 4 Smaller Charts for each style
                climbTypes.forEach { type ->
                    val filteredClimbs = climbs.filter { it.climbType == type }
                    val typeAvg = if (filteredClimbs.isEmpty()) 0f else filteredClimbs.map { it.gradeValue }.average().toFloat()
                    
                    ClimbChart(
                        filteredClimbs,
                        typeAvg,
                        type,
                        150.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun GradeDistributionChart(climbs: List<ClimbEntity>) {
    val grades = ClimbGrade.entries
    val maxCount = grades.maxOfOrNull { grade -> climbs.count { it.gradeValue == grade.value } }?.coerceAtLeast(1) ?: 1

    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text("Grade Distribution (Count)", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            val w = size.width
            val h = size.height
            val barWidth = w / grades.size
            val textPaint = Paint().apply {
                color = android.graphics.Color.BLACK
                textAlign = Paint.Align.CENTER
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
            }

            grades.forEachIndexed { index, grade ->
                val count = climbs.count { it.gradeValue == grade.value }
                val barHeight = (count.toFloat() / maxCount) * (h - 40f) 
                val x = index * barWidth
                val y = h - barHeight

                drawRect(
                    color = grade.color,
                    topLeft = Offset(x + (barWidth * 0.1f), y),
                    size = Size(barWidth * 0.8f, barHeight)
                )

                if (count > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        count.toString(),
                        x + barWidth / 2,
                        y - 10f,
                        textPaint
                    )
                }
                
                drawContext.canvas.nativeCanvas.drawText(
                    grade.label.take(5),
                    x + barWidth / 2,
                    h - 5f,
                    Paint().apply {
                        color = android.graphics.Color.DKGRAY
                        textAlign = Paint.Align.CENTER
                        textSize = 24f
                    }
                )
            }
        }
    }
}

@Composable
fun ClimbChart(
    climbs: List<ClimbEntity>,
    averageGrade: Float,
    title: String,
    height: androidx.compose.ui.unit.Dp = 250.dp
) {
    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            val w = size.width
            val h = size.height
            val maxGradeValue = 5f

            val avgY = h - ((averageGrade / maxGradeValue) * h)
            drawLine(
                color = Color.Red,
                start = Offset(0f, avgY),
                end = Offset(w, avgY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            if (climbs.isNotEmpty()) {
                val barWidth = w / climbs.size
                climbs.forEachIndexed { index, climb ->
                    val x = index * barWidth
                    val value = climb.gradeValue.toFloat()
                    val barHeight = ((value + 0.2f) / (maxGradeValue + 0.2f)) * h
                    val y = h - barHeight
                    val gradeColor = ClimbGrade.entries.find { it.value == climb.gradeValue }?.color ?: Color.Gray
                    
                    drawRect(
                        color = gradeColor,
                        topLeft = Offset(x + (barWidth * 0.1f), y),
                        size = Size(barWidth * 0.8f, barHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun GradeButton(grade: ClimbGrade, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = grade.color),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(150.dp).height(150.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = grade.label,
            color = grade.textColor,
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun HistoryDialog(
    climbs: List<ClimbEntity>,
    onDelete: (ClimbEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Climb History") },
        text = {
            if (climbs.isEmpty()) {
                Text("No climbs logged yet.")
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    val reversedClimbs = climbs.reversed()
                    items(reversedClimbs.size) { index ->
                        val climb = reversedClimbs[index]
                        val grade = ClimbGrade.entries.find { it.value == climb.gradeValue }
                        ListItem(
                            headlineContent = { Text("${grade?.label ?: "Unknown"} (${climb.climbType})") },
                            supportingContent = {
                                Text(SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(climb.timestamp)))
                            },
                            trailingContent = {
                                IconButton(onClick = { onDelete(climb) }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
