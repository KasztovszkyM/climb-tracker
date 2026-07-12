package bme.prompteng.android.climbtracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bme.prompteng.android.climbtracker.ui.components.ClimbetterHeader
import coil.compose.AsyncImage

import androidx.compose.material.icons.filled.CheckCircle
import bme.prompteng.android.climbtracker.model.ClimbGrade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ClimbViewModel, onHome: () -> Unit) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectivelyDark = isDarkMode ?: isSystemDark

    val name by viewModel.profileName.collectAsState()
    val height by viewModel.profileHeight.collectAsState()
    val weight by viewModel.profileWeight.collectAsState()
    val selectedGrade by viewModel.profileGrade.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    
    var isEditingName by rememberSaveable { mutableStateOf(false) }
    var tempName by rememberSaveable { mutableStateOf("") }

    val tips = listOf(
        "Pay attention to foot placement.",
        "Keep your arms straight when possible to save energy.",
        "Engage your core for better stability.",
        "Trust your feet on small holds.",
        "Warm up your fingers before trying hard projects.",
        "Take deep breaths to stay calm on the wall.",
        "Brush the holds to improve friction."
    )
    var currentTip by remember { mutableStateOf(tips.random()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> 
        if (uri != null) {
            viewModel.updateProfileImageUri(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Header (Matching TrackerScreen style)
        ClimbetterHeader(
            onHome = onHome,
            isDarkMode = isDarkMode,
            onToggleDarkMode = { viewModel.toggleDarkMode() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Picture
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(if (effectivelyDark) Color.DarkGray else Color.LightGray)
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = if (effectivelyDark) Color.LightGray else Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name (Editable)
        if (isEditingName) {
            val isNameValid = tempName.isNotBlank() && tempName.length <= 20
            TextField(
                value = tempName,
                onValueChange = { if (it.length <= 20) tempName = it },
                modifier = Modifier.padding(horizontal = 32.dp),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = Color(0xFF00BCD4),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color(0xFF00BCD4),
                    unfocusedIndicatorColor = Color.LightGray,
                    errorContainerColor = Color.Transparent
                ),
                singleLine = true,
                isError = !isNameValid,
                supportingText = {
                    if (!isNameValid) {
                        Text(
                            text = if (tempName.isBlank()) "Name cannot be empty" else "Name too long",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { 
                    if (isNameValid) {
                        viewModel.updateProfileName(tempName)
                        isEditingName = false
                    }
                }),
                trailingIcon = {
                    IconButton(
                        onClick = { 
                            if (isNameValid) {
                                viewModel.updateProfileName(tempName)
                                isEditingName = false 
                            }
                        },
                        enabled = isNameValid
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle, 
                            contentDescription = "Save", 
                            tint = if (isNameValid) Color(0xFF00BCD4) else Color.Gray
                        )
                    }
                }
            )
        } else {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color(0xFF00BCD4),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.clickable { 
                    tempName = name
                    isEditingName = true 
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Card
        val statsCardColor = if (effectivelyDark) Color(0xFF333333) else Color.Black
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = statsCardColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Height Input
                UnitNumberInput(
                    label = "Height",
                    value = height,
                    unit = "cm",
                    min = 50,
                    max = 250,
                    onValueSelected = { viewModel.updateProfileHeight(it) }
                )

                // Weight Input
                UnitNumberInput(
                    label = "Weight",
                    value = weight,
                    unit = "kg",
                    min = 20,
                    max = 250,
                    onValueSelected = { viewModel.updateProfileWeight(it) }
                )
                
                // Grade Dropdown
                GradeDropdown(
                    selectedGrade = selectedGrade,
                    onGradeSelected = { viewModel.updateProfileGrade(it) },
                    darkTheme = effectivelyDark
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Common Instructions (Now yellow)
        val tipsCardColor = Color(0xFFFFF59D)
        val tipsTextColor = Color.Black
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clickable { currentTip = tips.random() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = tipsCardColor)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Common instructions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = tipsTextColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentTip,
                    fontSize = 16.sp,
                    color = tipsTextColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun UnitNumberInput(
    label: String,
    value: Int,
    unit: String,
    min: Int = 0,
    max: Int = 999,
    onValueSelected: (Int) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    val isError = textValue.toIntOrNull()?.let { it !in min..max } ?: true

    Column {
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        
        TextField(
            value = textValue,
            onValueChange = { newValue ->
                val filtered = newValue.filter { it.isDigit() }
                if (filtered.length <= 3) {
                    textValue = filtered
                    filtered.toIntOrNull()?.let { 
                        if (it in min..max) {
                            onValueSelected(it)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(
                        text = if (textValue.isEmpty()) "Required" else "Range: $min - $max",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                errorContainerColor = Color.White
            ),
            suffix = {
                Text(text = unit, color = Color.Gray)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { /* Done action if needed */ }),
            singleLine = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeDropdown(selectedGrade: ClimbGrade, onGradeSelected: (ClimbGrade) -> Unit, darkTheme: Boolean) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "Target grade",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedGrade.label,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                ClimbGrade.entries.forEach { grade ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = grade.label,
                                color = Color.Black
                            ) 
                        },
                        onClick = {
                            onGradeSelected(grade)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
