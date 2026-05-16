package bme.prompteng.android.climbtracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import bme.prompteng.android.climbtracker.ui.components.ClimbetterHeader
import bme.prompteng.android.climbtracker.model.ClimbingHold
import bme.prompteng.android.climbtracker.ui.beta.BetaGeneratorViewModel
import bme.prompteng.android.climbtracker.ui.beta.BetaUiState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import android.net.Uri
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.UUID

// --- 1. Chat Message Data Model ---
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String? = null,
    val imageUri: Uri? = null,
    val isRouteBeta: Boolean = false,
    val holds: List<ClimbingHold> = emptyList(),
    val imageAspectRatio: Float? = null,
    val betaDescription: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    viewModel: BetaGeneratorViewModel = viewModel(factory = BetaGeneratorViewModel.Factory),
    climbViewModel: ClimbViewModel,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode by climbViewModel.isDarkMode.collectAsState()
    val chatMessages by viewModel.currentMessages.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentConversationId by viewModel.currentConversationId.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedImageUri = uri
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is BetaUiState.Success || uiState is BetaUiState.Error) {
            viewModel.resetState()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Conversations",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                NavigationDrawerItem(
                    label = { Text("New Chat") },
                    selected = currentConversationId == null,
                    onClick = {
                        viewModel.startNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(conversations) { conversation ->
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        conversation.title,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    IconButton(onClick = { viewModel.deleteConversation(conversation.id) }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            selected = conversation.id == currentConversationId,
                            onClick = {
                                viewModel.selectConversation(conversation.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                ClimbetterHeader(
                    onHome = onHome,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { climbViewModel.toggleDarkMode() },
                    onHistory = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                ChatInputBar(
                    inputText = inputText,
                    attachedImageUri = attachedImageUri,
                    onTextChange = { inputText = it },
                    onAddClick = {
                        pickMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    onRemoveAttachment = { attachedImageUri = null },
                    onSendClick = {
                        if (inputText.isNotBlank() || attachedImageUri != null) {
                            attachedImageUri?.let { uri ->
                                viewModel.generateBeta(
                                    context,
                                    uri,
                                    inputText
                                )
                            } ?: run {
                                // If text only, we might want to support it later, 
                                // but current generateBeta requires an image.
                                // For now, let's keep it consistent with current functionality.
                            }
                            inputText = ""
                            attachedImageUri = null
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chatMessages, key = { it.id }) { message ->
                        if (message.isRouteBeta) {
                            BotResultBubble(
                                imageUri = message.imageUri,
                                holds = message.holds,
                                imageAspectRatio = message.imageAspectRatio,
                                description = message.betaDescription
                            )
                        } else {
                            ChatBubble(message = message)
                        }
                    }

                    if (uiState is BetaUiState.Loading) {
                        item { AiTypingIndicator() }
                    }
                }
            }
        }
    }
}

// --- UI Components ---
@Composable
fun ChatInputBar(
    inputText: String,
    attachedImageUri: Uri?,
    onTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {

            // NEW: Attachment Preview Area
            if (attachedImageUri != null) {
                Box(modifier = Modifier.padding(start = 56.dp, bottom = 8.dp)) {
                    AsyncImage(
                        model = attachedImageUri,
                        contentDescription = "Attachment preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    // Remove attachment button
                    IconButton(
                        onClick = onRemoveAttachment,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Standard Input Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    placeholder = { Text("Ask about the route...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onSendClick) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .background(backgroundColor, shape)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            // Display Image if exists
            if (message.imageUri != null) {
                AsyncImage(
                    model = message.imageUri,
                    contentDescription = "Uploaded wall",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = if (message.text != null) 8.dp else 0.dp)
                )
            }

            // Display Text if exists
            if (message.text != null) {
                Text(text = message.text, color = textColor)
            }
        }
    }
}

@Composable
fun BotResultBubble(imageUri: Uri?, holds: List<ClimbingHold>, imageAspectRatio: Float?, description: String?) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .padding(12.dp)
                .widthIn(max = 320.dp) // Slightly wider for description
        ) {
            Text(
                text = "Here is the suggested beta!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
                fontWeight = FontWeight.Bold
            )

            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Dynamic Box that strictly follows the image's aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspectRatio ?: 1f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Processed wall",
                        contentScale = ContentScale.FillBounds, // Fill the aspect-ratio-correct box
                        modifier = Modifier.fillMaxSize()
                    )
                }

                RouteOverlayCanvas(holds = holds, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun RouteOverlayCanvas(holds: List<ClimbingHold>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (holds.isEmpty()) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height

        holds.forEachIndexed { index, hold ->
            val actualX = hold.x * canvasWidth
            val actualY = hold.y * canvasHeight

            // Draw arrow from previous hold to current hold
            if (index > 0) {
                val prevHold = holds[index - 1]
                val prevX = prevHold.x * canvasWidth
                val prevY = prevHold.y * canvasHeight

                // Draw the line (Arrow shaft)
                drawLine(
                    color = Color.Green,
                    start = androidx.compose.ui.geometry.Offset(prevX, prevY),
                    end = androidx.compose.ui.geometry.Offset(actualX, actualY),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round,
                    alpha = 0.8f
                )

                // Draw Arrowhead
                drawArrowHead(
                    start = androidx.compose.ui.geometry.Offset(prevX, prevY),
                    end = androidx.compose.ui.geometry.Offset(actualX, actualY),
                    color = Color.Green
                )
            }
        }

        // Draw holds (Circles)
        holds.forEach { hold ->
            val actualX = hold.x * canvasWidth
            val actualY = hold.y * canvasHeight

            val holdColor = when {
                hold.isStart -> Color.Blue
                hold.isTop -> Color.Red
                else -> Color.Yellow
            }
            drawCircle(
                color = holdColor,
                radius = 20f,
                center = androidx.compose.ui.geometry.Offset(actualX, actualY),
                style = Stroke(width = 6f) // Hollow circle for better visibility of the hold
            )
            // Smaller solid dot in center
            drawCircle(
                color = holdColor,
                radius = 6f,
                center = androidx.compose.ui.geometry.Offset(actualX, actualY)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(
    start: androidx.compose.ui.geometry.Offset,
    end: androidx.compose.ui.geometry.Offset,
    color: Color
) {
    val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowLength = 40f
    val arrowAngle = Math.PI / 6 // 30 degrees

    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(
            (end.x - arrowLength * Math.cos(angle - arrowAngle)).toFloat(),
            (end.y - arrowLength * Math.sin(angle - arrowAngle)).toFloat()
        )
        moveTo(end.x, end.y)
        lineTo(
            (end.x - arrowLength * Math.cos(angle + arrowAngle)).toFloat(),
            (end.y - arrowLength * Math.sin(angle + arrowAngle)).toFloat()
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 8f, cap = StrokeCap.Round)
    )
}

@Composable
fun AiTypingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Analyzing the wall...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}