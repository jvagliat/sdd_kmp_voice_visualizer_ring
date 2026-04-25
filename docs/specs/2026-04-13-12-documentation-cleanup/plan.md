# Plan: Documentation Cleanup

## Group 1: Move Old Versions to experiments/

1. Move obsolete visualizer versions from root package to `experiments/` subpackage:
   - `VoiceVisualizerRingV1.kt` → `experiments/VoiceVisualizerRingV1.kt`
   - `VoiceVisualizerRingV2.kt` → `experiments/VoiceVisualizerRingV2.kt`
   - `VoiceVisualizerRingV3.kt` → `experiments/VoiceVisualizerRingV3.kt`
   - `VoiceVisualizerRingV4.kt` → `experiments/VoiceVisualizerRingV4.kt`
   - `VoiceVisualizerRingV5.kt` → `experiments/VoiceVisualizerRingV5.kt`
   - Keep `VoiceVisualizerRingV6Gml51Cloudy.kt` and `VoiceVisualizerRingV8.kt` in root (V6Gml51Cloudy is V8's base; V8 is V9's base).
   - Move `docs/html_analysis.md`, `docs/html_vs_v4.md`, `docs/html_para_desarrollador_final.html` to `experiments/`.
   - Move `docs/ring_voice_html_prototype_demo.mov` and `.wav` to `experiments/`.

---

## Group 2: Centralized Bitácora

2. Create `docs/bitacora_estrategias.md` — centralize implementation history (250 lines):
   - One section per version (V1..V9): hypothesis, approach, drawPath count, problems discovered, conclusion.
   - Content migrated from the inline comment blocks at the top of each `VoiceVisualizerRingVx.kt` file.

---

## Group 3: Public API Documentation

3. Rewrite `VoiceVisualizerRing.kt` header block (dispatcher file):
   - Replace V1..V8 version history with reference to `docs/bitacora_estrategias.md`.
   - Add public API contract documentation: component purpose, audio-reactive behavior, parameter descriptions with ranges and defaults.
   - Style: block comments (not KDoc), consistent with existing file conventions.

---

## Group 4: Developer Documentation in V9

4. Add comprehensive inline documentation to `VoiceVisualizerRingV9.kt` (231 lines added):
   - Header: purpose and visual behavior (what the user sees).
   - Parameters section: each parameter's control, typical ranges, visual effects.
   - Internals section organized by code area:
     - Constants and keyframes: what each group represents.
     - `VoiceVisualizerRingV9` composable: animation loop (`LaunchedEffect`), smoothing filter (Stage 1 + Stage 2), scale/brightness model, 3-canvas structure.
     - `BlobsCanvasV9`: how it reads state, calculates per-layer scale and brightness.
     - `buildBlobFrameV9`: keyframe interpolation, CSS radius normalization, Bézier path construction.

---

## Group 5: Rename useBandEnergy → filterVoiceFrequencies

5. Rename `useBandEnergy` to `filterVoiceFrequencies` throughout:
   - `PlayerViewModel.load()` parameter
   - `App.kt` state variable and composable signatures
   - All call sites and the Switch label in source sheets
   - "band-energy" → "filterVoiceFrequencies" in UI labels

---

## Commits

- `1e627f8` — Move V1-V6, V8 to experiments/
- `d2c3bed` — Restore V6Gml51Cloudy and V8 to root (only V1-V5 are obsolete)
- `617b478` — Rename useBandEnergy → filterVoiceFrequencies
- `2825650` — VoiceVisualizerRing.kt: replace version block with public API docs
- `a98ac59` — Create docs/bitacora_estrategias.md (250 lines)
- `f41f48a` — VoiceVisualizerRingV9.kt: add developer documentation (231 lines added)
