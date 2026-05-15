package bme.prompteng.android.climbtracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import bme.prompteng.android.climbtracker.ui.components.ClimbetterHeader
import bme.prompteng.android.climbtracker.model.Exercise
import bme.prompteng.android.climbtracker.model.TrainingFocus
import bme.prompteng.android.climbtracker.model.WorkoutCategory
import bme.prompteng.android.climbtracker.model.WorkoutPlan

@Composable
fun TrainingScreen(viewModel: ClimbViewModel, onBack: () -> Unit, onHome: () -> Unit) {
    val currentState by viewModel.trainingState.collectAsState()
    val isLoading by viewModel.isLoadingPlan.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Logo Header (Matching TrackerScreen/ProfileScreen style)
            ClimbetterHeader(
                onHome = onHome,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode() }
            )

            AnimatedContent(
                targetState = currentState,
                label = "TrainingTransition",
                modifier = Modifier.weight(1f)
            ) { state ->
                when (state) {
                    is TrainingState.CategorySelection -> {
                        CategorySelectionContent(
                            onCategorySelected = { category ->
                                if (category == WorkoutCategory.TRAIN) {
                                    viewModel.setTrainingState(TrainingState.TrainingFocusSelection)
                                } else {
                                    if (viewModel.currentWorkout.value == null) {
                                        viewModel.startManualWorkout(category)
                                    } else {
                                        viewModel.setTrainingState(TrainingState.WorkoutExecution(category))
                                    }
                                }
                            }
                        )
                    }
                    is TrainingState.TrainingFocusSelection -> {
                        FocusSelectionContent(
                            onFocusSelected = { focus ->
                                if (viewModel.currentWorkout.value == null) {
                                    viewModel.startManualWorkout(WorkoutCategory.TRAIN, focus)
                                } else {
                                    viewModel.setTrainingState(TrainingState.WorkoutExecution(WorkoutCategory.TRAIN, focus))
                                }
                            },
                            onBack = { viewModel.setTrainingState(TrainingState.CategorySelection) }
                        )
                    }
                    is TrainingState.WorkoutExecution -> {
                        WorkoutExecutionContent(
                            viewModel = viewModel,
                            isLoading = isLoading,
                            onFinish = { viewModel.resetWorkout() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelectionContent(onCategorySelected: (WorkoutCategory) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryButton("Warm up", onClick = { onCategorySelected(WorkoutCategory.WARMUP) })
        Spacer(modifier = Modifier.height(24.dp))
        CategoryButton("Train", onClick = { onCategorySelected(WorkoutCategory.TRAIN) })
        Spacer(modifier = Modifier.height(24.dp))
        CategoryButton("Stretch", onClick = { onCategorySelected(WorkoutCategory.STRETCH) })
    }
}

@Composable
fun CategoryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun FocusSelectionContent(onFocusSelected: (TrainingFocus) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Train",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(TrainingFocus.entries.toTypedArray().size) { index ->
                val focus = TrainingFocus.entries[index]
                FocusButton(focus, onClick = { onFocusSelected(focus) })
            }
        }
    }
}

@Composable
fun FocusButton(focus: TrainingFocus, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            focus.label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WorkoutExecutionContent(
    viewModel: ClimbViewModel,
    isLoading: Boolean,
    onFinish: () -> Unit
) {
    val workoutState = viewModel.currentWorkout.collectAsState()
    val workout: WorkoutPlan? = workoutState.value
    val exercises: List<Exercise> = workout?.exercises ?: emptyList()
    var showExercisePicker by remember { mutableStateOf(false) }

    val trainingState = viewModel.trainingState.collectAsState()
    val currentCategory = (trainingState.value as? TrainingState.WorkoutExecution)?.category ?: WorkoutCategory.WARMUP

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                workout?.title ?: "Loading...",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (exercises.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AddTask,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No exercises added yet.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val state = viewModel.lastRequestedState
                        if (state != null) {
                            viewModel.generateWorkout(state.first, state.second)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate with AI", color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manual Selection", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        } else {
            // Exercise List
            val listState = rememberLazyListState()
            var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
            var draggingOffset by remember { mutableFloatStateOf(0f) }
            val density = LocalDensity.current

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                itemsIndexed(items = exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                    val currentIndex by rememberUpdatedState(index)
                    val isDragging = draggedItemIndex == currentIndex
                    //val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")
                    
                    Box(
                        modifier = Modifier
                            .animateItem()
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) draggingOffset else 0f
                                scaleX = if (isDragging) 1.05f else 1.0f
                                scaleY = if (isDragging) 1.05f else 1.0f
                                alpha = if (isDragging) 0.9f else 1.0f
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { _ ->
                                        draggedItemIndex = currentIndex
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingOffset += dragAmount.y
                                        
                                        // Estimate item height including spacing for smoother transitions
                                        val itemHeight = with(density) { 90.dp.toPx() }
                                        val threshold = itemHeight * 0.5f
                                        
                                        if (draggingOffset > threshold && currentIndex < exercises.size - 1) {
                                            viewModel.moveExercise(currentIndex, currentIndex + 1)
                                            draggedItemIndex = currentIndex + 1
                                            draggingOffset -= itemHeight
                                        } else if (draggingOffset < -threshold && currentIndex > 0) {
                                            viewModel.moveExercise(currentIndex, currentIndex - 1)
                                            draggedItemIndex = currentIndex - 1
                                            draggingOffset += itemHeight
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemIndex = null
                                        draggingOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = null
                                        draggingOffset = 0f
                                    }
                                )
                            }
                    ) {
                        ExerciseListItem(
                            exercise = exercise,
                            onToggle = { viewModel.toggleExerciseCompletion(exercise.id) },
                            onRemove = { viewModel.removeExercise(exercise.id) }
                        )
                    }
                }
                item {
                    TextButton(
                        onClick = { showExercisePicker = true },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add More Exercises", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val state = viewModel.lastRequestedState
                        if (state != null) {
                            viewModel.generateWorkout(state.first, state.second)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regenerate AI", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Button(
                    onClick = onFinish,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Finish", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        // Progress Bar
        val completedCount = exercises.count { it.isCompleted }
        val progress = if (exercises.isEmpty()) 0f else completedCount.toFloat() / exercises.size.toFloat()

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            category = currentCategory,
            onDismiss = { showExercisePicker = false },
            onExerciseSelected = { exercise ->
                viewModel.addExercise(exercise)
            }
        )
    }
}

@Composable
fun ExercisePickerDialog(
    category: WorkoutCategory,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    val library = bme.prompteng.android.climbtracker.model.ExerciseLibrary[category] ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Exercise") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(library.size) { index ->
                    val exercise = library[index]
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onExerciseSelected(exercise)
                                // We keep the dialog open to add multiple? 
                                // User said "hand pick", so maybe multiple is good.
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(exercise.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val detail = buildString {
                                exercise.durationSeconds?.let { append("${it}s ") }
                                exercise.reps?.let { append(it) }
                            }
                            if (detail.isNotBlank()) {
                                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun ExerciseListItem(
    exercise: Exercise,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (exercise.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (exercise.isCompleted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle Icon
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (exercise.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (exercise.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle() }
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (exercise.isCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                val detail = buildString {
                    exercise.durationSeconds?.let { append("${it}s ") }
                    exercise.reps?.let { append(it) }
                }
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        }
    }
}
