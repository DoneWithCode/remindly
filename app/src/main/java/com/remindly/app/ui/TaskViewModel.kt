package com.remindly.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.remindly.app.RemindlyApp
import com.remindly.app.data.AppSettings
import com.remindly.app.data.Category
import com.remindly.app.data.Task
import com.remindly.app.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as RemindlyApp).repository
    private val settingsStore = (app as RemindlyApp).settingsStore
    private val scheduler = (app as RemindlyApp).alarmScheduler

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Due today or earlier — the "Today" block. */
    val todayTasks: StateFlow<List<Task>> =
        repo.todayAndOverdue().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tomorrow onwards — the "Upcoming" block. */
    val upcomingTasks: StateFlow<List<Task>> =
        repo.upcoming().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completedTasks: StateFlow<List<Task>> =
        repo.completedTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _categoryFilter = MutableStateFlow<Category?>(null)
    val categoryFilter: StateFlow<Category?> = _categoryFilter.asStateFlow()

    /** All open tasks, respecting the category filter chosen on the Active tab. */
    val activeTasks: StateFlow<List<Task>> =
        combine(repo.activeTasks(), _categoryFilter) { tasks, filter ->
            if (filter == null) tasks else tasks.filter { it.category == filter }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setCategoryFilter(category: Category?) {
        _categoryFilter.value = category
    }

    // ---- Task mutations -------------------------------------------------

    suspend fun loadTask(id: Long): Task? = repo.getTask(id)

    fun save(task: Task, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        onSaved(repo.upsert(task))
    }

    /** Quick-add: title only, due today, notifies at the default reminder time. */
    fun quickAdd(title: String, date: LocalDate = LocalDate.now(), category: Category = Category.GENERAL) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repo.upsert(Task(title = clean, dueDate = date, dueTime = null, category = category))
        }
    }

    fun toggleDone(task: Task) = viewModelScope.launch {
        repo.setDone(task, done = !task.isDone)
    }

    fun delete(task: Task) = viewModelScope.launch { repo.delete(task) }

    fun clearHistory() = viewModelScope.launch { repo.clearCompletedHistory() }

    // ---- Settings -------------------------------------------------------

    fun setDefaultTime(time: LocalTime) = viewModelScope.launch {
        settingsStore.setDefaultTime(time.hour, time.minute)
        repo.rescheduleAll()
    }

    fun setAutoCompleteHours(hours: Int) = viewModelScope.launch {
        settingsStore.setAutoCompleteHours(hours)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsStore.setThemeMode(mode) }

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExact()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                TaskViewModel(app)
            }
        }
    }
}
