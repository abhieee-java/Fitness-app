package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.JudgeAiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FitnessViewModel(
    private val repository: FitnessRepository,
    private val judgeClient: JudgeAiClient = JudgeAiClient()
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.initializeDefaultExercisesIfEmpty()
            // Make sure target is initialized
            repository.getNutritionTarget()
        }
    }

    private val _currentDayFilter = MutableStateFlow(getTodayDayId())
    val currentDayFilter: StateFlow<String> = _currentDayFilter.asStateFlow()
    
    fun setDayFilter(dayId: String) {
        _currentDayFilter.value = dayId
    }

    fun getExercisesForDay(dayId: String): Flow<List<Exercise>> {
        return repository.getExercisesForDay(dayId)
    }

    fun getSetsForExerciseToday(exerciseId: Int): Flow<List<ExerciseSet>> {
        return repository.getSetsForExerciseOnDate(exerciseId, System.currentTimeMillis())
    }

    // Modernized addSet with immediate RPE analysis & auto-progression
    fun addSet(exercise: Exercise, weight: Float, reps: Int, rpe: Int, setNumber: Int) {
        viewModelScope.launch {
            val set = ExerciseSet(
                exerciseId = exercise.id,
                date = System.currentTimeMillis(),
                setNumber = setNumber,
                weight = weight,
                reps = reps,
                rpe = rpe
            )
            repository.insertSet(set)
            
            // Check PR
            val isPr = repository.checkAndSavePR(exercise.name, weight, System.currentTimeMillis())
            if (isPr) {
                _toastMessage.emit("New PR on ${exercise.name}: $weight lbs!")
            }

            // Update exercise record with last logged weight
            val updatedEx = exercise.copy(previousWeight = weight)
            repository.updateExercise(updatedEx)

            // Auto progressive overload analysis
            val targetRepsMax = getTargetRepsInt(exercise.targetReps)
            if (reps >= targetRepsMax) {
                val weightIncrease = when {
                    rpe <= 7 -> 10f // Extremely strong, jump by 10 lbs
                    rpe <= 8 -> 5f  // Excellent form and speed, bump by 5 lbs
                    else -> 0f      // Limit intensity, maintain
                }
                if (weightIncrease > 0f) {
                    val nextTarget = weight + weightIncrease
                    val progressionEx = updatedEx.copy(targetWeight = nextTarget)
                    repository.updateExercise(progressionEx)
                    _toastMessage.emit("The Judge: Rep target crushed at RPE $rpe. Target weight increased to $nextTarget lbs.")
                }
            } else if (reps < targetRepsMax - 2 && rpe >= 9) {
                // Struggle detected, reduce target slightly for recovery
                val nextTarget = (weight - 5f).coerceAtLeast(45f)
                val progressionEx = updatedEx.copy(targetWeight = nextTarget)
                repository.updateExercise(progressionEx)
                _toastMessage.emit("The Judge: Severe grind detected at RPE $rpe. Form preservation: Next target lowered to $nextTarget lbs.")
            }
        }
    }
    
    private fun getTargetRepsInt(targetReps: String): Int {
        val clean = targetReps.replace(Regex("[^0-9-]"), "")
        if (clean.contains("-")) {
            return clean.split("-").lastOrNull()?.toIntOrNull() ?: 8
        }
        return clean.toIntOrNull() ?: 8
    }

    fun deleteSet(setId: Int) {
        viewModelScope.launch {
            repository.deleteSet(setId)
        }
    }
    
    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
        }
    }

    // Flows for current progress logs and PRs
    val progressLogs: StateFlow<List<ProgressLog>> = repository.getAllProgressLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalRecords: StateFlow<List<PersonalRecord>> = repository.getAllPersonalRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProgressLog(bodyweight: Float, waist: Float) {
        viewModelScope.launch {
            repository.insertProgressLog(
                ProgressLog(date = System.currentTimeMillis(), bodyweight = bodyweight, waist = waist)
            )
        }
    }

    // Judge Check-In management
    val todayDateStr = computeTodayDateStr()

    val dailyCheckIn: StateFlow<DailyCheckIn?> = repository.getDailyCheckInFlow(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isCheckingIn = MutableStateFlow(false)
    val isCheckingIn: StateFlow<Boolean> = _isCheckingIn.asStateFlow()

    fun submitDailyCheckIn(bodyweight: Float, sleepDuration: Float, energyLevel: Int, sorenessLevel: Int) {
        viewModelScope.launch {
            _isCheckingIn.value = true
            val feedback = judgeClient.generateCheckInFeedback(bodyweight, sleepDuration, energyLevel, sorenessLevel)
            val checkIn = DailyCheckIn(
                dateStr = todayDateStr,
                bodyweight = bodyweight,
                sleepDuration = sleepDuration,
                energyLevel = energyLevel,
                sorenessLevel = sorenessLevel,
                coachingMessage = feedback.first,
                intensityAdjustment = feedback.second
            )
            repository.insertDailyCheckIn(checkIn)
            
            // Automatically log body weight in progress history
            repository.insertProgressLog(
                ProgressLog(date = System.currentTimeMillis(), bodyweight = bodyweight, waist = null)
            )
            _isCheckingIn.value = false
            _toastMessage.emit("Check-in saved. Coach has updated today's workout split.")
        }
    }

    // Nutrition Logging and Target Management
    val loggedFoods: StateFlow<List<LoggedFood>> = repository.getLoggedFoodsForDate(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterLog: StateFlow<WaterLog?> = repository.getWaterLogFlow(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nutritionTarget: StateFlow<NutritionTarget?> = repository.getNutritionTargetFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _nutritionFeedback = MutableStateFlow("Recalculating status...")
    val nutritionFeedback: StateFlow<String> = _nutritionFeedback.asStateFlow()

    fun logFood(name: String, calories: Int, protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch {
            repository.insertLoggedFood(
                LoggedFood(name = name, calories = calories, protein = protein, carbs = carbs, fat = fat, dateStr = todayDateStr)
            )
            recalculateNutritionFeedback()
        }
    }

    fun removeFood(id: Int) {
        viewModelScope.launch {
            repository.deleteLoggedFood(id)
            recalculateNutritionFeedback()
        }
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            val current = waterLog.value?.amountMl ?: 0
            repository.insertWaterLog(
                WaterLog(dateStr = todayDateStr, amountMl = current + ml)
            )
            recalculateNutritionFeedback()
        }
    }

    fun updateNutritionTarget(calories: Int, protein: Int, carbs: Int, fat: Int, pin: String) {
        viewModelScope.launch {
            repository.insertNutritionTarget(
                NutritionTarget(id = 1, calories = calories, protein = protein, carbs = carbs, fat = fat, pin = pin)
            )
            _toastMessage.emit("Advanced Settings: Nutrition targets updated!")
            recalculateNutritionFeedback()
        }
    }

    fun recalculateNutritionFeedback() {
        viewModelScope.launch {
            val target = nutritionTarget.value ?: NutritionTarget()
            val list = loggedFoods.value
            val totalCal = list.sumOf { it.calories }
            val totalProtein = list.sumOf { it.protein }
            val totalCarbs = list.sumOf { it.carbs }
            val totalFat = list.sumOf { it.fat }
            val water = waterLog.value?.amountMl ?: 0

            val feedbackText = judgeClient.generateNutritionFeedback(
                calories = totalCal, targetCalories = target.calories,
                protein = totalProtein, targetProtein = target.protein,
                carbs = totalCarbs, targetCarbs = target.carbs,
                fat = totalFat, targetFat = target.fat,
                waterMl = water
            )
            _nutritionFeedback.value = feedbackText
        }
    }

    // Workout session completion & summary comments
    val workoutSession: StateFlow<WorkoutSession?> = repository.getWorkoutSessionFlow(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSummarizingWorkout = MutableStateFlow(false)
    val isSummarizingWorkout: StateFlow<Boolean> = _isSummarizingWorkout.asStateFlow()

    fun completeWorkoutToday(workoutType: String, exercises: List<Exercise>, sets: List<ExerciseSet>) {
        viewModelScope.launch {
            _isSummarizingWorkout.value = true
            val summary = judgeClient.generateWorkoutSummary(workoutType, sets, exercises)
            val session = WorkoutSession(
                dateStr = todayDateStr,
                workoutType = workoutType,
                performanceComment = summary["performance"] ?: "",
                strengthProgressionComment = summary["progression"] ?: "",
                recoveryComment = summary["recovery"] ?: "",
                weightGuidanceComment = summary["guidance"] ?: "",
                completed = true
            )
            repository.insertWorkoutSession(session)
            _isSummarizingWorkout.value = false
            _toastMessage.emit("Workout complete! Today's session has been analyzed by The Judge.")
        }
    }

    // Chat with Judge
    val chatMessages: StateFlow<List<ChatMessage>> = repository.getAllChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isJudgeTyping = MutableStateFlow(false)
    val isJudgeTyping: StateFlow<Boolean> = _isJudgeTyping.asStateFlow()

    fun sendMessageToJudge(text: String) {
        viewModelScope.launch {
            val userMsg = ChatMessage(text = text, isUser = true, timestamp = System.currentTimeMillis())
            repository.insertChatMessage(userMsg)
            
            _isJudgeTyping.value = true
            
            // Build Context
            val recentBw = progressLogs.value.lastOrNull()?.bodyweight
            val context = buildString {
                if (recentBw != null) append("Current Bodyweight: $recentBw lbs.\n")
                val topPRs = personalRecords.value.take(5)
                if (topPRs.isNotEmpty()) {
                    append("Recent PRs:\n")
                    topPRs.forEach { append("- ${it.exerciseName}: ${it.maxWeight} lbs\n") }
                }
                val check = dailyCheckIn.value
                if (check != null) {
                    append("Today's Check-in: sleep=${check.sleepDuration}hrs, energy=${check.energyLevel}/10, soreness=${check.sorenessLevel}/10, intensity adjustment=${check.intensityAdjustment}\n")
                }
            }

            val recentHistory = chatMessages.value.takeLast(10)
            val response = judgeClient.askJudge(recentHistory, text, context)
            
            val judgeMsg = ChatMessage(text = response, isUser = false, timestamp = System.currentTimeMillis())
            repository.insertChatMessage(judgeMsg)
            
            _isJudgeTyping.value = false
        }
    }

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun getTodayDayId(): String {
        val c = Calendar.getInstance()
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Mon" // Sunday split default
        }
    }

    private fun computeTodayDateStr(): String {
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return df.format(Date())
    }
}
