package bme.prompteng.android.climbtracker.model

import com.google.gson.annotations.SerializedName

// Response from the Roboflow Object Detection API
data class RoboflowResponse(
    val predictions: List<Prediction>
)

// Represents a single detected climbing hold
data class Prediction(
    val x: Float, // Center X coordinate
    val y: Float, // Center Y coordinate
    val width: Float,
    val height: Float,
    @SerializedName("class")
    val className: String, // e.g., "hold", "volume"
    val confidence: Float // e.g., 0.85
)