package com.remindly.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** How often a reminder repeats after it fires. */
enum class RepeatRule(val label: String) {
    NONE("Does not repeat"),
    DAILY("Daily"),
    WEEKDAYS("Every weekday"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly");

    /** Returns the next due date after [from], or null when the task does not repeat. */
    fun next(from: LocalDate): LocalDate? = when (this) {
        NONE -> null
        DAILY -> from.plusDays(1)
        WEEKDAYS -> generateSequence(from.plusDays(1)) { it.plusDays(1) }
            .first { it.dayOfWeek.value <= 5 }
        WEEKLY -> from.plusWeeks(1)
        MONTHLY -> from.plusMonths(1)
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

    /** Advances a repeating task to its next occurrence, or returns null if it does not repeat. */
    fun nextOccurrence(): Task? {
        val nextDate = repeat.next(dueDate) ?: return null
        return copy(id = id, dueDate = nextDate, isDone = false, completedAt = null, autoCompleted = false)
    }
}
