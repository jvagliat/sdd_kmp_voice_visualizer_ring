# Validation: Visualizer V1 to V4

Implementation is complete and ready to merge when all of the following pass.

## Automated Checks

- [x] `./gradlew :composeApp:compileKotlinJvm` exits 0 (all version files compile)
- [x] `VoiceVisualizerRing.kt` (dispatcher) exists and forwards to V4
- [x] `VoiceVisualizerRingV1.kt` through `VoiceVisualizerRingV4.kt` all exist in commonMain

### Specific test coverage required

- [x] `VoiceVisualizerRingV1.kt` contains filled blob drawing with stroke-stack glow (outer strokes before fill, inner strokes via `clipPath(Intersect)`)
- [x] `VoiceVisualizerRingV2.kt` contains `clipPath(Difference)` for outer glow and `clipPath(Intersect)` for inner glow, no fill operations
- [x] `VoiceVisualizerRingV3.kt` uses `Modifier.blur` on 2 of 3 canvases, draws blobs as FILLED, includes CSS radii normalization
- [x] `VoiceVisualizerRingV4.kt` uses `Modifier.blur` on 2 of 3 canvases, draws all 3 blobs as STROKE on every canvas, includes radii normalization
- [x] All 4 versions reference `KEYFRAME_RADII` and `PHASE_OFFSETS` matching the spec values
- [x] Each version file contains a header comment documenting hypothesis, approach, drawPath count, and discovered failures

## Manual Checks

- [x] V1 renders 3 concentric filled discs ("onion ring" effect) — the failure that motivated V2
- [x] V2 renders a hollow ring but with visible discrete bands and step artifacts at 12 o'clock / 6 o'clock
- [x] V3 renders a filled glowing disc (not a hollow ring) — blur preserves central mass of filled blobs
- [x] V4 renders a hollow ring with blurred halos that animate on JVM desktop (frozen on Android, diagnosed)
- [x] `App.kt` displays `VoiceVisualizerRing` (cyan) in the 320.dp slot with DebugPanel below
- [x] The 3 phase-offset blobs are visually distinguishable in V4's sharp canvas (they intersect and cross)
- [x] Morphing animation runs at ~60 FPS on JVM desktop with the `FpsMeter` readings stable
- [x] Volume reactivity works: playing audio causes the ring to pulse and brighten
- [x] `TASKS.md` marks T7 as done with V1–V4 iteration summary and notes the `Modifier.blur` deviation

## Definition of Done

All automated checks pass, all manual checks confirmed, V4 is the active version dispatched from `VoiceVisualizerRing.kt`, V1–V3 are preserved as parallel files with documented failure analysis, and the component is verified on JVM desktop. The frozen halo issue on Android is documented as a known problem for the next iteration. Commits: `0b94523`, `0be4614`, `8c3572c`.
