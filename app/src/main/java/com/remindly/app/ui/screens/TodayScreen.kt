package com.remindly.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remindly.app.data.Task
import com.remindly.app.ui.TaskViewModel
import com.remindly.app.ui.components.EmptyState
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.TaskRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val headerFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM")

/**
 * The home screen: a one-line quick-add box, everything due today (overdue first),
 * then a preview of what is coming up.
 */
@Composable
fun TodayScreen(
    viewModel: TaskViewModel,
    contentPadding: PaddingValues,
    onOpenTask: (Long) -> Unit
) {
    val today by viewModel.todayTasks.collectAsStateWithLifecycle()
    val upcoming by viewModel.upcomingTasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val date = LocalDate.now()
    val overdue = today.filter { it.dueDate < date }
    val dueToday = today.filter { it.dueDate == date }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp
        )
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = date.format(headerFormatter),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = summaryLine(overdue, dueToday),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { QuickAddBar(onAdd = { viewModel.quickAdd(it) }) }

        if (overdue.isNotEmpty()) {
            item { SectionHeader("Overdue") }
            items(overdue, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    isOverdue = true,
                    onToggle = { viewModel.toggleDone(task) },
                    onClick = { onOpenTask(task.id) }
                )
            }
        }

        item { SectionHeader("Today") }
        if (dueToday.isEmpty()) {
            item {
                Text(
                    text = "Nothing scheduled for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        } else {
            items(dueToday, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    isOverdue = task.isOverdue(settings.defaultHour, settings.defaultMinute),
                    onToggle = { viewModel.toggleDone(task) },
                    onClick = { onOpenTask(task.id) }
                )
            }
        }

        if (upcoming.isNotEmpty()) {
            item { SectionHeader("Upcoming") }
            items(upcoming, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    isOverdue = false,
                    onToggle = { viewModel.toggleDone(task) },
                    onClick = { onOpenTask(task.id) }
                )
            }
        }

        if (today.isEmpty() && upcoming.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.WbSunny,
                    title = "All clear",
                    subtitle = "Add your first reminder with the + button or the quick-add box above."
                )
            }
        }
    }
}

private fun summaryLine(overdue: List<Task>, dueToday: List<Task>): String = when {
    overdue.isEmpty() && dueToday.isEmpty() -> "No reminders due today"
    overdue.isEmpty() -> "${dueToday.size} due today"
    else -> "${dueToday.size} due today · ${overdue.size} overdue"
}

/** Type a title, hit enter, done — the task lands on today at the default reminder time. */
@Composable
private fun QuickAddBar(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Quick add for today…") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        onAdd(text)
                        text = ""
                    }
                )
            )
            IconButton(
                onClick = {
                    onAdd(text)
                    text = ""
                },
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    }
}
