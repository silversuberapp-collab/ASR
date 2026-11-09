package com.example.upsccompanion.data

import java.time.LocalDate

data class AspirantProfile(
    val name: String,
    val examYear: String,
    val prelimsDate: String,
    val mainsDate: String,
    val optionalSubject: String,
    val dailyHours: Int,
    val preparationMode: String
)

object TopicStatus {
    const val NOT_STARTED = "Not started"
    const val IN_PROGRESS = "In progress"
    const val COMPLETED = "Completed"
    val all = listOf(NOT_STARTED, IN_PROGRESS, COMPLETED)
}

data class SyllabusTopic(
    val id: String,
    val paper: String,
    val subject: String,
    val title: String,
    val status: String = TopicStatus.NOT_STARTED,
    /** 0 means not rated yet, otherwise 1 to 5. */
    val confidence: Int = 0,
    /** Date the topic was first completed or last revised (ISO yyyy-MM-dd), or null. */
    val lastRevised: String? = null,
    val revisionCount: Int = 0,
    /** True for topics the user added, which can also be deleted. */
    val custom: Boolean = false
) {
    /**
     * Next revision date on a simple expanding schedule: 1, 3, 7, 15, 30, then every 60 days.
     * Only completed topics are scheduled.
     */
    fun nextRevision(): LocalDate? {
        if (status != TopicStatus.COMPLETED) return null
        val last = lastRevised?.let { Dates.parseOrNull(it) } ?: return null
        val gap = REVISION_GAPS_DAYS[revisionCount.coerceIn(0, REVISION_GAPS_DAYS.lastIndex)]
        return last.plusDays(gap)
    }

    companion object {
        val REVISION_GAPS_DAYS = longArrayOf(1, 3, 7, 15, 30, 60)
    }
}

data class StudyTask(
    val id: String,
    val title: String,
    val subject: String,
    val dueDate: String,
    val minutes: Int,
    val completed: Boolean = false,
    /** Date the task was ticked off, used for the study streak. */
    val completedOn: String? = null
)

data class StudyNote(
    val id: String,
    val title: String,
    val subject: String,
    val body: String,
    val updatedAt: String,
    val pinned: Boolean = false
)

data class CurrentAffair(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val date: String,
    val source: String
)

data class TestAttempt(
    val id: String,
    val title: String,
    val subject: String,
    val score: Double,
    val maximum: Double,
    val date: String
) {
    val percentage: Int
        get() = if (maximum <= 0.0) 0 else Math.round((score / maximum) * 100).toInt().coerceIn(0, 100)
}

/** A block of study time, logged by the focus timer or entered by hand. */
data class StudySession(
    val id: String,
    val subject: String,
    val minutes: Int,
    val date: String
)

/** The focus timer that is currently running or paused. Times are wall clock milliseconds. */
data class FocusRun(
    val subject: String,
    val plannedMinutes: Int,
    val startedAt: Long,
    val pausedAt: Long? = null,
    val pausedTotal: Long = 0
) {
    fun elapsedMillis(now: Long): Long = ((pausedAt ?: now) - startedAt - pausedTotal).coerceAtLeast(0)
    fun remainingMillis(now: Long): Long = (plannedMinutes * 60_000L - elapsedMillis(now)).coerceAtLeast(0)
    val isPaused: Boolean get() = pausedAt != null
}
