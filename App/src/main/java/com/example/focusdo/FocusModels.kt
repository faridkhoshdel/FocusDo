package com.example.focusdo

/** User-selectable energy state used by FocusDo's smart recommendations. */
enum class EnergyLevel { LOW, MEDIUM, HIGH }

data class DailyGoal(
    val targetMinutes: Int = 60,
    val completedMinutes: Int = 0
) {
    val progress: Float
        get() = (completedMinutes.toFloat() / targetMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class SmartTaskScore(
    val taskId: Long,
    val score: Int,
    val reasons: List<String>
)

object SmartTaskScorer {
    fun score(task: Task, energy: EnergyLevel = EnergyLevel.MEDIUM, now: Long = System.currentTimeMillis()): SmartTaskScore {
        val priorityScore = task.priority.coerceIn(0, 2) * 25
        val ageDays = ((now - task.createdAt).coerceAtLeast(0L) / DAY_MS).toInt()
        val ageScore = (ageDays * 4).coerceAtMost(20)
        val energyScore = when (energy) {
            EnergyLevel.LOW -> if (task.priority == 0) 20 else 5
            EnergyLevel.MEDIUM -> 12
            EnergyLevel.HIGH -> if (task.priority == 2) 20 else 10
        }
        val score = (priorityScore + ageScore + energyScore).coerceIn(0, 100)
        val reasons = buildList {
            if (task.priority == 2) add("High priority")
            if (ageDays > 0) add("Waiting $ageDays day(s)")
            add("Fits ${energy.name.lowercase()} energy")
        }
        return SmartTaskScore(task.id, score, reasons)
    }

    fun nextTask(tasks: List<Task>, energy: EnergyLevel = EnergyLevel.MEDIUM): Task? =
        tasks.filterNot { it.done }.maxByOrNull { score(it, energy).score }

    private const val DAY_MS = 86_400_000L
}

data class FocusDna(
    val completedTasks: Int,
    val activeTasks: Int,
    val focusMinutes: Int,
    val completionRate: Int,
    val consistency: Int
)
