package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun HomeScreen(viewModel: FitnessViewModel, onNavigateToWorkout: (String) -> Unit) {
    val currentDayFilter by viewModel.currentDayFilter.collectAsState()
    val progressLogs by viewModel.progressLogs.collectAsState()
    val personalRecords by viewModel.personalRecords.collectAsState()

    val currentBw = progressLogs.lastOrNull()?.bodyweight?.toString() ?: "--"
    val prCount = personalRecords.size

    val schedule = listOf(
        DaySchedule("Mon", "Push", "Chest, Shoulders, Triceps", AccentPush),
        DaySchedule("Tue", "Pull", "Back, Biceps, Rear Delts", AccentPull),
        DaySchedule("Wed", "Legs", "Quads, Hams, Glutes", AccentLegs),
        DaySchedule("Thu", "Cardio+Abs", "Steady-state & Core", AccentCardio),
        DaySchedule("Fri", "Upper", "Upper-body Volume", AccentUpper),
        DaySchedule("Sat", "Lower", "Posterior Chain", AccentLower),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Hero Section
        Text(
            text = "bd",
            style = MaterialTheme.typography.displayLarge.copy(color = MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "TRAIN LIKE YOU MEAN IT.",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // User Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(title = "BODYWEIGHT", value = "$currentBw lbs", modifier = Modifier.weight(1f))
            StatCard(title = "TOTAL PRs", value = "$prCount", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "WEEKLY SPLIT",
            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(schedule) { day ->
                val isToday = currentDayFilter == day.id
                DayCard(day = day, isToday = isToday) {
                    viewModel.setDayFilter(day.id)
                    onNavigateToWorkout(day.id)
                }
            }
        }
    }
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

@Composable
fun DayCard(day: DaySchedule, isToday: Boolean, onClick: () -> Unit) {
    val borderColor = if (isToday) day.color else Color.Transparent
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isToday) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        animationSpec = androidx.compose.animation.core.tween(200)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .testTag("day_card_${day.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = day.id.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Box(modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(day.color))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = day.type,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = day.color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = day.focus,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isToday) {
                Text("TODAY", style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary))
            }
        }
    }
}

data class DaySchedule(val id: String, val type: String, val focus: String, val color: Color)
