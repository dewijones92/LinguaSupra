# CLAUDE.md

Read this first. It is the entry point for any Claude Code session in this repo.

## What is this

**LinguaSupra** is a personal Android app that motivates the user (dewijones92) to do daily Duolingo lessons across multiple languages. Default plan: **1 Mandarin · 1 Latin · 5 Welsh** per day, configurable in-app. The defining feature is a **persistent ongoing notification** (the "banner") with tap-to-record per-language buttons. Smart-escalating reminders fire morning / afternoon / evening if anything is still outstanding.

## Stack

| | |
|---|---|
| Language | Kotlin 2.3.21 (AGP 9.2 has built-in Kotlin support — no `kotlin-android` plugin) |
| UI | Jetpack Compose + Material 3 (Compose BOM 2026.04.01), single-Activity |
| Persistence | Room 2.7.1 via KSP 2.3.7 |
| State | Plain Kotlin Flow + simple ServiceLocator (`AppContainer`) — no Hilt, no NgRx-style store |
| Build | AGP 9.2.0, Gradle 9.5.0, JDK 21 Temurin |
| `minSdk` | 33 · `targetSdk` 35 · `compileSdk` 35 |

## Architectural invariants — do NOT change these without checking with the user

These were carefully chosen during planning. Reverting them re-introduces problems we already considered.

1. **No foreground service for the banner.** The banner is an ongoing low-importance notification (`setOngoing(true)`, `IMPORTANCE_LOW`) posted from a regular component. The notification record is owned by `NotificationManagerService`, so the process can be reaped — idle CPU/battery is effectively zero. If you ever feel tempted to wrap it in an FGS for "reliability", don't — Android 14+ tightened FGS types and our use case fits none of them cleanly.
2. **`AlarmManager.setAndAllowWhileIdle()` (inexact)** for reminders, not `setExactAndAllowWhileIdle`. This avoids the `SCHEDULE_EXACT_ALARM` permission gauntlet (auto-revoked on update on Android 13+, Play Store rejection-bait). 09/14/19 nudges fire within a 10-min Doze maintenance window, which is fine for "you should do your Welsh".
3. **Day-rollover is derived on read**, not via an alarm. Every `Completion` row carries `day_local_iso` (`yyyy-MM-dd` in `ZoneId.systemDefault()`); today's count is `WHERE day_local_iso = :today`. UI re-renders on `Intent.ACTION_DATE_CHANGED`. No 00:01 alarm.
4. **DB lives in internal `filesDir`**, NOT external storage. `adb pull`-ability comes from a debug-only `ExportDbReceiver` that copies to `getExternalFilesDir(null)` on demand. Production data path stays clean.
5. **Banner UI is `RemoteViews` + `DecoratedCustomViewStyle`** with `addView(R.id.banner_rows_container, rowRemoteViews)` per active language. Three-action `addAction` is too few for ≥4 languages; `RemoteViews` lifts the cap.
6. **Emojis on the banner are rasterised to `Bitmap`** via `EmojiBitmapFactory` (cached). `RemoteViews` ImageButton/ImageView can't render emoji unicode reliably across launchers.
7. **All `PendingIntent`s use `FLAG_IMMUTABLE`** (mandatory on API 31+).
8. **Auto Backup is on** (`data_extraction_rules.xml` includes `lingua.db`). Quotas survive factory reset via Google Drive. Exclude WAL/SHM.
9. **Over-quota is allowed.** Welsh 7/5 is a valid state (the user wants to track real effort). Don't clamp.
10. **Banner sort order is fixed `display_order`**, not most-pending-first. The user prefers a calm, predictable banner.

## Commands

```bash
# debug APK
./gradlew :app:assembleDebug

# JVM unit tests (DateProvider arithmetic, etc.)
./gradlew :app:testDebugUnitTest

# Instrumented tests on connected device/emulator (Repository, banner, reminders)
./gradlew :app:connectedDebugAndroidTest

# Lint
./gradlew :app:lintDebug

# Install on the antennapod AVD
./gradlew :app:installDebug
```

Android SDK lives at **`/home/dewi/code/android-sdk`** (set via `local.properties`, gitignored). The dev AVD is **`antennapod`** (API 35). Boot it with:

```bash
$ANDROID_HOME/emulator/emulator -avd antennapod -no-snapshot-load -no-audio -no-boot-anim &
```

## Repo layout

```
app/src/
├── main/java/com/dewijones/linguasupra/
│   ├── data/            # Room: Language, Completion, DAOs, AppDatabase + SeedCallback,
│   │                    #   Repository, DateProvider, AppContainer (manual DI)
│   ├── notify/          # Ongoing banner: Notifications (channels), BannerNotificationManager,
│   │                    #   BannerRemoteViewsBuilder*, CompletionReceiver, EmojiBitmapFactory
│   ├── schedule/        # Phase 5+: ReminderScheduler, ReminderReceiver, BootReceiver
│   ├── ui/              # Phase 4: HomeScreen, SettingsScreen, theme/
│   └── debug/           # Phase 8 (debug build only): ExportDbReceiver
├── main/res/layout/     # banner_collapsed.xml, banner_expanded.xml, banner_row.xml
├── main/res/xml/        # data_extraction_rules.xml (Auto Backup config)
├── test/                # JVM unit tests
├── androidTest/         # Instrumented (feature) tests
└── sharedTest/          # Helpers reachable from BOTH test/ and androidTest/ (e.g. MutableTestClock)
```

## Branching & PRs

Trunk-based, short-lived feature branches. Convention:

- `main` is always green and deployable.
- Branch naming: `feature/<thing>` · `fix/<thing>` · `chore/<thing>` · `docs/<thing>` · `ci/<thing>`.
- Squash-merge into `main`. Linear history.
- **Conventional Commit messages**: `feat:`, `fix:`, `chore:`, `test:`, `docs:`, `ci:`.
- Co-Authored-By trailer on every commit Claude makes.
- **Never** force-push to `main`. **Never** `--no-verify` to skip hooks.

CI (`.github/workflows/android.yml`) runs on every PR + push to main:
- **`build`** job (required): assemble + lint + unit tests (~3–5 min).
- **`instrumented`** job (advisory): connectedDebugAndroidTest on an API-35 emulator (~10–15 min).

## Tests

Tests are first-class. The user explicitly asked for "well written feature tests".

- **JVM unit tests** (`app/src/test/`): pure logic only — date arithmetic, progress calculations, copy formatting. Fast feedback loop.
- **Instrumented feature tests** (`app/src/androidTest/`): full-flow tests that use real Room + a controllable `MutableTestClock`, covering each user-visible behaviour (record completion, day rollover, over-quota, reminder skip-when-complete, boot persistence, settings CRUD).
- **`sharedTest/`** holds helpers usable from both — wired via `kotlin.srcDir("src/sharedTest/java")` in both source sets.

## Working with the codebase

- **Personal app**, single user (the dev). British English. Playful, emoji-forward UI tone.
- **DRY** is the default — but don't over-abstract for hypothetical reuse.
- **No comments explaining what code does**; only why if non-obvious.
- **Don't introduce Hilt** unless the manual `AppContainer` actually breaks down; right now it's clean.
- **Don't add backwards-compat shims** — minSdk 33 means we can use modern APIs directly.
- **Verify on the emulator** before declaring a phase done. The user trusts the AVD as ground truth — screenshots into `dev/screenshots/` are good evidence to attach to PRs.

## Plan reference

The full multi-phase plan lives at `/home/dewi/.claude/plans/hey-i-am-just-indexed-kettle.md`. Use it as a reference for: aesthetic direction (playful, emoji-forward, Material 3 expressive), the 14-step verification bar, the stretch ideas (streaks, weekly heatmap, language-specific encouragement copy), and the open OEM caveats.
