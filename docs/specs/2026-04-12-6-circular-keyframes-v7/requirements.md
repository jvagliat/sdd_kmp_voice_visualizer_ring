# Requirements: Circular keyframes with localized protuberance (V7)

## Scope

Experimental fork of V6 exploring three changes: circular keyframes with localized protrusion, a parameterized `blurRadius`, and `relativeMotion` (per-blob cycle duration variation). The experiment was evaluated and archived — the circular keyframe direction was not chosen. `relativeMotion` was migrated to V8.

### Circular keyframes

| Keyframe | TL-h | TR-h | BR-h | BL-h | TL-v | TR-v | BR-v | BL-v |
|----------|------|------|------|------|------|------|------|------|
| 0%/100%  | 65   | 35   | 50   | 50   | 50   | 50   | 65   | 35   |
| 33%      | 50   | 50   | 65   | 35   | 65   | 35   | 50   | 50   |
| 66%      | 35   | 65   | 50   | 50   | 50   | 50   | 35   | 65   |

Opposite pairs on the same edge sum to 100 (e.g., TL-h=65, TR-h=35 → sum=100) to avoid CSS normalization from flattening the bump.

### New parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `blurRadius` | `Float` | `15f` | Near halo blur radius in dp. Far halo = `blurRadius × 2.5f`. |
| `relativeMotion` | `Boolean` | `false` | When `true`, blobs use distinct cycle durations (8000/8720/9440 ms). |

### relativeMotion behavior

When `relativeMotion = true`:
- Blob 0: `CYCLE_MS = 8000L`
- Blob 1: `CYCLE_MS = 8720L`
- Blob 2: `CYCLE_MS = 9440L`

Phase offsets (0.0, 0.3125, 0.625) are retained in both modes. The different cycle durations cause the phase relationship to drift continuously, producing organic opening/closing motion between the three rings.

### Visibility / Behavior

- Shape is nearly circular with a visible "flare" protrusion that rotates around the ring over the 8s cycle.
- `blurRadius` allows runtime adjustment of glow intensity from the DebugPanel.
- `relativeMotion` creates a distinctive visual effect where the three rings breathe independently.
- All other V6 behavior (Cloudy key-trick, 3-Canvas stack, volume reactivity, brightness lerping) is preserved.

## Decisions

- **Archived experiment** — circular keyframes with localized protrusion were evaluated against V6's original keyframes and not chosen as the visual direction.
- **relativeMotion kept** — the parameter was considered valuable and migrated to V8 (the active branch).
- **blurRadius migrated** — the parameter was also ported to V6 via commit `c3d98a4`.

## Context

- See `docs/bitacora_estrategias.md` section **V7** for the full rationale and conclusion.
- Fork of V6: `VoiceVisualizerRingV6Gml51Cloudy.kt` is the base.
- Commits: `d065e6e` (V7 creation), `c3d98a4` (blurRadius backported to V6), `b5cfde4` (wiring in App.kt and dispatcher), `d727d8f` (TASKS.md update).

## Out of scope

- Smoothing filter / dynamic brightness (addressed in V8).
- Changes to Cloudy integration or key-trick mechanism.
- Changes to the blob morphing easing or phase offset values.
