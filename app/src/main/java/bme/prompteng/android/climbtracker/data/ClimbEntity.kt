package bme.prompteng.android.climbtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "climbs")
data class ClimbEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gradeValue: Int,
    val timestamp: Long = System.currentTimeMillis()
)