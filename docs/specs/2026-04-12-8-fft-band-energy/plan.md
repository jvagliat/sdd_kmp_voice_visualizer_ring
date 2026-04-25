# Plan: FFT Band Energy + Demo Voice Asset

## Group 1: FFT and Voice-Band Parser

1. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/audio/Fft.kt` — stdlib-only radix-2 Cooley-Tukey FFT implementation. Accept `FloatArray` of real samples, return complex spectrum as `FloatArray` (re/im interleaved). `fftSize = 256` (→ `expectedLen` = 512 for stereo L+R to mono).

2. Create `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/audio/WavBandEnergy.kt` — `parseWavBandEnergies()` replicating the HTML `AnalyserNode` pipeline: L+R to mono, Blackman window, FFT size 256 / hop 128, per-bin IIR smoothing factor 0.85, dB mapping `[-100, -30]` to byte `[0..255]`, average of bins 2..14 (voice band), bucket mean, p99 normalization across all buckets. Change `AmplitudeTrack.bucketMs` from `Int` to `Double` so both parsers expose their actual fractional bucket width.

---

## Group 2: Demo Voice Asset

3. Add `composeApp/src/commonMain/composeResources/files/audio/demo_voice_prototype.wav` — CC voice asset from the client's video demo (PCM 44100 Hz, stereo, 16-bit, ~41.5 s). Coexists with the Prelude WAV for A/B comparison.

4. Copy reference materials to `docs/` — client video demo `.mov` and source `.wav` for visual validation.

---

## Group 3: ViewModel Integration

5. Modify `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/player/PlayerViewModel.kt` — add `useBandEnergy: Boolean = false` parameter to `load()`. When `true`, call `parseWavBandEnergies()` instead of `parseWavAmplitudes()`. Default `false` preserves existing behavior. Add stop-if-playing guard before re-loading.

6. Modify `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/App.kt` — point `ASSET_PATH` to `demo_voice_prototype.wav` and set `USE_BAND_ENERGY = true` as default harness configuration.

---

## Group 4: Debug Panel Selector

7. Extend `App.kt` — add `DebugEffectsPanel` with asset selector (dropdown between Prelude and demo voice) and `useBandEnergy` toggle switch. Allows A/B switching at runtime without recompiling.

---

## Commits

- `46df11a` — Fft.kt + WavBandEnergy.kt + bucketMs type change
- `fa254d6` — demo_voice_prototype.wav asset
- `580c5ff` — App default asset + band-energy on
- `aa3079b` — PlayerViewModel useBandEnergy flag
- `8c8ad1a` — docs: client video demo + source WAV
- `0437336` — TASKS.md update
- `8af79fc` — DebugEffectsPanel: asset selector + band-energy toggle
- `f15cd49` — PlayerViewModel stop-if-playing on re-load
