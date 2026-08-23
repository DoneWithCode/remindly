package com.remindly.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.remindly.app.RemindlyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fired by AlarmManager at a task's due moment. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pending = goAsync()
        val app = context.applicationContext as RemindlyApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.repository.getTask(taskId) ?: return@launch
                if (task.isDone) return@launch

                NotificationHelper.show(context, task)

                // A repeating task immediately arms its next occurrence so the chain
                // never breaks, even if the user ignores this notification.
                app.repository.advanceRecurrence(task)?.let { next ->
                    app.repository.upsert(next)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
