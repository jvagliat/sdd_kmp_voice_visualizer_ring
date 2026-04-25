# Requirements: Mobile Layout + Platform Detection

## Scope

Add responsive layout that detects mobile platforms and presents an optimized UI (expanded ring, metrics grid, icon bar with bottom sheets, color picker). Wire Android audio playback with real `writeBytesToCache` implementation. Add system bar safe area padding.

### Platform Detection

| Platform | `isMobilePlatform()` | Notes |
|----------|---------------------|-------|
| Android | `true` | Physical mobile devices |
| iOS | `true` | Physical mobile devices |
| JVM | `false` | Desktop window |
| wasmJs | `false` | Browser |

### Mobile Layout Components

| Component | Description |
|-----------|-------------|
| Ring | Expanded to fill available width |
| Metrics grid | 2×3 grid below ring showing FPS, position, duration, volume, etc. |
| Icon bar | Source, Effects, Palette icons at bottom |
| Bottom sheets | Each icon opens a sheet: Source (asset selector + band-energy toggle), Effects (sliders), Palette (color picker) |

### Color Picker Presets

| Preset | Color reference | Notes |
|--------|----------------|-------|
| Ámbar | Amber/gold | Warm tone |
| Cian | Cyan #00FFFF | Original default |
| Menta | Mint green/emerald | Cool green |
| Violeta | Violet/lavender | Electric purple |
| Hielo | Ice blue/glacial | Pale blue |

### Visibility / Behavior

- Layout selection: `isMobilePlatform() || portrait aspect ratio` triggers mobile layout.
- Desktop layout preserved unchanged — no regression in side panel behavior.
- `accentColor` derived from color preset and applied consistently to all UI chrome (FPS text, sliders, icons, buttons, volume bar, switches).
- `safeDrawingPadding` applied at root to avoid system bar overlap on Android/iOS.
- Color picker appears in DebugEffectsPanel on Desktop (below sliders) and in Palette bottom sheet on Mobile.

### Android Wiring

- `AppContextHolder` singleton holds `Application` context, initialized in `MainActivity.onCreate`.
- `writeBytesToCache` Android actual writes bytes to `context.cacheDir` — replaces `NotImplementedError` stub.
- `kmp-audio-recorder-player` initialized in `MainActivity.onCreate`.

## Decisions

- **expect/actual for platform detection** — KMP-idiomatic, no reflection or classpath hacks.
- **BoxWithConstraints for responsive branch** — single composable tree, runtime switch based on constraints + platform.
- **Color presets over free-form picker** — 5 curated presets match the client's design references; simpler UX.
- **safeDrawingPadding over manual insets** — Compose Multiplatform API handles all platform edge cases.
- **AppContextHolder pattern** — standard Android approach for accessing context outside Activity.

## Context

- See `AGENTS.md`: component must work on Android + iOS targets.
- See `TASKS.md`: T18 tracks mobile layout tasks; T18d tracks verification.
- Existing pattern: `writeBytesToCache` expect/actual already exists for JVM, iOS (stub), wasmJs (stub).
- `PlayerViewModel` already abstract over platform; only file cache and audio player are platform-specific.

## Out of scope

- iOS-specific audio wiring (still stub).
- Layout animations or transitions between mobile/desktop.
- Landscape-specific mobile layout.
- Accessibility (screen readers, font scaling).
