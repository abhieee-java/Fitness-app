package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Exercise::class,
        ExerciseSet::class,
        ProgressLog::class,
        PersonalRecord::class,
        ChatMessage::class,
        DailyCheckIn::class,
        LoggedFood::class,
        WaterLog::class,
        NutritionTarget::class,
        WorkoutSession::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fitnessDao(): FitnessDao
}
