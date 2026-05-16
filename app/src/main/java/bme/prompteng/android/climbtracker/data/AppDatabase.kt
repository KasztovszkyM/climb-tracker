package bme.prompteng.android.climbtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.TypeConverters

@Database(entities = [ClimbEntity::class, ChatConversationEntity::class, ChatMessageEntity::class, WorkoutPlanEntity::class], version = 6, exportSchema = false)
@TypeConverters(WorkoutConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun climbDao(): ClimbDao
    abstract fun chatDao(): ChatDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "boulder_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}