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
        get() = (if (isWorkMode) workMinutes else breakMinutes).coerceAtLeast(1) * 60
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
    private val _dashboardStats = MutableStateFlow(DashboardStats())

    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats.asStateFlow()
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(TaskFilter.ALL)
    val dialogState = MutableStateFlow(TaskDialogState())
    val timer = MutableStateFlow(TimerState())

    private var timerJob: Job? = null
    private var saveJob: Job? = null

    val filteredTasks = combine(_tasks, query, filter) { allTasks, rawQuery, selectedFilter ->
        val q = rawQuery.trim()
        allTasks.filter { task ->
            val matchesQuery = q.isEmpty() || task.title.contains(q, true) || task.note.contains(q, true)
            val matchesFilter = when (selectedFilter) {
                TaskFilter.ALL -> true
                TaskFilter.ACTIVE -> !task.done
                TaskFilter.COMPLETED -> task.done
            }
            matchesQuery && matchesFilter
        }.sortedWith(compareBy<Task> { it.done }.thenByDescending { it.priority }.thenByDescending { it.createdAt })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.resetDailyStatsIfNeeded()
            _tasks.value = repository.load()
            refreshDashboard()
        }
    }

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: TaskFilter) { filter.value = value }
    fun openAddDialog() { dialogState.value = TaskDialogState(visible = true) }
    fun openEditDialog(task: Task) {
        dialogState.value = TaskDialogState(true, task.id, task.title, task.note, task.priority)
    }
    fun dismissDialog() { dialogState.value = TaskDialogState() }
    fun onDialogTitleChange(value: String) { dialogState.value = dialogState.value.copy(title = value) }
    fun onDialogNoteChange(value: String) { dialogState.value = dialogState.value.copy(note = value) }
    fun onDialogPriorityChange(value: Int) { dialogState.value = dialogState.value.copy(priority = value.coerceIn(0, 2)) }

    fun saveDialog() {
        val state = dialogState.value
        val title = state.title.trim()
        if (title.isEmpty()) return
        val now = System.currentTimeMillis()
        val current = _tasks.value.toMutableList()
        if (state.editingId == null) {
            current += Task(now, title, state.note.trim(), state.priority, false, now)
        } else {
            val index = current.indexOfFirst { it.id == state.editingId }
            if (index >= 0) current[index] = current[index].copy(title = title, note = state.note.trim(), priority = state.priority)
        }
        persist(current)
        dismissDialog()
    }

    fun toggleDone(task: Task) {
        persist(_tasks.value.map { if (it.id == task.id) it.copy(done = !it.done) else it })
    }
    fun deleteTask(task: Task) { persist(_tasks.value.filterNot { it.id == task.id }) }

    private fun persist(newTasks: List<Task>) {
        _tasks.value = newTasks
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            repository.save(newTasks)
            refreshDashboard()
        }
    }

    fun startPauseTimer() {
        if (timer.value.isRunning) {
            timerJob?.cancel()
            timer.value = timer.value.copy(isRunning = false)
            return
        }
        if (timer.value.remainingSeconds <= 0) resetTimer()
        timer.value = timer.value.copy(isRunning = true)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timer.value.isRunning && timer.value.remainingSeconds > 0) {
                delay(1_000)
                timer.value = timer.value.copy(remainingSeconds = (timer.value.remainingSeconds - 1).coerceAtLeast(0))
            }
            if (timer.value.isRunning && timer.value.remainingSeconds == 0) completeTimerPhase()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        timer.value = timer.value.copy(remainingSeconds = timer.value.totalSeconds, isRunning = false)
    }

    fun setTimerMode(isWork: Boolean) {
        timerJob?.cancel()
        timer.value = timer.value.copy(
            isWorkMode = isWork,
            remainingSeconds = (if (isWork) timer.value.workMinutes else timer.value.breakMinutes).coerceAtLeast(1) * 60,
            isRunning = false
        )
    }

    private fun completeTimerPhase() {
        val current = timer.value
        viewModelScope.launch {
            if (current.isWorkMode) repository.recordFocusSession(current.workMinutes)
            refreshDashboard()
        }
        val nextWorkMode = !current.isWorkMode
        timer.value = current.copy(
            isWorkMode = nextWorkMode,
            remainingSeconds = (if (nextWorkMode) current.workMinutes else current.breakMinutes).coerceAtLeast(1) * 60,
            isRunning = false
        )
    }

    private suspend fun refreshDashboard() {
        _dashboardStats.value = repository.loadDashboardStats()
    }

    override fun onCleared() {
        timerJob?.cancel()
        saveJob?.cancel()
        super.onCleared()
    }
}
