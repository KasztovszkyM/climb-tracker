package bme.prompteng.android.climbtracker.ui

import android.widget.Toast
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.customui.DefaultPlayerUiController
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
    var videoIdToPlay by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Logo Header (Matching TrackerScreen/ProfileScreen style)
            ClimbetterHeader(
                onHome = onHome,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode() }
            )

            // Simple state-based content instead of AnimatedContent to debug LayoutNode crash
            Box(modifier = Modifier.weight(1f)) {
                when (val state = currentState) {
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
                            onStart = { viewModel.startGuidedWorkout() },
                            onBack = { viewModel.resetWorkout() },
                            onPlayVideo = { videoUrl ->
                                videoIdToPlay = extractYoutubeVideoId(videoUrl)
                            }
                        )
                    }
                    is TrainingState.ActiveExercise -> {
                        ActiveExerciseContent(
                            viewModel = viewModel,
                            exerciseIndex = state.exerciseIndex,
                            onBack = {
                                val workout = viewModel.currentWorkout.value
                                if (workout != null) {
                                    viewModel.setTrainingState(TrainingState.WorkoutExecution(workout.category, workout.focus))
                                } else {
                                    viewModel.setTrainingState(TrainingState.CategorySelection)
                                }
                            }
                        )
                    }
                    is TrainingState.ExerciseDone -> {
                        ExerciseDoneContent(
                            viewModel = viewModel,
                            exerciseIndex = state.exerciseIndex,
                            onExit = {
                                val workout = viewModel.currentWorkout.value
                                if (workout != null) {
                                    viewModel.setTrainingState(TrainingState.WorkoutExecution(workout.category, workout.focus))
                                } else {
                                    viewModel.setTrainingState(TrainingState.CategorySelection)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Full-screen Video Overlay using Dialog to isolate layout lifecycle
        videoIdToPlay?.let { videoId ->
            key(videoId) {
                VideoPlayerOverlay(
                    videoId = videoId,
                    onDismiss = { videoIdToPlay = null }
                )
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
    onStart: () -> Unit,
    onBack: () -> Unit,
    onPlayVideo: (String) -> Unit
) {
    val workoutState = viewModel.currentWorkout.collectAsState()
    val workout: WorkoutPlan? = workoutState.value
    val exercises: List<Exercise> = workout?.exercises ?: emptyList()
    var showExercisePicker by remember { mutableStateOf(false) }
    var expandedExerciseId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

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
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
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
                    val isExpanded = expandedExerciseId == exercise.id
                    val isDragging = draggedItemIndex == currentIndex
                    
                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) draggingOffset else 0f
                                scaleX = if (isDragging) 1.05f else 1.0f
                                scaleY = if (isDragging) 1.05f else 1.0f
                                alpha = if (isDragging) 0.9f else 1.0f
                            }
                    .pointerInput(exercise.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { _ ->
                                // Collapse any expanded item for performance during drag
                                expandedExerciseId = null
                                draggedItemIndex = currentIndex
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggingOffset += dragAmount.y
                                
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
                                viewModel.persistCurrentWorkout()
                            },
                            onDragCancel = {
                                draggedItemIndex = null
                                draggingOffset = 0f
                                viewModel.persistCurrentWorkout()
                            }
                        )
                    }
                    ) {
                        ExerciseListItem(
                            exercise = exercise,
                            isExpanded = isExpanded,
                            onExpandToggle = {
                                expandedExerciseId = if (isExpanded) null else exercise.id
                            },
                            onPlayVideo = onPlayVideo,
                            onToggle = { viewModel.toggleExerciseCompletion(exercise.id) },
                            onRemove = { viewModel.removeExercise(exercise.id) }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(
                            onClick = { showExercisePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add More", color = MaterialTheme.colorScheme.primary)
                        }
                        
                        if (exercises.isNotEmpty()) {
                            TextButton(
                                onClick = { 
                                    viewModel.undoLastExercise()
                                    Toast.makeText(context, "Last exercise removed", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Undo", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
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
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Workout", color = MaterialTheme.colorScheme.onPrimary)
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
                Toast.makeText(context, "Added: ${exercise.name}", Toast.LENGTH_SHORT).show()
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = exercise.name,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!exercise.videoUrl.isNullOrBlank()) {
                                    Icon(
                                        Icons.Default.VideoLibrary,
                                        contentDescription = "Video available",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
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
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = if (exercise.isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (exercise.isCompleted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column {
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
                        .clickable { onExpandToggle() }
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

                if (!exercise.videoUrl.isNullOrBlank() && !isExpanded) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Has Video",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(start = 56.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth()
                ) {
                    exercise.instruction?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (!exercise.videoUrl.isNullOrBlank()) {
                        VideoThumbnail(
                            videoId = extractYoutubeVideoId(exercise.videoUrl) ?: "",
                            onPlayClick = { onPlayVideo(exercise.videoUrl) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerOverlay(videoId: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    YoutubePlayerLibrary(
                        videoId = videoId,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.height(56.dp).padding(horizontal = 24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Close Video", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun YoutubePlayerLibrary(videoId: String, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cleanVideoId = videoId.trim()
    val playerRef = remember { mutableStateOf<YouTubePlayer?>(null) }
    val lastLoadedVideoId = remember { mutableStateOf("") }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            YouTubePlayerView(context).apply {
                enableAutomaticInitialization = false

                val options = IFramePlayerOptions.Builder(context)
                    .controls(0) // Hide YouTube Web UI entirely to remove clutter
                    .fullscreen(0)
                    .ivLoadPolicy(3)
                    .rel(0)
                    .modestBranding(1)
                    .build()

                val listener = object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        playerRef.value = youTubePlayer
                        val defaultPlayerUiController = DefaultPlayerUiController(this@apply, youTubePlayer)
                        defaultPlayerUiController.showVideoTitle(false)
                        defaultPlayerUiController.showYouTubeButton(false)
                        defaultPlayerUiController.showFullscreenButton(false)
                        defaultPlayerUiController.showMenuButton(false)
                        defaultPlayerUiController.showCurrentTime(true)
                        defaultPlayerUiController.showDuration(true)

                        this@apply.setCustomPlayerUi(defaultPlayerUiController.rootView)

                        youTubePlayer.unMute()
                        // Ensure we load the LATEST video ID even if it changed during init
                        val latestId = lastLoadedVideoId.value.ifBlank { cleanVideoId }
                        youTubePlayer.loadVideo(latestId, 0f)
                        lastLoadedVideoId.value = latestId
                    }

                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        android.util.Log.e("YoutubePlayer", "Internal Player Error for $cleanVideoId: $error")
                    }
                }

                initialize(listener, options)
                lifecycleOwner.lifecycle.addObserver(this)
            }
        },
        update = { _ ->
            // Handle video changes if the view is reused
            val p = playerRef.value
            if (p != null && lastLoadedVideoId.value != cleanVideoId) {
                p.loadVideo(cleanVideoId, 0f)
                lastLoadedVideoId.value = cleanVideoId
            }
        },
        onRelease = { view ->
            lifecycleOwner.lifecycle.removeObserver(view)
            view.release()
        }
    )
}

@Composable
fun ActiveExerciseContent(
    viewModel: ClimbViewModel,
    exerciseIndex: Int,
    onBack: () -> Unit
) {
    val workout by viewModel.currentWorkout.collectAsState()
    val exercise = workout?.exercises?.getOrNull(exerciseIndex) ?: return
    var timeLeft by remember(exerciseIndex) { mutableStateOf(exercise.durationSeconds ?: 0) }
    val isTimerRunning = timeLeft > 0

    LaunchedEffect(exerciseIndex, isTimerRunning) {
        if (isTimerRunning) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            viewModel.nextExercise(exerciseIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Exercise ${exerciseIndex + 1}/${workout?.exercises?.size}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Text(
            text = exercise.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Video Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            val videoId = extractYoutubeVideoId(exercise.videoUrl ?: "")
            if (videoId != null) {
                YoutubePlayerLibrary(videoId = videoId, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Video Available", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer/Reps Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (exercise.durationSeconds != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIME", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format(java.util.Locale.US, "%02d:%02d", timeLeft / 60, timeLeft % 60),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (!exercise.reps.isNullOrBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("REPS", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = exercise.reps,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        exercise.instruction?.let {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.skipExercise(exerciseIndex) },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Skip")
            }
            Button(
                onClick = { viewModel.nextExercise(exerciseIndex) },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
fun ExerciseDoneContent(
    viewModel: ClimbViewModel,
    exerciseIndex: Int,
    onExit: () -> Unit
) {
    val workout by viewModel.currentWorkout.collectAsState()
    val nextExercise = workout?.exercises?.getOrNull(exerciseIndex + 1)
    var timeLeft by remember { mutableStateOf(15) }
    
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        viewModel.startNextExercise(exerciseIndex + 1)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Well done!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Take a short breath.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (nextExercise != null) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("NEXT UP:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
            Text(
                nextExercise.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Starting in",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            "${timeLeft}s",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { viewModel.startNextExercise(exerciseIndex + 1) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Start Now")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onExit) {
            Text("Exit Workout", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun VideoThumbnail(videoId: String, onPlayClick: () -> Unit, modifier: Modifier = Modifier) {
    val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onPlayClick() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = "Video Thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Play Button Overlay
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}


fun extractYoutubeVideoId(url: String): String? {
    val regex = Regex("(?:v=|(?:embed|shorts|youtu.be)/)([a-zA-Z0-9_-]{11})")
    return regex.find(url)?.groupValues?.get(1)
}
