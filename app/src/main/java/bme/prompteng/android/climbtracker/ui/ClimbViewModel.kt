package bme.prompteng.android.climbtracker.ui

import android.app.Application
import android.content.Context
import android.net.Uri
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
import bme.prompteng.android.climbtracker.network.YouTubeApiService
import bme.prompteng.android.climbtracker.network.YouTubeSearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Calendar
import kotlin.math.abs
import androidx.core.content.edit
import androidx.core.net.toUri

sealed class TrainingState {
    object CategorySelection : TrainingState()
    object TrainingFocusSelection : TrainingState()
    data class WorkoutExecution(val category: WorkoutCategory, val focus: TrainingFocus? = null) : TrainingState()
    data class ActiveExercise(val exerciseIndex: Int) : TrainingState()
    data class ExerciseDone(val exerciseIndex: Int) : TrainingState()
}

class ClimbViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val climbDao = db.climbDao()
    private val workoutDao = db.workoutDao()
    private val geminiApi = GeminiApi.create()
    private val youtubeApi = YouTubeApiService.create()
    private val context = application.applicationContext

    private val gson = com.google.gson.Gson()

    private val apiKey = BuildConfig.GEMINI_API_KEY_TRAINING

    private val prefs = application.getSharedPreferences("climb_prefs", Context.MODE_PRIVATE)

    private val _filterDate = MutableStateFlow<Long?>(null)
    val filterDate: StateFlow<Long?> = _filterDate.asStateFlow()

    private val _currentQuote = MutableStateFlow("")
    val currentQuote: StateFlow<String> = _currentQuote.asStateFlow()

    private var allQuotes = listOf<String>()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val current = _isDarkMode.value ?: false
        val newMode = !current
        _isDarkMode.value = newMode
        prefs.edit { putBoolean("dark_mode", newMode) }
    }

    // Profile fields
    private val _profileName = MutableStateFlow(prefs.getString("profile_name", "Profile") ?: "Profile")
    val profileName = _profileName.asStateFlow()

    private val _profileHeight = MutableStateFlow(prefs.getInt("profile_height", 160))
    val profileHeight = _profileHeight.asStateFlow()

    private val _profileWeight = MutableStateFlow(prefs.getInt("profile_weight", 55))
    val profileWeight = _profileWeight.asStateFlow()

    private val _profileGrade = MutableStateFlow(
        ClimbGrade.entries.find { it.name == prefs.getString("profile_grade", ClimbGrade.WHITE.name) } ?: ClimbGrade.WHITE
    )
    val profileGrade = _profileGrade.asStateFlow()

    private val _profileImageUri = MutableStateFlow<Uri?>(
        prefs.getString("profile_image_uri", null)?.toUri()
    )
    val profileImageUri = _profileImageUri.asStateFlow()

    fun updateProfileName(name: String) {
        _profileName.value = name
        prefs.edit { putString("profile_name", name) }
    }

    fun updateProfileHeight(height: Int) {
        _profileHeight.value = height
        prefs.edit { putInt("profile_height", height) }
    }

    fun updateProfileWeight(weight: Int) {
        _profileWeight.value = weight
        prefs.edit { putInt("profile_weight", weight) }
    }

    fun updateProfileGrade(grade: ClimbGrade) {
        _profileGrade.value = grade
        prefs.edit { putString("profile_grade", grade.name) }
    }

    fun updateProfileImageUri(uri: Uri?) {
        if (uri == null) {
            _profileImageUri.value = null
            prefs.edit { remove("profile_image_uri") }
            return
        }

        // Copy to internal storage for persistence
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = File(context.filesDir, "profile_image.jpg")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val internalUri = Uri.fromFile(file)
                _profileImageUri.value = internalUri
                prefs.edit { putString("profile_image_uri", internalUri.toString()) }
            } catch (e: Exception) {
                _profileImageUri.value = uri
                prefs.edit { putString("profile_image_uri", uri.toString()) }
            }
        }
    }

    init {
        loadQuotes()
        refreshQuote()
        _isDarkMode.value = if (prefs.contains("dark_mode")) prefs.getBoolean("dark_mode", false) else null

        viewModelScope.launch {
            workoutDao.getCurrentWorkout().collect { entity ->
                if (entity != null && _currentWorkout.value == null) {
                    val plan = WorkoutPlan(
                        id = entity.id,
                        title = entity.title,
                        category = entity.category,
                        focus = entity.focus,
                        exercises = entity.exercises
                    )
                    _currentWorkout.value = plan
                    _trainingState.value = TrainingState.WorkoutExecution(plan.category, plan.focus)
                }
            }
        }
    }

    private suspend fun persistWorkout(plan: WorkoutPlan?) {
        if (plan == null) {
            workoutDao.clearWorkout()
        } else {
            workoutDao.insertWorkout(
                bme.prompteng.android.climbtracker.data.WorkoutPlanEntity(
                    id = plan.id,
                    title = plan.title,
                    category = plan.category,
                    focus = plan.focus,
                    exercises = plan.exercises
                )
            )
        }
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

    val allClimbs: StateFlow<List<ClimbEntity>> = climbDao.getAllClimbs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAverageGrade: StateFlow<Float> = allClimbs
        .map { list ->
            if (list.isEmpty()) 0f
            else list.map { it.gradeValue }.average().toFloat()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val climbs: StateFlow<List<ClimbEntity>> = allClimbs
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
        val plan = WorkoutPlan(
            title = focus?.label ?: category.name.lowercase().replaceFirstChar { it.uppercase() },
            category = category,
            exercises = emptyList()
        )
        _currentWorkout.value = plan
        _trainingState.value = TrainingState.WorkoutExecution(category, focus)
        viewModelScope.launch { persistWorkout(plan) }
    }

    fun addExercise(exercise: Exercise) {
        viewModelScope.launch {
            val current = _currentWorkout.value ?: return@launch
            
            // Fetch video URL if it's missing
            val exerciseWithUrl = if (exercise.videoUrl.isNullOrBlank()) {
                exercise.copy(videoUrl = fetchVideoUrl(exercise.name))
            } else {
                exercise
            }
            
            val updated = current.exercises + exerciseWithUrl.copy(id = java.util.UUID.randomUUID().toString())
            val newPlan = current.copy(exercises = updated)
            _currentWorkout.value = newPlan
            persistWorkout(newPlan)
        }
    }

    private suspend fun fetchVideoUrl(exerciseName: String): String? {
        return try {
            // Append strict intent keywords to ensure a clean, single-exercise focus
            val searchQuery = "$exerciseName climbing exercise technique tutorial"
            val response = youtubeApi.searchVideos(query = searchQuery)
            if (response.isSuccessful) {
                response.body()?.items?.firstOrNull()?.id?.videoId?.let {
                    "https://www.youtube.com/watch?v=$it"
                }
            } else {
                android.util.Log.e("ClimbViewModel", "YouTube API Error: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ClimbViewModel", "YouTube Search failed for $exerciseName", e)
            null
        }
    }

    fun undoLastExercise() {
        val current = _currentWorkout.value ?: return
        if (current.exercises.isNotEmpty()) {
            val updated = current.exercises.dropLast(1)
            val newPlan = current.copy(exercises = updated)
            _currentWorkout.value = newPlan
            viewModelScope.launch { persistWorkout(newPlan) }
        }
    }

    fun generateWorkout(category: WorkoutCategory, focus: TrainingFocus? = null) {
        lastRequestedState = category to focus
        viewModelScope.launch {
            _isLoadingPlan.value = true
            try {
                val height = profileHeight.value
                val weight = profileWeight.value
                val profileGradeVal = profileGrade.value.value.toFloat()
                val avg = averageGrade.value
                
                // Use profile grade if no climbs yet, otherwise average them for a realistic current level
                val effectiveGrade = if (avg == 0f) profileGradeVal else (avg + profileGradeVal) / 2f
                val gradeName = ClimbGrade.entries.minByOrNull { abs(it.value.toFloat() - effectiveGrade) }?.label ?: "Unknown"

                val basePrompt = """
                    You are an elite climbing coach. Generate a high-quality 5-step exercise sequence for:
                    - Athlete Level: $gradeName (Skill Score: ${"%.1f".format(effectiveGrade)}/5.0)
                    - Physical Profile: $height cm, $weight kg
                    - Session Focus: ${if (category == WorkoutCategory.TRAIN) focus?.label ?: "general climbing" else category.name.lowercase()}
                    
                    Guidelines:
                    1. Progression: The 5 exercises must follow a logical flow (e.g., specific warm-up -> main strength effort -> accessory/cool-down).
                    2. Difficulty Scaling: 
                       - For a $gradeName climber (${"%.1f".format(effectiveGrade)}/5.0), ensure intensity is challenging but safe.
                       - Height ($height cm): If >180cm, focus on core tension, high-foot stability, and managing leverage. If <165cm, focus on explosive movement and high-reach techniques.
                       - Weight ($weight kg): If >85kg, emphasize controlled movements to protect finger tendons and shoulders. If <65kg, focus on pure strength-to-weight ratio exercises.
                    3. Coaching Cues: Instructions must be professional and actionable (e.g., 'Maintain active shoulders', 'Engage glutes', 'Precise foot placement').
                    4. Realistic Metrics: 
                       - Use 'DurationSeconds' as a raw number of SECONDS for timed holds or cardio (e.g., 60). Do not use minutes.
                       - Use 'Reps' for strength movements (e.g., '10 reps', '3 sets of 5').
                       - Scale metrics specifically for $gradeName level: if they are a beginner (White/Blue), keep reps manageable. If advanced (Red/Black), use high-intensity low-rep or long-duration metrics.
                    
                    Format: Name|DurationSeconds|Reps|Instruction
                    (Use '-' for N/A. No headers, no markdown formatting, no intro/outro text. Just 5 lines.)
                """.trimIndent()

                val promptText = when (category) {
                    WorkoutCategory.WARMUP -> "Generate a climbing warmup plan. $basePrompt"
                    WorkoutCategory.STRETCH -> "Generate a post-climbing stretching plan. $basePrompt"
                    WorkoutCategory.TRAIN -> "Generate a targeted training plan. $basePrompt"
                }

                val request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText))))
                )

                val response = geminiApi.generateContent(apiKey, request)
                val content = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                android.util.Log.d("ClimbViewModel", "Gemini Response: $content")

                val baseExercises = content.lines()
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
                                    reps = parts.getOrNull(2)?.takeIf { it != "-" && it.isNotBlank() },
                                    instruction = parts.getOrNull(3)?.takeIf { it != "-" && it.isNotBlank() },
                                    videoUrl = null // Will fetch via YouTube API
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
                                    reps = null,
                                    instruction = null,
                                    videoUrl = null
                                )
                            }
                        }
                    }

                if (baseExercises.isEmpty()) {
                    throw Exception("Parsed exercise list is empty")
                }

                // Fetch YouTube videos for each exercise in parallel
                val exercises = baseExercises.map { exercise ->
                    viewModelScope.async(Dispatchers.IO) {
                        exercise.copy(videoUrl = fetchVideoUrl(exercise.name))
                    }
                }.awaitAll()

                val plan = WorkoutPlan(
                    title = focus?.label
                        ?: category.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                    category = category,
                    focus = focus,
                    exercises = exercises
                )
                _currentWorkout.value = plan
                _trainingState.value = TrainingState.WorkoutExecution(category, focus)
                persistWorkout(plan)
            } catch (e: Exception) {
                android.util.Log.e("ClimbViewModel", "Error generating workout, using fallback", e)
                
                // Use ExerciseLibrary as fallback when API fails
                val baseFallback = ExerciseLibrary[category]?.take(5) ?: emptyList()
                
                // Fetch videos even for fallback exercises
                val fallbackExercises = baseFallback.map { exercise ->
                    viewModelScope.async(Dispatchers.IO) {
                        exercise.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            videoUrl = fetchVideoUrl(exercise.name)
                        )
                    }
                }.awaitAll()

                val plan = WorkoutPlan(
                    title = "${category.name.lowercase().replaceFirstChar { it.uppercase() }} (Offline Mode)",
                    category = category,
                    focus = focus,
                    exercises = fallbackExercises
                )
                _currentWorkout.value = plan
                _trainingState.value = TrainingState.WorkoutExecution(category, focus)
                persistWorkout(plan)
            } finally {
                _isLoadingPlan.value = false
            }
        }
    }

    fun resetWorkout() {
        _currentWorkout.value = null
        lastRequestedState = null
        _trainingState.value = TrainingState.CategorySelection
        viewModelScope.launch { persistWorkout(null) }
    }

    fun startGuidedWorkout() {
        val workout = _currentWorkout.value ?: return
        if (workout.exercises.isNotEmpty()) {
            _trainingState.value = TrainingState.ActiveExercise(0)
        }
    }

    fun nextExercise(currentIndex: Int) {
        val workout = _currentWorkout.value ?: return
        // Mark current as completed
        toggleExerciseCompletion(workout.exercises[currentIndex].id, true)
        
        if (currentIndex < workout.exercises.size - 1) {
            _trainingState.value = TrainingState.ExerciseDone(currentIndex)
        } else {
            resetWorkout()
        }
    }

    fun skipExercise(currentIndex: Int) {
        val workout = _currentWorkout.value ?: return
        if (currentIndex < workout.exercises.size - 1) {
            _trainingState.value = TrainingState.ActiveExercise(currentIndex + 1)
        } else {
            resetWorkout()
        }
    }

    fun startNextExercise(nextIndex: Int) {
        _trainingState.value = TrainingState.ActiveExercise(nextIndex)
    }

    fun toggleExerciseCompletion(exerciseId: String, completed: Boolean? = null) {
        val current = _currentWorkout.value ?: return
        val updatedExercises = current.exercises.map {
            if (it.id == exerciseId) it.copy(isCompleted = completed ?: !it.isCompleted) else it
        }
        val newPlan = current.copy(exercises = updatedExercises)
        _currentWorkout.value = newPlan
        viewModelScope.launch { persistWorkout(newPlan) }
    }

    fun removeExercise(exerciseId: String) {
        val current = _currentWorkout.value ?: return
        val updatedExercises = current.exercises.filter { it.id != exerciseId }
        val newPlan = current.copy(exercises = updatedExercises)
        _currentWorkout.value = newPlan
        viewModelScope.launch { persistWorkout(newPlan) }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val current = _currentWorkout.value ?: return
        val exercises = current.exercises.toMutableList()
        if (fromIndex in exercises.indices && toIndex in exercises.indices) {
            val item = exercises.removeAt(fromIndex)
            exercises.add(toIndex, item)
            val newPlan = current.copy(exercises = exercises)
            _currentWorkout.value = newPlan
            // Performance: We don't persist on every swap during drag
        }
    }

    fun persistCurrentWorkout() {
        viewModelScope.launch {
            persistWorkout(_currentWorkout.value)
        }
    }

    fun generateTrainingPlan() {
        viewModelScope.launch {
            _isLoadingPlan.value = true
            try {
                val profileGradeVal = profileGrade.value.value.toFloat()
                val avg = averageGrade.value
                val effectiveGrade = if (avg == 0f) profileGradeVal else (avg + profileGradeVal) / 2f
                val gradeName = ClimbGrade.entries.minByOrNull { abs(it.value.toFloat() - effectiveGrade) }?.label ?: "Unknown"
                
                val height = profileHeight.value
                val weight = profileWeight.value

                val promptText = """
                    I am a boulderer. 
                    - My current skill level is '$gradeName' (Numeric value: ${"%.1f".format(effectiveGrade)} out of 5.0). 
                    - Physical Profile: $height cm, $weight kg.
                    
                    Based on this, provide a simple, maximum 10-step personalized training plan to help me progress. 
                    Consider my physical profile:
                    - If I am tall (>180cm), include advice on high-foot technique and core tension.
                    - If I am heavier (>85kg), emphasize joint health and controlled power.

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
