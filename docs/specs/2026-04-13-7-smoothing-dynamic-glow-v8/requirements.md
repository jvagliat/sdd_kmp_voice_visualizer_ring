# Requirements: Smoothing + dynamic glow (V8)

## Scope

Fork of V6 (`VoiceVisualizerRingV6Gml51Cloudy.kt`) adding a two-stage low-pass filter for liquid organic motion and dynamic brightness that makes the glow breathe with volume. Incorporates `relativeMotion` from V7.

### Two-stage low-pass filter (T21)

Raw volume never reaches the visual targets directly. It passes through:

| Stage | Operation | Constants |
|-------|-----------|-----------|
| Stage 1 (pre-smooth) | `preSmoothed = preSmoothed × 0.85 + raw × 0.15` | `PRE_SMOOTH_DECAY = 0.85f`, `PRE_SMOOTH_NEW = 0.15f` |
| Stage 2 (visual lerp) | `currentScale += (target - current) × 0.15` | `LERP_FACTOR = 0.15f` |

`targetScale` and `targetBright` are computed from `preSmoothed` (not from `rawVolume`). This ensures both attack and release have the same inertia, eliminating the asymmetric snap on volume peaks.

### Dynamic brightness (T22)

| Property | V6 value | V8 value |
|----------|---------|---------|
| `targetBright` floor | `0.4f` | `0.05f` |
| `currentBright` initial | `0.4f` | `0.05f` |

At `volume = 0f`, the ring fades to a barely-visible shimmer. As volume increases, the glow smoothly intensifies. The fade-out is smooth because `targetBright` is computed from the pre-smoothed signal.

### Inherited from V7

- `relativeMotion: Boolean` parameter: when `true`, each blob uses a distinct `CYCLE_MS` (8000/8720/9440 ms). Phase offsets drift over time → rings open and close independently.

### Visibility / Behavior

- Ring scale and brightness respond to volume with a liquid, organic feel — no abrupt jumps.
- Glow fades to near-invisible during silence and smoothly breathes back with audio.
- All V6 behavior preserved: Cloudy key-trick at ~15fps, 3-Canvas stack, 3-blob phase offsets, volume reactivity, CSS-normalized radii.
- `relativeMotion` works identically to V7 when enabled.

## Decisions

- **Fork of V6, not V7** — V7's circular keyframes were not the chosen direction. V6 is the canonical base; only `relativeMotion` is ported from V7.
- **Pre-smooth decay at 0.85** — empirical value that produces motion matching the HTML prototype's organic feel. Attack and release are symmetric.
- **Floor at 0.05f (not 0.0f)** — a subtle residual shimmer at silence looks better than a completely invisible ring.

## Context

- See `docs/bitacora_estrategias.md` section **V8** for the full rationale.
- Fork of V6: `VoiceVisualizerRingV6Gml51Cloudy.kt` is the base.
- `relativeMotion` originates from V7 (`docs/bitacora_estrategias.md` section **V7**).
- Commits: `7fd1462` (V8 creation + V7 archival), `4492ec2` (TASKS.md update).
- Constants match `AGENTS.md` section "Suavizado Lerp" (extended with pre-smooth stage).

## Out of scope

- Exposing smoothing constants as parameters (addressed in V9 via `inputSmoothing` and `responsiveness`).
- Changes to blob keyframes (V7 experiment archived).
- Changes to Cloudy integration or key-trick mechanism.
- Changes to the 3-Canvas stack architecture.
