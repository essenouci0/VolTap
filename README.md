# VolTap

Use your phone's **volume keys as screen taps**. Press Volume Up to tap one spot on the screen, Volume Down to tap another. VolTap starts by itself when you open an app you set up, and stops when you leave it.

Android only. Written in Kotlin with Jetpack Compose.

## How it works

1. **Add an app** under *App profiles* (a game, a camera, a reader, anything).
2. Press **Choose a screenshot**. Pick a screenshot of that app, taken on this phone. Tap or drag on it to put the blue ring (Volume Up) and the red ring (Volume Down) on the buttons you want tapped. A magnifier and 1 px arrow buttons help you be exact, then **Save positions**.
3. Open the app any time. VolTap detects it and turns on. Volume keys now tap the saved spots. Leave the app and the keys behave normally again.

Positions are saved separately for portrait and landscape. Take the screenshot in the other orientation to set those positions (VolTap reads the orientation from the screenshot).

### Languages

English and Arabic (full right-to-left layout). Settings → Appearance → Language lets you follow the system or pick one.

### Per-app options

- Start automatically
- Hold to repeat (slow, normal, fast), with a warning about online games
- Hide targets (no rings drawn over the app)
- Lock volume level (the keys never change the sound)

### Global options

Tap speed, show targets, vibrate on tap, start notice, ignore keyboard and notification shade, theme (light, dark, system), and a **latency test**.

## Latency

Nothing is truly instant, but VolTap keeps the path short:

- the volume key is received inside the accessibility service itself;
- the tap gesture for each position is built once and reused;
- the tap is sent on key **press**, not on release;
- the touch is very short (2 to 30 ms depending on *Tap speed*).

The delay that remains comes from Android's gesture injection and varies by phone. Use *Settings → Latency test* to see the number on your device. A faster engine through Shizuku is planned and is shown as "soon" in Settings.

## Permissions

VolTap needs its **accessibility service** turned on (Settings → Accessibility → VolTap). It is used for exactly three things: knowing which app is in front, receiving volume key presses, and performing taps. It does not read screen content and has no internet permission.

Some online games forbid automated input. Use *Hold to repeat* with care.

## Build

Requires JDK 17 and the Android SDK (compile SDK 35).

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. The GitHub Actions workflow in `.github/workflows/build.yml` builds it on every push and uploads it as an artifact.

## Project layout

```
app/src/main/java/com/voltap/app/
  VolTapService.kt   accessibility service: app detection, key handling, taps, latency test
  Overlays.kt        faint ring markers over the app
  Lang.kt            in-app language
  ui/ShotScreen.kt   screenshot-based tap placement
  Models.kt          profiles, settings, storage
  MainActivity.kt    navigation
  ui/                theme, logo, shared components, screens
```

## Status

Built by GitHub Actions on every push; the newest APK is on the Releases page (`latest`).
