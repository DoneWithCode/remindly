package com.remindly.app.data

import com.remindly.app.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Single source of truth for tasks. Every write also keeps the alarm for that
 * task in sync, so no caller has to remember to schedule or cancel anything.
 */
class TaskRepository(
    private val dao: TaskDao,
    private val settingsStore: SettingsStore,
    private val scheduler: AlarmScheduler
) {
    val settings: Flow<AppSettings> = settingsStore.settings

    fun activeTasks(): Flow<List<Task>> = dao.observeActive()
    fun todayAndOverdue(today: LocalDate = LocalDate.now()): Flow<List<Task>> =
        dao.observeTodayAndOverdue(today.toEpochDay())
    fun upcoming(today: LocalDate = LocalDate.now()): Flow<List<Task>> =
        dao.observeUpcoming(today.toEpochDay(), limit = 25)
    fun completedTasks(): Flow<List<Task>> = dao.observeCompleted()
    fun dueTodayCount(today: LocalDate = LocalDate.now()): Flow<Int> =
        dao.countDueToday(today.toEpochDay())

    suspend fun getTask(id: Long): Task? = dao.getById(id)

    suspend fun upsert(task: Task): Long {
        val id = if (task.id == 0L) dao.insert(task) else {
            dao.update(task); task.id
        }
        rescheduleAlarm(task.copy(id = id))
        return id
    }

    suspend fun delete(task: Task) {
        scheduler.cancel(task.id)
        dao.delete(task)
    }

    /**
     * Completing a repeating task rolls it forward to its next occurrence instead of
     * closing it, so a daily reminder keeps working forever.
     */
    suspend fun setDone(task: Task, done: Boolean, auto: Boolean = false) {
        if (done) {
            val next = advanceRecurrence(task)
            if (next != null) {
                dao.update(next)
                rescheduleAlarm(next)
            } else {
                dao.markDone(task.id, System.currentTimeMillis(), auto)
                scheduler.cancel(task.id)
            }
        } else {
            dao.markActive(task.id)
            rescheduleAlarm(task.copy(isDone = false))
        }
    }

    suspend fun setDone(taskId: Long, done: Boolean, auto: Boolean = false) {
        dao.getById(taskId)?.let { setDone(it, done, auto) }
    }

    suspend fun clearCompletedHistory() = dao.clearCompleted()

    /** Rolls a repeating task to its next occurrence using the user's default time. */
    suspend fun advanceRecurrence(task: Task): Task? {
        val s = settingsStore.settings.first()
        return task.nextOccurrence(s.defaultHour, s.defaultMinute)
    }

    /**
     * Re-arms every open task, rolling any missed recurrences forward first.
     * Called on app start, after a reboot, an app update or a clock change.
     */
    suspend fun rescheduleAll() {
        dao.getActiveOnce().forEach { task -> rescheduleAlarm(task) }
    }

    /**
     * Auto-complete rule: an open, non-repeating task whose due moment passed more than
     * [AppSettings.autoCompleteAfterHours] ago is closed automatically and filed under Done.
     * Returns the number of tasks closed.
     */
    suspend fun runAutoComplete(now: Long = System.currentTimeMillis()): Int {
        val s = settingsStore.settings.first()
        if (s.autoCompleteAfterHours <= 0) return 0
        val cutoffMillis = s.autoCompleteAfterHours * 60L * 60L * 1000L
        var closed = 0
        dao.getActiveOnce()
            .filter { it.repeat == RepeatRule.NONE }
            .filter { now - it.triggerAt(s.defaultHour, s.defaultMinute) > cutoffMillis }
            .forEach {
                dao.markDone(it.id, now, auto = true)
                scheduler.cancel(it.id)
                closed++
            }
        return closed
    }

    private suspend fun rescheduleAlarm(task: Task) {
        val s = settingsStore.settings.first()
        scheduler.cancel(task.id)
        if (task.isDone) return

        // AlarmManager cannot fire in the past. A repeating task whose moment has
        // already gone by — created a minute late, or missed while the phone was off —
        // would otherwise sit overdue forever with no alarm attached. Roll it forward.
        val stale = task.triggerAt(s.defaultHour, s.defaultMinute) <= System.currentTimeMillis()
        if (stale && task.repeat != RepeatRule.NONE) {
            val next = task.nextOccurrence(s.defaultHour, s.defaultMinute)
            if (next != null) {
                dao.update(next)
                scheduler.schedule(next, s.defaultHour, s.defaultMinute)
                return
            }
        }

        scheduler.schedule(task, s.defaultHour, s.defaultMinute)
    }
}
