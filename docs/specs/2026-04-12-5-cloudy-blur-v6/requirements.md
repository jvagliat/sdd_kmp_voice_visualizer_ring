# Requirements: Cloudy library integration with key-trick (V6)

## Scope

Replace the V5 multi-pass fake-blur with real gaussian blur via the `skydoves/Cloudy` library, solving the Android hardware layer caching problem from V4 by using a `key(blurTick)` trick that forces subtree recreation at ~15fps.

### Cloudy platform coverage

| Platform | Implementation |
|----------|---------------|
| Android 31+ | `RenderEffect` (GPU) |
| Android ≤30 | C++ native fallback (NEON/SIMD) |
| iOS | Skia Metal |
| Desktop | Skia GPU |
| WASM | Skia GPU |

### Architecture: 3-Canvas stack

| Canvas | Content | Blur | Refresh rate |
|--------|---------|------|-------------|
| Far halo | 3 blobs, wide stroke | `.cloudy(blurRadius × 2.5f)` via `key(blurTick)` | ~15fps |
| Near halo | 3 blobs, medium stroke | `.cloudy(blurRadius)` via `key(blurTick)` | ~15fps |
| Sharp ring | 3 blobs, thin stroke | None | 60fps |

### drawPath budget

| Mode | Count |
|------|-------|
| Full | 9 (3 blobs × 3 canvases) |
| Low | 6 |

### The key-trick mechanism

Cloudy captures a bitmap of its child Canvas and caches it. When the Canvas content animates at 60fps, Cloudy does not detect changes and freezes on the first frame. The `key(blurTick)` trick forces Compose to dispose the old subtree and create a new one (including a fresh Cloudy instance) each time the tick value changes. The tick increments every ~66ms (~15fps), giving the capture+blur pipeline enough time to complete.

### Visibility / Behavior

- The blur halos update at ~15fps, producing a slightly softer temporal feel compared to the 60fps sharp ring — this is acceptable and expected.
- The sharp ring (no blur) provides crisp, fluid animation at full framerate.
- All 3 blobs render on all 3 canvases, maintaining the organic shimmer from phase offset intersections.
- Volume reactivity, scale pulsation, brightness lerping, blob morphing, and CSS-normalized radii are inherited unchanged.

## Decisions

- **Cloudy over Modifier.blur** — Cloudy has platform fallbacks (C++ native on Android ≤30, Skia on iOS/Desktop/WASM) whereas `Modifier.blur` is Android-only and suffers the same caching issue.
- **key() trick over invalidate()** — Compose has no public API to invalidate a Cloudy render. The `key()` trick is the reliable way to force a full subtree recreation.
- **~15fps blur refresh** — empirical sweet spot: fast enough for visual fluidity, slow enough for the capture+blur pipeline to keep up without frame drops.

## Context

- See `docs/bitacora_estrategias.md` section **V6** for the full rationale.
- V4's `Modifier.blur` freezing documented in `docs/bitacora_estrategias.md` section **V4**.
- See `SPEC.md` sections 4.1–4.6 for prototipo-derived constants.
- Cloudy library: `com.skydoves:cloudy` (added to `gradle/libs.versions.toml`).

## Out of scope

- Smoothing filter / dynamic brightness (addressed in V8).
- `relativeMotion` parameter (addressed in V7, migrated to V8).
- Circular keyframes experiment (addressed in V7).
- Changes to the blob keyframes or morphing cycle.
