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

    // Daily Check-Ins
    @Query("SELECT * FROM daily_check_ins WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getDailyCheckIn(dateStr: String): DailyCheckIn?

    @Query("SELECT * FROM daily_check_ins WHERE dateStr = :dateStr")
    fun getDailyCheckInFlow(dateStr: String): Flow<DailyCheckIn?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyCheckIn(checkIn: DailyCheckIn)

    // Logged Foods
    @Query("SELECT * FROM logged_foods WHERE dateStr = :dateStr ORDER BY id ASC")
    fun getLoggedFoodsForDate(dateStr: String): Flow<List<LoggedFood>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedFood(food: LoggedFood)

    @Query("DELETE FROM logged_foods WHERE id = :id")
    suspend fun deleteLoggedFood(id: Int)

    // Water Logs
    @Query("SELECT * FROM water_logs WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getWaterLog(dateStr: String): WaterLog?

    @Query("SELECT * FROM water_logs WHERE dateStr = :dateStr")
    fun getWaterLogFlow(dateStr: String): Flow<WaterLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLog)

    // Nutrition Targets
    @Query("SELECT * FROM nutrition_targets WHERE id = 1 LIMIT 1")
    suspend fun getNutritionTarget(): NutritionTarget?

    @Query("SELECT * FROM nutrition_targets WHERE id = 1")
    fun getNutritionTargetFlow(): Flow<NutritionTarget?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionTarget(target: NutritionTarget)

    // Workout Sessions
    @Query("SELECT * FROM workout_sessions WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getWorkoutSession(dateStr: String): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE dateStr = :dateStr")
    fun getWorkoutSessionFlow(dateStr: String): Flow<WorkoutSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession)
}
