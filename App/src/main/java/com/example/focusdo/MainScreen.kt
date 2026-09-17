package com.example.focusdo

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    val tasks by viewModel.filteredTasks.collectAsState()
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val dialog by viewModel.dialogState.collectAsState()
    val timer by viewModel.timer.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = viewModel::openAddDialog,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_task)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.tasks)
                        )
                    },
                    label = { Text(stringResource(R.string.tasks)) }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.focus)
                        )
                    },
                    label = { Text(stringResource(R.string.focus)) }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.dashboard)
                        )
                    },
                    label = { Text(stringResource(R.string.dashboard)) }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.about)
                        )
                    },
                    label = { Text(stringResource(R.string.about)) }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> TasksScreen(
                padding = padding,
                tasks = tasks,
                query = query,
                filter = filter,
                onQueryChange = viewModel::setQuery,
                onFilterChange = viewModel::setFilter,
                onToggleDone = viewModel::toggleDone,
                onDelete = viewModel::deleteTask,
                onEdit = viewModel::openEditDialog
            )

            1 -> FocusScreen(
                padding = padding,
                timer = timer,
                onStartPause = viewModel::startPauseTimer,
                onReset = viewModel::resetTimer,
                onModeChange = viewModel::setTimerMode
            )

            2 -> DashboardScreen()
            3 -> AboutScreen()
        }
    }

    if (dialog.visible) {
        AddEditTaskDialog(
            state = dialog,
            onDismiss = viewModel::dismissDialog,
            onTitleChange = viewModel::onDialogTitleChange,
            onNoteChange = viewModel::onDialogNoteChange,
            onPriorityChange = viewModel::onDialogPriorityChange,
            onSave = viewModel::saveDialog
        )
    }
}

@Composable
private fun TasksScreen(
    padding: PaddingValues,
    tasks: List<Task>,
    query: String,
    filter: TaskFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (TaskFilter) -> Unit,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.search)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == TaskFilter.ALL,
                onClick = { onFilterChange(TaskFilter.ALL) },
                label = { Text(stringResource(R.string.all)) }
            )

            FilterChip(
                selected = filter == TaskFilter.ACTIVE,
                onClick = { onFilterChange(TaskFilter.ACTIVE) },
                label = { Text(stringResource(R.string.active)) }
            )

            FilterChip(
                selected = filter == TaskFilter.COMPLETED,
                onClick = { onFilterChange(TaskFilter.COMPLETED) },
                label = { Text(stringResource(R.string.completed)) }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_tasks))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleDone = onToggleDone,
                        onDelete = onDelete,
                        onEdit = onEdit
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit
) {
    val priorityText = when (task.priority) {
        0 -> stringResource(R.string.low)
        2 -> stringResource(R.string.high)
        else -> stringResource(R.string.medium)
    }

    val priorityColor = when (task.priority) {
        0 -> MaterialTheme.colorScheme.tertiary
        2 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggleDone(task) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit(task) }
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )

                    Text(
                        text = priorityText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            IconButton(onClick = { onDelete(task) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }
    }
}

@Composable
private fun AddEditTaskDialog(
    state: TaskDialogState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPriorityChange: (Int) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (state.editingId == null) stringResource(R.string.add_task)
                else stringResource(R.string.edit_task)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.task_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.note)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    stringResource(R.string.priority),
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = state.priority == 0,
                        onClick = { onPriorityChange(0) },
                        label = { Text(stringResource(R.string.low)) }
                    )

                    FilterChip(
                        selected = state.priority == 1,
                        onClick = { onPriorityChange(1) },
                        label = { Text(stringResource(R.string.medium)) }
                    )

                    FilterChip(
                        selected = state.priority == 2,
                        onClick = { onPriorityChange(2) },
                        label = { Text(stringResource(R.string.high)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = state.title.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun FocusScreen(
    padding: PaddingValues,
    timer: TimerState,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    onModeChange: (Boolean) -> Unit
) {
    val progress = if (timer.totalSeconds == 0) {
        0f
    } else {
        timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.focus_mode),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = timer.isWorkMode,
                onClick = { onModeChange(true) },
                label = { Text(stringResource(R.string.work)) }
            )

            FilterChip(
                selected = !timer.isWorkMode,
                onClick = { onModeChange(false) },
                label = { Text(stringResource(R.string.break_)) }
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            formatSeconds(timer.remainingSeconds),
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStartPause,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (timer.isRunning) stringResource(R.string.pause)
                    else stringResource(R.string.start)
                )
            }

            OutlinedButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.reset))
            }
        }
    }
}

@Composable
private fun SettingsScreen(padding: PaddingValues) {
    val locales = AppCompatDelegate.getApplicationLocales()
    val currentLanguage = if (locales.isEmpty()) "system" else locales.toLanguageTags()

    val currentNightMode = AppCompatDelegate.getDefaultNightMode()
    val currentTheme = when (currentNightMode) {
        AppCompatDelegate.MODE_NIGHT_NO -> "light"
        AppCompatDelegate.MODE_NIGHT_YES -> "dark"
        else -> "system"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.language),
            style = MaterialTheme.typography.titleMedium
        )

        OptionRow(
            label = stringResource(R.string.system),
            selected = currentLanguage == "system",
            onClick = { setLanguage("system") }
        )

        OptionRow(
            label = stringResource(R.string.english),
            selected = currentLanguage.startsWith("en"),
            onClick = { setLanguage("en") }
        )

        OptionRow(
            label = stringResource(R.string.persian),
            selected = currentLanguage.startsWith("fa"),
            onClick = { setLanguage("fa") }
        )

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.theme),
            style = MaterialTheme.typography.titleMedium
        )

        OptionRow(
            label = stringResource(R.string.system),
            selected = currentTheme == "system",
            onClick = { setTheme("system") }
        )

        OptionRow(
            label = stringResource(R.string.light),
            selected = currentTheme == "light",
            onClick = { setTheme("light") }
        )

        OptionRow(
            label = stringResource(R.string.dark),
            selected = currentTheme == "dark",
            onClick = { setTheme("dark") }
        )
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun setLanguage(tag: String) {
    if (tag == "system") {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}

private fun setTheme(theme: String) {
    val mode = when (theme) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode)
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", m, s)
}