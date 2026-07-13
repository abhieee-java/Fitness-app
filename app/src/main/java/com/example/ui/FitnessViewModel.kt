package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.JudgeAiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class FitnessViewModel(
    private val repository: FitnessRepository,
    private val judgeClient: JudgeAiClient = JudgeAiClient()
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.initializeDefaultExercisesIfEmpty()
        }
    }

    private val _currentDayFilter = MutableStateFlow(getCurrentDayId())
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

    fun addSet(exercise: Exercise, weight: Float, reps: Int, setNumber: Int) {
        viewModelScope.launch {
            val set = ExerciseSet(
                exerciseId = exercise.id,
                date = System.currentTimeMillis(),
                setNumber = setNumber,
                weight = weight,
                reps = reps
            )
            repository.insertSet(set)
            
            // Check PR
            val isPr = repository.checkAndSavePR(exercise.name, weight, System.currentTimeMillis())
            if (isPr) {
                // emit PR event
                _toastMessage.emit("New PR on ${exercise.name}: $weight lbs!")
            }
        }
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
            }

            // We only send the last 10 messages for context window saving
            val recentHistory = chatMessages.value.takeLast(10)
            
            val response = judgeClient.askJudge(recentHistory, text, context)
            
            val judgeMsg = ChatMessage(text = response, isUser = false, timestamp = System.currentTimeMillis())
            repository.insertChatMessage(judgeMsg)
            
            _isJudgeTyping.value = false
        }
    }

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private fun getCurrentDayId(): String {
        val c = Calendar.getInstance()
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Mon" // Sunday defaults to Mon for 6 day split, or rest
        }
    }
}
