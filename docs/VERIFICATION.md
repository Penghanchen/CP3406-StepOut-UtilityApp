# Verification record

Project: StepOut · CP3406 Assignment 1 Utility App
Student: Penghan Chen · 14758250
Date: 29 October 2026

## Executed checks

The final source was checked on Windows with JDK 21.0.4, Gradle 8.13, Android Gradle Plugin 8.13.2, Android SDK Platform 36 and Build Tools 35.0.0. The complete verification command was:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest
```

| Check | Actual result |
| --- | --- |
| Kotlin and Android resource compilation | Passed |
| Planner and formatting tests | 14 passed |
| API response validation and interval mapping tests | 6 passed |
| ViewModel loading, failure, refresh and city concurrency tests | 6 passed |
| Compose interaction tests | 4 passed using Robolectric API 35 |
| Compose snapshot export | 1 passed using Robolectric API 34 native graphics |
| Total local automated tests | **31 passed, 0 failed, 0 skipped** |
| Connected Android instrumentation tests | **3 passed, 0 failed, 0 skipped** on API 37 |
| `assembleDebug` and instrumentation APK assembly | Passed |
| `lintDebug` | Passed; **0 errors, 17 warnings**, all dependency or build-tool update advisories |
| Emulator acceleration | WHPX operational; Windows reports HypervisorPresent=True after restart |
| Actual application installation and cold launch | Passed on Android 17 / API 37 |
| Live Open-Meteo forecast | Loaded current conditions, hourly outlook and a walking window for Townsville |
| Manual device acceptance checks | Completed as described below |

Dependencies remain pinned to the tested versions, including the course starter's core toolchain. No lint warnings were suppressed.

Local test fixtures exercise interval alignment, gaps, missing precipitation, duration coverage, threshold equality, nighttime rejection, the exact one-hour staleness boundary and temperature conversion. ViewModel tests cover failed-refresh retention, new-city failure clearing old data, responses that ignore cancellation, preference changes without redundant requests and the 30-minute refresh boundary.

## Actual emulator and device tests

The installed Medium_Phone_API_37.0 AVD runs Android 17 / API 37, x86_64, in Android Emulator 37.2.12.0. Windows Hypervisor Platform is active and the emulator reports WHPX (10.0.26200) installed and usable. CPU virtualization is hardware accelerated. The portrait display is 1080 by 2400 pixels at 420 dpi; host GPU rendering was enabled.

`MainActivitySmokeTest` runs the actual activity and verifies:

1. Temperature units and the selected tab survive activity recreation.
2. Android Back returns from Settings to Walk.
3. City, walk duration, rain limit, temperature units and appearance survive recreation, and the settings page scrolls to source attribution, licence and author information.

Each device test restores default preferences before and after execution. These assertions do not depend on weather-service availability. Separate manual checks confirmed actual network integration.

## Manual acceptance and correction

- **Live loading:** a fresh app launch retrieved Townsville's modelled conditions, hourly forecast and walking suggestion. City-local display time differed correctly from the emulator's UTC status clock.
- **Themes and units:** light and dark screens and their system-bar icons were inspected; Celsius/Fahrenheit changes displayed correctly.
- **Process restart:** Fahrenheit and dark appearance remained selected after force-stopping and reopening the application; preferences were subsequently restored to defaults.
- **Offline behavior:** disabling emulator connectivity produced an understandable initial-load error with no invented weather. A failed refresh within the same session retained the last successful same-city forecast and labelled the failure.
- **Recovery:** restoring connectivity and selecting Try again loaded a fresh forecast and cleared the error state.
- **Layout:** portrait, landscape, vertical page scrolling, the horizontal hourly outlook and 200% system font size were inspected. Source links and author details remained reachable at the enlarged font size.
- **Scrolling correction:** enlarged text exposed scroll drawing into the transparent status-bar area. The shared content container now clips its drawing after applying Scaffold insets. The corrected settings footer was inspected again at 200% font size, and the complete local/device test suites passed after the change.

The emulator was restored to portrait orientation, normal font size, enabled connectivity and the default application preferences. The app remains installed for further inspection.

## Snapshot provenance and compatibility limits

Files in `docs/screenshots/` are actual light, dark and settings captures from the running API 37 emulator with live weather on 29 October 2026. The Robolectric screenshot test independently generates deterministic fixture previews in `app/build/reports/screenshots/`.

No physical Android device was tested. API 26 compatibility is declared and compiled but has not been exercised on an API 26 runtime. The successful API 37 run does not establish compatibility with every Android version or manufacturer device. Hourly, city-centre weather estimates remain subject to the limitations described in the README.

## Version control

This submission is delivered as one final source commit. It does not demonstrate a multiweek incremental commit history.
