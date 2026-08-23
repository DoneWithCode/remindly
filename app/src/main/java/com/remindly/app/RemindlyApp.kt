package com.remindly.app

import android.app.Application
import com.remindly.app.alarm.AlarmScheduler
import com.remindly.app.alarm.NotificationHelper
import com.remindly.app.data.AppDatabase
import com.remindly.app.data.SettingsStore
import com.remindly.app.data.TaskRepository
import com.remindly.app.work.AutoCompleteWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container. The app is small enough that a DI framework would
 * add more ceremony than it removes.
 */
class RemindlyApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    val repository: TaskRepository by lazy {
        TaskRepository(
            dao = AppDatabase.get(this).taskDao(),
            settingsStore = settingsStore,
            scheduler = alarmScheduler
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        AutoCompleteWorker.enqueuePeriodic(this)

        appScope.launch {
            // Catch up on anything missed while the app was closed.
            repository.runAutoComplete()
            repository.rescheduleAll()
        }
    }
}
