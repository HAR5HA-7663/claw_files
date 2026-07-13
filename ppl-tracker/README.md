# 🏋️ PPL Tracker

A tiny, fully-offline Android app for a 4-week **Push / Pull / Legs (5-day)**
program. Open it at the gym and it shows **today's** workout automatically —
tap off each exercise, log the weight you used, and it remembers everything
between sessions.

- **Package:** `com.ppl.tracker` · **Version:** 1.0
- **minSdk 26 (Android 8.0)** · **targetSdk 34** · fully offline · no permissions
- **Dark theme**, big readable text for mid-workout use
- UI written with the plain Android View toolkit (no AndroidX / Compose)
- Program data is embedded in the app (see `Program.kt`) — nothing to import

**Prebuilt debug APK:** [`app-debug.apk`](./app-debug.apk) (~740 KB)

---

## Features

1. **Today, automatically.** On launch it computes today's date, finds the
   matching week + day in the cycle, and shows only that workout.
2. **Day title + exercise list**, e.g. *"Week 2 — Push A: Chest Focus"* with each
   exercise's `sets x reps`.
3. **Checkboxes** for each exercise. Ticks are saved locally (SharedPreferences)
   and survive restarts. They're keyed to the calendar date, so **each new day
   starts fresh**.
4. **Weight logging.** A number field next to each exercise. Weights are saved
   per date; the next time that exercise comes up, it shows **"Last: X kg"** from
   your most recent previous session.
5. **Week/Day picker** at the top (two dropdowns + a **Today** button) to browse
   any other day of the program.
6. **Cycle logic.** The program is 4 weeks. **Week 1 starts Monday, 13 July 2026.**
   After Week 4 it loops back to Week 1 forever. **Saturday & Sunday are rest
   days** — they show *"Rest Day 💤"* with a recovery note.

### Weekly schedule

| Day | Workout |
|-----|---------|
| Monday | Push A |
| Tuesday | Pull A |
| Wednesday | Legs |
| Thursday | Push B |
| Friday | Pull B |
| Saturday / Sunday | Rest |

---

## Install

1. Copy `app-debug.apk` to your phone (or `adb install app-debug.apk`).
2. Allow "install from unknown sources" if prompted.
3. Open **PPL Tracker**.

It's a debug-signed APK, so it installs side-by-side without a Play listing.

---

## Building it yourself

### Option A — normal Android tooling (recommended)

Open the project in **Android Studio**, or from a machine with a normal Android
SDK and internet access:

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Everything is standard: `settings.gradle.kts`, `build.gradle.kts`,
`app/build.gradle.kts`, and a Gradle wrapper are all included. AGP 8.5.2 /
Kotlin 2.0.21 / Gradle 8.7.

### Option B — the manual pipeline used to produce the committed APK

This repo's `app-debug.apk` was **not** built with Gradle. The CI/build
environment it was produced in blocks Google's Maven host (`dl.google.com`),
so the Android Gradle Plugin, AndroidX and Compose cannot be downloaded there
and `./gradlew` cannot run. Because the app deliberately uses only the bare
Android framework, it can instead be built straight from the command-line tools:

```
kotlinc  →  dx (dex)  →  aapt2 (resources)  →  zipalign  →  apksigner
```

`build.sh` runs that pipeline. It expects `android.jar` (API 34), a Kotlin
compiler, and `aapt2` / `dx` / `zipalign` / `apksigner` to be available (the
last four come from the Debian `android-sdk-build-tools` / `dalvik-exchange` /
`apksigner` packages). It's here mainly to document exactly how the APK was
made and to reproduce it in the same environment:

```bash
./build.sh
# -> build/app-debug.apk
```

Prefer **Option A** on your own machine — it's the normal, supported path.

---

## Project layout

```
ppl-tracker/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/ppl/tracker/
│       │   ├── MainActivity.kt      # all UI + logic (built in code)
│       │   └── Program.kt           # the embedded 4-week program data
│       └── res/                     # strings, colors, dumbbell launcher icon
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/
├── build.sh                         # manual command-line build
└── app-debug.apk                    # prebuilt, debug-signed
```
