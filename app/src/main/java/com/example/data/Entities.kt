package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayId: String, // "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    val name: String,
    val equipment: String,
    val targetSets: Int,
    val targetReps: String,
    val restTimeSeconds: Int,
    val techniqueStyle: String,
    val notes: String,
    val executionCue: String,
    val targetWeight: Float = 135f, // Added target weight
    val previousWeight: Float = 135f // Added previous weight
)

@Entity(tableName = "exercise_sets")
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: Int,
    val date: Long, // timestamp for the day or exact time
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val rpe: Int? = null // Added RPE (1-10 difficulty)
)

@Entity(tableName = "progress_logs")
data class ProgressLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val bodyweight: Float?,
    val waist: Float?
)

@Entity(tableName = "personal_records")
data class PersonalRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseName: String,
    val maxWeight: Float,
    val date: Long
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)

@Entity(tableName = "daily_check_ins")
data class DailyCheckIn(
    @PrimaryKey val dateStr: String, // "YYYY-MM-DD"
    val bodyweight: Float,
    val sleepDuration: Float,
    val energyLevel: Int, // 1-10
    val sorenessLevel: Int, // 1-10
    val coachingMessage: String,
    val intensityAdjustment: String // e.g. "Normal", "-10%", "+10%"
)

@Entity(tableName = "logged_foods")
data class LoggedFood(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val dateStr: String // "YYYY-MM-DD"
)

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey val dateStr: String, // "YYYY-MM-DD"
    val amountMl: Int
)

@Entity(tableName = "nutrition_targets")
data class NutritionTarget(
    @PrimaryKey val id: Int = 1,
    val calories: Int = 2800,
    val protein: Int = 180,
    val carbs: Int = 320,
    val fat: Int = 80,
    val pin: String = "1234" // For protected settings
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey val dateStr: String, // "YYYY-MM-DD"
    val workoutType: String,
    val performanceComment: String = "",
    val strengthProgressionComment: String = "",
    val recoveryComment: String = "",
    val weightGuidanceComment: String = "",
    val completed: Boolean = false
)
