# Validation: Circular keyframes with localized protuberance (V7)

Implementation is complete and ready to merge when all of the following pass.

## Automated Checks

- [x] Project compiles on all KMP targets with no errors
- [x] `VoiceVisualizerRingV7.kt` uses Cloudy (same as V6) with `key(blurTick)` trick intact
- [x] `blurRadius` parameter is exposed in the composable signature with default `15f`
- [x] `relativeMotion` parameter is exposed in the composable signature with default `false`
- [x] Three distinct `CYCLE_MS` values (8000/8720/9440) are used when `relativeMotion = true`

### Specific test coverage required

- [x] Circular keyframe arrays produce radii where opposite-edge pairs sum to 100
- [x] `blurRadius` scales far halo blur at `× 2.5f` and near halo at `× 1.0f`
- [x] Dispatcher exposes `blurRadius` and `relativeMotion` to App.kt
- [x] App.kt DebugPanel includes blurRadius slider (2–60dp) and relativeMotion switch
- [x] All existing V6 functionality (Cloudy, key-trick, volume reactivity) is preserved

## Manual Checks

- [x] Ring shape is nearly circular with a visible protrusion that rotates around the ring per cycle
- [x] `relativeMotion = true` causes the three blobs to drift apart and reconverge over time
- [x] `blurRadius` slider adjusts glow spread in real time on JVM desktop
- [x] No regressions in Cloudy blur rendering (no freezing, correct refresh at ~15fps)
- [x] V7 file moved to `experiments/` directory after evaluation (commit `7fd1462`)

## Definition of Done

All automated checks pass, all manual checks confirmed, V7 is archived in `experiments/` via commit `7fd1462`, `relativeMotion` parameter is migrated to V8, and no leftover debug code remains in the active codebase.
