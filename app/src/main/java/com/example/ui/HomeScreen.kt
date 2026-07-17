package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyCheckIn
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: FitnessViewModel, onNavigateToWorkout: (String) -> Unit) {
    val dailyCheckIn by viewModel.dailyCheckIn.collectAsState()
    val isCheckingIn by viewModel.isCheckingIn.collectAsState()
    val progressLogs by viewModel.progressLogs.collectAsState()
    val personalRecords by viewModel.personalRecords.collectAsState()

    val currentBw = progressLogs.lastOrNull()?.bodyweight?.toString() ?: "180"
    val prCount = personalRecords.size

    val todayDayId = viewModel.getTodayDayId()
    val todayWorkoutName = when (todayDayId) {
        "Mon" -> "Push Day"
        "Tue" -> "Pull Day"
        "Wed" -> "Leg Day"
        "Thu" -> "Cardio & Abs"
        "Fri" -> "Upper Body"
        "Sat" -> "Lower Body"
        else -> "Rest & Recovery"
    }

    val scheduleMap = mapOf(
        "Mon" to DaySchedule("Mon", "Push", "Chest, Shoulders, Triceps", AccentPush),
        "Tue" to DaySchedule("Tue", "Pull", "Back, Biceps, Rear Delts", AccentPull),
        "Wed" to DaySchedule("Wed", "Legs", "Quads, Hams, Glutes", AccentLegs),
        "Thu" to DaySchedule("Thu", "Cardio+Abs", "Steady-state & Core", AccentCardio),
        "Fri" to DaySchedule("Fri", "Upper", "Upper-body Volume", AccentUpper),
        "Sat" to DaySchedule("Sat", "Lower", "Posterior Chain", AccentLower)
    )

    val todaySchedule = scheduleMap[todayDayId] ?: DaySchedule("Mon", "Push", "Chest, Shoulders, Triceps", AccentPush)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .animateContentSize()
    ) {
        // Hero Header
        Text(
            text = "bd",
            style = MaterialTheme.typography.displayLarge.copy(color = MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "TRAIN LIKE YOU MEAN IT.",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (dailyCheckIn == null) {
            // Check-In Wizard (Before Starting Workout)
            JudgeCheckInForm(
                initialWeight = currentBw,
                isCheckingIn = isCheckingIn,
                onSubmit = { weight, sleep, energy, soreness ->
                    viewModel.submitDailyCheckIn(weight, sleep, energy, soreness)
                }
            )
        } else {
            // Checked In Home State
            val checkIn = dailyCheckIn!!

            // Today's Date
            Text(
                text = getFormattedDate().uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // User Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(title = "CURRENT WEIGHT", value = "${checkIn.bodyweight} lbs", modifier = Modifier.weight(1f))
                StatCard(title = "TOTAL PRs", value = "$prCount", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // The Judge Coaching & Intensity Assessment Card
            Text(
                text = "COACH'S READOUT",
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = checkIn.intensityAdjustment.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = checkIn.coachingMessage,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Today's Workout Card (Only display today's workout)
            Text(
                text = "TODAY'S WORKOUT",
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_workout_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = todayWorkoutName.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = todaySchedule.color
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(50))
                                .background(todaySchedule.color)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FOCUS: ${todaySchedule.focus.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Call to Action
                    Button(
                        onClick = {
                            viewModel.setDayFilter(todayDayId)
                            onNavigateToWorkout(todayDayId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = todaySchedule.color),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("start_workout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START TRAINING",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JudgeCheckInForm(
    initialWeight: String,
    isCheckingIn: Boolean,
    onSubmit: (Float, Float, Int, Int) -> Unit
) {
    var weightStr by remember { mutableStateOf(initialWeight) }
    var sleepStr by remember { mutableStateOf("8.0") }
    var energy by remember { mutableStateOf(8f) }
    var soreness by remember { mutableStateOf(3f) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("judge_checkin_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Text(
                    text = "THE JUDGE CHECK-IN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            }

            Text(
                text = "Welcome back. I am The Judge. Before starting today's session, log your physical metrics so I can calibrate training intensity.",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            // Current Body Weight
            OutlinedTextField(
                value = weightStr,
                onValueChange = { weightStr = it },
                label = { Text("Current Body Weight (lbs)") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth().testTag("checkin_weight_input")
            )

            // Sleep Duration
            OutlinedTextField(
                value = sleepStr,
                onValueChange = { sleepStr = it },
                label = { Text("Sleep Duration Last Night (Hours)") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth().testTag("checkin_sleep_input")
            )

            // Energy Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ENERGY LEVEL",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${energy.toInt()}/10",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    )
                }
                Slider(
                    value = energy,
                    onValueChange = { energy = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("checkin_energy_slider")
                )
            }

            // Soreness Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MUSCLE SORENESS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${soreness.toInt()}/10",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    )
                }
                Slider(
                    value = soreness,
                    onValueChange = { soreness = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("checkin_soreness_slider")
                )
            }

            Button(
                onClick = {
                    val w = weightStr.toFloatOrNull() ?: 180f
                    val s = sleepStr.toFloatOrNull() ?: 8f
                    onSubmit(w, s, energy.toInt(), soreness.toInt())
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().testTag("submit_checkin_button"),
                enabled = !isCheckingIn
            ) {
                if (isCheckingIn) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "SUBMIT TO THE JUDGE",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                    )
                }
            }
        }
    }
}

private fun getFormattedDate(): String {
    val df = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    return df.format(Date())
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
