# Requirements: Fake-blur via multi-pass strokes (V5)

## Scope

Replace `Modifier.blur` (which freezes on Android due to hardware layer caching, as proven in V4) with a portable fake-blur approach using N stroke passes per blob inside a single Canvas. Zero external dependencies beyond `androidx.compose.*`.

### Glow pass configuration

| Group | Passes (Full) | extraWidth range | Alpha range | Passes (Low) |
|-------|---------------|-----------------|-------------|--------------|
| Far halo | 4 | 40dp → 0dp | 0.05 → 0.45 | 2 |
| Near halo | 3 | 15dp → 0dp | 0.15 → 0.65 | 2 |
| Sharp ring | 1 | 0dp | 0.85 | 1 |

### drawPath budget

| Mode | Count per blob | Total (3 blobs) |
|------|---------------|-----------------|
| Full | 8 | 24 |
| Low | 4 | 12 |

### Visibility / Behavior

- All 3 blobs (layer 0, 1, 2) animate with phase offsets (0.0, 0.3125, 0.625) as in V4.
- Glow falloff is visually acceptable: not pixel-perfect gaussian, but no visible banding at normal viewing distance.
- No freezing on any target (Android, iOS, Desktop, WASM) because no `RenderEffect` or hardware layers are involved.
- Volume reactivity, scale pulsation, brightness lerping, blob morphing, and CSS-normalized radii are inherited from V4 unchanged.

## Decisions

- **Single Canvas** — avoids the multi-Canvas overhead of V3/V4. All passes draw into one `DrawScope`.
- **No external deps** — V5 is the portable fallback; it uses only `androidx.compose.*` and Kotlin stdlib.
- **Far halo only in full mode** — `lowPerformanceMode` skips the outermost far halo passes to halve draw calls.

## Context

- See `docs/bitacora_estrategias.md` section **V5** for the full rationale and comparison to V4.
- See `AGENTS.md` for blob construction, keyframes, phase offsets, and CSS radius normalization.
- See `SPEC.md` sections 4.1–4.6 for the prototipo-derived constants.
- V4's `Modifier.blur` freezing is documented in `docs/bitacora_estrategias.md` section **V4**.

## Out of scope

- Real gaussian blur (addressed in V6 via Cloudy library).
- Dynamic brightness / smoothing filter (addressed in V8).
- `relativeMotion` parameter (addressed in V7/V8).
- Changes to the blob keyframes or morphing cycle.
