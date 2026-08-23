package com.remindly.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remindly.app.data.Category
import com.remindly.app.ui.TaskViewModel
import com.remindly.app.ui.components.EmptyState
import com.remindly.app.ui.components.SectionHeader
import com.remindly.app.ui.components.TaskRow
import com.remindly.app.ui.theme.color
import java.time.LocalDate

/** Every open task in one list, with a category filter across the top. */
@Composable
fun ActiveScreen(
    viewModel: TaskViewModel,
    contentPadding: PaddingValues,
    onOpenTask: (Long) -> Unit
) {
    val tasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val filter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp
        )
    ) {
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("All") }
                )
                Category.entries.forEach { category ->
                    FilterChip(
                        selected = filter == category,
                        onClick = {
                            viewModel.setCategoryFilter(if (filter == category) null else category)
                        },
                        label = { Text(category.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = category.color().copy(alpha = 0.20f)
                        )
                    )
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.ChecklistRtl,
                    title = "No active tasks",
                    subtitle = "Tasks you create will appear here until they are completed."
                )
            }
        } else {
            // Group by relative date so long lists stay scannable.
            val grouped = tasks.groupBy {
                when {
                    it.dueDate < today -> "Overdue"
                    it.dueDate == today -> "Today"
                    it.dueDate == today.plusDays(1) -> "Tomorrow"
                    it.dueDate <= today.plusDays(7) -> "This week"
                    else -> "Later"
                }
            }
            listOf("Overdue", "Today", "Tomorrow", "This week", "Later").forEach { section ->
                val group = grouped[section].orEmpty()
                if (group.isNotEmpty()) {
                    item(key = "header_$section") { SectionHeader(section) }
                    items(group, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            isOverdue = task.isOverdue(settings.defaultHour, settings.defaultMinute),
                            onToggle = { viewModel.toggleDone(task) },
                            onClick = { onOpenTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}
