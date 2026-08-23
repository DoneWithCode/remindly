package com.remindly.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.remindly.app.RemindlyApp
import com.remindly.app.work.AutoCompleteWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Alarms do not survive a reboot, an app update or a clock change, so we re-arm
 * every open task whenever one of those happens. This is what makes reminders
 * persist across restarts.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }

        val pending = goAsync()
        val app = context.applicationContext as RemindlyApp

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.rescheduleAll()
                AutoCompleteWorker.enqueuePeriodic(context)
            } finally {
                pending.finish()
            }
        }
    }
}
