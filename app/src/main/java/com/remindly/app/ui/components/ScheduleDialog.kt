package com.remindly.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.remindly.app.data.RepeatRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/** The next :00 or :30 boundary, so a new reminder always starts on a tidy future slot. */
fun nextSlot(now: LocalDateTime = LocalDateTime.now()): LocalDateTime {
    val base = now.withSecond(0).withNano(0)
    return if (base.minute < 30) base.withMinute(30) else base.plusHours(1).withMinute(0)
}

/** What the dialog hands back once the user presses Schedule. */
data class ScheduleResult(
    val title: String,
    val date: LocalDate,
    val time: LocalTime,
    val repeat: RepeatRule,
    val intervalMinutes: Int?
)

/**
 * Asks when a task should remind before it is saved, so nothing silently
 * inherits a default time the user never chose.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun ScheduleDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleResult) -> Unit
) {
    val slot = remember { nextSlot() }

    var title by remember { mutableStateOf(initialTitle) }
    var date by remember { mutableStateOf(slot.toLocalDate()) }
    var time by remember { mutableStateOf(slot.toLocalTime()) }
    var repeat by remember { mutableStateOf(RepeatRule.NONE) }
    var customMinutes by remember { mutableStateOf("30") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Opened from an empty quick-add box? Put the cursor in the title straight away.
    val titleFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (initialTitle.isBlank()) {
            runCatching {
                titleFocus.requestFocus()
                keyboard?.show()
            }
        }
    }

    val today = LocalDate.now()
    val intervalValid = repeat != RepeatRule.CUSTOM || (customMinutes.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("When should this remind you?") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocus)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = date == today,
                        onClick = { date = today },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = date == today.plusDays(1),
                        onClick = { date = today.plusDays(1) },
                        label = { Text("Tomorrow") }
                    )
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(date.format(dateFormatter), maxLines = 1) }
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)
                    )
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = { Text(time.format(timeFormatter)) }
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        RepeatRule.NONE,
                        RepeatRule.HOURLY,
                        RepeatRule.EVERY_3_HOURS,
                        RepeatRule.EVERY_8_HOURS,
                        RepeatRule.DAILY,
                        RepeatRule.CUSTOM
                    ).forEach { option ->
                        FilterChip(
                            selected = repeat == option,
                            onClick = { repeat = option },
                            label = { Text(option.label, maxLines = 1) }
                        )
                    }
                }

                if (repeat == RepeatRule.CUSTOM) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { input ->
                            customMinutes = input.filter { it.isDigit() }.take(5)
                        },
                        label = { Text("Every N minutes") },
                        singleLine = true,
                        isError = !intervalValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (LocalDateTime.of(date, time).isBefore(LocalDateTime.now())) {
                    Text(
                        text = "That moment has already passed — the first reminder will be skipped.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && intervalValid,
                onClick = {
                    onConfirm(
                        ScheduleResult(
                            title = title.trim(),
                            date = date,
                            time = time,
                            repeat = repeat,
                            intervalMinutes = if (repeat == RepeatRule.CUSTOM) {
                                customMinutes.toIntOrNull()
                            } else {
                                null
                            }
                        )
                    )
                }
            ) { Text("Schedule") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
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
            initialHour = time.hour,
            initialMinute = time.minute,
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
}
