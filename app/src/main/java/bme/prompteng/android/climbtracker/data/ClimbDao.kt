package bme.prompteng.android.climbtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClimbDao {
    @Insert
    suspend fun insertClimb(climb: ClimbEntity)

    @Query("SELECT * FROM climbs ORDER BY timestamp ASC")
    fun getAllClimbs(): Flow<List<ClimbEntity>>

    @Query("DELETE FROM climbs WHERE id = (SELECT MAX(id) FROM climbs)")
    suspend fun deleteLastClimb()
}