package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FitnessRepository(private val dao: FitnessDao) {

    fun getExercisesForDay(dayId: String): Flow<List<Exercise>> = dao.getExercisesForDay(dayId)

    suspend fun insertExercise(exercise: Exercise) = dao.insertExercise(exercise)
    suspend fun updateExercise(exercise: Exercise) = dao.updateExercise(exercise)
    suspend fun deleteExercise(exercise: Exercise) = dao.deleteExercise(exercise)
    
    suspend fun initializeDefaultExercisesIfEmpty() {
        if (dao.getExerciseCount() == 0) {
            val defaults = listOf(
                // Push (Monday)
                Exercise(0, "Mon", "Bench Press", "Barbell", 4, "5-8", 120, "Controlled negative", "Keep shoulders retracted", "Push the floor away"),
                Exercise(0, "Mon", "Incline Dumbbell Press", "Dumbbells", 3, "8-10", 90, "Deep stretch", "Don't flare elbows", "Squeeze at the top"),
                Exercise(0, "Mon", "Overhead Press", "Barbell", 3, "6-8", 120, "Strict form", "Keep core tight", "Punch the ceiling"),
                Exercise(0, "Mon", "Tricep Pushdowns", "Cable", 3, "10-15", 60, "Constant tension", "Use a rope", "Spread at the bottom"),
                
                // Pull (Tuesday)
                Exercise(0, "Tue", "Deadlift", "Barbell", 3, "5", 180, "Heavy", "Keep back straight", "Hinge at hips"),
                Exercise(0, "Tue", "Pull-Ups", "Bodyweight", 3, "AMRAP", 90, "Full ROM", "Dead hang to chin over bar", "Pull elbows down"),
                Exercise(0, "Tue", "Barbell Row", "Barbell", 4, "8-10", 90, "Explosive positive", "Keep torso horizontal", "Pull to belly button"),
                Exercise(0, "Tue", "Bicep Curls", "Dumbbells", 3, "10-12", 60, "Squeeze", "No swinging", "Pinky up"),

                // Legs (Wednesday)
                Exercise(0, "Wed", "Squat", "Barbell", 4, "5-8", 180, "Deep depth", "Brace core", "Chest up"),
                Exercise(0, "Wed", "Leg Press", "Machine", 3, "10-12", 120, "Constant tension", "Don't lock out knees", "Drive through heels"),
                Exercise(0, "Wed", "Romanian Deadlift", "Barbell", 3, "8-10", 120, "Hamstring stretch", "Soft knees", "Push hips back"),
                Exercise(0, "Wed", "Calf Raises", "Machine", 4, "15-20", 60, "Slow eccentric", "Full stretch", "Explode up"),

                // Cardio + Abs (Thursday)
                Exercise(0, "Thu", "Running", "Treadmill", 1, "20 mins", 0, "Zone 2", "Steady state", "Breathe"),
                Exercise(0, "Thu", "Hanging Leg Raises", "Pull-up Bar", 3, "10-15", 60, "Strict", "No swinging", "Roll pelvis up"),
                Exercise(0, "Thu", "Plank", "Bodyweight", 3, "60s", 60, "Braced", "Keep back flat", "Squeeze glutes"),

                // Upper (Friday)
                Exercise(0, "Fri", "Weighted Dips", "Dip Station", 3, "8-10", 120, "Lean forward", "Deep stretch", "Explode up"),
                Exercise(0, "Fri", "Lat Pulldown", "Cable", 3, "10-12", 90, "Wide grip", "Control negative", "Pull to upper chest"),
                Exercise(0, "Fri", "Lateral Raises", "Dumbbells", 4, "15-20", 60, "Constant tension", "Slight lean forward", "Pour the water"),

                // Lower (Saturday)
                Exercise(0, "Sat", "Front Squat", "Barbell", 3, "6-8", 180, "Upright torso", "High elbows", "Brace hard"),
                Exercise(0, "Sat", "Bulgarian Split Squats", "Dumbbells", 3, "8-12", 90, "Deep depth", "Front foot elevated", "Drive through heel"),
                Exercise(0, "Sat", "Leg Curls", "Machine", 3, "12-15", 60, "Squeeze at top", "Control negative", "Hips down")
            )
            defaults.forEach { insertExercise(it) }
        }
    }

    fun getSetsForExerciseOnDate(exerciseId: Int, date: Long): Flow<List<ExerciseSet>> {
        val startOfDay = getStartOfDay(date)
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000
        return dao.getSetsForExerciseOnDate(exerciseId, startOfDay, endOfDay)
    }

    suspend fun insertSet(exerciseSet: ExerciseSet) {
        dao.insertSet(exerciseSet)
    }
    
    suspend fun deleteSet(setId: Int) {
        dao.deleteSet(setId)
    }

    fun getAllProgressLogs(): Flow<List<ProgressLog>> = dao.getAllProgressLogs()

    suspend fun insertProgressLog(log: ProgressLog) = dao.insertProgressLog(log)

    fun getAllPersonalRecords(): Flow<List<PersonalRecord>> = dao.getAllPersonalRecords()

    suspend fun checkAndSavePR(exerciseName: String, weight: Float, date: Long): Boolean {
        val currentPR = dao.getPersonalRecordForExercise(exerciseName)
        if (currentPR == null || weight > currentPR.maxWeight) {
            dao.insertPersonalRecord(PersonalRecord(exerciseName = exerciseName, maxWeight = weight, date = date))
            return true
        }
        return false
    }

    fun getAllChatMessages(): Flow<List<ChatMessage>> = dao.getAllChatMessages()

    suspend fun insertChatMessage(message: ChatMessage) = dao.insertChatMessage(message)

    // Daily Check-Ins
    suspend fun getDailyCheckIn(dateStr: String): DailyCheckIn? = dao.getDailyCheckIn(dateStr)
    fun getDailyCheckInFlow(dateStr: String): Flow<DailyCheckIn?> = dao.getDailyCheckInFlow(dateStr)
    suspend fun insertDailyCheckIn(checkIn: DailyCheckIn) = dao.insertDailyCheckIn(checkIn)

    // Logged Foods
    fun getLoggedFoodsForDate(dateStr: String): Flow<List<LoggedFood>> = dao.getLoggedFoodsForDate(dateStr)
    suspend fun insertLoggedFood(food: LoggedFood) = dao.insertLoggedFood(food)
    suspend fun deleteLoggedFood(id: Int) = dao.deleteLoggedFood(id)

    // Water Logs
    suspend fun getWaterLog(dateStr: String): WaterLog? = dao.getWaterLog(dateStr)
    fun getWaterLogFlow(dateStr: String): Flow<WaterLog?> = dao.getWaterLogFlow(dateStr)
    suspend fun insertWaterLog(waterLog: WaterLog) = dao.insertWaterLog(waterLog)

    // Nutrition Targets
    suspend fun getNutritionTarget(): NutritionTarget? {
        val target = dao.getNutritionTarget()
        if (target == null) {
            val defaultTarget = NutritionTarget()
            dao.insertNutritionTarget(defaultTarget)
            return defaultTarget
        }
        return target
    }
    fun getNutritionTargetFlow(): Flow<NutritionTarget?> = dao.getNutritionTargetFlow()
    suspend fun insertNutritionTarget(target: NutritionTarget) = dao.insertNutritionTarget(target)

    // Workout Sessions
    suspend fun getWorkoutSession(dateStr: String): WorkoutSession? = dao.getWorkoutSession(dateStr)
    fun getWorkoutSessionFlow(dateStr: String): Flow<WorkoutSession?> = dao.getWorkoutSessionFlow(dateStr)
    suspend fun insertWorkoutSession(session: WorkoutSession) = dao.insertWorkoutSession(session)

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
