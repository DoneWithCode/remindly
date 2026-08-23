package com.remindly.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remindly.app.ui.RemindlyRoot
import com.remindly.app.ui.TaskViewModel
import com.remindly.app.ui.theme.RemindlyTheme

/**
 * Single Activity. All screens are Compose destinations inside [RemindlyRoot].
 */
class MainActivity : ComponentActivity() {

    /** Task id carried in from a notification tap, if any. */
    private var openTaskId by mutableStateOf<Long?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled in Settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        openTaskId = intent.taskIdExtra()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val vm: TaskViewModel = viewModel(factory = TaskViewModel.Factory)
            val settings by vm.settings.collectAsStateWithLifecycle()

            RemindlyTheme(themeMode = settings.themeMode) {
                RemindlyRoot(
                    viewModel = vm,
                    openTaskId = openTaskId,
                    onOpenTaskHandled = { openTaskId = null }
                )
            }
        }
    }

    /** The activity is singleTask, so later notification taps arrive here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTaskId = intent.taskIdExtra()
    }

    private fun Intent?.taskIdExtra(): Long? =
        this?.getLongExtra(EXTRA_OPEN_TASK_ID, -1L)?.takeIf { it > 0L }

    companion object {
        const val EXTRA_OPEN_TASK_ID = "extra_open_task_id"
    }
}
