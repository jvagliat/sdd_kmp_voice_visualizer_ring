# Requirements: Visualizer V1 to V4

## Scope

Implement the `VoiceVisualizerRing` Compose Canvas component through four iterative versions (V1→V4), each diagnosing and solving problems discovered in the previous version. The final V4 becomes the active implementation dispatched from `VoiceVisualizerRing.kt`.

### Shared component API (all versions)

```kotlin
@Composable
fun VoiceVisualizerRing(
    volume: Float,
    color: Color,
    intensity: Float = 1.0f,
    thickness: Float = 5.0f,
    glowSpread: Float = 1.0f,
    lowPerformanceMode: Boolean = false,
    modifier: Modifier = Modifier
)
```

### Version-by-version requirements

| Version | Approach | Problem discovered | Why discarded |
|---------|----------|-------------------|---------------|
| V1 | Filled blobs + stroke-stack glow (outer/inner halos as wide strokes) | **Onion ring**: filled layers are opaque discs, not hollow rings. Alpha does not dissolve the hard edges between concentric discs. | Fills are discs, not rings. |
| V2 | Hollow ring via `clipPath(Difference/Intersect)` over wide strokes, no fills | **11 hard bands**: constant-alpha clipped strokes have no gaussian falloff. **Step artifact**: adjacent `KEYFRAME_RADII` values sum past 100% without CSS normalization, creating path self-intersection. | Stack-of-strokes cannot approximate gaussian glow. Radii need normalization. |
| V3 | `Modifier.blur` + CSS radii normalization + blobs FILLED | **Filled center**: filled blob covers interior; blur preserves central mass → looks like a glowing disc. **Only one blob visible**: sharp canvas draws only blob[0]; layers 1–2 contribute only to blurred halo where their outlines dissolve. | Fills + blur = disc, not ring. All 3 phase offsets must appear in all canvases. |
| V4 | `Modifier.blur` + strokes on all canvases + 3 phase offsets | **Frozen halos on Android**: `Modifier.blur` creates a `RenderEffect` (hardware layer) that caches canvas content. State reads inside the draw block do not invalidate that layer → blurred canvases freeze on first frame. Only the sharp ring (no blur) animates. | `Modifier.blur` is not viable for content that animates at 60 FPS on Android. |

### Core blob geometry (shared across all versions)

- 3 blob layers with phase offsets: `[0.0, 0.3125, 0.625]`
- 3 keyframes with 8 border-radius values each, 8-second cycle, ease-in-out
- `KEYFRAME_RADII` matches the CSS prototype exactly
- `KEYFRAME_ROTATIONS`: `[0, 120, 240]` degrees

### V3/V4 radii normalization

```
factor = min(1, w/(h1+h2), w/(h3+h4), h/(v1+v4), h/(v2+v3))
if factor < 1 → all radii multiplied by factor
```

Same behavior as CSS border-radius when adjacent values exceed the container dimension.

### V4 canvas structure

| Canvas | Content | Blur |
|--------|---------|------|
| Far halo | 3 blobs as STROKE, width = `thickness × 2.5` | `Modifier.blur(40 × glowSpread dp)` |
| Near halo | 3 blobs as STROKE, width = `thickness × 1.6` | `Modifier.blur(15 × glowSpread dp)` |
| Sharp ring | 3 blobs as STROKE, width = `thickness` | None |

### Dispatcher

`VoiceVisualizerRing.kt` is a thin forwarding wrapper that calls `VoiceVisualizerRingV4` with all parameters. V1/V2/V3 preserved as parallel files for iteration history.

## Decisions

- **Conscious deviation from spec's "no Modifier.blur" rule** — V3 and V4 use `Modifier.blur` despite the AGENTS.md constraint. Documented in the commit and in TASKS.md. The trade-off: real gaussian blur looks better than stroke stacks but causes hardware layer caching on Android (the V4 problem that V5/V6 later solve).
- **CSS radii normalization introduced in V3** — the prototype's border-radius values can sum past 100% (e.g. 45+60=105). Browsers normalize this automatically; Compose does not. The explicit normalization prevents path self-intersection artifacts.
- **All 3 blobs on all canvases in V4** — V3 only drew blob[0] in the sharp canvas. V4 draws all 3 blobs on every canvas, making the organic intersection pattern visible in both the sharp outlines and the blurred halos.
- **Versioned files preserved** — V1/V2/V3 are kept in-tree alongside V4 (not buried in git) so the iteration history is immediately accessible.

## Context

- See `SPEC.md` section 5: Compose Canvas implementation approach.
- See `AGENTS.md`: component API, keyframe data, glow structure, performance rules.
- See `docs/bitacora_estrategias.md`: V1–V4 entries with detailed hypotheses, approaches, drawPath counts, and failure analysis.
- See `specs/techs-and-archi.md`: architecture diagram showing the dispatcher pattern.

## Out of scope

- Solving the V4 frozen halo problem (that is V5 and V6, Tasks 4 and 5).
- Cloudy blur integration (V6).
- Input smoothing and parameterization (V8, V9).
- FFT band-energy analysis (Task 8).
- Mobile layout or platform detection (Task 10).
