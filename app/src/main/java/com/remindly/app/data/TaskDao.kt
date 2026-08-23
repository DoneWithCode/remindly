package com.remindly.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val TIME_ORDER = "CASE WHEN dueTime IS NULL THEN 0 ELSE 1 END, dueTime ASC"

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>

    /** Every open task, soonest first. Used by the Active tab. */
    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY dueDate ASC, $TIME_ORDER")
    fun observeActive(): Flow<List<Task>>

    /** Anything due today or already past — the top block of the Today screen. */
    @Query("SELECT * FROM tasks WHERE isDone = 0 AND dueDate <= :today ORDER BY dueDate ASC, $TIME_ORDER")
    fun observeTodayAndOverdue(today: Long): Flow<List<Task>>

    /** Tomorrow onwards — the "Upcoming" block of the Today screen. */
    @Query("SELECT * FROM tasks WHERE isDone = 0 AND dueDate > :today ORDER BY dueDate ASC, $TIME_ORDER LIMIT :limit")
    fun observeUpcoming(today: Long, limit: Int): Flow<List<Task>>

    /** Completion history, most recently finished first. */
    @Query("SELECT * FROM tasks WHERE isDone = 1 ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<Task>>

    /** Non-reactive snapshot, used by BootReceiver and the auto-complete worker. */
    @Query("SELECT * FROM tasks WHERE isDone = 0")
    suspend fun getActiveOnce(): List<Task>

    @Query("UPDATE tasks SET isDone = 1, completedAt = :at, autoCompleted = :auto WHERE id = :id")
    suspend fun markDone(id: Long, at: Long, auto: Boolean)

    @Query("UPDATE tasks SET isDone = 0, completedAt = NULL, autoCompleted = 0 WHERE id = :id")
    suspend fun markActive(id: Long)

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun clearCompleted()

    @Query("SELECT COUNT(*) FROM tasks WHERE isDone = 0 AND dueDate <= :today")
    fun countDueToday(today: Long): Flow<Int>
}
