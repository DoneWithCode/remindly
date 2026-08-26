package com.remindly.app

import com.remindly.app.data.Category
import com.remindly.app.data.RepeatRule
import com.remindly.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RepeatRuleTest {

    private val friday = LocalDate.of(2026, 3, 6) // a Friday
    private val nineThirty = LocalTime.of(9, 30)

    /** Pinned so these assertions don't drift as the real calendar moves on. */
    private val fridayMidMorning = LocalDateTime.of(friday, LocalTime.of(10, 0))

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
            dueTime = nineThirty,
            category = Category.WORK,
            repeat = RepeatRule.DAILY,
            isDone = true,
            completedAt = 1L
        )
        val next = task.nextOccurrence(now = fridayMidMorning)!!
        assertEquals(friday.plusDays(1), next.dueDate)
        assertTrue(!next.isDone)
        assertNull(next.completedAt)
    }

    @Test
    fun `a daily task missed for days skips to the next future slot`() {
        val task = Task(
            title = "Water the plants",
            dueDate = friday,
            dueTime = nineThirty,
            repeat = RepeatRule.DAILY
        )
        // Pretend the phone was off until the following Wednesday afternoon.
        val wednesday = LocalDateTime.of(LocalDate.of(2026, 3, 11), LocalTime.of(15, 0))
        val next = task.nextOccurrence(now = wednesday)!!

        // Thursday 9:30, not a backlog of five missed mornings.
        assertEquals(LocalDate.of(2026, 3, 12), next.dueDate)
        assertTrue(LocalDateTime.of(next.dueDate, next.dueTime).isAfter(wednesday))
    }

    @Test
    fun `an interval task catches up instead of firing a backlog`() {
        val task = Task(
            title = "Drink water",
            dueDate = friday,
            dueTime = LocalTime.of(3, 36),
            repeat = RepeatRule.EVERY_3_HOURS
        )
        val twoDaysLater = LocalDateTime.of(LocalDate.of(2026, 3, 8), LocalTime.of(12, 19))
        val next = task.nextOccurrence(now = twoDaysLater)!!

        assertTrue(LocalDateTime.of(next.dueDate, next.dueTime).isAfter(twoDaysLater))
        // Still aligned to the original :36 offset.
        assertEquals(36, next.dueTime!!.minute)
    }

    @Test
    fun `custom interval uses the task's own minute count`() {
        val task = Task(
            title = "Stretch",
            dueDate = friday,
            dueTime = LocalTime.of(9, 0),
            repeat = RepeatRule.CUSTOM,
            repeatIntervalMinutes = 45
        )
        val next = task.nextOccurrence(now = LocalDateTime.of(friday, LocalTime.of(9, 1)))!!
        assertEquals(LocalTime.of(9, 45), next.dueTime)
    }

    @Test
    fun `a custom repeat with no interval does not repeat`() {
        val task = Task(
            title = "Broken",
            dueDate = friday,
            repeat = RepeatRule.CUSTOM,
            repeatIntervalMinutes = null
        )
        assertNull(task.nextOccurrence(now = fridayMidMorning))
    }

    @Test
    fun `all day task resolves to the default reminder time`() {
        val task = Task(title = "Pay rent", dueDate = friday, dueTime = null)
        val at = task.triggerAt(defaultHour = 9, defaultMinute = 0)
        val expected = friday.atTime(9, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, at)
    }
}
