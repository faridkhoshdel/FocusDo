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
        val json = prefs.getString(KEY, "[]") ?: "[]"

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

        prefs.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val KEY = "tasks"
    }
}