package bme.prompteng.android.climbtracker.model

import androidx.compose.ui.graphics.Color

enum class ClimbGrade(val value: Int, val label: String, val color: Color, val textColor: Color = Color.Black) {
    WHITE(0, "Very Easy", Color(0xD9D9DEDC)),
    BLUE(1, "Easy", Color(0xFF5BA4D9)),
    YELLOW(2, "Medium", Color(0xFFF3E37C)),
    GREEN(3, "Hard", Color(0xFF75C485)),
    RED(4, "Very Hard", Color(0xFFD95A5A)),
    BLACK(5, "Extremely Hard", Color(0xFF222222), Color.White)
}