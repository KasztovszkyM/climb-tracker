package bme.prompteng.android.climbtracker.model

data class Exercise(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val durationSeconds: Int? = null,
    val reps: String? = null,
    val instruction: String? = null,
    val videoUrl: String? = null,
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
        Exercise(name = "Jumping Jacks", durationSeconds = 60, instruction = "Perform classic jumping jacks at a steady pace to get your heart rate up."),
        Exercise(name = "Arm Circles", durationSeconds = 30, instruction = "Rotate your arms in large circles, 15s forward and 15s backward."),
        Exercise(name = "Finger Curls", reps = "20", instruction = "Using a light weight or edge, curl your fingers to warm up the tendons."),
        Exercise(name = "Scapular Pull-ups", reps = "10", instruction = "Hang from a bar and pull your shoulder blades down and back without bending your elbows."),
        Exercise(name = "Easy Traverse", durationSeconds = 300, instruction = "Move horizontally across the climbing wall using any large holds."),
        Exercise(name = "Wrist Rotations", durationSeconds = 30, instruction = "Rotate your wrists in both directions to warm up the joints."),
        Exercise(name = "Dynamic Leg Swings", reps = "10 each side", instruction = "Swing your legs forward and backward to loosen up your hips.")
    ),
    WorkoutCategory.STRETCH to listOf(
        Exercise(name = "Forearm Stretch", durationSeconds = 30, instruction = "Extend one arm forward, palm up, and gently pull your fingers back with the other hand."),
        Exercise(name = "Shoulder Opener", durationSeconds = 30, instruction = "Interlace your fingers behind your back and gently lift your arms."),
        Exercise(name = "Child's Pose", durationSeconds = 60, instruction = "Kneel on the floor, sit on your heels, and reach your arms forward on the ground."),
        Exercise(name = "Hamstring Stretch", durationSeconds = 30, instruction = "Sit on the floor with one leg extended and reach for your toes."),
        Exercise(name = "Cobra Stretch", durationSeconds = 30, instruction = "Lie on your stomach and push your upper body up with your hands, keeping your hips down."),
        Exercise(name = "Lat Stretch", durationSeconds = 30, instruction = "Grab a vertical bar or doorframe and lean your weight back to stretch your side."),
        Exercise(name = "Doorway Chest Stretch", durationSeconds = 45, instruction = "Stand in a doorway with your arms on the frame and lean forward.")
    ),
    WorkoutCategory.TRAIN to listOf(
        Exercise(name = "4x4 Power Endurance", reps = "4 sets", instruction = "Pick 4 bouldering problems below your limit and climb them back-to-back without rest."),
        Exercise(name = "Hangboard Protocol", durationSeconds = 10, reps = "6 reps", instruction = "Hang for 10 seconds on a medium edge, followed by 50 seconds of rest."),
        Exercise(name = "Technique Drills", durationSeconds = 600, instruction = "Focus on silent feet or straight arms while climbing easy routes."),
        Exercise(name = "Campus Board", reps = "3 sets", instruction = "Use the campus board for explosive pulling movements without using your feet."),
        Exercise(name = "Core Blast", durationSeconds = 300, instruction = "Perform a circuit of planks, leg raises, and Russian twists."),
        Exercise(name = "Pull-ups", reps = "3 sets of 8", instruction = "Standard pull-ups with a wide grip, focusing on controlled movement."),
        Exercise(name = "Max Bouldering", durationSeconds = 3600, instruction = "Project routes that are at or slightly above your maximum capability.")
    )
)
