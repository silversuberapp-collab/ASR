package com.example.upsccompanion.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Dates {
    private val pretty = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val short = DateTimeFormatter.ofPattern("dd MMM")
    private val weekday = DateTimeFormatter.ofPattern("EEE")

    fun today(): String = LocalDate.now().toString()
    fun todayDate(): LocalDate = LocalDate.now()

    fun parseOrNull(value: String): LocalDate? = try { LocalDate.parse(value) } catch (_: Exception) { null }

    fun pretty(value: String): String = parseOrNull(value)?.format(pretty) ?: value
    fun short(value: String): String = parseOrNull(value)?.format(short) ?: value
    fun short(value: LocalDate): String = value.format(short)
    fun weekday(value: LocalDate): String = value.format(weekday)

    /** Days from today until the date, or null if the date cannot be read. */
    fun daysUntil(value: String): Long? = parseOrNull(value)?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

    /** Material date pickers work in UTC milliseconds. */
    fun toPickerMillis(value: String): Long? =
        parseOrNull(value)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    fun fromPickerMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

    fun greeting(): String = when (LocalTime.now().hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }
}
