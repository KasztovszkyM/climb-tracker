package bme.prompteng.android.climbtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClimbetterHeader(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    isDarkMode: Boolean? = null,
    onToggleDarkMode: (() -> Unit)? = null,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "CLIMBETTER",
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { onHome() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color(0xFF4DB6AC)
                )
            )

            if (onToggleDarkMode != null) {
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = if (isDarkMode == true) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark Mode"
                    )
                }
            }
        }

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
