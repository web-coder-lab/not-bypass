# NOT BYPASS

Private Android browser. Mozilla GeckoView is the engine. There is no custom backend, no analytics, and no app-managed database.

## Identity

| Item | Value |
|---|---|
| Name | NOT BYPASS |
| Package | `com.notbypass.browser` |
| Version | 1.0.0 |
| Engine | Mozilla GeckoView `140.0.20250707120347` (arm64-v8a) |
| UI | Jetpack Compose |
| Minimum Android | 8.0 (API 26) |
| Target / compile | 36 |
| Storage | App-private JSON files |
| License of engine | MPL-2.0 |

## What is implemented

- Real GeckoView runtime and sessions
- Open URL, search, back, forward, reload, stop
- Multiple tabs and a separated private session
- Bookmarks saved in local JSON
- Settings saved in local JSON
- 22 settings categories as real screens
- Browser Experience presets (interface only; engine stays GeckoView)
- Android DownloadManager for downloads
- Native notification channels
- In-app notification center
- Network connectivity check
- Tracking protection set to GeckoView Standard
- HTTPS indicator from GeckoView security info
- History disabled by default
- Backup disabled

## Honest limits

- Profiles do not change the engine to Chromium or DuckDuckGo.
- Private browsing does not hide activity from websites, networks, or ISPs.
- Proxy settings are stored. Full GeckoView proxy application is limited by Android and the engine. Automatic rotation is not implemented.
- Web Push is not implemented.
- Tracker counts are not invented.
- Gallery placement depends on Android MediaStore and the download destination. Files go to Downloads.
- QR scanning uses no third-party scanner in this build. Paste a URL instead.
- Voice search is not wired in this build.
- Reader mode is not a separate engine feature in this build.
- This build is arm64-v8a only. 32-bit ARM and x86 are not included.
- Latest GeckoView 159 needs Android API 37 and AGP 9.1. This environment only has API 36, so GeckoView 140 is used.

## Build

```bash
export ANDROID_HOME=/opt/android-sdk
./gradlew assembleDebug test
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Privacy

No browsing data is sent to a developer server. Settings and bookmarks stay in the app's private files. History is off by default.
