package com.remindly.app.alarm

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.remindly.app.MainActivity
import com.remindly.app.R
import com.remindly.app.data.Task
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object NotificationHelper {

    const val CHANNEL_ID = "reminders"
    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled() below
    fun show(context: Context, task: Task) {
        val openApp = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TASK_ID, task.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = actionIntent(context, task.id, NotificationActionReceiver.ACTION_DONE)
        val snoozeIntent = actionIntent(context, task.id, NotificationActionReceiver.ACTION_SNOOZE)

        val subtitle = buildString {
            task.dueTime?.let { append(it.format(timeFormat)).append(" · ") }
            append(task.category.label)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(task.description?.takeIf { it.isNotBlank() } ?: subtitle)
            .setSubText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_check, context.getString(R.string.action_done), doneIntent)
            .addAction(R.drawable.ic_snooze, context.getString(R.string.action_snooze), snoozeIntent)

        task.description?.takeIf { it.isNotBlank() }?.let {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
        }

        // POST_NOTIFICATIONS may have been revoked; notify() throws nothing but is a no-op,
        // and NotificationManagerCompat guards the permission check for us.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(task.id.toInt(), builder.build())
        }
    }

    fun cancel(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }

    private fun actionIntent(context: Context, taskId: Long, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            data = Uri.parse("remindly://action/$action/$taskId")
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
