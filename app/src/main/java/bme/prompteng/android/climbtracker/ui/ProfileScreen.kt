package bme.prompteng.android.climbtracker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    var name by rememberSaveable { mutableStateOf("Jon Doe") }
    var height by rememberSaveable { mutableIntStateOf(160) }
    var weight by rememberSaveable { mutableIntStateOf(55) }
    var selectedGrade by rememberSaveable { mutableStateOf(ClimbGrade.WHITE) }
    var profileImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    
    var isEditingName by remember { mutableStateOf(false) }

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
    ) { uri -> profileImageUri = uri }

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
            TextField(
                value = name,
                onValueChange = { name = it },
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
                    unfocusedIndicatorColor = Color.LightGray
                ),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { isEditingName = false }) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Save", tint = Color(0xFF00BCD4))
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
                modifier = Modifier.clickable { isEditingName = true }
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
                    onValueSelected = { height = it }
                )

                // Weight Input
                UnitNumberInput(
                    label = "Weight",
                    value = weight,
                    unit = "kg",
                    onValueSelected = { weight = it }
                )
                
                // Grade Dropdown
                GradeDropdown(
                    selectedGrade = selectedGrade,
                    onGradeSelected = { selectedGrade = it },
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
    onValueSelected: (Int) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

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
                // Only allow digits
                val filtered = newValue.filter { it.isDigit() }
                textValue = filtered
                filtered.toIntOrNull()?.let { onValueSelected(it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.Black,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            suffix = {
                Text(text = unit, color = Color.Gray)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            text = "Grade",
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
                    .menuAnchor()
                    .clip(RoundedCornerShape(24.dp)),
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
