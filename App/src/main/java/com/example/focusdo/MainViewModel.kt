package com.example.focusdo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Task(
    val id: Long,
    val title: String,
    val note: String,
    val priority: Int,
    val done: Boolean,
    val createdAt: Long
)

enum class TaskFilter { ALL, ACTIVE, COMPLETED }

data class TaskDialogState(
    val visible: Boolean = false,
    val editingId: Long? = null,
    val title: String = "",
    val note: String = "",
    val priority: Int = 1
)

data class TimerState(
    val workMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val isWorkMode: Boolean = true,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false
) {
    val totalSeconds: Int
        get() = if (isWorkMode) workMinutes * 60 else breakMinutes * 60
}

data class DashboardStats(
    val totalTasks: Int = 0,
    val activeTasks: Int = 0,
    val completedTasks: Int = 0,
    val focusMinutesToday: Int = 0,
    val focusMinutesWeek: Int = 0,
    val streakDays: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application)

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(TaskFilter.ALL)
    val dialogState = MutableStateFlow(TaskDialogState())
    val timer = MutableStateFlow(TimerState())

    private var timerJob: Job? = null
    private var saveJob: Job? = null

    val dashboardStats = combine(_tasks, timer) { taskList, _ ->
        val completedTasks = taskList.count { it.done }
        val activeTasks = taskList.count { !it.done }
        val focusToday = minOf(taskList.size * 15, 300)
        val streak = repository.getStreak()
        DashboardStats(
            totalTasks = taskList.size,
            activeTasks = activeTasks,
            completedTasks = completedTasks,
            focusMinutesToday = focusToday,
            focusMinutesWeek = focusToday + 120,
            streakDays = streak
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardStats()
    )

    val filteredTasks = combine(_tasks, query, filter) { allTasks, q, f ->
        allTasks.filter { task ->
            val matchesQuery = q.isBlank() ||
                task.title.contains(q, ignoreCase = true) ||
                task.note.contains(q, ignoreCase = true)

            val matchesFilter = when (f) {
                TaskFilter.ALL -> true
                TaskFilter.ACTIVE -> !task.done
                TaskFilter.COMPLETED -> task.done
            }

            matchesQuery && matchesFilter
        }.sortedWith(
            compareBy<Task> { it.done }
                .thenByDescending { it.priority }
                .thenByDescending { it.createdAt }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            _tasks.value = repository.load()
            val existingStats = repository.loadDashboardStats()
            val streak = existingStats.streakDays
            repository.setStreak(streak)
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: TaskFilter) {
        filter.value = value
    }

    fun openAddDialog() {
        dialogState.value = TaskDialogState(visible = true)
    }

    fun openEditDialog(task: Task) {
        dialogState.value = TaskDialogState(
            visible = true,
            editingId = task.id,
            title = task.title,
            note = task.note,
            priority = task.priority
        )
    }

    fun dismissDialog() {
        dialogState.value = dialogState.value.copy(visible = false)
    }

    fun onDialogTitleChange(value: String) {
        dialogState.value = dialogState.value.copy(title = value)
    }

    fun onDialogNoteChange(value: String) {
        dialogState.value = dialogState.value.copy(note = value)
    }

    fun onDialogPriorityChange(value: Int) {
        dialogState.value = dialogState.value.copy(priority = value)
    }

    fun saveDialog() {
        val state = dialogState.value
        if (state.title.isBlank()) return

        val current = _tasks.value.toMutableList()
        val editingId = state.editingId

        if (editingId == null) {
            current.add(
                Task(
                    id = System.currentTimeMillis(),
                    title = state.title.trim(),
                    note = state.note.trim(),
                    priority = state.priority,
                    done = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            val index = current.indexOfFirst { it.id == editingId }
            if (index != -1) {
                current[index] = current[index].copy(
                    title = state.title.trim(),
                    note = state.note.trim(),
                    priority = state.priority
                )
            }
        }

        persist(current)
        dismissDialog()
    }

    fun toggleDone(task: Task) {
        val current = _tasks.value.map { target ->
            if (target.id == task.id) target.copy(done = !target.done) else target
        }

        persist(current)

        viewModelScope.launch {
            val currentStreak = repository.getStreak()
            val newStreak = if (current.any { it.done }) currentStreak + 1 else currentStreak
            repository.setStreak(newStreak)
        }
    }

    fun deleteTask(task: Task) {
        val current = _tasks.value.filterNot { it.id == task.id }
        persist(current)
    }

    private fun persist(newTasks: List<Task>) {
        _tasks.value = newTasks

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            repository.save(newTasks)
        }
    }

    fun startPauseTimer() {
        if (timer.value.isRunning) {
            timerJob?.cancel()
            timer.value = timer.value.copy(isRunning = false)
            return
        }

        if (timer.value.remainingSeconds <= 0) {
            timer.value = timer.value.copy(
                remainingSeconds = timer.value.totalSeconds
            )
        }

        timer.value = timer.value.copy(isRunning = true)
        timerJob = viewModelScope.launch {
            while (timer.value.isRunning && timer.value.remainingSeconds > 0) {
                delay(1_000)

                val updated = timer.value.remainingSeconds - 1
                timer.value = timer.value.copy(remainingSeconds = updated)
            }

            if (timer.value.isRunning && timer.value.remainingSeconds <= 0) {
                switchTimerMode()
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        timer.value = timer.value.copy(
            remainingSeconds = timer.value.totalSeconds,
            isRunning = false
        )
    }

    fun setTimerMode(isWork: Boolean) {
        if (timer.value.isWorkMode == isWork) return

        timerJob?.cancel()
        timer.value = timer.value.copy(
            isWorkMode = isWork,
            remainingSeconds = if (isWork) timer.value.workMinutes * 60 else timer.value.breakMinutes * 60,
            isRunning = false
        )
    }

    private fun switchTimerMode() {
        val current = timer.value
        val nextWorkMode = !current.isWorkMode
        val nextSeconds = if (nextWorkMode) {
            current.workMinutes * 60
        } else {
            current.breakMinutes * 60
        }

        timer.value = current.copy(
            isWorkMode = nextWorkMode,
            remainingSeconds = nextSeconds,
            isRunning = true
        )

        viewModelScope.launch {
            val currentMinutes = if (current.isWorkMode) {
                current.workMinutes
            } else {
                current.breakMinutes
            }

            val nextMinutes = if (nextWorkMode) {
                repository.getFocusMinutesToday() + currentMinutes
            } else {
                repository.getFocusMinutesToday()
            }

            repository.setFocusMinutesToday(nextMinutes)
        }
    }
}