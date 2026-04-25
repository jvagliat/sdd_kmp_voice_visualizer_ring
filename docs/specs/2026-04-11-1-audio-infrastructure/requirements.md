# Requirements: Audio Infrastructure

## Scope

Build the complete audio pipeline from WAV file to a reactive `volume: Float` flow that the visualizer can consume. Includes: offline WAV parsing into pre-computed RMS amplitude buckets, platform-specific cache file writing, a desktop JVM audio player, a PlayerViewModel bridging playback to volume state, a debug test harness UI, FPS meters, and a 60 Hz volume tick rate on JVM/Windows.

### WAV Parser

| Field | Behavior |
|-------|----------|
| Input | Raw WAV bytes from compose-resources |
| PCM formats | Mono/stereo, 8/16/24/32-bit LE |
| Output | `AmplitudeTrack` with per-bucket RMS values normalized to peak |
| Bucket size | Matches `updateIntervalMs` granularity (~16 ms) |

### Cache File (expect/actual)

| Platform | Behavior |
|----------|----------|
| JVM | Write to `java.io.tmpdir/kmp_voice_ring/<filename>`, return path |
| Android | `NotImplementedError` (desktop-first iteration) |
| iOS | `NotImplementedError` |
| wasmJs | `NotImplementedError` |

### DesktopAudioRecorderPlayer

| Method | Implementation |
|--------|---------------|
| `play(file)` | `javax.sound.sampled.Clip` with `AudioInputStream` |
| `pause()` / `resume()` | `clip.stop()` / `clip.start()` |
| `stop()` | `clip.stop()` + `clip.close()` |
| `seek(ms)` | `clip.framePosition = (ms/1000 * sampleRate).toInt()` |
| `setVolume(float)` | `FloatControl.Type.MASTER_GAIN` |
| Progress ticker | Coroutine `delay(updateIntervalMs)` loop emitting `PlaybackProgress` |

### PlayerViewModel

| Flow | Type | Description |
|------|------|-------------|
| `state` | `StateFlow<PlayerState>` | Idle/Loading/Ready/Playing/Paused/Error |
| `volume` | `StateFlow<Float>` | 0.0..1.0, driven by pre-computed RMS bucket at current position |
| `positionMs` | `StateFlow<Long>` | Current playback position |
| `durationMs` | `StateFlow<Long>` | Total duration from WAV header |

### DebugPanel

- Dark background composable
- State label, position/duration readout, volume bar
- Play/Pause/Stop buttons gated by `PlayerState`
- Loads test asset on first composition
- Disposes player on teardown

### FPS Meters

| Meter | Location | What it measures |
|-------|----------|-----------------|
| `volumeFps` | Playback listener callback | Rate of volume bucket updates |
| `frameFps` | `withFrameMillis` loop | Compose frame rate |
| `drawFps` | Canvas draw block | Draw invalidation rate |

### 60 Hz Tick Rate

- `updateIntervalMs = 16` on the player instance
- Windows: `timeBeginPeriod(1)` via JNA on a daemon thread to lower kernel timer granularity from ~15.6 ms to ~1 ms

## Decisions

- **Pre-computed RMS over runtime metering** — the `kmp-audio-recorder-player` library does not expose amplitude during playback. Parsing the WAV offline and indexing by position is deterministic and zero-latency vs audio.
- **expect/actual for cache and player factory** — each platform needs different file I/O and player backends. The expect/actual pattern keeps the ViewModel platform-agnostic.
- **javax.sound.sampled.Clip for desktop** — standard JDK API, no native dependencies, supports all needed operations. Recording intentionally left unsupported.
- **FpsMeter stdlib-only** — avoids pulling in a benchmarking library. Uses `TimeSource.Monotonic.markNow()` with a 1-second sliding window.

## Context

- See `specs/techs-and-archi.md`: pipeline architecture diagram (WAV → WavAmplitude → PlayerViewModel → volume).
- See `DONE.md`: T3a, T3b, T4, T5a, T5b, T5c, T6, T6b, T6c.
- Test asset: CC BY 3.0, Jan Morgenstern "Prelude" from Blender/Peach Open Movie.

## Out of scope

- No FFT or frequency-band analysis (that is Task 8 / T14).
- No mobile platform player implementations (Android/iOS) beyond stubs.
- No visualizer component rendering (that is Task 3).
- No band-energy toggle or asset selector (that is a later task).
