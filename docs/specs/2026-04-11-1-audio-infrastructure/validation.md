# Validation: Audio Infrastructure

Implementation is complete and ready to merge when all of the following pass.

## Automated Checks

- [x] `./gradlew :composeApp:compileKotlinJvm` exits 0
- [x] `./gradlew :composeApp:compileKotlinAndroid` exits 0 (stubs compile)

### Specific test coverage required

- [x] `WavAmplitude.kt` parses the test WAV and produces an `AmplitudeTrack` with non-empty RMS buckets
- [x] `writeBytesToCache` JVM actual writes a file to temp dir and returns a valid path
- [x] `DesktopAudioRecorderPlayer.play()` starts playback and the progress ticker emits `PlaybackProgress`
- [x] `DesktopAudioRecorderPlayer.pause()` / `resume()` cycle works without crash
- [x] `PlayerViewModel.load()` transitions state: Idle → Loading → Ready
- [x] `PlayerViewModel.play()` transitions: Ready → Playing and `volume` flow emits non-zero values during playback

## Manual Checks

- [x] JVM desktop app launches with dark background and DebugPanel showing state, position, volume bar
- [x] Play button starts audio playback; volume bar animates in sync with audible audio
- [x] Pause/Resume and Stop buttons work correctly
- [x] `volumeFps` meter reads ~60 FPS (or close) during playback on Windows
- [x] `frameFps` meter reads ~60 FPS during composition
- [x] Test asset `Jan_Morgenstern_-_01_-_Prelude.wav` loads without error
- [x] FPS meters display in the DebugPanel without introducing jank

## Definition of Done

All automated checks pass, all manual checks confirmed, the audio pipeline runs end-to-end on JVM desktop (WAV → parse → cache → play → volume flow → DebugPanel), and volume updates at ~60 Hz. Commits: `1284f2b`, `b58a87b`, `c5b986f`, `da5a79c`, `1b48f04`, `c100dbc`, `02e0ea6`.
