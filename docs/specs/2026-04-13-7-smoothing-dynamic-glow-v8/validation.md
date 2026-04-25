# Validation: Smoothing + dynamic glow (V8)

Implementation is complete and ready to merge when all of the following pass.

## Automated Checks

- [x] Project compiles on all KMP targets with no errors
- [x] `VoiceVisualizerRingV8.kt` contains two-stage low-pass filter: `preSmoothed = preSmoothed × 0.85 + raw × 0.15`, then lerp at 0.15
- [x] `targetBright` floor is `0.05f` (not `0.4f`)
- [x] `currentBright` initialized at `0.05f`
- [x] `PRE_SMOOTH_DECAY_V8 = 0.85f` and `PRE_SMOOTH_NEW_V8 = 0.15f` constants are defined
- [x] `relativeMotion` parameter inherited from V7 with distinct cycle durations (8000/8720/9440 ms)

### Specific test coverage required

- [x] Raw volume signal never reaches `targetScale`/`targetBright` without passing through `preSmoothed`
- [x] `preSmoothed` state is maintained across frames via `remember { mutableFloatStateOf(0f) }`
- [x] Dispatcher `VoiceVisualizerRing.kt` delegates to V8
- [x] V7 file has been moved to `experiments/` directory
- [x] All existing V6 functionality preserved: Cloudy key-trick, 3-Canvas stack, volume reactivity, 3-blob phase offsets

## Manual Checks

- [x] Volume peaks produce smooth, liquid ring scale changes with no jerky snaps — verified on JVM desktop
- [x] Glow fades to near-invisible at `volume = 0f` and smoothly brightens with increasing audio
- [x] Fade-out is smooth (no abrupt disappearance) due to pre-smoothed signal
- [x] `relativeMotion = true` causes the three rings to drift and reconverge organically
- [x] No regressions in Cloudy blur rendering (no freezing, correct ~15fps refresh)
- [x] Overall motion matches the organic feel of the HTML prototype video

## Definition of Done

All automated checks pass, all manual checks confirmed, and the implementation is verified via commit `7fd1462` with V7 archived to `experiments/` and no leftover debug code.
