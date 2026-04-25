# Plan: Audio Infrastructure

## Group 1: WAV Parser

1. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/audio/WavAmplitude.kt`: single-pass PCM WAV parser (mono/stereo, 8/16/24/32-bit LE) producing an `AmplitudeTrack` with per-bucket RMS values normalized to peak → `1284f2b`.

---

## Group 2: Platform Cache + Test Asset

2. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/platform/CacheFile.kt`: expect `writeBytesToCache(filename, bytes): String`. Add JVM actual writing under `java.io.tmpdir/kmp_voice_ring/`. Add android/ios/wasmJs actuals throwing `NotImplementedError` (desktop-first iteration) → `b58a87b`.

3. Ship CC BY 3.0 test asset `composeApp/src/commonMain/composeResources/files/audio/Jan_Morgenstern_-_01_-_Prelude.wav` (~5.7 MB, 2ch, 16-bit, 44100 Hz, ~32.29 s) → `b58a87b`.

---

## Group 3: Desktop Player

4. Create `composeApp/src/jvmMain/kotlin/com/iattraxia/kmp_voice_ring/player/DesktopAudioRecorderPlayer.kt`: real JVM implementation using `javax.sound.sampled.Clip` (playback, pause/resume, stop, seek, volume via `MASTER_GAIN`). Coroutine ticker emits `PlaybackProgress` at `updateIntervalMs`. Recording methods stay unsupported → `c5b986f`.

5. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/player/AudioPlayerFactory.kt`: expect `createPlayer()` that returns `AudioRecorderPlayer`. JVM actual returns `DesktopAudioRecorderPlayer`; android/ios/wasmJs actuals delegate to the library's `createAudioRecorderPlayer()` → `c5b986f`.

---

## Group 4: PlayerViewModel

6. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/player/PlayerViewModel.kt`: plain Kotlin class with `StateFlow<PlayerState>` (Idle/Loading/Ready/Playing/Paused/Error), `StateFlow<Float>` for volume/positionMs/durationMs. `load()` reads compose-resources asset, parses WAV amplitudes, materializes bytes to cache. Playback listener maps `currentPosition` to pre-computed bucket index and drives the volume flow → `da5a79c`.

---

## Group 5: DebugPanel Test Harness

7. Rewrite `App.kt` to wire `PlayerViewModel` + `createPlayer()` into the entry composable. Dark background, 320.dp slot with a `DebugPanel` showing state, positionMs, durationMs, and a live volume bar. Play/Pause/Stop buttons gated by player state. `LaunchedEffect` loads the test asset on first composition; `DisposableEffect` disposes the player on teardown → `1b48f04`.

---

## Group 6: FPS Meters + 60 Hz Tick Rate

8. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/debug/FpsMeter.kt`: stdlib-only FPS counter using `TimeSource.Monotonic` with a 1-second window. Wire `volumeFps`, `frameFps`, and `drawFps` meters into `App.kt` and `PlayerViewModel` → `c100dbc`.

9. Set `updateIntervalMs=16` on the player for ~60 Hz volume updates. Add `raiseWindowsTimerResolution` in `jvmMain/main.kt` via JNA `timeBeginPeriod(1)` on a daemon thread so `delay(16)` actually yields ~60 Hz instead of the ~15.6 ms default Windows timer granularity → `02e0ea6`.
