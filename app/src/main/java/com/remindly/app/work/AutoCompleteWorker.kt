package com.remindly.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.remindly.app.RemindlyApp
import java.util.concurrent.TimeUnit

/**
 * Hourly sweep that applies the auto-complete rule: an open, non-repeating task whose
 * due moment passed more than N hours ago is filed under Done automatically.
 * N is configurable in Settings (and can be turned off entirely).
 */
class AutoCompleteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RemindlyApp
        return runCatching { app.repository.runAutoComplete() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        private const val UNIQUE_NAME = "auto_complete_sweep"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoCompleteWorker>(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
