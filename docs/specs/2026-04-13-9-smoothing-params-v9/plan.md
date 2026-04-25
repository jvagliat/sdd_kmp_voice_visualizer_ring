# Plan: Smoothing Parameters (V9)

## Group 1: VoiceVisualizerRingV9 Composable

1. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/VoiceVisualizerRingV9.kt` — fork of `VoiceVisualizerRingV8.kt`. Add two composable parameters:
   - `inputSmoothing: Float = 0.85f` — Stage 1 decay (`PRE_SMOOTH_DECAY`). Range 0.0–0.95. Higher = more smoothing.
   - `responsiveness: Float = 0.15f` — Stage 2 lerp factor (`LERP_FACTOR`). Range 0.01–1.0. Higher = snappier.
   - `PRE_SMOOTH_NEW` derived as `1f - inputSmoothing` (not an independent parameter).
   - Both values read via `rememberUpdatedState` inside the `LaunchedEffect` so parameter changes take effect immediately without restarting the animation loop.

2. Update `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/VoiceVisualizerRing.kt` — dispatcher points to V9 instead of V8.

---

## Group 2: UI — Bottom Sheet with Sliders

3. Modify `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/App.kt`:
   - Add `inputSmoothing` and `responsiveness` state variables in `App()`.
   - Add a third bottom sheet (icon `GraphicEq`) with two `EffectSlider` composables:
     - `"inputSmoothing"`: range `0f..0.95f`
     - `"responsiveness"`: range `0.01f..1f`
   - Wire sliders in both Desktop layout (bottom sheet) and Mobile layout (bottom sheet from icon bar).
   - Pass both values through to `VoiceVisualizerRingV9`.

---

## Commits

- `11acd29` — TASKS.md: insert T26 spec
- `5397113` — VoiceVisualizerRingV9 + dispatcher + App sliders + bottom sheet
