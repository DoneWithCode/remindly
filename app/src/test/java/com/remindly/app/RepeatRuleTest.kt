package com.remindly.app

import com.remindly.app.data.Category
import com.remindly.app.data.RepeatRule
import com.remindly.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class RepeatRuleTest {

    private val friday = LocalDate.of(2026, 3, 6) // a Friday

    @Test
    fun `none does not repeat`() {
        assertNull(RepeatRule.NONE.next(friday))
    }

    @Test
    fun `weekdays skips the weekend`() {
        assertEquals(LocalDate.of(2026, 3, 9), RepeatRule.WEEKDAYS.next(friday)) // Monday
    }

    @Test
    fun `weekly adds seven days`() {
        assertEquals(LocalDate.of(2026, 3, 13), RepeatRule.WEEKLY.next(friday))
    }

    @Test
    fun `completing a repeating task rolls it forward and reopens it`() {
        val task = Task(
            id = 1,
            title = "Standup",
            dueDate = friday,
            dueTime = LocalTime.of(9, 30),
            category = Category.WORK,
            repeat = RepeatRule.DAILY,
            isDone = true,
            completedAt = 1L
        )
        val next = task.nextOccurrence()!!
        assertEquals(friday.plusDays(1), next.dueDate)
        assertTrue(!next.isDone)
        assertNull(next.completedAt)
    }

    @Test
    fun `all day task resolves to the default reminder time`() {
        val task = Task(title = "Pay rent", dueDate = friday, dueTime = null)
        val at = task.triggerAt(defaultHour = 9, defaultMinute = 0)
        val expected = friday.atTime(9, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, at)
    }
}
