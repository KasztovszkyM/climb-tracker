package bme.prompteng.android.climbtracker.model

data class Exercise(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val durationSeconds: Int? = null,
    val reps: String? = null,
    val isCompleted: Boolean = false
)

data class WorkoutPlan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val category: WorkoutCategory,
    val focus: TrainingFocus? = null,
    val exercises: List<Exercise> = emptyList()
)

enum class WorkoutCategory {
    WARMUP, TRAIN, STRETCH
}

enum class TrainingFocus(val label: String) {
    SLAB("Slab"), DYNO("Dyno"), OVERHANG("Overhang"), STATIC("Static"),
    ARMS("Arms"), LEGS("Legs"), ABS("Abs"), WHOLE_BODY("Whole body")
}

val ExerciseLibrary = mapOf(
    WorkoutCategory.WARMUP to listOf(
        Exercise(name = "Jumping Jacks", durationSeconds = 60),
        Exercise(name = "Arm Circles", durationSeconds = 30),
        Exercise(name = "Finger Curls", reps = "20"),
        Exercise(name = "Scapular Pull-ups", reps = "10"),
        Exercise(name = "Easy Traverse", durationSeconds = 300),
        Exercise(name = "Wrist Rotations", durationSeconds = 30),
        Exercise(name = "Dynamic Leg Swings", reps = "10 each side")
    ),
    WorkoutCategory.STRETCH to listOf(
        Exercise(name = "Forearm Stretch", durationSeconds = 30),
        Exercise(name = "Shoulder Opener", durationSeconds = 30),
        Exercise(name = "Child's Pose", durationSeconds = 60),
        Exercise(name = "Hamstring Stretch", durationSeconds = 30),
        Exercise(name = "Cobra Stretch", durationSeconds = 30),
        Exercise(name = "Lat Stretch", durationSeconds = 30),
        Exercise(name = "Doorway Chest Stretch", durationSeconds = 45)
    ),
    WorkoutCategory.TRAIN to listOf(
        Exercise(name = "4x4 Power Endurance", reps = "4 sets"),
        Exercise(name = "Hangboard Protocol", durationSeconds = 10, reps = "6 reps"),
        Exercise(name = "Technique Drills", durationSeconds = 600),
        Exercise(name = "Campus Board", reps = "3 sets"),
        Exercise(name = "Core Blast", durationSeconds = 300),
        Exercise(name = "Pull-ups", reps = "3 sets of 8"),
        Exercise(name = "Max Bouldering", durationSeconds = 3600)
    )
)
