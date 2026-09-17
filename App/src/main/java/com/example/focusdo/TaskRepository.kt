package com.example.focusdo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("focusdo_db", Context.MODE_PRIVATE)

    suspend fun load(): List<Task> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_TASKS, "[]") ?: "[]"

        try {
            val array = JSONArray(json)
            val result = mutableListOf<Task>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    Task(
                        id = obj.optLong("id"),
                        title = obj.optString("title"),
                        note = obj.optString("note"),
                        priority = obj.optInt("priority", 1),
                        done = obj.optBoolean("done", false),
                        createdAt = obj.optLong("createdAt")
                    )
                )
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun save(tasks: List<Task>) = withContext(Dispatchers.IO) {
        val array = JSONArray()

        tasks.forEach { task ->
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("title", task.title)
            obj.put("note", task.note)
            obj.put("priority", task.priority)
            obj.put("done", task.done)
            obj.put("createdAt", task.createdAt)
            array.put(obj)
        }

        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    suspend fun setFocusMinutesToday(minutes: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_FOCUS_TODAY, minutes).apply()
    }

    suspend fun getFocusMinutesToday(): Int = withContext(Dispatchers.IO) {
        prefs.getInt(KEY_FOCUS_TODAY, 0)
    }

    suspend fun setFocusMinutesWeek(minutes: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_FOCUS_WEEK, minutes).apply()
    }

    suspend fun getFocusMinutesWeek(): Int = withContext(Dispatchers.IO) {
        prefs.getInt(KEY_FOCUS_WEEK, 0)
    }

    suspend fun setStreak(streak: Int) = withContext(Dispatchers.IO) {
        prefs.edit().putInt(KEY_STREAK, streak).apply()
    }

    suspend fun getStreak(): Int = withContext(Dispatchers.IO) {
        prefs.getInt(KEY_STREAK, 0)
    }

    suspend fun setLastFocusDate(date: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_LAST_FOCUS_DATE, date).apply()
    }

    suspend fun getLastFocusDate(): String = withContext(Dispatchers.IO) {
        prefs.getString(KEY_LAST_FOCUS_DATE, "") ?: ""
    }

    suspend fun loadDashboardStats(): DashboardStats = withContext(Dispatchers.IO) {
        val tasks = load()
        val focusToday = getFocusMinutesToday()
        val focusWeek = getFocusMinutesWeek()
        val streak = getStreak()

        DashboardStats(
            totalTasks = tasks.size,
            activeTasks = tasks.count { !it.done },
            completedTasks = tasks.count { it.done },
            focusMinutesToday = focusToday,
            focusMinutesWeek = focusWeek,
            streakDays = streak
        )
    }

    companion object {
        private const val KEY_TASKS = "tasks"
        private const val KEY_FOCUS_TODAY = "focus_minutes_today"
        private const val KEY_FOCUS_WEEK = "focus_minutes_week"
        private const val KEY_STREAK = "focus_streak"
        private const val KEY_LAST_FOCUS_DATE = "last_focus_date"
    }
}