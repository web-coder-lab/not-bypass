# Build report

Date: 2026-10-31

## Build status

Source compiles. Debug APK was not produced in this environment.

`compileDebugKotlin` completed. Packaging failed later when the Gradle process was killed.

Cause: the machine has about 1.9 GB RAM and swap cannot be enabled. GeckoView packaging needs more memory. The daemon exited with `DaemonDisappearedException` during `mergeDebugGlobalSynthetics` / asset packaging.

## How to build the APK

On a machine with at least 8 GB RAM:

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug test
```

Expected APK:

`app/build/outputs/apk/debug/app-debug.apk`

## Engine

| Item | Value |
|---|---|
| Requested latest stable | GeckoView `159.0.20261022150525` |
| Used | GeckoView `140.0.20250707120347` arm64-v8a |
| Reason | 159 requires Android API 37 and AGP 9.1. This environment only has API 36. |
| ABI | arm64-v8a only |

## Implemented

- Real GeckoView runtime and sessions
- URL/search, back, forward, reload, stop
- Tabs and private sessions
- Local JSON bookmarks and settings
- 22 settings categories as real screens
- Browser Experience presets (UI only; engine stays GeckoView)
- Android DownloadManager
- Notification channels and in-app notice list
- Network connectivity check
- Tracking protection set to GeckoView Standard
- History off by default
- Backup disabled

## Not complete

- Installable APK from this environment
- Physical-device tests
- Web Push
- Full proxy application
- QR scanner
- Voice search
- Reader mode
- Gallery-specific MediaStore routing
- Invented tracker counts (intentionally not added)

## Tests

Unit tests are in `app/src/test`. They were not finished in this environment because packaging killed the build before the test task completed. They can be run with `./gradlew test` on a larger machine.
