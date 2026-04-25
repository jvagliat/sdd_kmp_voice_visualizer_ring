# Validation: FFT Band Energy + Demo Voice Asset

Implementation is complete and ready to merge when all of the following pass.

## Automated Checks

- [x] Project compiles across all targets (Android, iOS, JVM, wasmJs) with no errors
- [x] `Fft.kt` produces correct magnitude spectrum for known test signals (impulse, sine)
- [x] `parseWavBandEnergies()` returns `AmplitudeTrack` with non-empty buckets for valid stereo PCM WAV input
- [x] Both `parseWavAmplitudes()` and `parseWavBandEnergies()` compile and produce output from the same WAV file
- [x] `PlayerViewModel.load(useBandEnergy = true)` loads without error
- [x] `PlayerViewModel.load(useBandEnergy = false)` preserves existing RMS behavior

### Specific test coverage required

- [x] FFT radix-2: 256-sample input produces 128-bin magnitude spectrum
- [x] WavBandEnergy: stereo 16-bit 44100 Hz WAV → non-zero bucket values in voice range
- [x] WavBandEnergy: IIR smoothing produces temporally smooth output (no discontinuities)
- [x] WavBandEnergy: p99 normalization maps output to 0..1 range
- [x] AmplitudeTrack.bucketMs is Double; consumers correctly divide positionMs by it

## Manual Checks

- [x] Demo voice WAV plays correctly in JVM desktop harness (`ASSET_PATH = demo_voice_prototype.wav`)
- [x] Ring animates with band-energy data — visually smoother voice-reactive response vs RMS
- [x] Asset selector in DebugEffectsPanel switches between Prelude and demo voice at runtime
- [x] Band-energy toggle in DebugEffectsPanel switches between RMS and FFT pipelines at runtime
- [x] Stop-if-playing guard works: changing asset while playing stops playback and reloads

## Definition of Done

All automated checks pass, manual A/B between RMS and band-energy confirmed on JVM desktop, both parsers coexist in the codebase without regression, and the harness defaults to the canonical validation configuration (demo voice + band-energy enabled).
