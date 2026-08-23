package com.remindly.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remindly.app.ui.TaskViewModel
import com.remindly.app.ui.components.EmptyState
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.TaskRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")

/** Completion history, grouped by the day the task was finished. */
@Composable
fun DoneScreen(
    viewModel: TaskViewModel,
    contentPadding: PaddingValues,
    onOpenTask: (Long) -> Unit,
    showClearDialog: Boolean,
    onDismissClearDialog: () -> Unit
) {
    val done by viewModel.completedTasks.collectAsStateWithLifecycle()

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearDialog,
            title = { Text("Clear history?") },
            text = { Text("This permanently deletes all ${done.size} completed tasks.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    onDismissClearDialog()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearDialog) { Text("Cancel") }
            }
        )
    }

    if (done.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            title = "Nothing completed yet",
            subtitle = "Tasks you tick off — or that close automatically — are archived here.",
            modifier = Modifier.padding(contentPadding)
        )
        return
    }

    val grouped = done.groupBy { task ->
        Instant.ofEpochMilli(task.completedAt ?: task.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        )
    ) {
        item {
            Text(
                text = "${done.size} completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        grouped.forEach { (day, tasks) ->
            item(key = "h_$day") { SectionHeader(relativeDay(day)) }
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    isOverdue = false,
                    onToggle = { viewModel.toggleDone(task) }, // un-tick restores it to Active
                    onClick = { onOpenTask(task.id) }
                )
            }
        }
    }
}

private fun relativeDay(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(dayFormatter)
    }
}
