# Build report v1.1.0

Status: source rewritten and statically reviewed. **Not compiled and not run** in the authoring environment (no Android SDK, no network). First build on CI or your machine may need small fixes.

Check first if the build fails (GeckoView 140 API names written from memory):
1. `NavigationDelegate.onLocationChange` signature (4 parameters with `hasUserGesture`)
2. `GeckoRuntimeSettings.setAllowInsecureConnections`, `HTTPS_ONLY`, `ALLOW_ALL`
3. `StorageController.ClearFlags.PERMISSIONS`, `ALL_CACHES`, `COOKIES`, `ALL`
4. `setAutomaticFontSizeAdjustment`, `setFontSizeFactor`

Engine: GeckoView 140.0.20250707120347 (arm64-v8a). Minimum Android 8.0 (API 26).
APK: `app/build/outputs/apk/debug/app-debug.apk` after `./gradlew assembleDebug`.
