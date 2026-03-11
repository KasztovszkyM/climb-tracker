package bme.prompteng.android.climbtracker.model

import androidx.compose.ui.graphics.Color

enum class ClimbGrade(val value: Int, val label: String, val color: Color, val textColor: Color = Color.Black) {
    WHITE(0, "Nagyon Könnyű", Color(0xFFF5F5F5)),
    BLUE(1, "Könnyű", Color(0xFF5BA4D9)),
    YELLOW(2, "Közepes", Color(0xFFF3E37C)),
    GREEN(3, "Nehéz", Color(0xFF75C485)),
    RED(4, "Nagyon Nehéz", Color(0xFFD95A5A)),
    BLACK(5, "Extrém Nehéz", Color(0xFF222222), Color.White)
}