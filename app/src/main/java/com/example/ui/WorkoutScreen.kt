package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Exercise
import com.example.data.ExerciseSet
import com.example.data.WorkoutSession
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(viewModel: FitnessViewModel, dayId: String) {
    val exercises by viewModel.getExercisesForDay(dayId).collectAsState(initial = emptyList())
    val workoutSession by viewModel.workoutSession.collectAsState()
    val isSummarizing by viewModel.isSummarizingWorkout.collectAsState()

    var activeRestTime by remember { mutableStateOf(0) }
    var activeExerciseName by remember { mutableStateOf("") }

    val schedule = listOf(
        DaySchedule("Mon", "Push", "Chest, Shoulders, Triceps", AccentPush),
        DaySchedule("Tue", "Pull", "Back, Biceps, Rear Delts", AccentPull),
        DaySchedule("Wed", "Legs", "Quads, Hams, Glutes", AccentLegs),
        DaySchedule("Thu", "Cardio+Abs", "Steady-state & Core", AccentCardio),
        DaySchedule("Fri", "Upper", "Upper-body Volume", AccentUpper),
        DaySchedule("Sat", "Lower", "Posterior Chain", AccentLower),
    )
    val dayInfo = schedule.find { it.id == dayId } ?: schedule[0]

    // We collect all sets logged for all exercises today
    val allLoggedSets = remember { mutableStateListOf<ExerciseSet>() }

    // Helper to trigger recalculation or completion
    val triggerCompleteWorkout: () -> Unit = {
        viewModel.completeWorkoutToday(dayInfo.type, exercises, allLoggedSets)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = dayInfo.id.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = dayInfo.type.uppercase(),
                style = MaterialTheme.typography.displayLarge.copy(color = dayInfo.color),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Focus: ${dayInfo.focus}",
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (workoutSession != null && workoutSession!!.completed) {
                // Post-Workout Summary State
                val session = workoutSession!!
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().testTag("workout_completed_badge")
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "SESSION COMPLETE",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "Excellent effort today. The Judge has formulated your training feedback report below.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimaryContainer),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "THE JUDGE'S ASSESSMENT",
                            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    item {
                        FeedbackCategoryCard(title = "PERFORMANCE", text = session.performanceComment, color = dayInfo.color, icon = Icons.Default.Star)
                    }
                    item {
                        FeedbackCategoryCard(title = "PROGRESSION STATUS", text = session.strengthProgressionComment, color = dayInfo.color, icon = Icons.Default.TrendingUp)
                    }
                    item {
                        FeedbackCategoryCard(title = "RECOVERY OUTLOOK", text = session.recoveryComment, color = dayInfo.color, icon = Icons.Default.BatteryChargingFull)
                    }
                    item {
                        FeedbackCategoryCard(title = "NEXT SESSION GUIDANCE", text = session.weightGuidanceComment, color = MaterialTheme.colorScheme.primary, icon = Icons.Default.Forward)
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                // Active Workout Exercises List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        ExerciseCard(
                            viewModel = viewModel,
                            exercise = exercise,
                            accentColor = dayInfo.color,
                            onSetLogged = { set ->
                                allLoggedSets.add(set)
                                activeRestTime = exercise.restTimeSeconds
                                activeExerciseName = exercise.name
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Complete Workout Button
                    item {
                        Button(
                            onClick = triggerCompleteWorkout,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("complete_workout_button"),
                            enabled = !isSummarizing
                        ) {
                            if (isSummarizing) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Celebration, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SUBMIT WORKOUT TO THE JUDGE",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp)) // padding for timer overlay space
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = activeRestTime > 0,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it },
                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
            ),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it },
                animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RestTimerOverlay(
                timeRemaining = activeRestTime,
                exerciseName = activeExerciseName,
                onTick = { activeRestTime-- },
                onSkip = { activeRestTime = 0 }
            )
        }
    }
}

@Composable
fun FeedbackCategoryCard(title: String, text: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = color)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}

@Composable
fun ExerciseCard(
    viewModel: FitnessViewModel,
    exercise: Exercise,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onSetLogged: (ExerciseSet) -> Unit
) {
    val sets by viewModel.getSetsForExerciseToday(exercise.id).collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }

    val highRepsDefault = getTargetRepsInt(exercise.targetReps).toString()
    var weightInput by remember { mutableStateOf(exercise.targetWeight.toString()) }
    var repsInput by remember { mutableStateOf(highRepsDefault) }
    var rpeVal by remember { mutableStateOf(8f) }

    // Synchronize weight update when target weight changes in VM
    LaunchedEffect(exercise.targetWeight) {
        weightInput = exercise.targetWeight.toString()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
            .clickable { expanded = !expanded }
            .testTag("exercise_${exercise.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = "${exercise.targetSets} SETS x ${exercise.targetReps} • ${exercise.equipment.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PREVIOUS: ${exercise.previousWeight} lbs | TARGET: ${exercise.targetWeight} lbs",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = accentColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sets.size >= exercise.targetSets) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${sets.size}/${exercise.targetSets}",
                        style = MaterialTheme.typography.titleMedium.copy(color = if (sets.size >= exercise.targetSets) accentColor else MaterialTheme.colorScheme.onSurface)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("CUE: ${exercise.executionCue.uppercase()}", style = MaterialTheme.typography.labelMedium.copy(color = accentColor, fontWeight = FontWeight.Bold))
                    Text("TECHNIQUE: ${exercise.techniqueStyle}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))

                    Spacer(modifier = Modifier.height(16.dp))

                    sets.forEach { set ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Set ${set.setNumber}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                if (set.rpe != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("RPE ${set.rpe}", style = MaterialTheme.typography.labelSmall.copy(color = accentColor, fontWeight = FontWeight.Bold))
                                }
                            }
                            Text("${set.weight} lbs x ${set.reps} reps", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Form for adding a Set
                    Text(
                        text = "LOG NEXT SET",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("lbs") },
                            modifier = Modifier.weight(1.2f).testTag("weight_input_${exercise.id}"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedLabelColor = accentColor
                            )
                        )
                        OutlinedTextField(
                            value = repsInput,
                            onValueChange = { repsInput = it },
                            label = { Text("reps") },
                            modifier = Modifier.weight(1f).testTag("reps_input_${exercise.id}"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedLabelColor = accentColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // RPE difficulty slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RPE (Difficulty rating: 1-10)",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "${rpeVal.toInt()}/10",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black, color = accentColor)
                        )
                    }
                    Slider(
                        value = rpeVal,
                        onValueChange = { rpeVal = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("rpe_slider_${exercise.id}")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val w = weightInput.toFloatOrNull()
                            val r = repsInput.toIntOrNull()
                            if (w != null && r != null) {
                                val setNum = sets.size + 1
                                val loggedSet = ExerciseSet(
                                    exerciseId = exercise.id,
                                    date = System.currentTimeMillis(),
                                    setNumber = setNum,
                                    weight = w,
                                    reps = r,
                                    rpe = rpeVal.toInt()
                                )
                                viewModel.addSet(exercise, w, r, rpeVal.toInt(), setNum)
                                onSetLogged(loggedSet)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("log_set_button_${exercise.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Log Set", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RECORD SET ${sets.size + 1}", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }
            }
        }
    }
}

@Composable
fun RestTimerOverlay(
    timeRemaining: Int,
    exerciseName: String,
    onTick: () -> Unit,
    onSkip: () -> Unit
) {
    LaunchedEffect(timeRemaining) {
        if (timeRemaining > 0) {
            delay(1000)
            onTick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp).testTag("rest_timer_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = String.format("%02d:%02d", timeRemaining / 60, timeRemaining % 60),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                        Text(
                            text = "Rest Timer: $exerciseName",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                    }
                }
                TextButton(onClick = onSkip) {
                    Text("SKIP", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer))
                }
            }
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

data class DaySchedule(val id: String, val type: String, val focus: String, val color: Color)
