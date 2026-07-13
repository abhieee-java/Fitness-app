package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {
    @Query("SELECT * FROM exercises WHERE dayId = :dayId")
    fun getExercisesForDay(dayId: String): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)
    
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun getExerciseCount(): Int

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId AND date >= :startOfDay AND date < :endOfDay ORDER BY setNumber ASC")
    fun getSetsForExerciseOnDate(exerciseId: Int, startOfDay: Long, endOfDay: Long): Flow<List<ExerciseSet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(exerciseSet: ExerciseSet)
    
    @Query("DELETE FROM exercise_sets WHERE id = :setId")
    suspend fun deleteSet(setId: Int)

    @Query("SELECT * FROM progress_logs ORDER BY date ASC")
    fun getAllProgressLogs(): Flow<List<ProgressLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressLog(log: ProgressLog)

    @Query("SELECT * FROM personal_records ORDER BY date DESC")
    fun getAllPersonalRecords(): Flow<List<PersonalRecord>>

    @Query("SELECT * FROM personal_records WHERE exerciseName = :exerciseName ORDER BY maxWeight DESC LIMIT 1")
    suspend fun getPersonalRecordForExercise(exerciseName: String): PersonalRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecord(record: PersonalRecord)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)
}
