package bme.prompteng.android.climbtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_plans LIMIT 1")
    fun getCurrentWorkout(): Flow<WorkoutPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutPlanEntity)

    @Query("DELETE FROM workout_plans")
    suspend fun clearWorkout()
}
