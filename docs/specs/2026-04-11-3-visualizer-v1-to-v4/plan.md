# Plan: Visualizer V1 to V4

## Group 1: Versioned Implementations

1. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/VoiceVisualizerRingV1.kt`: filled stacks + stroke-stack glow. 3 blob layers drawn as filled shapes with halo strokes at both sides of the contour. Outer glow: 3 wide strokes before fill. Inner glow: 3 strokes inside `clipPath(Intersect)`. Outline only on layer 0. DrawPath count: Full 14 | Low 6 → `0b94523`.

2. Create `VoiceVisualizerRingV2.kt`: hollow ring via `clipPath(Difference/Intersect)` over wide strokes. All fills removed. Outer glow: stroke + `clipPath(Difference)`. Inner glow: stroke + `clipPath(Intersect)`. Outline on layer 0 only. DrawPath count: Full 11 | Low 7 → `0b94523`.

3. Create `VoiceVisualizerRingV3.kt`: `Modifier.blur` + CSS-style radii normalization + blobs as FILLED. 3 Canvas apilados in a `Box`. Far halo and near halo draw filled blobs + `Modifier.blur`. Sharp ring draws only blob[0] as stroke. Radii normalization factor: `min(1, w/(h1+h2), w/(h3+h4), h/(v1+v4), h/(v2+v3))`. DrawPath count: Full 7 | Low 4 → `0b94523`.

4. Create `VoiceVisualizerRingV4.kt`: `Modifier.blur` + strokes on all canvases + 3 phase offsets. All 3 canvases draw all 3 blobs as STROKE. Stroke widths: far = `thickness×2.5`, near = `thickness×1.6`, sharp = `thickness`. Radii normalization inherited from V3. DrawPath count: Full 9 | Low 6 → `0b94523`.

5. Create `VoiceVisualizerRing.kt`: thin dispatcher composable that forwards all parameters to `VoiceVisualizerRingV4` → `0b94523`.

---

## Group 2: App Integration

6. Update `App.kt`: replace the DebugPanel 320.dp slot with `VoiceVisualizerRing` (cyan default). Move DebugPanel to its own 320×240.dp box below the transport buttons so FPS meters and state remain visible while the ring runs → `0be4614`.

---

## Group 3: Task Tracking

7. Update `TASKS.md`: mark T7 as done with a one-paragraph recap of what each version tried and why V4 is active. Note the conscious deviation from the spec's no-`Modifier.blur` rule → `8c3572c`.
