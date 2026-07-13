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
    val executionCue: String
)

@Entity(tableName = "exercise_sets")
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: Int,
    val date: Long, // timestamp for the day or exact time
    val setNumber: Int,
    val weight: Float,
    val reps: Int
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
