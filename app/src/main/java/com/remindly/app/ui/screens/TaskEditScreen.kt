package com.remindly.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.remindly.app.data.Category
import com.remindly.app.data.RepeatRule
import com.remindly.app.data.Task
import com.remindly.app.ui.TaskViewModel
import com.remindly.app.ui.components.nextSlot
import com.remindly.app.ui.theme.color
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/**
 * One screen for both creating and editing. [taskId] of 0 means "new task".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskViewModel,
    taskId: Long,
    contentPadding: PaddingValues,
    onDone: () -> Unit
) {
    var loaded by remember { mutableStateOf(taskId == 0L) }
    var existing by remember { mutableStateOf<Task?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val startSlot = remember { nextSlot() }
    var date by remember { mutableStateOf(startSlot.toLocalDate()) }
    var time by remember { mutableStateOf<LocalTime?>(startSlot.toLocalTime()) }
    var category by remember { mutableStateOf(Category.GENERAL) }
    var repeat by remember { mutableStateOf(RepeatRule.NONE) }
    var customMinutes by remember { mutableStateOf("30") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId != 0L) {
            viewModel.loadTask(taskId)?.let { task ->
                existing = task
                title = task.title
                description = task.description.orEmpty()
                date = task.dueDate
                time = task.dueTime
                category = task.category
                repeat = task.repeat
                task.repeatIntervalMinutes?.let { customMinutes = it.toString() }
            }
            loaded = true
        }
    }

    Scaffold(
        modifier = Modifier.padding(contentPadding),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(if (taskId == 0L) "New reminder" else "Edit reminder") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            FieldLabel("When")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(date.format(dateFormatter)) }
                )
                AssistChip(
                    onClick = { showTimePicker = true },
                    label = { Text(time?.format(timeFormatter) ?: "All day") }
                )
                if (time != null) {
                    TextButton(onClick = { time = null }) { Text("Clear time") }
                }
            }

            // One-tap shortcuts covering the majority of real scheduling.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickDateChip("Today", LocalDate.now(), date) { date = it }
                QuickDateChip("Tomorrow", LocalDate.now().plusDays(1), date) { date = it }
                QuickDateChip("Next week", LocalDate.now().plusWeeks(1), date) { date = it }
            }

            FieldLabel("Category")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Category.entries.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = option.color().copy(alpha = 0.20f)
                        )
                    )
                }
            }

            FieldLabel("Repeat")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RepeatRule.NONE,
                    RepeatRule.HOURLY,
                    RepeatRule.EVERY_3_HOURS,
                    RepeatRule.EVERY_8_HOURS,
                    RepeatRule.CUSTOM
                ).forEach { option ->
                    FilterChip(
                        selected = repeat == option,
                        onClick = { repeat = option },
                        label = { Text(option.label) }
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RepeatRule.DAILY,
                    RepeatRule.WEEKDAYS,
                    RepeatRule.WEEKLY,
                    RepeatRule.MONTHLY
                ).forEach { option ->
                    FilterChip(
                        selected = repeat == option,
                        onClick = { repeat = option },
                        label = { Text(option.label) }
                    )
                }
            }

            if (repeat == RepeatRule.CUSTOM) {
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { input -> customMinutes = input.filter { it.isDigit() }.take(5) },
                    label = { Text("Repeat every N minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text(describeMinutes(customMinutes.toIntOrNull())) },
                    isError = (customMinutes.toIntOrNull() ?: 0) <= 0,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (repeat.isInterval) {
                Text(
                    text = "Interval reminders start from the date and time above, then repeat " +
                        "until you delete the task. Set a time so the first one lands where you expect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    val task = (existing ?: Task(title = "", dueDate = date)).copy(
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        dueDate = date,
                        dueTime = time,
                        category = category,
                        repeat = repeat,
                        repeatIntervalMinutes =
                            if (repeat == RepeatRule.CUSTOM) customMinutes.toIntOrNull() else null,
                        // Re-opening a completed task from the editor puts it back in Active.
                        isDone = false,
                        completedAt = null,
                        autoCompleted = false
                    )
                    viewModel.save(task)
                    onDone()
                },
                enabled = title.isNotBlank() &&
                    (repeat != RepeatRule.CUSTOM || (customMinutes.toIntOrNull() ?: 0) > 0),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) { Text("Save reminder") }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        // DatePicker returns UTC midnight; read it back in UTC to avoid an off-by-one day.
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = time?.hour ?: 9,
            initialMinute = time?.minute ?: 0,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete reminder?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    existing?.let { viewModel.delete(it) }
                    showDeleteConfirm = false
                    onDone()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickDateChip(
    label: String,
    value: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) }
    )
}

/** Turns a raw minute count into something readable under the custom field. */
private fun describeMinutes(minutes: Int?): String = when {
    minutes == null || minutes <= 0 -> "Enter a number greater than zero"
    minutes < 60 -> "Every $minutes minutes"
    minutes % 60 == 0 && minutes < 1440 -> {
        val h = minutes / 60
        if (h == 1) "Every hour" else "Every $h hours"
    }
    minutes % 1440 == 0 -> {
        val d = minutes / 1440
        if (d == 1) "Every day" else "Every $d days"
    }
    else -> "Every ${minutes / 60}h ${minutes % 60}m"
}
