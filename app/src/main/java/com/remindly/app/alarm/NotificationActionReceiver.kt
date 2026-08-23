package com.remindly.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.remindly.app.RemindlyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the "Done" and "Snooze" buttons on a reminder notification. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pending = goAsync()
        val app = context.applicationContext as RemindlyApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> {
                        app.repository.setDone(taskId, done = true)
                        NotificationHelper.cancel(context, taskId)
                    }
                    ACTION_SNOOZE -> {
                        app.alarmScheduler.scheduleAt(
                            taskId,
                            System.currentTimeMillis() + SNOOZE_MILLIS
                        )
                        NotificationHelper.cancel(context, taskId)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.remindly.app.ACTION_DONE"
        const val ACTION_SNOOZE = "com.remindly.app.ACTION_SNOOZE"
        private const val SNOOZE_MILLIS = 10 * 60 * 1000L // 10 minutes
    }
}
