package com.remindly.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.remindly.app.data.Task

/**
 * Wraps AlarmManager. One alarm per task, keyed by the task id so scheduling twice
 * simply replaces the previous alarm.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: Task, defaultHour: Int, defaultMinute: Int) {
        val triggerAt = task.triggerAt(defaultHour, defaultMinute)
        // Don't arm alarms for moments that have already passed; the auto-complete
        // sweep and the overdue section handle those instead.
        if (triggerAt <= System.currentTimeMillis()) return
        scheduleAt(task.id, triggerAt)
    }

    /** Used by both normal scheduling and the notification's snooze action. */
    fun scheduleAt(taskId: Long, triggerAtMillis: Long) {
        val pi = pendingIntent(taskId, PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                // Graceful fallback when the user has not granted "Alarms & reminders":
                // still reliable, just not guaranteed to the exact minute.
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, TEN_MINUTES, pi
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact", e)
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, TEN_MINUTES, pi)
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(pendingIntent(taskId, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun pendingIntent(taskId: Long, flags: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_TASK_ID, taskId)
            // Make the intent unique per task so PendingIntent doesn't collapse them.
            data = android.net.Uri.parse("remindly://task/$taskId")
        }
        return PendingIntent.getBroadcast(
            context, taskId.toInt(), intent, flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REMIND = "com.remindly.app.ACTION_REMIND"
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val TEN_MINUTES = 10 * 60 * 1000L
        private const val TAG = "AlarmScheduler"
    }
}
