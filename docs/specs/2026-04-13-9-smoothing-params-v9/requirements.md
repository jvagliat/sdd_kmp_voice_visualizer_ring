# Requirements: Smoothing Parameters (V9)

## Scope

Expose the two smoothing constants from V8 as composable parameters in V9, with runtime sliders for interactive tuning. The fork preserves V8 behavior with default values while enabling hot-reload of smoothing without animation restart.

### New Parameters

| Parameter | Type | Default | Range | Maps to |
|-----------|------|---------|-------|---------|
| `inputSmoothing` | Float | 0.85f | 0.0–0.95 | Stage 1 `PRE_SMOOTH_DECAY` |
| `responsiveness` | Float | 0.15f | 0.01–1.0 | Stage 2 `LERP_FACTOR` |

### Derived Values

- `PRE_SMOOTH_NEW = 1f - inputSmoothing` — not an independent parameter.
- `PRE_SMOOTH_DECAY = inputSmoothing` — direct mapping.

### Visibility / Behavior

- Default values (`inputSmoothing=0.85f`, `responsiveness=0.15f`) produce identical behavior to V8.
- Changes via sliders take effect on the next frame — no animation restart, no visual glitch.
- `rememberUpdatedState` used inside `LaunchedEffect` to read latest values without recomposition triggering a new effect launch.
- Bottom sheet with icon `GraphicEq` appears in both Desktop and Mobile layouts.
- Each slider shows parameter name, current value, and usable range.

## Decisions

- **Fork V8 (not modify in-place)** — V8 remains as a stable reference; V9 is the active version.
- **rememberUpdatedState for hot-reload** — avoids `LaunchedEffect` restart which would reset `elapsedMs` and cause a visual jump in the blob morphing animation.
- **PRE_SMOOTH_NEW derived** — `decay + new = 1.0` is a mathematical identity; exposing both independently would allow inconsistent combinations.
- **Dispatcher points to V9** — V9 becomes the active rendering path immediately.

## Context

- See `AGENTS.md`: smoothing lerp at 0.15 per frame (section: Suavizado Lerp).
- See `VoiceVisualizerRingV8.kt`: `PRE_SMOOTH_DECAY = 0.85f`, `LERP_FACTOR = 0.15f` are the constants being parameterized.
- See `SPEC.md` section 4.5: volume processing pipeline that these two stages control.
- Existing pattern: `EffectSlider` composable already used in DebugEffectsPanel for thickness/glowSpread/intensity.

## Out of scope

- Changing the 2-stage smoothing architecture (only the constants are parameterized).
- Audio processing changes (smoothing is purely visual).
- Removing V8 or changing the dispatcher pattern.
