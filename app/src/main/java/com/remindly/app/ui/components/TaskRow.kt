package com.remindly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remindly.app.data.RepeatRule
import com.remindly.app.data.Task
import com.remindly.app.ui.theme.color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

/** Human-friendly due label: "Today · 9:00 AM", "Overdue · Mon, 3 Mar", "Tomorrow". */
fun Task.dueLabel(today: LocalDate = LocalDate.now()): String {
    val datePart = when (dueDate) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> dueDate.format(dateFormatter)
    }
    val timePart = dueTime?.format(timeFormatter)
    return if (timePart != null) "$datePart · $timePart" else "$datePart · All day"
}

@Composable
fun TaskRow(
    task: Task,
    isOverdue: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })

            Column(
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(task.category.color(), CircleShape)
                    )
                    Text(
                        text = task.dueLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue && !task.isDone) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.repeat != RepeatRule.NONE) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = task.repeat.label,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (task.autoCompleted) {
                    Text(
                        text = "Auto-completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
