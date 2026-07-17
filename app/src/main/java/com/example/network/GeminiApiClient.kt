package com.example.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class JudgeAiClient {
    suspend fun askJudge(
        chatHistory: List<com.example.data.ChatMessage>,
        newQuestion: String,
        context: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "You need to add a valid Gemini API key in the secrets panel to talk to The Judge."
        }

        val systemPrompt = """
            You are 'The Judge', a blunt but helpful strength coach. 
            You are direct, serious, concise, and have a little dry humour. 
            You are not childish, and not overly motivational. 
            You respond to workout questions, PR updates, diet questions, and bodyweight trends.
            If the user mentions food, analyze diet (protein, calories, timing). 
            Avoid empty praise. Never break character.
            
            Current User Context:
            ${context}
        """.trimIndent()

        val contents = chatHistory.map {
            Content(
                parts = listOf(Part(text = it.text)),
                role = if (it.isUser) "user" else "model"
            )
        }.toMutableList()

        contents.add(Content(parts = listOf(Part(text = newQuestion)), role = "user"))

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(temperature = 0.6f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No comment."
        } catch (e: Exception) {
            "Error connecting to The Judge: ${e.message}"
        }
    }

    suspend fun generateCheckInFeedback(
        bodyweight: Float,
        sleep: Float,
        energy: Int,
        soreness: Int
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalCheckInFeedback(bodyweight, sleep, energy, soreness)
        }

        val systemPrompt = """
            You are 'The Judge', a blunt, serious, expert strength coach.
            Analyze this check-in:
            - Weight: $bodyweight lbs
            - Sleep: $sleep hours
            - Energy: $energy/10
            - Soreness: $soreness/10
            
            Give a brief, realistic, 1-2 sentence assessment. 
            On the next line, provide the exact text "ADJUSTMENT: <adjustment>" where adjustment is "Normal load", "Reduce load by 10%", or "Increase load / PR intensity".
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Analyze check-in")))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            parseCheckInResponse(text, bodyweight, sleep, energy, soreness)
        } catch (e: Exception) {
            getLocalCheckInFeedback(bodyweight, sleep, energy, soreness)
        }
    }

    private fun getLocalCheckInFeedback(bodyweight: Float, sleep: Float, energy: Int, soreness: Int): Pair<String, String> {
        val adj: String
        val msg: String
        if (sleep < 6f || energy < 5 || soreness > 7) {
            adj = "Reduce load by 10%"
            msg = "Under-rested ($sleep hrs sleep), low energy ($energy/10), or high soreness ($soreness/10). Today we drop intensity by 10% to protect recovery. Ego lifting is prohibited."
        } else if (sleep >= 8f && energy >= 8 && soreness <= 3) {
            adj = "Increase load / PR intensity"
            msg = "Prime conditions detected ($sleep hrs sleep, energy $energy/10). Soreness is low. No excuses. Increase weight or aim for a PR today. Destroy it."
        } else {
            adj = "Normal load"
            msg = "Standard training conditions. Load is normal. Focus on perfect execution and progressive overload. Let's work."
        }
        return Pair(msg, adj)
    }

    private fun parseCheckInResponse(text: String, bodyweight: Float, sleep: Float, energy: Int, soreness: Int): Pair<String, String> {
        val lines = text.lines()
        val adjLine = lines.find { it.contains("ADJUSTMENT:", ignoreCase = true) }
        val adjustment = adjLine?.substringAfter("ADJUSTMENT:")?.trim() ?: "Normal load"
        val coaching = lines.filter { !it.contains("ADJUSTMENT:", ignoreCase = true) }.joinToString(" ").trim()
        val finalCoaching = if (coaching.isBlank()) getLocalCheckInFeedback(bodyweight, sleep, energy, soreness).first else coaching
        return Pair(finalCoaching, adjustment)
    }

    suspend fun generateWorkoutSummary(
        workoutType: String,
        setsLogged: List<com.example.data.ExerciseSet>,
        exercises: List<com.example.data.Exercise>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val exerciseMap = exercises.associateBy { it.id }
        val summaryDetails = setsLogged.map { set ->
            val exName = exerciseMap[set.exerciseId]?.name ?: "Exercise"
            "$exName: Set ${set.setNumber} - ${set.weight} lbs x ${set.reps} reps (RPE ${set.rpe ?: "N/A"})"
        }.joinToString("\n")

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalWorkoutSummary(workoutType, setsLogged, exercises)
        }

        val systemPrompt = """
            You are 'The Judge', a direct, non-nonsense strength coach.
            Review the user's completed workout split '$workoutType' with these logged sets:
            $summaryDetails

            Analyze performance, strength progression, and recovery.
            Provide your feedback in exactly 4 lines, starting with:
            PERFORMANCE: <comment>
            PROGRESSION: <comment>
            RECOVERY: <comment>
            GUIDANCE: <comment on whether to increase, maintain, or decrease weight next session>
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Analyze my workout session")))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            parseWorkoutSummaryResponse(text, workoutType, setsLogged, exercises)
        } catch (e: Exception) {
            getLocalWorkoutSummary(workoutType, setsLogged, exercises)
        }
    }

    private fun getLocalWorkoutSummary(
        workoutType: String,
        setsLogged: List<com.example.data.ExerciseSet>,
        exercises: List<com.example.data.Exercise>
    ): Map<String, String> {
        val highRpes = setsLogged.count { (it.rpe ?: 0) >= 9 }
        val totalSets = setsLogged.size
        
        val perf = if (totalSets >= 8) "Heavy volume completed. You pushed through $totalSets sets of $workoutType." else "Minimal volume logged today. Only $totalSets sets total."
        val prog = if (highRpes >= 3) "Intensity was high with $highRpes sets at RPE 9+. Mechanical tension was maximized." else "RPE levels indicate room for overload. Ensure you are pushing closer to failure."
        val rec = "Make sure hydration and recovery nutrition are handled immediately. 30g protein minimum."
        val guid = if (highRpes == 0 && totalSets > 0) "Increase target weights next session by 5 lbs. Form looks solid." else "Maintain or increase select weights. Keep progression linear."

        return mapOf(
            "performance" to perf,
            "progression" to prog,
            "recovery" to rec,
            "guidance" to guid
        )
    }

    private fun parseWorkoutSummaryResponse(
        text: String,
        workoutType: String,
        setsLogged: List<com.example.data.ExerciseSet>,
        exercises: List<com.example.data.Exercise>
    ): Map<String, String> {
        val lines = text.lines()
        val perf = lines.find { it.startsWith("PERFORMANCE:", ignoreCase = true) }?.substringAfter(":")?.trim()
        val prog = lines.find { it.startsWith("PROGRESSION:", ignoreCase = true) }?.substringAfter(":")?.trim()
        val rec = lines.find { it.startsWith("RECOVERY:", ignoreCase = true) }?.substringAfter(":")?.trim()
        val guid = lines.find { it.startsWith("GUIDANCE:", ignoreCase = true) }?.substringAfter(":")?.trim()

        val fallback = getLocalWorkoutSummary(workoutType, setsLogged, exercises)
        return mapOf(
            "performance" to (perf ?: fallback["performance"]!!),
            "progression" to (prog ?: fallback["progression"]!!),
            "recovery" to (rec ?: fallback["recovery"]!!),
            "guidance" to (guid ?: fallback["guidance"]!!)
        )
    }

    suspend fun generateNutritionFeedback(
        calories: Int,
        targetCalories: Int,
        protein: Int,
        targetProtein: Int,
        carbs: Int,
        targetCarbs: Int,
        fat: Int,
        targetFat: Int,
        waterMl: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalNutritionFeedback(calories, targetCalories, protein, targetProtein, carbs, targetCarbs, fat, targetFat, waterMl)
        }

        val systemPrompt = """
            You are 'The Judge', a direct, serious strength coach.
            Analyze today's intake compared to targets:
            - Calories: $calories / $targetCalories kcal
            - Protein: $protein / $targetProtein g
            - Carbs: $carbs / $targetCarbs g
            - Fat: $fat / $targetFat g
            - Water: $waterMl ml
            
            Provide exactly one line of realistic, direct coaching advice. No fluff, no friendly filler.
            Reference specific values like "You still need ${targetProtein - protein}g of protein." if applicable.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Analyze nutrition")))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Intake logged."
        } catch (e: Exception) {
            getLocalNutritionFeedback(calories, targetCalories, protein, targetProtein, carbs, targetCarbs, fat, targetFat, waterMl)
        }
    }

    private fun getLocalNutritionFeedback(
        calories: Int,
        targetCalories: Int,
        protein: Int,
        targetProtein: Int,
        carbs: Int,
        targetCarbs: Int,
        fat: Int,
        targetFat: Int,
        waterMl: Int
    ): String {
        return when {
            protein < targetProtein -> "You still need ${targetProtein - protein}g of protein. Feed the muscle, don't starve it."
            calories > targetCalories -> "You've exceeded your calorie target by ${calories - targetCalories} kcal. Tighten up the discipline."
            waterMl < 2000 -> "Dehydration kills performance. You've only had ${waterMl}ml of water. Drink up."
            else -> "Recovery nutrition looks good. Targets are satisfied. Rest and let the growth happen."
        }
    }
}
