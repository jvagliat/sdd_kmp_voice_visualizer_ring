# Plan: Fake-blur via multi-pass strokes (V5)

## Group 1: New composable implementation

1. Create `VoiceVisualizerRingV5.kt` in `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/` implementing a single-Canvas composable with no `Modifier.blur` and no external dependencies.

2. Implement multi-pass glow per blob using painter's algorithm (back-to-front):
   - Far halo group: 4 passes with `extraWidth` decreasing 40dp→0dp, `alpha` increasing 0.05→0.45
   - Near halo group: 3 passes with `extraWidth` decreasing 15dp→0dp, `alpha` increasing 0.15→0.65
   - Sharp ring: 1 pass at `alpha` 0.85
   - In `lowPerformanceMode`, halve the passes per group (far halo 2 passes, near halo 2 passes, sharp 1 pass)

3. Each blob draws the same `drawPath` with `Stroke(width)` where `width = thickness + extraWidth`, back to front, producing a simulated gaussian falloff without hardware layers.

---

## Group 2: Dispatcher and integration

4. Update `VoiceVisualizerRing.kt` dispatcher to select and delegate to `VoiceVisualizerRingV5`, replacing the V4 wiring.

5. Preserve the existing public API (`volume`, `color`, `intensity`, `thickness`, `glowSpread`, `lowPerformanceMode`, `modifier`) unchanged.

---

## Group 3: Validation

6. Verify drawPath call counts: Full mode = 24 (3 blobs × 8 passes), Low mode = 12.
7. Confirm no `Modifier.blur`, no `RenderEffect`, no hardware layers anywhere in V5 code.
8. Run on JVM desktop target and confirm animation runs at 60fps with no freezing.
