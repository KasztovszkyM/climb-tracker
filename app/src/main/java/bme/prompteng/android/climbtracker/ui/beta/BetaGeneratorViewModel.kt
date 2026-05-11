package bme.prompteng.android.climbtracker.ui.beta

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import bme.prompteng.android.climbtracker.data.repository.ClimbingBetaRepository
import bme.prompteng.android.climbtracker.network.ClimbingBetaApi
import bme.prompteng.android.climbtracker.ui.utils.copyUriToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class BetaUiState {
    object Idle : BetaUiState()
    object Loading : BetaUiState()

    // Success state holding the full response object
    data class Success(val response: bme.prompteng.android.climbtracker.model.BetaGenerationResponse) : BetaUiState()

    data class Error(val message: String) : BetaUiState()
}

class BetaGeneratorViewModel(
    private val repository: ClimbingBetaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BetaUiState>(BetaUiState.Idle)
    val uiState: StateFlow<BetaUiState> = _uiState.asStateFlow()

    fun generateBeta(context: Context, imageUri: Uri, userPrompt: String) {
        _uiState.value = BetaUiState.Loading

        viewModelScope.launch {
            try {
                // Run the file copying on the background (IO) thread
                val file = withContext(Dispatchers.IO) {
                    copyUriToFile(context, imageUri)
                }

                // Provide a default prompt if the user didn't provide one
                val finalPrompt = userPrompt.ifBlank {
                    "Please find a climbable route on this wall."
                }

                // Call the repository with the prepared file
                val result = repository.generateBetaFromImage(context, file, finalPrompt)

                result.fold(
                    onSuccess = { response ->
                        _uiState.value = BetaUiState.Success(response)
                    },
                    onFailure = { exception ->
                        _uiState.value = BetaUiState.Error(exception.message ?: "Unknown error")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BetaUiState.Error("An error occurred: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = BetaUiState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Create the API client
                val geminiApi = ClimbingBetaApi.create()

                // Pass the API to the Repository
                val repository = ClimbingBetaRepository(geminiApi = geminiApi)

                // Return the ViewModel
                BetaGeneratorViewModel(repository)
            }
        }
    }
}

// Factory to manually create the ViewModel with its required dependencies
class BetaGeneratorViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BetaGeneratorViewModel::class.java)) {
            val geminiApi = ClimbingBetaApi.create()
            val repository = ClimbingBetaRepository(geminiApi = geminiApi)
            return BetaGeneratorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
