# NOT BYPASS

Private Android browser. Mozilla GeckoView is the engine. There is no custom backend, no analytics, and no app-managed database.

> Status: **v1.0.0**. Signed release is published by GitHub Actions when a `v*` tag is pushed.

## Identity

| Item | Value |
|---|---|
| Name | NOT BYPASS |
| Package | `com.notbypass.browser` |
| Version | 1.0.0 |
| versionCode | 1 |
| Engine | Mozilla GeckoView `140.0.20250707120347` (arm64-v8a) |
| UI | Jetpack Compose |
| Minimum Android | 8.0 (API 26) |
| Target / compile | 36 |
| Storage | App-private JSON files |
| Backup | Disabled |
| Website | [GitHub Pages site](https://web-coder-lab.github.io/not-bypass/) |
| Author | Syed Muhammad Hussain Shah, Pakistan |

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
- Offline message: `You internet is not available please cheak and try again`
- allowBackup false, full backup rules blocked

## What is not in this build

- No custom server
- No account system
- No telemetry
- No extension store
- No desktop sync
- No iOS version
- Release APK is arm64-v8a only

## Install

1. Open the [releases page](https://github.com/web-coder-lab/not-bypass/releases/tag/v1.0.0).
2. Download `NOTBYPASS-v1.0.0.apk`.
3. On the phone, allow install from the browser if Android asks.
4. Open the app. Grant notification and download access only when a feature needs them.

Source archive for the same tag is attached as `NOTBYPASS-v1.0.0-source.zip`.

APK SHA-256:

```
5dbf4ba35d5e09c856ce8b0fa4b986385a4b535ca2f86cbf92d6e5e6cc51f958
```

Certificate SHA-256:

```
aea3a1714e059a47ebd34ee22c06d5f9f25ccfb08e98592eb5578a91ecb59289
```

Direct links:

- APK: https://github.com/web-coder-lab/not-bypass/releases/download/v1.0.0/NOTBYPASS-v1.0.0.apk
- SHA-256: https://github.com/web-coder-lab/not-bypass/releases/download/v1.0.0/NOTBYPASS-v1.0.0.apk.sha256
- Source: https://github.com/web-coder-lab/not-bypass/releases/download/v1.0.0/NOTBYPASS-v1.0.0-source.zip


## Signing

Release builds are signed inside GitHub Actions. The keystore never lives in the repository.

Secrets used by `.github/workflows/release.yml`:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS` (`notbypass`)
- `KEY_PASSWORD`

The certificate subject is `CN=Syed Muhammad Hussain Shah`. After the first release, the certificate SHA-256 is printed in the Actions log by `apksigner verify --print-certs`.

## Build locally

```bash
# requires Android SDK 36 and JDK 17
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
```

Release signing needs the same environment variables the Actions job uses.

## CI

- `.github/workflows/build.yml` runs on every push to `main`. It builds a debug APK and uploads it as an artifact.
- `.github/workflows/release.yml` runs on a `v*` tag. It builds the signed release APK, writes the SHA-256 file, packs a source zip, and publishes a GitHub Release.

## Source layout

```
app/src/main/java/com/notbypass/browser/
  MainActivity.kt
  NotBypassApp.kt
  BrowserEngine.kt
  Screens.kt
  Design.kt
  JsonStore.kt
  Models.kt
  AppVm.kt
```

## Website

The mobile page lives in `docs/`. GitHub Pages serves it from the `docs` folder on `main`.

- `docs/index.html`
- `docs/style.css`
- `docs/app.js`

The page is sized for a phone. It links to the latest release APK and the repository.

## License

App source in this repository is released by the author. GeckoView remains under the Mozilla Public License 2.0.
