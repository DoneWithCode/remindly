package com.remindly.app.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

/** Keeps java.time values in the database as plain integers. */
class Converters {
    // dueDate is non-null in the entity, so this pair is non-null too.
    @TypeConverter
    fun dateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun timeToSecondOfDay(time: LocalTime?): Int? = time?.toSecondOfDay()

    @TypeConverter
    fun secondOfDayToTime(value: Int?): LocalTime? =
        value?.let { LocalTime.ofSecondOfDay(it.toLong()) } // ofSecondOfDay takes a Long

    @TypeConverter
    fun repeatToName(rule: RepeatRule): String = rule.name

    @TypeConverter
    fun nameToRepeat(value: String): RepeatRule =
        runCatching { RepeatRule.valueOf(value) }.getOrDefault(RepeatRule.NONE)

    @TypeConverter
    fun categoryToName(category: Category): String = category.name

    @TypeConverter
    fun nameToCategory(value: String): Category =
        runCatching { Category.valueOf(value) }.getOrDefault(Category.GENERAL)
}
