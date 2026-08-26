package com.remindly.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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

/** Compact chip labels — the full ones wrap badly in a dialog. */
private fun RepeatRule.chipLabel(): String = when (this) {
    RepeatRule.NONE -> "Once"
    RepeatRule.HOURLY -> "1 hour"
    RepeatRule.EVERY_3_HOURS -> "3 hours"
    RepeatRule.EVERY_8_HOURS -> "8 hours"
    RepeatRule.CUSTOM -> "Custom"
    RepeatRule.DAILY -> "Daily"
    RepeatRule.WEEKDAYS -> "Weekdays"
    RepeatRule.WEEKLY -> "Weekly"
    RepeatRule.MONTHLY -> "Monthly"
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
    val isPast = LocalDateTime.of(date, time).isBefore(LocalDateTime.now())

    AlertDialog(
        onDismissRequest = onDismiss,
        // The platform default width squeezes the chips; go near-full-width instead.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("New reminder") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What is it?") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocus)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("When")

                    // Date and time side by side, equal width — reads as one control.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = date.format(dateFormatter),
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = time.format(timeFormatter),
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = date == today,
                            onClick = { date = today },
                            label = { Text("Today", maxLines = 1) }
                        )
                        FilterChip(
                            selected = date == today.plusDays(1),
                            onClick = { date = today.plusDays(1) },
                            label = { Text("Tomorrow", maxLines = 1) }
                        )
                        FilterChip(
                            selected = date == today.plusWeeks(1),
                            onClick = { date = today.plusWeeks(1) },
                            label = { Text("Next week", maxLines = 1) }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Repeat")
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
                                label = { Text(option.chipLabel(), maxLines = 1) }
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Plain-language confirmation of what is about to be saved.
                Text(
                    text = when {
                        isPast -> "That moment has already passed — the first reminder will be skipped."
                        repeat == RepeatRule.NONE ->
                            "Reminds once on ${date.format(dateFormatter)} at ${time.format(timeFormatter)}."
                        else ->
                            "Starts ${date.format(dateFormatter)} at ${time.format(timeFormatter)}, " +
                                "then repeats ${repeatSummary(repeat, customMinutes.toIntOrNull())}."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPast) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

private fun repeatSummary(rule: RepeatRule, customMinutes: Int?): String = when (rule) {
    RepeatRule.NONE -> "never"
    RepeatRule.HOURLY -> "every hour"
    RepeatRule.EVERY_3_HOURS -> "every 3 hours"
    RepeatRule.EVERY_8_HOURS -> "every 8 hours"
    RepeatRule.DAILY -> "every day"
    RepeatRule.WEEKDAYS -> "every weekday"
    RepeatRule.WEEKLY -> "every week"
    RepeatRule.MONTHLY -> "every month"
    RepeatRule.CUSTOM -> when {
        customMinutes == null || customMinutes <= 0 -> "on a custom interval"
        customMinutes % 60 == 0 -> "every ${customMinutes / 60}h"
        else -> "every $customMinutes minutes"
    }
}
