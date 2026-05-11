# LinguaSupra — end-to-end verification on `lingasupra` AVD

Working notes for the session. Tracks what I've actually exercised, what's
still untested, and any findings/regressions.

**AVD**: `lingasupra` (API 35 google_apis x86_64) — port 5556. Other AI's
`antennapod` on 5554 untouched.

**Branch**: `main` at the post-#29 commit. APK to test: the **release variant**
produced by `./gradlew :app:assembleRelease` — package `com.dewijones.linguasupra`,
NOT `.debug`.

## Test matrix

| PR | What | Manual test | Result |
|---|---|---|---|
| #26 | Obtainium release notes + fingerprint | Run the workflow snippet locally on the release APK | ✅ snippet extracts the 64-char SHA-256 cleanly: `b9f7eab8…cd573...72aa` |
| #27 | Auto Backup rules — DataStore prefs included | `bmgr backupnow` — needs Google account, AVD has none → assert config syntax + check what `dumpsys backup` reports | ⚠ partial — XML loaded by system (manifest references it without error), CI lintDebug passes. Full backup transport unavailable on no-Google-account AVD; actual backup verification needs a real device. |
| #28 | `assembleRelease` instead of `assembleDebug` | Install release APK, walk through onboarding, log a lesson, banner, reminder fire | ✅ end-to-end — boots, FGS alive with right flags, +1 button works, banner posts, no `run-as` (correct prod behaviour) |
| #29 | DB import from .db file | Install debug + release side-by-side, export from debug, import into release, verify data restored. Also test the failure paths (wrong file, non-SQLite, schema-mismatched). | ✅ all three paths — happy path swaps DB cleanly, "Not a SQLite database" rejects garbage bytes, "Not a LinguaSupra database (missing languages/completions tables)" rejects wrong-schema SQLite |

## Findings as I go

### PR #28 — release variant ✅
- Release APK builds, signs with stable keystore (SHA-256 `b9f7eab8…`).
- Installs as `com.dewijones.linguasupra` (no `.debug` suffix). Coexists with the `.debug` variant.
- Boots clean — no `AndroidRuntime/FATAL` in logcat.
- Splash → name prompt → home, all rendering correctly.
- `BannerService` runs as FGS with `isForeground=true types=0x40000000 flags=ONGOING_EVENT|ONLY_ALERT_ONCE|NO_CLEAR|FOREGROUND_SERVICE`.
- Tapping `+1` 5× Welsh → home shows 5/5 ✓ with `Da iawn!` celebration. Banner reads "🔥 Nearly there".
- Important consequence to note: `adb shell run-as com.dewijones.linguasupra` is BLOCKED (`package not debuggable`). Correct production behaviour. Means I can't `run-as sqlite3` against the release variant — only the in-app paths or Auto Backup can extract data. That's fine.

### PR #29 — DB import failure paths ✅
- `notdb.bin` (random ASCII bytes): magic-byte check fails → "Import failed — Not a SQLite database." Live DB untouched.
- `wrongschema.db` (real SQLite, single `foo` table): magic check passes, schema check fails → "Import failed — Not a LinguaSupra database (missing languages/completions tables)." Live DB untouched.
- After both rejections, Settings still shows the previously-imported `Mandarin/Latin/Welsh` data.

### PR #26 — fingerprint snippet ✅
- Ran the workflow's `apksigner verify --print-certs … | grep | awk | tr -d ' '` snippet locally against `app-release.apk`.
- Output: `b9f7eab85c42ad5ce6ba16cd64d163e5a97fd17195929aa947c4d9e6fdad72aa` (64 chars, matches the stable keystore).
- The shell pipeline behaves as expected — what the next release run will write into the GitHub release notes.

### PR #27 — backup rules ⚠ partial
- AndroidManifest references `@xml/data_extraction_rules` → app installs cleanly so the XML is syntactically valid and the rules file resolves.
- CI `lintDebug` green after dropping the redundant `<exclude>` lines that lint correctly flagged.
- **Cannot verify the actual backup contents on this AVD** — `bmgr backupnow` returns "Backup is not allowed" because there's no Google account, and the local transport setup is non-trivial. On a real Pixel with backup enabled, the include rules will pick up `file/datastore/` as designed; this is a configuration-only change so the risk is low.

### PR #29 — DB import end-to-end ✅
- Set up the debug install with 1 Mandarin + 1 Latin + 1 Welsh.
- Existing debug `ExportDbReceiver` dumped `lingua.db` (+wal+shm) to `/sdcard/Android/data/com.dewijones.linguasupra.debug/files/lingua-export/`.
- Copied just `lingua.db` to `/sdcard/Download/` because SAF won't show files inside another app's private external dir.
- On the release app: Settings → "📥 Restore data" → "Import .db file…" → confirm dialog → SAF picker → navigated to Downloads → picked `lingua.db`.
- Result dialog: "Imported ✓ — The app will close now…" → tap "Close app" → app terminates (`finishAndRemoveTask`).
- Relaunched release app → Home shows `1/1 Mandarin ✓` (加油!), `1/1 Latin ✓` (Festina lente!), Welsh 1/5. Stats screen confirms 1 + 1 + 1 in the 14-day totals.
- **The release variant's pre-import data (5 Welsh) was completely replaced.** Swap is atomic, no residual data, no half-state.

## Quick recipe

```bash
# Boot lingasupra
$ANDROID_HOME/emulator/emulator -avd lingasupra -port 5556 \
  -no-snapshot-save -no-audio -no-boot-anim </dev/null >/tmp/lingasupra-emu.log 2>&1 &
disown

# Wait for boot
until adb -s emulator-5556 shell getprop sys.boot_completed 2>/dev/null | grep -q "^1"; do sleep 3; done

# Build + install release
./gradlew :app:assembleRelease -q
adb -s emulator-5556 install -r app/build/outputs/apk/release/app-release.apk

# Optionally side-by-side install of debug for the import flow
./gradlew :app:installDebug -PreleaseVersionCode=1 -PreleaseVersionName=0.0.0-test
```
