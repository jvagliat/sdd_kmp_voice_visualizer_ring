# Plan: Cloudy library integration with key-trick (V6)

## Group 1: Dependency setup

1. Add `com.skydoves:cloudy` dependency to `composeApp/build.gradle.kts` and `gradle/libs.versions.toml`.

---

## Group 2: New composable implementation

2. Create `VoiceVisualizerRingV6Gml51Cloudy.kt` in `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/` as a fork of V4's 3-Canvas architecture but replacing `Modifier.blur` with `com.skydoves.cloudy.cloudy()` modifier.

3. Implement the 3-Canvas stack inside a `Box`:
   - **Canvas 1 — Far halo:** draws 3 blobs as strokes (wide width), wrapped in `key(blurTick) { ... .cloudy(radius) }` at ~15fps refresh.
   - **Canvas 2 — Near halo:** draws 3 blobs as strokes (medium width), wrapped in `key(blurTick) { ... .cloudy(radius) }` at ~15fps refresh.
   - **Canvas 3 — Sharp ring:** draws 3 blobs as strokes (thin width), **no** Cloudy, animating at full 60fps.

4. Implement the `key(blurTick)` trick: a tick counter that increments every ~66ms inside the `LaunchedEffect` animation loop, forcing Compose to discard and recreate the Cloudy subtree on each tick so the blur captures the current Canvas state.

---

## Group 3: Dispatcher and integration

5. Update `VoiceVisualizerRing.kt` dispatcher to delegate to V6, replacing V5 as the active version.

6. Preserve the existing public API unchanged.

---

## Group 4: Validation

7. Verify drawPath call counts: Full mode = 9 (3 blobs × 3 canvases), Low mode = 6.
8. Confirm blur halos refresh at ~15fps while sharp ring runs at 60fps.
9. Run on JVM desktop and verify no freezing.
