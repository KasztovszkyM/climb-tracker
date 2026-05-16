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
import bme.prompteng.android.climbtracker.ui.utils.persistUriToInternalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import bme.prompteng.android.climbtracker.data.AppDatabase
import bme.prompteng.android.climbtracker.data.ChatConversationEntity
import bme.prompteng.android.climbtracker.data.ChatDao
import bme.prompteng.android.climbtracker.data.ChatMessageEntity
import bme.prompteng.android.climbtracker.model.ClimbingHold
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.core.net.toUri

sealed class BetaUiState {
    object Idle : BetaUiState()
    object Loading : BetaUiState()

    // Success state holding the full response object
    data class Success(val response: bme.prompteng.android.climbtracker.model.BetaGenerationResponse) : BetaUiState()

    data class Error(val message: String) : BetaUiState()
}

class BetaGeneratorViewModel(
    private val repository: ClimbingBetaRepository,
    private val chatDao: ChatDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<BetaUiState>(BetaUiState.Idle)
    val uiState: StateFlow<BetaUiState> = _uiState.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()

    val conversations = chatDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMessages = _currentConversationId.flatMapLatest { id ->
        if (id == null) {
            MutableStateFlow(emptyList<ChatMessageEntity>())
        } else {
            chatDao.getMessagesForConversation(id)
        }
    }.map { entities ->
        entities.map { entity ->
            bme.prompteng.android.climbtracker.ui.ChatMessage(
                id = entity.id,
                isUser = entity.isUser,
                text = entity.text,
                imageUri = entity.imagePath?.toUri(),
                isRouteBeta = entity.isRouteBeta,
                holds = entity.holdsJson?.let {
                    val type = object : TypeToken<List<ClimbingHold>>() {}.type
                    Gson().fromJson(it, type)
                } ?: emptyList(),
                imageAspectRatio = entity.imageAspectRatio,
                betaDescription = entity.betaDescription
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectConversation(id: String?) {
        _currentConversationId.value = id
    }

    fun startNewConversation() {
        _currentConversationId.value = null
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatDao.deleteConversation(id)
            if (_currentConversationId.value == id) {
                _currentConversationId.value = null
            }
        }
    }

    fun generateBeta(context: Context, imageUri: Uri, userPrompt: String) {
        _uiState.value = BetaUiState.Loading

        viewModelScope.launch {
            try {
                // Ensure we have a conversation
                var conversationId = _currentConversationId.value
                if (conversationId == null) {
                    val newConv = ChatConversationEntity(
                        title = userPrompt.takeIf { it.isNotBlank() } ?: "New Conversation"
                    )
                    chatDao.insertConversation(newConv)
                    conversationId = newConv.id
                    _currentConversationId.value = conversationId
                }

                // Save user message
                val internalFile = withContext(Dispatchers.IO) {
                    persistUriToInternalStorage(context, imageUri)
                }
                val internalUri = Uri.fromFile(internalFile)

                chatDao.insertMessage(
                    ChatMessageEntity(
                        conversationId = conversationId,
                        isUser = true,
                        text = userPrompt.takeIf { it.isNotBlank() },
                        imagePath = internalUri.toString(),
                        isRouteBeta = false,
                        holdsJson = null,
                        imageAspectRatio = null,
                        betaDescription = null
                    )
                )

                // Run the file copying on the background (IO) thread
                val file = withContext(Dispatchers.IO) {
                    copyUriToFile(context, internalUri)
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
                        
                        // Save bot message
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                conversationId = conversationId,
                                isUser = false,
                                text = null,
                                imagePath = internalUri.toString(),
                                isRouteBeta = true,
                                holdsJson = Gson().toJson(response.holds),
                                imageAspectRatio = response.imageAspectRatio,
                                betaDescription = response.description
                            )
                        )
                    },
                    onFailure = { exception ->
                        val errorMessage = exception.message ?: "Unknown error"
                        _uiState.value = BetaUiState.Error(errorMessage)
                        
                        // Save error message
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                conversationId = conversationId,
                                isUser = false,
                                text = "System message: $errorMessage",
                                imagePath = null,
                                isRouteBeta = false,
                                holdsJson = null,
                                imageAspectRatio = null,
                                betaDescription = null
                            )
                        )
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
                val context = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application).applicationContext
                val database = AppDatabase.getDatabase(context)
                
                // Create the API client
                val geminiApi = ClimbingBetaApi.create()

                // Pass the API to the Repository
                val repository = ClimbingBetaRepository(geminiApi = geminiApi)

                // Return the ViewModel
                BetaGeneratorViewModel(repository, database.chatDao())
            }
        }
    }
}

// Factory to manually create the ViewModel with its required dependencies
class BetaGeneratorViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BetaGeneratorViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val geminiApi = ClimbingBetaApi.create()
            val repository = ClimbingBetaRepository(geminiApi = geminiApi)
            return BetaGeneratorViewModel(repository, database.chatDao()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
