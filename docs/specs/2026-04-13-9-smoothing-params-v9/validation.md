# Validation: Smoothing Parameters (V9)

Implementation is complete and ready to merge when all of the following pass.

## Automated Checks

- [x] Project compiles across all targets (Android, iOS, JVM, wasmJs) with no errors
- [x] `VoiceVisualizerRingV9.kt` exists and accepts `inputSmoothing` and `responsiveness` parameters
- [x] Dispatcher in `VoiceVisualizerRing.kt` routes to V9 composable
- [x] Default values `inputSmoothing=0.85f`, `responsiveness=0.15f` produce identical output to V8

### Specific test coverage required

- [x] V9 composable renders without crash with all parameter combinations in valid ranges
- [x] `rememberUpdatedState` correctly propagates parameter changes inside `LaunchedEffect`
- [x] Changing `inputSmoothing` at runtime does not reset `elapsedMs` or blob morphing phase
- [x] Changing `responsiveness` at runtime does not reset `elapsedMs` or blob morphing phase
- [x] `PRE_SMOOTH_NEW` correctly derived as `1f - inputSmoothing` (never independent)

## Manual Checks

- [x] Sliders visible in Desktop layout bottom sheet (GraphicEq icon)
- [x] Sliders visible in Mobile layout bottom sheet (GraphicEq icon)
- [x] Moving `inputSmoothing` slider from 0 toward 0.95 visibly increases smoothing (slower, more inertial movement)
- [x] Moving `responsiveness` slider from 0.01 toward 1.0 visibly increases snappiness (faster reaction to volume changes)
- [x] Ring animation continues smoothly while adjusting sliders — no freeze or restart
- [x] Default slider positions match V8 behavior (no visual regression)

## Definition of Done

Both sliders are functional in Desktop and Mobile layouts, V9 is the active dispatcher target, parameter changes take effect immediately via `rememberUpdatedState` without animation restart, and default values match V8 behavior exactly.
