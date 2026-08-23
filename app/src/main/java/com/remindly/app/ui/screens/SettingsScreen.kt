package com.remindly.app.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remindly.app.data.ThemeMode
import com.remindly.app.ui.TaskViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val AUTO_COMPLETE_CHOICES = listOf(0, 6, 12, 24, 72)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TaskViewModel, contentPadding: PaddingValues) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
    ) {
        SettingsSection("Reminders") {
            SettingsRow(
                title = "Default reminder time",
                subtitle = "All-day tasks notify at " +
                    LocalTime.of(settings.defaultHour, settings.defaultMinute).format(timeFormatter),
                onClick = { showTimePicker = true }
            )
            HorizontalDivider()
            Column(Modifier.padding(16.dp)) {
                Text("Auto-complete overdue tasks", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Open, non-repeating tasks close themselves once they are this far past due.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AUTO_COMPLETE_CHOICES.forEach { hours ->
                        FilterChip(
                            selected = settings.autoCompleteAfterHours == hours,
                            onClick = { viewModel.setAutoCompleteHours(hours) },
                            label = { Text(if (hours == 0) "Never" else "${hours}h") }
                        )
                    }
                }
            }
        }

        SettingsSection("Appearance") {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }

        SettingsSection("Permissions") {
            val exactAllowed = viewModel.canScheduleExactAlarms()
            SettingsRow(
                title = if (exactAllowed) "Exact alarms allowed" else "Allow exact alarms",
                subtitle = if (exactAllowed)
                    "Reminders fire at the exact minute you chose."
                else
                    "Without this, reminders may arrive up to 10 minutes late. Tap to open system settings.",
                icon = true,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exactAllowed) {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        )
                    }
                }
            )
            HorizontalDivider()
            SettingsRow(
                title = "Notification settings",
                subtitle = "Sound, importance and Do Not Disturb behaviour.",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            )
        }

        SettingsSection("About") {
            SettingsRow(
                title = "Remindly 1.0",
                subtitle = "Local-only reminders. Nothing leaves your device.",
                onClick = {}
            )
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = settings.defaultHour,
            initialMinute = settings.defaultMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Default reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDefaultTime(LocalTime.of(state.hour, state.minute))
                    showTimePicker = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { Column { content() } }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (icon) {
            Icon(
                Icons.Default.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
