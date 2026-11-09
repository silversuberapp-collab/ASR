package com.example.upsccompanion.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.util.AtomicFile
import org.json.JSONArray
import java.io.File
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Holds all app data in Compose state and saves it to one JSON file on the device.
 *
 * There is one instance per process, so rotating the phone does not create a second copy
 * that could read the file while a save is still being written.
 */
class AppStore private constructor(context: Context) {

    private val file = AtomicFile(File(context.filesDir, SAVE_FILE))
    private val writer = Executors.newSingleThreadExecutor()

    var profile by mutableStateOf<AspirantProfile?>(null)
        private set
    var focus by mutableStateOf<FocusRun?>(null)
        private set

    val topics = mutableStateListOf<SyllabusTopic>()
    val tasks = mutableStateListOf<StudyTask>()
    val notes = mutableStateListOf<StudyNote>()
    val affairs = mutableStateListOf<CurrentAffair>()
    val tests = mutableStateListOf<TestAttempt>()
    val sessions = mutableStateListOf<StudySession>()

    init {
        val loaded = loadSaveFile() ?: migrateFromFirstVersion(context)
        if (loaded != null) apply(loaded) else topics.addAll(DefaultSyllabus.topics())
        if (loaded == null || !file.baseFile.exists()) persist()
        completeFocusIfDue(System.currentTimeMillis())
    }

    // ----------------------------- Profile -----------------------------

    fun saveProfile(value: AspirantProfile, isFirstSetup: Boolean = false) {
        profile = value
        // Starter tasks only on first setup, so editing the profile later never brings them back.
        if (isFirstSetup && tasks.isEmpty()) {
            val today = Dates.today()
            tasks.add(StudyTask(newId(), "Read a core subject", "General Studies", today, 90))
            tasks.add(StudyTask(newId(), "Read and analyse current affairs", "Current Affairs", today, 45))
            tasks.add(StudyTask(newId(), "Practise Prelims questions", "Prelims", today, 30))
        }
        persist()
    }

    // ----------------------------- Syllabus -----------------------------

    fun addTopic(paper: String, subject: String, title: String) {
        topics.add(SyllabusTopic(newId(), paper, subject, title, custom = true))
        persist()
    }

    fun updateTopic(topic: SyllabusTopic) {
        val i = topics.indexOfFirst { it.id == topic.id }
        if (i >= 0) topics[i] = topic
        persist()
    }

    /** Changes status. Completing a topic for the first time starts its revision schedule. */
    fun setTopicStatus(topic: SyllabusTopic, status: String) {
        val updated = when (status) {
            TopicStatus.COMPLETED -> topic.copy(status = status, lastRevised = topic.lastRevised ?: Dates.today())
            TopicStatus.NOT_STARTED -> topic.copy(status = status, lastRevised = null, revisionCount = 0)
            else -> topic.copy(status = status)
        }
        updateTopic(updated)
    }

    fun markRevised(topic: SyllabusTopic) {
        val firstTime = topic.lastRevised == null
        updateTopic(
            topic.copy(
                status = TopicStatus.COMPLETED,
                lastRevised = Dates.today(),
                revisionCount = if (firstTime) 0 else topic.revisionCount + 1
            )
        )
    }

    fun deleteTopic(id: String): Removed<SyllabusTopic>? = removeFrom(topics) { it.id == id }
    fun restoreTopic(r: Removed<SyllabusTopic>) = restoreTo(topics, r)

    /** Completed topics whose next revision date is today or earlier, most overdue first. */
    fun dueRevisions(today: LocalDate = Dates.todayDate()): List<Pair<SyllabusTopic, LocalDate>> =
        topics.mapNotNull { t -> t.nextRevision()?.let { t to it } }
            .filter { !it.second.isAfter(today) }
            .sortedBy { it.second }

    // ----------------------------- Tasks -----------------------------

    fun saveTask(task: StudyTask) {
        val i = tasks.indexOfFirst { it.id == task.id }
        if (i >= 0) tasks[i] = task else tasks.add(0, task)
        persist()
    }

    fun toggleTask(id: String) {
        val i = tasks.indexOfFirst { it.id == id }
        if (i >= 0) {
            val t = tasks[i]
            tasks[i] = if (t.completed) t.copy(completed = false, completedOn = null)
            else t.copy(completed = true, completedOn = Dates.today())
            persist()
        }
    }

    fun deleteTask(id: String): Removed<StudyTask>? = removeFrom(tasks) { it.id == id }
    fun restoreTask(r: Removed<StudyTask>) = restoreTo(tasks, r)

    /** Moves unfinished tasks from earlier days to today. Returns how many moved. */
    fun moveOverdueToToday(): Int {
        val today = Dates.today()
        var moved = 0
        for (i in tasks.indices) {
            val t = tasks[i]
            if (!t.completed && t.dueDate < today) { tasks[i] = t.copy(dueDate = today); moved++ }
        }
        if (moved > 0) persist()
        return moved
    }

    // ----------------------------- Notes -----------------------------

    fun saveNote(note: StudyNote) {
        val i = notes.indexOfFirst { it.id == note.id }
        if (i >= 0) notes[i] = note else notes.add(0, note)
        persist()
    }

    fun togglePin(id: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) { notes[i] = notes[i].copy(pinned = !notes[i].pinned); persist() }
    }

    fun deleteNote(id: String): Removed<StudyNote>? = removeFrom(notes) { it.id == id }
    fun restoreNote(r: Removed<StudyNote>) = restoreTo(notes, r)

    // ----------------------------- Current affairs -----------------------------

    fun addAffair(item: CurrentAffair) { affairs.add(0, item); persist() }
    fun deleteAffair(id: String): Removed<CurrentAffair>? = removeFrom(affairs) { it.id == id }
    fun restoreAffair(r: Removed<CurrentAffair>) = restoreTo(affairs, r)

    // ----------------------------- Tests -----------------------------

    fun addTest(test: TestAttempt) { tests.add(0, test); persist() }
    fun deleteTest(id: String): Removed<TestAttempt>? = removeFrom(tests) { it.id == id }
    fun restoreTest(r: Removed<TestAttempt>) = restoreTo(tests, r)

    // ----------------------------- Study time -----------------------------

    fun addSession(subject: String, minutes: Int, date: String = Dates.today()) {
        if (minutes <= 0) return
        sessions.add(0, StudySession(newId(), subject, minutes, date))
        persist()
    }

    fun deleteSession(id: String): Removed<StudySession>? = removeFrom(sessions) { it.id == id }
    fun restoreSession(r: Removed<StudySession>) = restoreTo(sessions, r)

    fun minutesOn(date: String): Int = sessions.filter { it.date == date }.sumOf { it.minutes }

    /**
     * Days in a row with any study logged or task ticked off. Today counts if it has activity;
     * if today is still empty the streak from yesterday is kept, so it does not reset in the morning.
     */
    fun streak(today: LocalDate = Dates.todayDate()): Int {
        val active = HashSet<String>()
        sessions.forEach { active.add(it.date) }
        tasks.forEach { t -> t.completedOn?.let { active.add(it) } }
        var day = if (today.toString() in active) today else today.minusDays(1)
        var count = 0
        while (day.toString() in active) { count++; day = day.minusDays(1) }
        return count
    }

    // ----------------------------- Focus timer -----------------------------

    fun startFocus(subject: String, minutes: Int) {
        focus = FocusRun(subject.ifBlank { "General Studies" }, minutes.coerceIn(1, 240), System.currentTimeMillis())
        persist()
    }

    fun pauseFocus() {
        val f = focus ?: return
        if (!f.isPaused) { focus = f.copy(pausedAt = System.currentTimeMillis()); persist() }
    }

    fun resumeFocus() {
        val f = focus ?: return
        val pausedAt = f.pausedAt ?: return
        focus = f.copy(pausedAt = null, pausedTotal = f.pausedTotal + (System.currentTimeMillis() - pausedAt))
        persist()
    }

    /** Ends the session early and logs the whole minutes studied. Returns the minutes logged. */
    fun finishFocus(): Int {
        val f = focus ?: return 0
        val minutes = (f.elapsedMillis(System.currentTimeMillis()) / 60_000L).toInt()
        focus = null
        if (minutes > 0) addSession(f.subject, minutes) else persist()
        return minutes
    }

    fun discardFocus() { focus = null; persist() }

    /** If the timer has run out, logs the full planned time. Returns true if it did. */
    fun completeFocusIfDue(now: Long): Boolean {
        val f = focus ?: return false
        if (f.isPaused || f.remainingMillis(now) > 0) return false
        focus = null
        // Log on the day the session started, in case it ran past midnight.
        val day = java.time.Instant.ofEpochMilli(f.startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        addSession(f.subject, f.plannedMinutes, day)
        return true
    }

    // ----------------------------- Backup -----------------------------

    fun exportJson(): String = JsonCodec.encode(snapshot())

    /** Replaces everything with the backup. Returns false and changes nothing if it cannot be read. */
    fun importJson(text: String): Boolean = try {
        val s = JsonCodec.decode(text)
        clearAll()
        apply(s)
        if (topics.isEmpty()) topics.addAll(DefaultSyllabus.topics())
        persist()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Backup could not be read", e)
        false
    }

    fun resetAll() {
        clearAll()
        topics.addAll(DefaultSyllabus.topics())
        persist()
    }

    // ----------------------------- Internals -----------------------------

    class Removed<T>(val index: Int, val item: T)

    private fun <T> removeFrom(list: SnapshotStateList<T>, match: (T) -> Boolean): Removed<T>? {
        val i = list.indexOfFirst(match)
        if (i < 0) return null
        val item = list.removeAt(i)
        persist()
        return Removed(i, item)
    }

    private fun <T> restoreTo(list: SnapshotStateList<T>, r: Removed<T>) {
        list.add(r.index.coerceIn(0, list.size), r.item)
        persist()
    }

    private fun snapshot() = AppSnapshot(profile, topics.toList(), tasks.toList(), notes.toList(), affairs.toList(), tests.toList(), sessions.toList(), focus)

    private fun apply(s: AppSnapshot) {
        profile = s.profile
        focus = s.focus
        topics.addAll(s.topics)
        tasks.addAll(s.tasks)
        notes.addAll(s.notes)
        affairs.addAll(s.affairs)
        tests.addAll(s.tests)
        sessions.addAll(s.sessions)
    }

    private fun clearAll() {
        profile = null; focus = null
        topics.clear(); tasks.clear(); notes.clear(); affairs.clear(); tests.clear(); sessions.clear()
    }

    /** Turns the state into text on the main thread, then writes it on a background thread. */
    private fun persist() {
        val text = JsonCodec.encode(snapshot())
        writer.execute {
            val out = try { file.startWrite() } catch (e: Exception) { Log.e(TAG, "Could not open save file", e); return@execute }
            try {
                out.write(text.toByteArray(Charsets.UTF_8))
                file.finishWrite(out)
            } catch (e: Exception) {
                file.failWrite(out)
                Log.e(TAG, "Could not save data", e)
            }
        }
    }

    private fun loadSaveFile(): AppSnapshot? = try {
        if (!file.baseFile.exists()) null else JsonCodec.decode(String(file.readFully(), Charsets.UTF_8))
    } catch (e: Exception) {
        Log.e(TAG, "Save file could not be read", e)
        null
    }

    /** Reads data saved by the first version of the app (SharedPreferences), if there is any. */
    private fun migrateFromFirstVersion(context: Context): AppSnapshot? = try {
        val prefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) null else {
            fun arr(key: String) = prefs.getString(key, null)?.let { JSONArray(it) }
            val oldTopics = JsonCodec.readList(arr("topics")) { JsonCodec.topicFromJson(it) }
            // Add starter topics that the first version did not have, keeping the user's progress.
            val haveIds = oldTopics.map { it.id }.toSet()
            val mergedTopics = oldTopics + DefaultSyllabus.topics().filter { it.id !in haveIds }
            AppSnapshot(
                profile = prefs.getString("profile", null)?.let { runCatching { JsonCodec.profileFromJson(org.json.JSONObject(it)) }.getOrNull() },
                topics = mergedTopics,
                tasks = JsonCodec.readList(arr("tasks")) { JsonCodec.taskFromJson(it) },
                notes = JsonCodec.readList(arr("notes")) { JsonCodec.noteFromJson(it) },
                affairs = JsonCodec.readList(arr("affairs")) { JsonCodec.affairFromJson(it) },
                tests = JsonCodec.readList(arr("tests")) { JsonCodec.testFromJson(it) },
                sessions = emptyList(),
                focus = null
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Old data could not be migrated", e)
        null
    }

    companion object {
        private const val TAG = "AppStore"
        private const val SAVE_FILE = "upsc_companion_data.json"
        private const val OLD_PREFS = "upsc_companion_local"

        @Volatile private var instance: AppStore? = null

        fun get(context: Context): AppStore =
            instance ?: synchronized(this) { instance ?: AppStore(context.applicationContext).also { instance = it } }

        fun newId(): String = UUID.randomUUID().toString()
    }
}
