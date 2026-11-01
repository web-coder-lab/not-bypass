# NOT BYPASS

Private Android browser. Mozilla GeckoView is the engine. There is no custom backend, no analytics, and no app-managed database.

> Status: **v1.1.0**. Signed release is published by GitHub Actions when a `v*` tag is pushed.

## Identity

| Item | Value |
|---|---|
| Name | NOT BYPASS |
| Package | `com.notbypass.browser` |
| Version | 1.1.0 |
| versionCode | 2 |
| Engine | Mozilla GeckoView `140.0.20250707120347` (arm64-v8a) |
| UI | Jetpack Compose |
| Minimum Android | 8.0 (API 26) |
| Target / compile | 36 |
| Storage | App-private JSON files |
| Backup | Disabled |
| Website | [GitHub Pages site](https://web-coder-lab.github.io/not-bypass/) |
| Author | Syed Muhammad Hussain Shah, Pakistan |

## What is implemented (v1.1.0)

- Chrome-style UI: rounded address pill, tab-count button, three-dot menu, new tab page with shortcuts, tab switcher (grid/list, private tabs), suggestions while typing
- Dark / AMOLED / Light / system themes, private-tab theme
- Real GeckoView navigation: back, forward, reload, stop, find in page, desktop site, text size, view source, full-screen video, popups/new windows, external-app links, context menu (links/images)
- Error pages: offline (exact requested message with Retry / Go Home / Network Settings), DNS, timeout, refused, certificate (never bypassed)
- Downloads streamed from the engine into MediaStore (images, videos, others), progress, cancel, retry, open, share, delete, notifications
- Bookmarks (edit, folders, search, sort, import/export), shortcuts, custom search engines
- Permission prompts for location / camera / microphone with Allow or Block
- Notification center (filters, read/unread, clear) and Android notifications
- Network monitor and on-demand diagnostic; Privacy dashboard with only verifiable status
- 25 settings pages; every switch is wired to real behaviour
- Local JSON storage with atomic writes; no history, no backend, no analytics

## Not implemented

Proxy (GeckoView has no supported API), Web Push, reader mode, QR scanner, voice search, autofill, search suggestions from the network, tab previews, pause/resume downloads.

## Build

`./gradlew assembleDebug test` (needs Android SDK 36, JDK 17, about 8 GB RAM). The GitHub Actions workflow builds the APK for you.
