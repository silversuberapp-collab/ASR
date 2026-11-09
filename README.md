# UPSC Companion 2.0

A personal Android app for UPSC CSE preparation. Everything is stored on the phone; there is no account and no internet permission.

## Open and run

1. Unzip, then in Android Studio choose **File > Open** and pick the `UPSCCompanion` folder.
2. Let Gradle sync. Studio downloads Gradle 8.14.3, Android Gradle Plugin 8.7.3, Kotlin 2.1.0 and the Compose libraries. If it offers to upgrade any of them, that is fine.
3. Connect your phone (USB debugging on) or start an emulator, then press **Run**.

Requirements: Android 8.0 (API 26) or newer on the phone; JDK 17 or newer for building (the one bundled with Android Studio works).

To make an APK file: **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.

## Keeping data from the first version

The application id is still `com.example.upsccompanion`. If you install this over the first version (built on the same computer, so the debug signing key matches), it reads the old saved profile, topics, tasks, notes, current affairs and test scores and moves them into the new save file. If Android refuses to install over the old app because of a signature mismatch, uninstalling the old one first will erase its data, so check the old app's screens before doing that.

## What is in the app

| Area | What it does |
| --- | --- |
| Home | Prelims and Mains countdown, study time today against your target, a 7 day chart, day streak, revisions due, today's tasks, progress per paper (including Essay and papers you add) |
| Syllabus | Starter list of GS I to IV, Essay and CSAT topics. Search, filter by paper or status, add or delete topics, rate confidence 1 to 5, spaced revision schedule (1, 3, 7, 15, 30, then every 60 days) |
| Planner | Tasks with date picker, edit by tapping, Today / Upcoming / Overdue / Done / All, move overdue tasks to today, undo after delete |
| Notes | Search, filter by subject, pin, edit, undo after delete |
| Focus timer (More) | Countdown timer that logs study time; survives switching apps and restarts because it uses the clock. Manual time logging too |
| Current affairs (More) | Category, summary, date, source; links open in the browser |
| Test analytics (More) | Score log with average, best and a trend chart; filter by paper |
| Profile and backup (More) | Edit profile, save a backup file, restore from a backup, erase all data |

Default dates are from UPSC's 2027 calendar: Prelims 23 May 2027, Mains from 20 August 2027.

## Known limits

* The focus timer does not ring or notify when it ends. It logs the session the next time the app is open.
* There is no cloud sync. Use **Save backup** regularly.

## Code layout

```
app/src/main/java/com/example/upsccompanion/
  MainActivity.kt
  data/        Models, AppStore (state + saving), JsonCodec (file format), DefaultSyllabus, Dates
  ui/App.kt    Navigation, top bar, bottom bar, snackbars, Back button handling
  ui/theme/    Colours and Material theme
  ui/components/  Shared cards, date field, chips, dialogs
  ui/screens/  One file per screen
```
