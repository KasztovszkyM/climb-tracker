package bme.prompteng.android.climbtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bme.prompteng.android.climbtracker.BuildConfig
import bme.prompteng.android.climbtracker.data.AppDatabase
import bme.prompteng.android.climbtracker.data.ClimbEntity
import bme.prompteng.android.climbtracker.model.*
import bme.prompteng.android.climbtracker.network.Content
import bme.prompteng.android.climbtracker.network.GeminiApi
import bme.prompteng.android.climbtracker.network.GeminiRequest
import bme.prompteng.android.climbtracker.network.Part
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import kotlin.math.abs

sealed class TrainingState {
    object CategorySelection : TrainingState()
    object TrainingFocusSelection : TrainingState()
    data class WorkoutExecution(val category: WorkoutCategory, val focus: TrainingFocus? = null) : TrainingState()
}

class ClimbViewModel(application: Application) : AndroidViewModel(application) {
    private val climbDao = AppDatabase.getDatabase(application).climbDao()
    private val geminiApi = GeminiApi.create()
    private val context = application.applicationContext

    private val apiKey = BuildConfig.GEMINI_API_KEY_TRAINING

    private val _filterDate = MutableStateFlow<Long?>(null)
    val filterDate: StateFlow<Long?> = _filterDate.asStateFlow()

    private val _currentQuote = MutableStateFlow("")
    val currentQuote: StateFlow<String> = _currentQuote.asStateFlow()

    private var allQuotes = listOf<String>()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val current = _isDarkMode.value ?: false
        _isDarkMode.value = !current
    }

    init {
        loadQuotes()
        refreshQuote()
    }

    private fun loadQuotes() {
        try {
            context.assets.open("quotes.txt").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                allQuotes = reader.readLines().filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            allQuotes = listOf("Climb on!")
        }
    }

    fun refreshQuote() {
        if (allQuotes.isNotEmpty()) {
            _currentQuote.value = allQuotes.random()
        }
    }

    val climbs: StateFlow<List<ClimbEntity>> = climbDao.getAllClimbs()
        .combine(_filterDate) { list, date ->
            if (date == null) list
            else {
                val cal = Calendar.getInstance().apply { timeInMillis = date }
                val startOfDay = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val endOfDay = Calendar.getInstance().apply {
                    timeInMillis = cal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                list.filter { it.timestamp in startOfDay..endOfDay }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilterDate(timestamp: Long?) {
        _filterDate.value = timestamp
    }

    // Derive the average grade reactively from the climbs flow
    val averageGrade: StateFlow<Float> = climbs
        .map { list ->
            if (list.isEmpty()) 0f
            else list.map { it.gradeValue }.average().toFloat()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    private val _trainingPlan = MutableStateFlow<String?>(null)
    val trainingPlan: StateFlow<String?> = _trainingPlan.asStateFlow()

    private val _currentWorkout = MutableStateFlow<WorkoutPlan?>(null)
    val currentWorkout: StateFlow<WorkoutPlan?> = _currentWorkout.asStateFlow()

    private val _isLoadingPlan = MutableStateFlow(false)
    val isLoadingPlan: StateFlow<Boolean> = _isLoadingPlan.asStateFlow()

    private val _trainingState = MutableStateFlow<TrainingState>(TrainingState.CategorySelection)
    val trainingState: StateFlow<TrainingState> = _trainingState.asStateFlow()

    var lastRequestedState: Pair<WorkoutCategory, TrainingFocus?>? = null
        private set

    fun setTrainingState(state: TrainingState) {
        _trainingState.value = state
    }

    fun addClimb(grade: ClimbGrade, type: String) {
        viewModelScope.launch {
            climbDao.insertClimb(ClimbEntity(gradeValue = grade.value, climbType = type))
        }
    }

    fun undoLastClimb() {
        viewModelScope.launch {
            climbDao.deleteLastClimb()
        }
    }

    fun deleteClimb(climb: ClimbEntity) {
        viewModelScope.launch {
            climbDao.deleteClimb(climb)
        }
    }

    fun startManualWorkout(category: WorkoutCategory, focus: TrainingFocus? = null) {
        lastRequestedState = category to focus
        _currentWorkout.value = WorkoutPlan(
            title = focus?.label ?: category.name.lowercase().replaceFirstChar { it.uppercase() },
            category = category,
            exercises = emptyList()
        )
        _trainingState.value = TrainingState.WorkoutExecution(category, focus)
    }

    fun addExercise(exercise: Exercise) {
        val current = _currentWorkout.value ?: return
        val updated = current.exercises + exercise.copy(id = java.util.UUID.randomUUID().toString())
        _currentWorkout.value = current.copy(exercises = updated)
    }

    fun generateWorkout(category: WorkoutCategory, focus: TrainingFocus? = null) {
        lastRequestedState = category to focus
        viewModelScope.launch {
            _isLoadingPlan.value = true
            try {
                val avg = averageGrade.value
                val gradeName = ClimbGrade.entries.minByOrNull { abs(it.value - avg) }?.label ?: "Unknown"

                val promptText = when (category) {
                    WorkoutCategory.WARMUP -> "Generate a 5-step climbing warmup plan for a $gradeName level climber. Strictly format each line as: Name|DurationSeconds|Reps. Use '-' if duration or reps not applicable. Example: Jumping Jacks|60|-. No headers or tables."
                    WorkoutCategory.STRETCH -> "Generate a 5-step post-climbing stretching plan for a $gradeName level climber. Strictly format each line as: Name|DurationSeconds|Reps. Use '-' if duration or reps not applicable. No headers or tables."
                    WorkoutCategory.TRAIN -> "Generate a 5-step training plan focusing on ${focus?.label ?: "general climbing"} for a $gradeName level climber. Strictly format each line as: Name|DurationSeconds|Reps. Use '-' if duration or reps not applicable. No headers or tables."
                }

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText))))
                )

                val response = geminiApi.generateContent(apiKey, request)
                val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                android.util.Log.d("ClimbViewModel", "Gemini Response: $content")

                val exercises = content.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .let { lines ->
                        // Strategy 1: Look for pipe-delimited lines (the requested format)
                        val pipeLines = lines.filter { line ->
                            line.contains("|") &&
                            !line.contains("---") &&
                            !line.contains("Duration", ignoreCase = true) &&
                            !line.contains("Reps", ignoreCase = true) &&
                            !line.contains("Exercise", ignoreCase = true)
                        }

                        if (pipeLines.isNotEmpty()) {
                            pipeLines.mapNotNull { line ->
                                // Handle markdown tables by removing leading/trailing pipes
                                val sanitizedLine = line.removePrefix("|").removeSuffix("|").trim()
                                val parts = sanitizedLine.split("|").map { it.trim() }

                                if (parts.isEmpty()) return@mapNotNull null

                                val rawName = parts[0]
                                // Remove leading numbers, markdown bolding, and generic list bullets
                                val cleanedName = rawName.replace(Regex("^(\\d+\\.|[-*])\\s*"), "")
                                    .replace("**", "")
                                    .trim()

                                if (cleanedName.isEmpty() || cleanedName.all { !it.isLetterOrDigit() }) return@mapNotNull null

                                // More robust duration parsing (extracts first sequence of digits)
                                val duration = parts.getOrNull(1)?.let { s ->
                                    Regex("\\d+").find(s)?.value?.toIntOrNull()
                                }

                                Exercise(
                                    name = cleanedName,
                                    durationSeconds = duration,
                                    reps = parts.getOrNull(2)?.takeIf { it != "-" && it.isNotBlank() }
                                )
                            }
                        } else {
                            // Strategy 2: Fallback to list-based parsing if no pipes found
                            lines.filter { line ->
                                !line.contains("Duration", ignoreCase = true) &&
                                !line.contains("Reps", ignoreCase = true) &&
                                !line.contains("Exercise", ignoreCase = true) &&
                                !line.startsWith("#")
                            }.mapNotNull { line ->
                                val cleanedName = line.replace(Regex("^(\\d+\\.|[-*])\\s*"), "")
                                    .replace("**", "")
                                    .trim()

                                if (cleanedName.isEmpty() || cleanedName.all { !it.isLetterOrDigit() }) return@mapNotNull null

                                // Try to extract duration if it's like "Name: 60s" or "Name (60s)"
                                val durationMatch = Regex("[:(]\\s*(\\d+)").find(line)
                                val duration = durationMatch?.groupValues?.get(1)?.toIntOrNull()

                                // The name is everything before the first colon or parenthesis
                                val displayName = cleanedName.split(Regex("[:(]")).first().trim()

                                Exercise(
                                    name = displayName.ifEmpty { cleanedName },
                                    durationSeconds = duration,
                                    reps = null
                                )
                            }
                        }
                    }

                if (exercises.isEmpty()) {
                    throw Exception("Parsed exercise list is empty")
                }

                _currentWorkout.value = WorkoutPlan(
                    title = focus?.label
                        ?: category.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                    category = category,
                    exercises = exercises
                )
                _trainingState.value = TrainingState.WorkoutExecution(category, focus)
            } catch (e: Exception) {
                android.util.Log.e("ClimbViewModel", "Error generating workout, using fallback", e)
                
                // Use ExerciseLibrary as fallback when API fails
                val fallbackExercises = ExerciseLibrary[category]?.take(5) ?: emptyList()

                _currentWorkout.value = WorkoutPlan(
                    title = "${category.name.lowercase().replaceFirstChar { it.uppercase() }} (Offline Mode)",
                    category = category,
                    exercises = fallbackExercises.map { it.copy(id = java.util.UUID.randomUUID().toString()) }
                )
                _trainingState.value = TrainingState.WorkoutExecution(category, focus)
            } finally {
                _isLoadingPlan.value = false
            }
        }
    }

    fun resetWorkout() {
        _currentWorkout.value = null
        lastRequestedState = null
        _trainingState.value = TrainingState.CategorySelection
    }

    fun toggleExerciseCompletion(exerciseId: String) {
        val current = _currentWorkout.value ?: return
        val updatedExercises = current.exercises.map {
            if (it.id == exerciseId) it.copy(isCompleted = !it.isCompleted) else it
        }
        _currentWorkout.value = current.copy(exercises = updatedExercises)
    }

    fun removeExercise(exerciseId: String) {
        val current = _currentWorkout.value ?: return
        val updatedExercises = current.exercises.filter { it.id != exerciseId }
        _currentWorkout.value = current.copy(exercises = updatedExercises)
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val current = _currentWorkout.value ?: return
        val exercises = current.exercises.toMutableList()
        if (fromIndex in exercises.indices && toIndex in exercises.indices) {
            val item = exercises.removeAt(fromIndex)
            exercises.add(toIndex, item)
            _currentWorkout.value = current.copy(exercises = exercises)
        }
    }

    fun generateTrainingPlan() {
        viewModelScope.launch {
            _isLoadingPlan.value = true
            try {
                val avg = averageGrade.value
                val gradeName = ClimbGrade.entries.minByOrNull { abs(it.value - avg) }?.label ?: "Unknown"

                val promptText = """
                    I am a boulderer. My current average climbing level is around '$gradeName' (Numeric value: $avg out of 5). 
                    Based on this, provide a simple, maximum 10-step personalized training plan to help me progress.
                    Keep it concise, formatting it as a numbered list.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText))))
                )

                val response = geminiApi.generateContent(apiKey, request)
                _trainingPlan.value = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not generate plan."
            } catch (e: Exception) {
                _trainingPlan.value = "Error generating plan: ${e.localizedMessage}"
            } finally {
                _isLoadingPlan.value = false
            }
        }
    }
}
