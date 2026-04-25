# Plan: Circular keyframes with localized protuberance (V7)

## Group 1: New composable implementation

1. Create `VoiceVisualizerRingV7.kt` in `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/` as a fork of V6 (`VoiceVisualizerRingV6Gml51Cloudy.kt`).

2. Redesign keyframes for circular shape with localized protrusion:
   - Most radii stay at ~48-50% (nearly circular baseline).
   - Each keyframe elevates one opposite pair to 65%/35% (sum = 100, avoiding CSS normalization flattening).
   - Combined with `KEYFRAME_ROTATIONS`, the protrusion travels around the ring over the 8s cycle.

3. Add `blurRadius: Float = 15f` parameter to control near halo blur; far halo scales at `blurRadius × 2.5f`. Replaces hardcoded 15dp/40dp constants from V6.

4. Add `relativeMotion: Boolean = false` parameter: when `true`, each of the 3 blobs uses a distinct cycle duration (`CYCLE_MS` = 8000/8720/9440 ms) so phase offsets drift over time, causing rings to open and close relative to each other. Phase offsets are kept in both modes to avoid jarring jumps when toggling at runtime.

---

## Group 2: Dispatcher and integration

5. Update `VoiceVisualizerRing.kt` dispatcher to expose `blurRadius` and `relativeMotion` and delegate to V7.

6. Add `blurRadius` slider (2–60dp) and `relativeMotion` switch to `App.kt` (both mobile settings sheet and desktop DebugPanel).

---

## Group 3: Validation

7. Verify that circular keyframes produce a nearly-round ring with a visible protrusion that rotates each cycle.
8. Confirm `relativeMotion` causes the 3 blobs to drift apart and back together over time.
9. Run on JVM desktop and verify no regressions.

---

## Group 4: Archive

10. After evaluation, archive V7 to `experiments/` — the circular keyframe direction was not chosen. `relativeMotion` is migrated to V8.
