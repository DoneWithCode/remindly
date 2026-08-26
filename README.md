# Remindly

A small, local-only reminder app for Android. Create a task, give it a date (and
optionally a time), and it notifies you. Tick it off — or let it close itself —
and it moves to a Done history you can browse.

No accounts, no network calls, no analytics. Everything lives in a Room database
on the device.

---

## Build and run

**Requirements**

| Tool | Version |
|---|---|
| Android Studio | Ladybug (2024.2.1) or newer |
| JDK | 17 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| compileSdk / targetSdk | 35 (Android 15) |
| minSdk | 26 (Android 8.0) |

`minSdk 26` is deliberate: `java.time` and notification channels are both
available natively from API 26, so the project needs no core-library desugaring.

**In Android Studio**

1. `File → Open` and select the `Remindly` folder.
2. Let Gradle sync (it downloads AGP, Kotlin, Compose, Room and WorkManager).
3. Pick a device or emulator running API 26+ and press Run.

**From the command line**

The Gradle wrapper JAR is not checked in. Generate it once with a local Gradle
8.9 install, then use the wrapper as normal:

```bash
gradle wrapper --gradle-version 8.9   # only needed once
./gradlew assembleDebug                # APK -> app/build/outputs/apk/debug/
./gradlew installDebug                 # build + install on a connected device
./gradlew test                         # JVM unit tests
```

**Building without a computer (GitHub Actions)**

`.github/workflows/build-apk.yml` builds a debug APK on every push to `main` and
attaches it to a rolling `latest` release, so the APK can be downloaded and
installed straight from a phone browser. It can also be triggered by hand from
the Actions tab via **Run workflow**.

The workflow generates the Gradle wrapper on the runner (`gradle wrapper`) rather
than relying on a committed JAR, and runs `testDebugUnitTest` instead of `test`
— the latter builds both variants to run the same tests twice.

**Permissions the app asks for**

- `POST_NOTIFICATIONS` — requested at first launch on Android 13+. Without it
  reminders are scheduled but silent.
- `SCHEDULE_EXACT_ALARM` — special access on Android 12+. Settings → Permissions
  has a row that deep-links to the system screen. If it is denied the app falls
  back to `setWindow` with a 10-minute window, so reminders still fire, just not
  to the exact minute.
- `RECEIVE_BOOT_COMPLETED` — no user prompt; used to re-arm alarms after reboot.

---

## What the app does

**Core**

- Title plus optional notes.
- A due date, and optionally a due time. Leave the time off and the task is
  "all day": it notifies at the default reminder time set in Settings (9:00 AM
  out of the box).
- A notification fires at the due moment, with **Done** and **Snooze 10m**
  buttons that work without opening the app.
- Tap the checkbox to complete a task. Completed tasks move to **Done**, grouped
  by the day they were finished. Un-ticking one sends it back to Active.
- Edit or delete anything at any time from the task editor.
- Alarms are re-armed on boot, on app update, and on a clock or timezone change,
  so nothing is lost across restarts.

**Smart features (the three that earn their keep)**

1. **Recurring reminders** — two families. *Interval* rules (every hour, every 3
   hours, every 8 hours, or a custom gap in minutes) can fire many times a day.
   *Date* rules (daily, weekdays, weekly, monthly) advance the due date and keep
   the same time. A repeating task never enters the Done list; completing it
   rolls it forward and re-schedules the alarm. The next occurrence is also armed
   the moment the notification fires, so the chain survives being ignored. If the
   device was off, interval rules skip ahead to the next future slot rather than
   firing a backlog of missed alarms.
2. **Auto-complete rule** — an open, non-repeating task whose due moment passed
   more than N hours ago closes itself and is filed under Done, tagged
   "Auto-completed". N is configurable in Settings (6h / 12h / 24h / 72h /
   Never; default 24h). A `WorkManager` job runs this sweep hourly and again on
   every app start, so the Active list never fills up with stale items.
3. **Quick-add + categories** — the Today screen has a one-line box: type a
   title, press enter, and a scheduling dialog asks for the date, time and repeat
   before anything is saved, so no reminder silently inherits a default time.
   Tasks carry a category (General / Work / Personal / Health / Shopping) shown
   as a colour dot, and the Active tab filters by it with a chip row.

**Navigation**

Four bottom-bar destinations, no nesting, no hidden gestures:

| Tab | What's there |
|---|---|
| **Today** | Overdue, then today's tasks, then a preview of what's upcoming. Quick-add box at the top. Badge shows how many are due. |
| **Active** | Every open task, grouped Overdue / Today / Tomorrow / This week / Later, with a category filter. |
| **Done** | Completion history grouped by day. Overflow action clears it. |
| **Settings** | Default reminder time, auto-complete window, theme, permission shortcuts. |

A `+` FAB on Today and Active opens the full editor.

---

## Project layout

```
app/src/main/java/com/remindly/app/
├── RemindlyApp.kt              Application: dependency container, channel setup,
│                               startup catch-up sweep
├── MainActivity.kt             The only Activity; hosts Compose, handles the
│                               notification deep link
├── data/
│   ├── Task.kt                 Entity + Category / RepeatRule enums, due-time and
│   │                           recurrence maths
│   ├── Converters.kt           LocalDate <-> epoch day, LocalTime <-> second of day
│   ├── TaskDao.kt              Queries, all Flow-based
│   ├── AppDatabase.kt          Room database (v1)
│   ├── TaskRepository.kt       Single source of truth; every write syncs its alarm
│   └── SettingsStore.kt        DataStore preferences
├── alarm/
│   ├── AlarmScheduler.kt       AlarmManager wrapper, one alarm per task id
│   ├── NotificationHelper.kt   Channel + notification with Done / Snooze actions
│   ├── ReminderReceiver.kt     Fires at the due moment; advances recurrences
│   ├── NotificationActionReceiver.kt   Done / Snooze handlers
│   └── BootReceiver.kt         Re-arms everything after reboot / update / clock change
├── work/
│   └── AutoCompleteWorker.kt   Hourly auto-complete sweep
└── ui/
    ├── AppNav.kt               Scaffold, bottom bar, NavHost
    ├── TaskViewModel.kt        One ViewModel shared by all screens
    ├── theme/                  Colour scheme + Material 3 theme
    ├── components/             TaskRow, EmptyState, SectionHeader
    └── screens/                Today, Active, Done, Settings, TaskEdit
```

The UI is Jetpack Compose with Material 3, so there are no XML layout files —
each screen in `ui/screens/` is the equivalent of a Fragment plus its layout.
`res/` holds only strings, colours, the window theme, notification icons and the
adaptive launcher icon.

---

## Database schema

Single table, `tasks`:

| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK | auto-generated |
| `title` | TEXT | required |
| `description` | TEXT? | optional notes |
| `dueDate` | INTEGER | epoch day, so date comparisons happen in SQL |
| `dueTime` | INTEGER? | second of day; `NULL` means all-day |
| `category` | TEXT | enum name |
| `repeat` | TEXT | enum name |
| `repeatIntervalMinutes` | INTEGER? | gap in minutes when `repeat` is `CUSTOM` |
| `isDone` | INTEGER | 0/1 |
| `completedAt` | INTEGER? | epoch millis, orders the Done history |
| `autoCompleted` | INTEGER | 0/1, set by the sweep |
| `createdAt` | INTEGER | epoch millis |

Currently at **version 2**; v2 added `repeatIntervalMinutes` via `MIGRATION_1_2`
in `AppDatabase`. Schema export is off (`exportSchema = false`), because the
debug and release KSP tasks race each other writing the same schema file when
both variants build in one Gradle invocation. Turn it back on with a
per-variant `room.schemaLocation` if you start needing schema diffs in review.

`AppDatabase` deliberately does **not** use `fallbackToDestructiveMigration` —
add a `Migration` whenever you bump the version so nobody loses their history.

---

## How scheduling works

```
TaskRepository.upsert(task)
        │
        ├── Room write
        └── AlarmScheduler.cancel(id) → schedule(id, triggerAt)
                                            │
                          setExactAndAllowWhileIdle (or setWindow fallback)
                                            │
                                      ReminderReceiver
                                            │
                     ┌──────────────────────┴──────────────────────┐
              NotificationHelper.show()              task.nextOccurrence()
                     │                                       │
        Done / Snooze buttons →                     re-armed for next date
        NotificationActionReceiver
```

One `PendingIntent` per task, keyed by `task.id`, so re-scheduling replaces the
previous alarm rather than stacking. Alarms in the past are never armed — the
overdue section and the auto-complete sweep handle those instead.

---

## Colour scheme

An indigo primary with a teal secondary, defined once in
`ui/theme/Color.kt` and mapped to Material 3 `lightColorScheme` /
`darkColorScheme` in `Theme.kt`. Theme follows the system by default and can be
forced to Light or Dark in Settings. Category dots use five muted tones so the
list stays quiet.

If you would rather use Material You, swap the schemes in `RemindlyTheme` for
`dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` behind a
`Build.VERSION.SDK_INT >= 31` check.

---

## Known limits

- Snooze is fixed at 10 minutes; there is no snooze-duration picker.
- Recurrence covers fixed intervals and simple date rules — no "every 3rd
  Tuesday" or "weekdays at 9 and 5" style rules.
- Interval reminders fire indefinitely; there is no end date or occurrence count.
- Aggressive OEM battery managers (Xiaomi, Oppo, Vivo, Samsung's stricter modes)
  can still delay alarms, and short intervals such as hourly are the first thing
  they throttle. Exempting the app from battery optimisation is the usual fix;
  the app does not prompt for it.
- No widget or wear support.
