package bme.prompteng.android.climbtracker.model

// Response model (the server sends this back)
data class BetaGenerationResponse(
    val routeId: String,
    val holds: List<ClimbingHold>, // The model defined in the previous response
    val status: String,
    val imageAspectRatio: Float,
    val description: String? = null // New field for climbing technique description
)

