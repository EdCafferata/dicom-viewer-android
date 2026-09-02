# Dicom Viewer (Android)

🔒 Laatste security check: 2026-09-02 21:27 CEST

Android port of [Dicom Viewer by The IT Crowd](https://github.com/EdCafferata/DICOM-player), a free native medical DICOM image viewer — series navigator, Window/Level presets, cine playback for multi-frame studies (e.g. cardiac angiography), and frame export.

Native Kotlin + Jetpack Compose, not a cross-platform framework — the DICOM parser (file meta header, explicit/implicit VR, raw and JPEG/JPEG-Lossless pixel data) is a 1:1 port of the iOS app's own hand-written parser, not a third-party DICOM library.

## Status

**Working:**
- DICOM file parsing: explicit/implicit VR, raw and encapsulated (JPEG, JPEG Lossless/SOF3) pixel data, multi-frame series
- Series navigator — groups files by Series Instance UID, header-only parse so a whole folder groups quickly
- Viewer: pinch-to-zoom/pan, cine playback with adjustable FPS, frame slider, frame export (PNG/JPEG) via share sheet
- Window/Level presets (Abdomen/Long/Bot/Hersenen) for raw grayscale pixel data, re-windowed from the original raw samples via Rescale Slope/Intercept — not available for JPEG-compressed or color images, same limitation as iOS
- Bundled demo DICOM files (cardiac angiography) for trying the app without importing your own scan
- Import via the system document picker (SAF), recent-files tracking, delete

**Not yet ported:**
- **Tip jar** — the iOS app uses StoreKit 2; Android's equivalent is Google Play Billing, which requires product IDs configured in a Google Play Console listing. No Play Console account exists yet for this app (that's a paid, account-creation step only the account owner can do).

## Requirements

- JDK 17+ (project built and tested with Homebrew's `openjdk@21`)
- Android SDK, compileSdk 36, minSdk 26
- Gradle 9.6.1+ (via the included wrapper)

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools   # or your own SDK path
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Licence

GPL-3.0 — see [LICENSE](LICENSE), same as the iOS app.
