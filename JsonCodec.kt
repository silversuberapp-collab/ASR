package com.example.upsccompanion.data

import org.json.JSONArray
import org.json.JSONObject

/** Everything the app saves, as one snapshot. Used for the save file and for backups. */
data class AppSnapshot(
    val profile: AspirantProfile?,
    val topics: List<SyllabusTopic>,
    val tasks: List<StudyTask>,
    val notes: List<StudyNote>,
    val affairs: List<CurrentAffair>,
    val tests: List<TestAttempt>,
    val sessions: List<StudySession>,
    val focus: FocusRun?
)

object JsonCodec {
    const val FORMAT_VERSION = 2
    const val DEFAULT_PRELIMS = "2027-05-23"
    const val DEFAULT_MAINS = "2027-08-20"

    // ----- writing -----

    fun encode(s: AppSnapshot): String = JSONObject().apply {
        put("app", "UPSC Companion")
        put("version", FORMAT_VERSION)
        s.profile?.let { put("profile", profileToJson(it)) }
        put("topics", JSONArray().apply { s.topics.forEach { put(topicToJson(it)) } })
        put("tasks", JSONArray().apply { s.tasks.forEach { put(taskToJson(it)) } })
        put("notes", JSONArray().apply { s.notes.forEach { put(noteToJson(it)) } })
        put("affairs", JSONArray().apply { s.affairs.forEach { put(affairToJson(it)) } })
        put("tests", JSONArray().apply { s.tests.forEach { put(testToJson(it)) } })
        put("sessions", JSONArray().apply { s.sessions.forEach { put(sessionToJson(it)) } })
        s.focus?.let { put("focus", focusToJson(it)) }
    }.toString()

    fun profileToJson(p: AspirantProfile): JSONObject = JSONObject()
        .put("name", p.name)
        .put("examYear", p.examYear)
        .put("prelimsDate", p.prelimsDate)
        .put("mainsDate", p.mainsDate)
        .put("optionalSubject", p.optionalSubject)
        .put("dailyHours", p.dailyHours)
        .put("preparationMode", p.preparationMode)

    private fun topicToJson(t: SyllabusTopic) = JSONObject()
        .put("id", t.id).put("paper", t.paper).put("subject", t.subject).put("title", t.title)
        .put("status", t.status).put("confidence", t.confidence)
        .put("lastRevised", t.lastRevised ?: JSONObject.NULL)
        .put("revisionCount", t.revisionCount).put("custom", t.custom)

    private fun taskToJson(t: StudyTask) = JSONObject()
        .put("id", t.id).put("title", t.title).put("subject", t.subject).put("dueDate", t.dueDate)
        .put("minutes", t.minutes).put("completed", t.completed)
        .put("completedOn", t.completedOn ?: JSONObject.NULL)

    private fun noteToJson(n: StudyNote) = JSONObject()
        .put("id", n.id).put("title", n.title).put("subject", n.subject).put("body", n.body)
        .put("updatedAt", n.updatedAt).put("pinned", n.pinned)

    private fun affairToJson(a: CurrentAffair) = JSONObject()
        .put("id", a.id).put("title", a.title).put("category", a.category).put("summary", a.summary)
        .put("date", a.date).put("source", a.source)

    private fun testToJson(t: TestAttempt) = JSONObject()
        .put("id", t.id).put("title", t.title).put("subject", t.subject)
        .put("score", t.score).put("maximum", t.maximum).put("date", t.date)

    private fun sessionToJson(s: StudySession) = JSONObject()
        .put("id", s.id).put("subject", s.subject).put("minutes", s.minutes).put("date", s.date)

    private fun focusToJson(f: FocusRun) = JSONObject()
        .put("subject", f.subject).put("plannedMinutes", f.plannedMinutes).put("startedAt", f.startedAt)
        .put("pausedAt", f.pausedAt ?: JSONObject.NULL).put("pausedTotal", f.pausedTotal)

    // ----- reading -----

    /** Reads a save file or backup. Throws if the text is not a UPSC Companion file. */
    fun decode(text: String): AppSnapshot {
        val root = JSONObject(text)
        require(root.has("topics") || root.has("profile")) { "Not a UPSC Companion backup" }
        return AppSnapshot(
            profile = root.optJSONObject("profile")?.let { profileFromJson(it) },
            topics = readList(root.optJSONArray("topics")) { topicFromJson(it) },
            tasks = readList(root.optJSONArray("tasks")) { taskFromJson(it) },
            notes = readList(root.optJSONArray("notes")) { noteFromJson(it) },
            affairs = readList(root.optJSONArray("affairs")) { affairFromJson(it) },
            tests = readList(root.optJSONArray("tests")) { testFromJson(it) },
            sessions = readList(root.optJSONArray("sessions")) { sessionFromJson(it) },
            focus = root.optJSONObject("focus")?.let { focusFromJson(it) }
        )
    }

    /** Skips single bad entries instead of losing the whole list. */
    fun <T> readList(array: JSONArray?, read: (JSONObject) -> T): List<T> {
        if (array == null) return emptyList()
        val out = ArrayList<T>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            try { out.add(read(o)) } catch (_: Exception) { }
        }
        return out
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).ifBlank { null }

    fun profileFromJson(o: JSONObject): AspirantProfile {
        val year = o.optString("examYear", "2027").ifBlank { "2027" }
        return AspirantProfile(
            name = o.getString("name"),
            examYear = year,
            prelimsDate = o.optString("prelimsDate", DEFAULT_PRELIMS).ifBlank { DEFAULT_PRELIMS },
            // The first version had no Mains date. Fill in the official 2027 date for 2027 aspirants.
            mainsDate = o.optString("mainsDate", "").ifBlank { if (year == "2027") DEFAULT_MAINS else "" },
            optionalSubject = o.optString("optionalSubject", "Not selected").ifBlank { "Not selected" },
            // The first version stored hours as a decimal number.
            dailyHours = o.optDouble("dailyHours", 6.0).toInt().coerceIn(1, 16),
            preparationMode = o.optString("preparationMode", "Full-time").ifBlank { "Full-time" }
        )
    }

    fun topicFromJson(o: JSONObject) = SyllabusTopic(
        id = o.getString("id"),
        paper = o.getString("paper"),
        subject = o.getString("subject"),
        title = o.getString("title"),
        status = o.optString("status", TopicStatus.NOT_STARTED).takeIf { it in TopicStatus.all } ?: TopicStatus.NOT_STARTED,
        confidence = o.optInt("confidence", 0).coerceIn(0, 5),
        lastRevised = o.optNullableString("lastRevised"),
        revisionCount = o.optInt("revisionCount", 0).coerceAtLeast(0),
        custom = if (o.has("custom")) o.optBoolean("custom", false) else !DefaultSyllabus.isDefaultId(o.getString("id"))
    )

    fun taskFromJson(o: JSONObject) = StudyTask(
        id = o.getString("id"),
        title = o.getString("title"),
        subject = o.optString("subject", "General Studies"),
        dueDate = o.getString("dueDate"),
        minutes = o.optInt("minutes", 30),
        completed = o.optBoolean("completed", false),
        completedOn = o.optNullableString("completedOn")
    )

    fun noteFromJson(o: JSONObject) = StudyNote(
        id = o.getString("id"),
        title = o.getString("title"),
        subject = o.optString("subject", "General Studies"),
        body = o.optString("body", ""),
        updatedAt = o.optString("updatedAt", Dates.today()),
        pinned = o.optBoolean("pinned", false)
    )

    fun affairFromJson(o: JSONObject) = CurrentAffair(
        id = o.getString("id"),
        title = o.getString("title"),
        category = o.optString("category", "General"),
        summary = o.optString("summary", ""),
        date = o.optString("date", Dates.today()),
        source = o.optString("source", "")
    )

    fun testFromJson(o: JSONObject) = TestAttempt(
        id = o.getString("id"),
        title = o.getString("title"),
        subject = o.optString("subject", "GS Prelims"),
        score = o.optDouble("score", 0.0),
        maximum = o.optDouble("maximum", 100.0),
        date = o.optString("date", Dates.today())
    )

    fun sessionFromJson(o: JSONObject) = StudySession(
        id = o.getString("id"),
        subject = o.optString("subject", "General Studies"),
        minutes = o.optInt("minutes", 0),
        date = o.getString("date")
    )

    fun focusFromJson(o: JSONObject) = FocusRun(
        subject = o.optString("subject", "General Studies"),
        plannedMinutes = o.optInt("plannedMinutes", 25).coerceAtLeast(1),
        startedAt = o.getLong("startedAt"),
        pausedAt = if (o.isNull("pausedAt")) null else o.optLong("pausedAt"),
        pausedTotal = o.optLong("pausedTotal", 0)
    )
}
