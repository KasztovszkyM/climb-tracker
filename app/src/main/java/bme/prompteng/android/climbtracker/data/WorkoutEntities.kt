package bme.prompteng.android.climbtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import bme.prompteng.android.climbtracker.model.Exercise
import bme.prompteng.android.climbtracker.model.WorkoutCategory
import bme.prompteng.android.climbtracker.model.TrainingFocus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val dbId: Int = 0,
    val id: String,
    val title: String,
    val category: WorkoutCategory,
    val focus: TrainingFocus?,
    val exercises: List<Exercise>
)

class WorkoutConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromExerciseList(value: List<Exercise>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toExerciseList(value: String): List<Exercise> {
        val listType = object : TypeToken<List<Exercise>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromCategory(value: WorkoutCategory): String {
        return value.name
    }

    @TypeConverter
    fun toCategory(value: String): WorkoutCategory {
        return WorkoutCategory.valueOf(value)
    }

    @TypeConverter
    fun fromFocus(value: TrainingFocus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toFocus(value: String?): TrainingFocus? {
        return value?.let { TrainingFocus.valueOf(it) }
    }
}
