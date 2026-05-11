package bme.prompteng.android.climbtracker.model

// These models represent the exact structure Google Gemini returns
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: ContentResponse? = null
)

data class ContentResponse(
    val parts: List<PartResponse>? = null
)

data class PartResponse(
    val text: String? = null // This text field will contain our target JSON string!
)