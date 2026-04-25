# Requirements: WasmJs Audio Playback

## Scope

Implement real audio playback in the wasmJs target using `HTMLAudioElement`, replacing the previous stub/error. Enable the voice visualizer ring to animate in sync with actual audio in the browser. Add Firebase Hosting configuration for web deployment.

### WasmAudioRecorderPlayer

| Field | Type | Notes |
|-------|------|-------|
| Audio element | `HTMLAudioElement` via `JsAudio` external interface | Created from base64 data URL |
| Timer resolution | ~16 ms | Coroutine ticker reads `currentTime` |
| Position source | `JsAudio.currentTime` | Wall-clock position from browser |
| Duration source | `JsAudio.duration` | May be `NaN` during initial load |
| End detection | `JsAudio.ended` | Boolean, drives `PlaybackState.Ended` |

### CacheFile (wasmJs)

| Field | Type | Notes |
|-------|------|-------|
| Encoding | Base64 | WAV bytes → `data:audio/wav;base64,...` |
| Output | String data URL | Passed to `new Audio(url)` |

### Firebase Hosting

| File | Purpose |
|------|---------|
| `.firebaserc` | Project alias configuration |
| `firebase.json` | Hosting config: source directory, rewrites, headers |
| `build_web.sh` | Release build script: gradle distribution + optional deploy |

### Visibility / Behavior

- Browser loads the app, user presses Play — WAV audio plays through browser speakers.
- Ring animates in sync with audio position from pre-computed amplitudes.
- `duration` emits real value once browser parses WAV metadata (handled `NaN` gracefully).
- Pause/Resume work: audio pauses and resumes at same position.
- Playback stops naturally at end of file (no loop).
- Browser autoplay policy: if `audio.play()` is blocked (no prior user interaction), state remains `Ready` until user interacts.

### Build Constraints

- Kotlin/Wasm requires `js("...")` to be the sole expression in a top-level function body — split into `newAudioJs()` + `newAudio()`.
- `JsAudio` external interface uses `JsAny` base type (Kotlin/Wasm interop).
- No external JavaScript dependencies — pure Kotlin/Wasm + browser APIs.

## Decisions

- **HTMLAudioElement over Web Audio API** — simpler API, sufficient for single-file playback; no need for AudioContext complexity.
- **Base64 data URL for cache** — avoids filesystem access (unavailable in browser); works for WAV sizes under ~10 MB.
- **Coroutine ticker over JS setInterval** — Kotlin-idiomatic, cancellable via `Job`, integrates with `kmp-audio-recorder-player` interface.
- **Split js() function** — Kotlin/Wasm compiler enforces single-expression rule; two-function split is the idiomatic workaround.
- **Firebase Hosting** — zero-config static hosting, matches the project's Google ecosystem.

## Context

- See `TASKS.md`: T24 tracks wasmJs audio verification; blocker note about wasmJs filesystem.
- See `Platform.wasmJs.kt`: previous `writeBytesToCache` threw `NotImplementedError`.
- Existing pattern: `AudioPlayerFactory` expect/actual creates platform-specific player instances.
- `PlayerViewModel` already consumes `PlaybackProgress` — no changes needed in common code.

## Out of scope

- Web Audio API (AudioContext, AudioWorklet) — HTMLAudioElement is sufficient.
- Streaming audio or progressive download — entire WAV is loaded in memory.
- Autoplay policy workaround beyond graceful degradation (user must interact first).
- iOS Safari-specific audio quirks (wasmJs runs in desktop browsers primarily).
