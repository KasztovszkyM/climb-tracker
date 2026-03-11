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
import kotlin.math.abs

class ClimbViewModel(application: Application) : AndroidViewModel(application) {
    private val climbDao = AppDatabase.getDatabase(application).climbDao()
    private val openAIApi = OpenAIApi.create()

    // Replace with your actual secure key handling
    private val apiKey = "YOUR_OPENAI_API_KEY"

    // Directly expose the database flow as a StateFlow
    val climbs: StateFlow<List<ClimbEntity>> = climbDao.getAllClimbs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun addClimb(grade: ClimbGrade) {
        viewModelScope.launch {
            climbDao.insertClimb(ClimbEntity(gradeValue = grade.value))
        }
    }

    fun undoLastClimb() {
        viewModelScope.launch {
            climbDao.deleteLastClimb()
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
