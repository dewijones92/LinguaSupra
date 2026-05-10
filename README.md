# LinguaSupra

Android app that nags me (gently and warmly) into doing my daily Duolingo lessons across multiple languages.

Default daily plan: **1 Mandarin · 1 Latin · 5 Welsh** (configurable in-app). A persistent ongoing notification shows today's progress and lets you tap "+1" per language without opening the app. Smart-escalating reminders fire morning / afternoon / evening if anything is still outstanding. Idle-battery cost is effectively zero — no foreground service, no long-running process.

## Stack

- **Kotlin 2.3 + Jetpack Compose + Material 3** (single-Activity).
- **Room 2.7** for SQLite. Auto Backup on, so quotas survive a factory reset via your Google account.
- **AGP 9.2 / Gradle 9.5 / JDK 21**.
- **No foreground service.** The banner is a low-importance ongoing notification; per-language "+1" buttons are wired through `BroadcastReceiver` + `goAsync()` writing straight to Room.
- **`AlarmManager.setAndAllowWhileIdle`** (inexact) for reminders — no `SCHEDULE_EXACT_ALARM` permission.
- **`minSdk` 33 · `targetSdk` 35 · `compileSdk` 35**.

## Build & test

```bash
# debug APK
./gradlew :app:assembleDebug

# JVM unit tests
./gradlew :app:testDebugUnitTest

# Instrumented (feature) tests — needs an emulator or device attached
./gradlew :app:connectedDebugAndroidTest

# Lint
./gradlew :app:lintDebug
```

Android SDK location is read from `local.properties` (gitignored) — set `sdk.dir=` to your install.

## Branching

Trunk-based with short-lived feature branches.

- `main` is always green and deployable.
- Anything other than the most trivial change goes on a branch and through a PR. Naming convention: `feature/<thing>` for new work, `fix/<thing>` for bug fixes, `chore/<thing>` for tooling/refactoring, `docs/<thing>` for documentation only.
- CI (`.github/workflows/android.yml`) runs on every PR and every push to `main`:
  - **`build`** job (required gate): `assembleDebug`, `lintDebug`, `testDebugUnitTest`. Fast (~3–5 min).
  - **`instrumented`** job (advisory until enabled in branch protection): boots an API-35 emulator and runs `connectedDebugAndroidTest`. Slow (~10–15 min).
- Squash-merge PRs to keep `main` linear. Conventional Commit messages (`feat:`, `fix:`, `chore:`, `test:`, `docs:`).
- Don't force-push to `main`. Don't `--no-verify` to skip hooks.

To enable branch protection (one-time, requires repo admin):
- Settings → Branches → Add rule for `main` → require PR review + require status check `Build · lint · unit tests`.

## Repo layout

```
app/
├── src/main/java/com/dewijones/linguasupra/
│   ├── data/        # Room: Language, Completion, DAOs, AppDatabase, Repository, DateProvider
│   ├── notify/      # ongoing banner: BannerNotificationManager, RemoteViews builder, CompletionReceiver
│   ├── schedule/    # AlarmManager wrapper + ReminderReceiver + BootReceiver
│   ├── ui/          # Compose Home + Settings screens, Material 3 theme
│   └── debug/       # ExportDbReceiver (debug-only, copies DB for adb pull)
├── src/test/        # JVM unit tests (DateProvider arithmetic, etc.)
├── src/androidTest/ # instrumented feature tests (Repository, banner flow, reminders, boot persistence)
└── src/sharedTest/  # helpers reachable from both test/ and androidTest/ (e.g. MutableTestClock)
```

## Verifying the banner manually

```bash
# Boot the emulator and install
./gradlew :app:installDebug

# Skip the runtime POST_NOTIFICATIONS dialogue in dev
adb shell pm grant com.dewijones.linguasupra.debug android.permission.POST_NOTIFICATIONS

# Launch
adb shell am start -n com.dewijones.linguasupra.debug/com.dewijones.linguasupra.MainActivity

# Drop the shade
adb shell cmd statusbar expand-notifications

# Force-stop the app — banner should remain (notification owned by NotificationManagerService)
adb shell am force-stop com.dewijones.linguasupra.debug
```

## Inspect the DB on a debug build

```bash
adb shell am broadcast -n com.dewijones.linguasupra.debug/.debug.ExportDbReceiver
adb pull /sdcard/Android/data/com.dewijones.linguasupra.debug/files/lingua.db
sqlite3 lingua.db '.schema'
```
