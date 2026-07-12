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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import bme.prompteng.android.climbtracker.data.ClimbEntity
import bme.prompteng.android.climbtracker.model.ClimbGrade
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextAlign

import bme.prompteng.android.climbtracker.ui.components.ClimbetterHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(viewModel: ClimbViewModel, onHome: () -> Unit) {
    val climbs: List<ClimbEntity> by viewModel.climbs.collectAsState()
    val allClimbs: List<ClimbEntity> by viewModel.allClimbs.collectAsState()
    val totalAverageGrade: Float by viewModel.totalAverageGrade.collectAsState()
    val filterDate: Long? by viewModel.filterDate.collectAsState()
    val currentQuote: String by viewModel.currentQuote.collectAsState()
    val isDarkMode: Boolean? by viewModel.isDarkMode.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val platformLocale = LocalLocale.current.platformLocale

    val todaysClimbs by remember(allClimbs) {
        derivedStateOf {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            allClimbs.filter { it.timestamp >= today }
        }
    }

    val todaysAvg by remember(todaysClimbs) {
        derivedStateOf {
            if (todaysClimbs.isEmpty()) 0f else todaysClimbs.map { it.gradeValue }.average().toFloat()
        }
    }

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
            // Header
            ClimbetterHeader(
                onHome = onHome,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode() }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Motivational Quote
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
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                // Select Type
                Text("Select Type:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(type, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val grades = ClimbGrade.entries.toTypedArray()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in grades.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GradeButton(
                                grade = grades[i],
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) { 
                                viewModel.addClimb(grades[i], selectedType)
                            }
                            if (i + 1 < grades.size) {
                                GradeButton(
                                    grade = grades[i + 1],
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) { 
                                    viewModel.addClimb(grades[i + 1], selectedType)
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.undoLastClimb() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.weight(1f).height(40.dp).padding(horizontal = 4.dp)
                    ) {
                        Text("Undo", style = MaterialTheme.typography.labelLarge)
                    }

                    Button(
                        onClick = { showHistoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f).height(40.dp).padding(horizontal = 4.dp)
                    ) {
                        Text("History", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Progression Graph (Landing Screen)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Today's Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ClimbChart(
                            climbs = todaysClimbs,
                            averageGrade = todaysAvg,
                            title = "",
                            height = 100.dp,
                            containerColor = Color.Transparent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Scroll down for statistics",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            // Scroll down approximately one screen height
                            scrollState.animateScrollTo(scrollState.value + 800)
                        }
                    }
                )
                
                // Gap to keep statistics off the starting page
                Spacer(modifier = Modifier.height(fullHeight * 0.2f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(modifier = Modifier.height(32.dp))

                // Section 2: Detailed Analysis
                Text("Detailed Analysis", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                // Overall Progression Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Overall Progression",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "All-Time Average: ${String.format(platformLocale, "%.2f", totalAverageGrade)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chart showing recent overall climbs (ignore date filter)
                        ClimbChart(
                            climbs = allClimbs.takeLast(50),
                            averageGrade = totalAverageGrade,
                            title = "Recent History",
                            height = 120.dp,
                            containerColor = Color.Transparent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date-filtered Statistics
                // Date Filter UI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (filterDate == null) "All Time" else SimpleDateFormat("yyyy-MM-dd", LocalLocale.current.platformLocale).format(Date(filterDate!!)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (filterDate != null) {
                        IconButton(onClick = { viewModel.setFilterDate(null) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Filter", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Grade Distribution (${if (filterDate == null) "All Time" else "Filtered"})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GradeDistributionChart(
                            climbs = climbs,
                            height = 150.dp,
                            containerColor = Color.Transparent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Breakdown by Style", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                climbTypes.forEach { type ->
                    val filteredClimbs = climbs.filter { it.climbType == type }
                    if (filteredClimbs.isNotEmpty()) {
                        val typeAvg = filteredClimbs.map { it.gradeValue }.average().toFloat()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                ClimbChart(
                                    climbs = filteredClimbs,
                                    averageGrade = typeAvg,
                                    title = type,
                                    height = 100.dp,
                                    containerColor = Color.Transparent
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // FAB
        val showFab by remember {
            derivedStateOf {
                val fabThreshold = with(density) { 200.dp.toPx() }
                scrollState.value > fabThreshold
            }
        }

        if (showFab) {
            SmallFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(0)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
            }
        }
    }
}

@Composable
fun GradeDistributionChart(
    climbs: List<ClimbEntity>,
    height: androidx.compose.ui.unit.Dp = 200.dp,
    containerColor: Color = Color.Transparent
) {
    val grades = ClimbGrade.entries
    val maxCount = grades.maxOfOrNull { grade -> climbs.count { it.gradeValue == grade.value } }?.coerceAtLeast(1) ?: 1
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val secondaryTextColor = MaterialTheme.colorScheme.outline

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        val chartModifier = if (containerColor != Color.Transparent) {
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(containerColor, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(8.dp)
        }
        
        Canvas(modifier = chartModifier) {
            val w = size.width
            val h = size.height
            val barWidth = w / grades.size
            val textPaint = Paint().apply {
                color = textColor.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
            }
            val labelPaint = Paint().apply {
                color = secondaryTextColor.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = 24f
            }

            val usableHeight = (h - 80f).coerceAtLeast(0f)
            val bottomPadding = 40f

            grades.forEachIndexed { index, grade ->
                val count = climbs.count { it.gradeValue == grade.value }
                val barHeight = (count.toFloat() / maxCount) * usableHeight 
                val x = index * barWidth
                val y = h - bottomPadding - barHeight

                drawRect(
                    color = grade.color,
                    topLeft = Offset(x + (barWidth * 0.1f), y),
                    size = Size(barWidth * 0.8f, barHeight)
                )

                // Add a subtle border for visibility
                drawRect(
                    color = secondaryTextColor.copy(alpha = 0.3f),
                    topLeft = Offset(x + (barWidth * 0.1f), y),
                    size = Size(barWidth * 0.8f, barHeight),
                    style = Stroke(width = 1f)
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
                    labelPaint
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
    height: androidx.compose.ui.unit.Dp = 250.dp,
    containerColor: Color = Color.Transparent
) {
    val secondaryTextColor = MaterialTheme.colorScheme.outline
    val errorColor = MaterialTheme.colorScheme.error

    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        if (title.isNotEmpty()) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = secondaryTextColor)
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        val chartModifier = if (containerColor != Color.Transparent) {
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(containerColor, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(8.dp)
        }

        Canvas(modifier = chartModifier) {
            val w = size.width
            val h = size.height
            val maxGradeValue = 5f

            if (climbs.isNotEmpty()) {
                val normalizedAvg = (averageGrade + 0.2f) / (maxGradeValue + 0.2f)
                val avgY = h - (normalizedAvg * h)
                drawLine(
                    color = errorColor,
                    start = Offset(0f, avgY),
                    end = Offset(w, avgY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

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

                    // Add a subtle border for visibility (only if bars are wide enough)
                    if (barWidth > 3.dp.toPx()) {
                        drawRect(
                            color = secondaryTextColor.copy(alpha = 0.3f),
                            topLeft = Offset(x + (barWidth * 0.1f), y),
                            size = Size(barWidth * 0.8f, barHeight),
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GradeButton(grade: ClimbGrade, modifier: Modifier = Modifier, onClick: () -> Unit) {
    BoxWithConstraints(modifier = modifier) {
        val useShortLabel = maxWidth < 120.dp
        val shortLabel = when (grade.name) {
            "WHITE" -> "V. Easy"
            "BLUE" -> "Easy"
            "YELLOW" -> "Med."
            "GREEN" -> "Hard"
            "RED" -> "V. Hard"
            "BLACK" -> "Extr."
            else -> grade.label
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = grade.color),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxSize(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text(
                text = if (useShortLabel) shortLabel else grade.label,
                color = grade.textColor,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
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
                                Text(SimpleDateFormat("MMM dd, HH:mm", LocalLocale.current.platformLocale).format(Date(climb.timestamp)))
                            },
                            trailingContent = {
                                IconButton(onClick = { onDelete(climb) }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
