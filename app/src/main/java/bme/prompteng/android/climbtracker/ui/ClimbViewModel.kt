package bme.prompteng.android.climbtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import bme.prompteng.android.climbtracker.data.AppDatabase
import bme.prompteng.android.climbtracker.data.ClimbEntity
import bme.prompteng.android.climbtracker.model.ClimbGrade
import bme.prompteng.android.climbtracker.network.ChatRequest
import bme.prompteng.android.climbtracker.network.Message
import bme.prompteng.android.climbtracker.network.OpenAIApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import kotlin.math.abs

class ClimbViewModel(application: Application) : AndroidViewModel(application) {
    private val climbDao = AppDatabase.getDatabase(application).climbDao()
    private val openAIApi = OpenAIApi.create()
    private val context = application.applicationContext

    // Replace with your actual secure key handling
    private val apiKey = "YOUR_OPENAI_API_KEY"

    private val _filterDate = MutableStateFlow<Long?>(null)
    val filterDate: StateFlow<Long?> = _filterDate.asStateFlow()

    private val _currentQuote = MutableStateFlow("")
    val currentQuote: StateFlow<String> = _currentQuote.asStateFlow()

    private var allQuotes = listOf<String>()

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

    // Directly expose the database flow as a StateFlow
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

    private val _isLoadingPlan = MutableStateFlow(false)
    val isLoadingPlan: StateFlow<Boolean> = _isLoadingPlan.asStateFlow()

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

    fun generateTrainingPlan() {
        viewModelScope.launch {
            _isLoadingPlan.value = true
            try {
                val avg = averageGrade.value
                val gradeName = ClimbGrade.entries.minByOrNull { abs(it.value - avg) }?.label ?: "Unknown"

                val promptText = """
                    I am a boulderer. My current average climbing level is around '$gradeName' (Numeric value: $avg out of 5). 
                    Based on this, provide a simple, maximum 5-step personalized training plan to help me progress.
                    Keep it concise, formatting it as a numbered list.
                """.trimIndent()

                val request = ChatRequest(
                    messages = listOf(Message(role = "user", content = promptText))
                )

                val response = openAIApi.getTrainingPlan("Bearer $apiKey", request)
                _trainingPlan.value = response.choices.firstOrNull()?.message?.content ?: "Could not generate plan."
            } catch (e: Exception) {
                _trainingPlan.value = "Error generating plan: ${e.localizedMessage}"
            } finally {
                _isLoadingPlan.value = false
            }
        }
    }
}
