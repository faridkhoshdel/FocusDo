package com.example.focusdo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TaskRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun load(): List<Task> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val title = obj.optString("title").trim()
                    if (title.isEmpty()) continue
                    add(
                        Task(
                            id = obj.optLong("id", index.toLong()),
                            title = title,
                            note = obj.optString("note"),
                            priority = obj.optInt("priority", 1).coerceIn(0, 2),
                            done = obj.optBoolean("done", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun save(tasks: List<Task>): Boolean = withContext(Dispatchers.IO) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("note", task.note)
                put("priority", task.priority)
                put("done", task.done)
                put("createdAt", task.createdAt)
            })
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).commit()
    }

    suspend fun loadDashboardStats(): DashboardStats = withContext(Dispatchers.IO) {
        val tasks = load()
        DashboardStats(
            totalTasks = tasks.size,
            activeTasks = tasks.count { !it.done },
            completedTasks = tasks.count { it.done },
            focusMinutesToday = prefs.getInt(KEY_FOCUS_TODAY, 0),
            focusMinutesWeek = prefs.getInt(KEY_FOCUS_WEEK, 0),
            streakDays = calculateCurrentStreak()
        )
    }

    suspend fun recordFocusSession(minutes: Int, date: LocalDate = LocalDate.now()) = withContext(Dispatchers.IO) {
        if (minutes <= 0) return@withContext
        val today = date.toString()
        val lastDate = prefs.getString(KEY_LAST_FOCUS_DATE, null)
        val previousStreak = prefs.getInt(KEY_STREAK, 0)
        val newStreak = when {
            lastDate == today -> previousStreak.coerceAtLeast(1)
            lastDate != null && runCatching {
                ChronoUnit.DAYS.between(LocalDate.parse(lastDate), date)
            }.getOrDefault(Long.MAX_VALUE) == 1L -> previousStreak + 1
            else -> 1
        }
        prefs.edit()
            .putInt(KEY_FOCUS_TODAY, prefs.getInt(KEY_FOCUS_TODAY, 0) + minutes)
            .putInt(KEY_FOCUS_WEEK, prefs.getInt(KEY_FOCUS_WEEK, 0) + minutes)
            .putInt(KEY_STREAK, newStreak)
            .putString(KEY_LAST_FOCUS_DATE, today)
            .commit()
    }

    suspend fun resetDailyStatsIfNeeded(date: LocalDate = LocalDate.now()) = withContext(Dispatchers.IO) {
        val storedDate = prefs.getString(KEY_STATS_DATE, null)
        if (storedDate != date.toString()) {
            prefs.edit().putInt(KEY_FOCUS_TODAY, 0).putString(KEY_STATS_DATE, date.toString()).commit()
        }
    }

    suspend fun getStreak(): Int = withContext(Dispatchers.IO) { calculateCurrentStreak() }

    private fun calculateCurrentStreak(): Int {
        val lastDate = prefs.getString(KEY_LAST_FOCUS_DATE, null) ?: return 0
        val days = runCatching { ChronoUnit.DAYS.between(LocalDate.parse(lastDate), LocalDate.now()) }
            .getOrDefault(Long.MAX_VALUE)
        return if (days <= 1) prefs.getInt(KEY_STREAK, 0) else 0
    }

    companion object {
        private const val PREFERENCES_NAME = "focusdo_db"
        private const val KEY_TASKS = "tasks"
        private const val KEY_FOCUS_TODAY = "focus_minutes_today"
        private const val KEY_FOCUS_WEEK = "focus_minutes_week"
        private const val KEY_STREAK = "focus_streak"
        private const val KEY_LAST_FOCUS_DATE = "last_focus_date"
        private const val KEY_STATS_DATE = "stats_date"
    }
}
