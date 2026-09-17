package com.example.focusdo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(stats: DashboardStats, goal: DailyGoal = DailyGoal()) {
    val completionRate = if (stats.totalTasks == 0) 0 else stats.completedTasks * 100 / stats.totalTasks
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.dashboard), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Focus DNA", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.today_focus), "${stats.focusMinutesToday}m", Modifier.weight(1f))
            StatCard(stringResource(R.string.tasks_completed), stats.completedTasks.toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.weekly_focus), "${stats.focusMinutesWeek}m", Modifier.weight(1f))
            StatCard(stringResource(R.string.streak), "${stats.streakDays} days", Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Completion rate: $completionRate%", fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { completionRate / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Daily goal: ${goal.completedMinutes}/${goal.targetMinutes} minutes")
                LinearProgressIndicator(progress = { goal.progress }, modifier = Modifier.fillMaxWidth())
                Text("${stats.activeTasks} active tasks • ${stats.totalTasks} total tasks", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
