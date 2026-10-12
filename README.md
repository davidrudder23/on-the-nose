# On The Nose

A guitar tuner and voice trainer for Android. It listens through the phone's microphone, works out the note being played or sung, and shows how flat or sharp it is on a needle gauge.

<img src="docs/screenshots/tuner.png" alt="The tuner showing B3 on string 2, 56 cents flat" width="270"> <img src="docs/screenshots/voice-training.png" alt="Voice training asking for the note D, with Play note and New note buttons" width="270">

## Features

- **Needle gauge** covering 50 cents flat to 50 cents sharp, with a green zone in the middle. The note and needle turn green within 5 cents of in tune, and the Flat or Sharp label turns red when the note is off on that side.
- **Tunings.** In a tuning, the tuner aims for the nearest open string and shows its number (1 is the highest-pitched string, 6 the lowest). Chromatic aims for the nearest note of any kind.

  | Tuning | Strings, low to high |
  |---|---|
  | Chromatic | any note |
  | Standard | E A D G B E |
  | Drop D | D A D G B E |
  | Half step down | D# G# C# F# A# D# |
  | Open G | D G D G B D |
  | Open D | D A D F# A D |
  | Open E minor | E B E G B E |
  | Open D minor | D A D F A D |
  | DADGAD | D A D G A D |

- **Voice training.** A second view picks a random note for you to sing and shows how close you are on the same gauge. Any octave counts, so it suits any vocal range. **Play note** sounds the target so you can hear it, and **New note** picks another.

- **Private.** Audio is analysed on the device and never recorded or sent anywhere. The app has no ads, no analytics and no internet permission. See the [privacy policy](https://davidrudder23.github.io/on-the-nose/privacy/).

## Building

You need JDK 21 and the Android SDK (platform 37, build-tools 37.0.0). Point `ANDROID_HOME` at the SDK, or put `sdk.dir=/path/to/Sdk` in a `local.properties` file at the project root.

```
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run the unit tests
./gradlew installDebug         # install on a connected device
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The app runs on Android 8.0 (API 26) and newer.

## How it works

- `audio/PitchDetector.kt` estimates the fundamental frequency with the YIN algorithm. It has no Android dependencies, so it is unit tested on the JVM with synthesized tones.
- `audio/PitchSource.kt` reads the microphone and emits a stream of detected pitches. The microphone is released whenever the app is in the background.
- `audio/Note.kt` and `audio/Tuning.kt` turn a frequency into the note to tune towards and the offset from it in cents.
- `audio/Tone.kt` and `audio/TonePlayer.kt` generate and play the target note for voice training.
- `ui/` holds the Jetpack Compose screens: the gauge shared by both views, the tuning picker, and voice training.

The app always opens on the tuner with the chromatic scale; it does not store any settings.

The app ID and the Kotlin package are both `org.noses.onthenose`.
