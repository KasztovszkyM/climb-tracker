package bme.prompteng.android.climbtracker.model

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

// The Part class MUST be a data class with nullable fields for Gson serialization
data class Part(
    // Optional text prompt
    val text: String? = null,

    // Optional image/media data
    @SerializedName("inline_data")
    val inlineData: InlineData? = null
)

data class InlineData(
    @SerializedName("mime_type") val mimeType: String,
    val data: String
)