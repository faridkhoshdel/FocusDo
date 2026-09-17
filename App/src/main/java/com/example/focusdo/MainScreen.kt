package com.example.focusdo

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var tab by remember { mutableStateOf(0) }
    val tasks by viewModel.filteredTasks.collectAsState()
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val dialog by viewModel.dialogState.collectAsState()
    val timer by viewModel.timer.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (tab == 0) FloatingActionButton(onClick = viewModel::openAddDialog) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text(stringResource(R.string.tasks)) })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.PlayArrow, null) }, label = { Text(stringResource(R.string.focus)) })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text(stringResource(R.string.dashboard)) })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> TasksScreen(padding, tasks, query, filter, viewModel::setQuery, viewModel::setFilter, viewModel::toggleDone, viewModel::deleteTask, viewModel::openEditDialog)
            1 -> FocusScreen(padding, timer, viewModel::startPauseTimer, viewModel::resetTimer, viewModel::setTimerMode)
            2 -> DashboardScreen(stats)
            else -> SettingsScreen(padding)
        }
    }
    if (dialog.visible) TaskDialog(dialog, viewModel::dismissDialog, viewModel::onDialogTitleChange, viewModel::onDialogNoteChange, viewModel::onDialogPriorityChange, viewModel::saveDialog)
}

@Composable
private fun TasksScreen(padding: PaddingValues, tasks: List<Task>, query: String, filter: TaskFilter, onQuery: (String) -> Unit, onFilter: (TaskFilter) -> Unit, onToggle: (Task) -> Unit, onDelete: (Task) -> Unit, onEdit: (Task) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search)) }, singleLine = true)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter == TaskFilter.ALL, { onFilter(TaskFilter.ALL) }, label = { Text(stringResource(R.string.all)) })
            FilterChip(filter == TaskFilter.ACTIVE, { onFilter(TaskFilter.ACTIVE) }, label = { Text(stringResource(R.string.active)) })
            FilterChip(filter == TaskFilter.COMPLETED, { onFilter(TaskFilter.COMPLETED) }, label = { Text(stringResource(R.string.completed)) })
        }
        if (tasks.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_tasks)) }
        else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.done, onCheckedChange = { onToggle(task) })
                        Column(Modifier.weight(1f).clickable { onEdit(task) }.padding(horizontal = 8.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (task.note.isNotBlank()) Text(task.note, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onDelete(task) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskDialog(state: TaskDialogState, dismiss: () -> Unit, title: (String) -> Unit, note: (String) -> Unit, priority: (Int) -> Unit, save: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (state.editingId == null) stringResource(R.string.add_task) else stringResource(R.string.edit_task)) }, text = {
        Column {
            OutlinedTextField(value = state.title, onValueChange = title, label = { Text(stringResource(R.string.task_title)) }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = state.note, onValueChange = note, label = { Text(stringResource(R.string.note)) }, minLines = 2)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0, 1, 2).forEach { value -> FilterChip(state.priority == value, { priority(value) }, label = { Text(when (value) { 0 -> stringResource(R.string.low); 2 -> stringResource(R.string.high); else -> stringResource(R.string.medium) }) }) }
            }
        }
    }, confirmButton = { TextButton(onClick = save, enabled = state.title.isNotBlank()) { Text(stringResource(R.string.save)) } }, dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun FocusScreen(padding: PaddingValues, timer: TimerState, start: () -> Unit, reset: () -> Unit, mode: (Boolean) -> Unit) {
    val progress = (timer.remainingSeconds.toFloat() / timer.totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.focus_mode), style = MaterialTheme.typography.titleLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(timer.isWorkMode, { mode(true) }, label = { Text(stringResource(R.string.work)) })
            FilterChip(!timer.isWorkMode, { mode(false) }, label = { Text(stringResource(R.string.break_)) })
        }
        Text(String.format(java.util.Locale.US, "%02d:%02d", timer.remainingSeconds / 60, timer.remainingSeconds % 60), style = MaterialTheme.typography.displayLarge)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = start) { Icon(if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (timer.isRunning) stringResource(R.string.pause) else stringResource(R.string.start)) }
            OutlinedButton(onClick = reset) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.reset)) }
        }
    }
}

@Composable
private fun SettingsScreen(padding: PaddingValues) {
    val locales = AppCompatDelegate.getApplicationLocales()
    val language = if (locales.isEmpty()) "system" else locales.toLanguageTags()
    val theme = when (AppCompatDelegate.getDefaultNightMode()) { AppCompatDelegate.MODE_NIGHT_NO -> "light"; AppCompatDelegate.MODE_NIGHT_YES -> "dark"; else -> "system" }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        SettingRow(stringResource(R.string.system), language == "system") { setLanguage("system") }
        SettingRow(stringResource(R.string.english), language.startsWith("en")) { setLanguage("en") }
        SettingRow(stringResource(R.string.persian), language.startsWith("fa")) { setLanguage("fa") }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
        SettingRow(stringResource(R.string.system), theme == "system") { setTheme("system") }
        SettingRow(stringResource(R.string.light), theme == "light") { setTheme("light") }
        SettingRow(stringResource(R.string.dark), theme == "dark") { setTheme("dark") }
    }
}

@Composable private fun SettingRow(label: String, selected: Boolean, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = selected, onClick = action); Spacer(Modifier.width(8.dp)); Text(label) }
}
private fun setLanguage(tag: String) { AppCompatDelegate.setApplicationLocales(if (tag == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)) }
private fun setTheme(theme: String) { AppCompatDelegate.setDefaultNightMode(when (theme) { "light" -> AppCompatDelegate.MODE_NIGHT_NO; "dark" -> AppCompatDelegate.MODE_NIGHT_YES; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }) }
