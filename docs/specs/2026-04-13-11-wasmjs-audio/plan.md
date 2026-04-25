# Plan: WasmJs Audio Playback

## Group 1: WasmAudioRecorderPlayer — HTMLAudioElement

1. Create `composeApp/src/wasmJsMain/kotlin/com/iattraxia/kmp_voice_ring/player/WasmAudioRecorderPlayer.kt`:
   - `external interface JsAudio : JsAny` with `currentTime`, `duration`, `ended` properties.
   - `js("new Audio(src)")` creates `HTMLAudioElement` from base64 data URL (`data:audio/wav;base64,...`).
   - Coroutine ticker reads `currentTime` / `duration` / `ended` every ~16 ms.
   - Emits `PlaybackProgress` with real `positionMs`, `durationMs`, and `ended` state.
   - `play()` calls JS audio play, `pause()` calls JS audio pause, `stop()` resets position.
   - Handle `duration` being `NaN` during initial load (metadata not yet available).

2. Modify `composeApp/src/wasmJsMain/kotlin/com/iattraxia/kmp_voice_ring/platform/CacheFile.wasmJs.kt` — encode WAV bytes as base64 data URL instead of throwing `NotImplementedError`.

3. Modify `composeApp/src/wasmJsMain/kotlin/com/iattraxia/kmp_voice_ring/player/AudioPlayerFactory.wasmJs.kt` — wire `WasmAudioRecorderPlayer` as the actual audio player.

---

## Group 2: Build Fix

4. Fix Kotlin/Wasm compiler constraint: `js()` must be the sole expression in a top-level function body. Split `newAudio` into:
   - `newAudioJs(src: String): JsAudio` — contains only `js("new Audio(src)")`.
   - `newAudio(src: String): JsAudio` — calls `newAudioJs()` and performs any needed cast.
   (Commit `7c43810`)

---

## Group 3: Firebase Hosting + Release Script

5. Create `.firebaserc` — Firebase project configuration.

6. Create `firebase.json` — Hosting config pointing wasmJs build output directory.

7. Update `.gitignore` — add `.firebase/` and `composeApp/release/`.

8. Create `build_web.sh` — shell script for release web builds (gradle wasmJs distribution + optional firebase deploy).

---

## Commits

- `ac8e8d4` — WasmAudioRecorderPlayer (v1: @JsFun approach) + CacheFile base64 data URL
- `7bf0cec` — Rewrite without JS interop (Kotlin 2.3 compat): monotonic timer stub
- `6fb9cde` — Real audio playback via HTMLAudioElement with JsAudio interface
- `7c43810` — Build fix: split newAudio into two functions for js() constraint
- `d727d8f` — TASKS.md: add T24 wasmJs audio verification
- `19935d7` — Firebase Hosting config (.firebaserc + firebase.json)
- `f5d6087` — .gitignore update + move docs to experiments/
- `49ef5d3` — build_web.sh release script
