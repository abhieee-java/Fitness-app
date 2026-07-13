package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Exercise
import com.example.data.ExerciseSet
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(viewModel: FitnessViewModel, dayId: String) {
    val exercises by viewModel.getExercisesForDay(dayId).collectAsState(initial = emptyList())
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

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseCard(
                        viewModel = viewModel,
                        exercise = exercise,
                        accentColor = dayInfo.color,
                        onSetLogged = {
                            activeRestTime = exercise.restTimeSeconds
                            activeExerciseName = exercise.name
                        },
                        modifier = Modifier.animateItem()
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // padding for timer
                }
            }
        }
        
        androidx.compose.animation.AnimatedVisibility(
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
fun ExerciseCard(
    viewModel: FitnessViewModel,
    exercise: Exercise,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onSetLogged: () -> Unit
) {
    val sets by viewModel.getSetsForExerciseToday(exercise.id).collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }

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
                        text = "${exercise.targetSets} sets x ${exercise.targetReps} • ${exercise.equipment}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Set ${set.setNumber}", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface))
                            Text("${set.weight} lbs x ${set.reps} reps", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var weightInput by remember { mutableStateOf("") }
                    var repsInput by remember { mutableStateOf("") }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("lbs") },
                            modifier = Modifier.weight(1f),
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
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedLabelColor = accentColor
                            )
                        )
                        IconButton(
                            onClick = {
                                val w = weightInput.toFloatOrNull()
                                val r = repsInput.toIntOrNull()
                                if (w != null && r != null) {
                                    viewModel.addSet(exercise, w, r, sets.size + 1)
                                    weightInput = ""
                                    repsInput = ""
                                    onSetLogged()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Log Set", tint = Color.White)
                        }
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp)
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
                            text = "Rest: $exerciseName",
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
