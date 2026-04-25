# Requirements: Scaffolding KMP

## Scope

Import a fresh Kotlin Multiplatform project from the Compose Multiplatform template, remove the legacy `js` target (only `wasmJs` is kept for web), consolidate shared web sources into `wasmJsMain`, and add the audio playback dependency that the test harness will consume.

### Targets to keep

| Target | Source set | Notes |
|--------|-----------|-------|
| Android | `androidMain` | `MainActivity.kt`, `AndroidManifest.xml` |
| iOS | `iosMain` | `MainViewController.kt` |
| JVM (Desktop) | `jvmMain` | `main.kt` with `composeDesktop` entry |
| wasmJs (Web) | `wasmJsMain` | `main.kt` with `@WasmJs` entry, `index.html`, `styles.css` |

### Targets to remove

| Target | Reason |
|--------|--------|
| `js` | Legacy JavaScript target superseded by wasmJs. No code depends on it. |

### Dependency to add

| Library | Version | Coordinates |
|---------|---------|-------------|
| kmp-audio-recorder-player | 1.0.0-alpha04 | `io.github.hyochan:kmp-audio-recorder-player` |

## Decisions

- **wasmJs over js** — the js target compiles to plain JavaScript which is being phased out in favor of Wasm in the Kotlin/JS roadmap. Keeping only wasmJs avoids maintaining two web targets.
- **Audio dep in commonMain** — placed in shared dependencies because the `PlayerViewModel` and `AudioPlayerFactory` expect/actual pattern lives in common code; platform-specific player wiring happens in actual implementations.

## Context

- See `specs/techs-and-archi.md`: targets list and dependency table.
- See `SPEC.md` section 2: platform is KMP (Android + iOS), desktop and wasmJs are bonus targets for the test harness.

## Out of scope

- No application logic beyond the dummy `App.kt` + `Greeting.kt` from the template.
- No audio code yet (that is Task 1).
- No visualizer component yet (that is Task 3).
