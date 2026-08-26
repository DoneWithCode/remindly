package com.remindly.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * How a reminder repeats.
 *
 * Two kinds live here. Interval rules ([fixedIntervalMinutes] set, or CUSTOM) fire
 * every N minutes and can repeat many times within a day. Date rules advance the
 * due date and keep the same time of day.
 */
enum class RepeatRule(val label: String, val fixedIntervalMinutes: Int? = null) {
    NONE("Once"),

    // Interval-based
    HOURLY("Every hour", 60),
    EVERY_3_HOURS("Every 3 hours", 180),
    EVERY_8_HOURS("Every 8 hours", 480),
    CUSTOM("Custom"),

    // Date-based
    DAILY("Daily"),
    WEEKDAYS("Weekdays"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    val isInterval: Boolean get() = fixedIntervalMinutes != null || this == CUSTOM

    /** Resolves the gap in minutes, falling back to the task's own value for CUSTOM. */
    fun intervalMinutes(custom: Int?): Int? =
        if (this == CUSTOM) custom?.takeIf { it > 0 } else fixedIntervalMinutes

    /** Next due date for the date-based rules; null for interval rules and NONE. */
    fun next(from: LocalDate): LocalDate? = when (this) {
        DAILY -> from.plusDays(1)
        WEEKDAYS -> generateSequence(from.plusDays(1)) { it.plusDays(1) }
            .first { it.dayOfWeek.value <= 5 }
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1)
        else -> null
    }
}

/** A lightweight tag so the list can be grouped and filtered at a glance. */
enum class Category(val label: String) {
    GENERAL("General"),
    WORK("Work"),
    PERSONAL("Personal"),
    HEALTH("Health"),
    SHOPPING("Shopping")
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val description: String? = null,

    /** Stored as epoch-day so it sorts and compares cheaply in SQL. */
    @ColumnInfo(name = "dueDate")
    val dueDate: LocalDate,

    /** Null means "all-day": the reminder fires at the default time set in Settings. */
    @ColumnInfo(name = "dueTime")
    val dueTime: LocalTime? = null,

    val category: Category = Category.GENERAL,
    val repeat: RepeatRule = RepeatRule.NONE,

    /** Gap in minutes when [repeat] is CUSTOM. Ignored otherwise. */
    val repeatIntervalMinutes: Int? = null,

    val isDone: Boolean = false,
    /** Epoch millis of completion; drives the ordering of the Done history. */
    val completedAt: Long? = null,
    /** True when the auto-complete rule closed the task rather than the user. */
    val autoCompleted: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
) {
    fun isAllDay(): Boolean = dueTime == null

    /**
     * The moment this task should notify.
     * All-day tasks resolve against the user's default reminder time.
     */
    fun triggerAt(defaultHour: Int, defaultMinute: Int): Long {
        val time = dueTime ?: LocalTime.of(defaultHour, defaultMinute)
        return LocalDateTime.of(dueDate, time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun isOverdue(defaultHour: Int, defaultMinute: Int, now: Long = System.currentTimeMillis()): Boolean =
        !isDone && triggerAt(defaultHour, defaultMinute) < now

    /**
     * Advances a repeating task to its next occurrence, or returns null if it does not repeat.
     *
     * Interval rules catch up: if the phone was off for a day, the next occurrence lands in
     * the future rather than firing a backlog of missed alarms all at once.
     */
    fun nextOccurrence(
        defaultHour: Int = 9,
        defaultMinute: Int = 0,
        now: LocalDateTime = LocalDateTime.now()
    ): Task? {
        val interval = repeat.intervalMinutes(repeatIntervalMinutes)

        if (interval != null) {
            val base = LocalDateTime.of(dueDate, dueTime ?: LocalTime.of(defaultHour, defaultMinute))
            var next = base.plusMinutes(interval.toLong())
            while (next.isBefore(now)) next = next.plusMinutes(interval.toLong())
            return copy(
                dueDate = next.toLocalDate(),
                dueTime = next.toLocalTime(),
                isDone = false,
                completedAt = null,
                autoCompleted = false
            )
        }

        val time = dueTime ?: LocalTime.of(defaultHour, defaultMinute)
        var nextDate = repeat.next(dueDate) ?: return null
        // Missed a few days? Skip to the next slot that is actually in the future.
        while (LocalDateTime.of(nextDate, time).isBefore(now)) {
            nextDate = repeat.next(nextDate) ?: return null
        }
        return copy(dueDate = nextDate, isDone = false, completedAt = null, autoCompleted = false)
    }
}
