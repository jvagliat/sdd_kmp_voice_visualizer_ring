# Requirements: FFT Band Energy + Demo Voice Asset

## Scope

Replicate the HTML `AnalyserNode` behavior as an offline pre-processing pipeline on the WAV file. Add a voice-band FFT energy parser (radix-2 Cooley-Tukey, Blackman window, bins 2-14, IIR smoothing 0.85, p99 normalization) alongside the existing RMS parser. Add a demo voice WAV asset for visual validation against the client's HTML prototype.

### FFT Pipeline

| Field | Value | Notes |
|-------|-------|-------|
| Algorithm | Radix-2 Cooley-Tukey | stdlib-only, no external deps |
| FFT size | 256 | power of 2, matches AnalyserNode |
| Hop size | 128 | 50% overlap |
| Window | Blackman | applied per frame before FFT |
| IIR smoothing | 0.85 | per-bin exponential moving average |
| dB range | [-100, -30] | mapped to byte 0..255 |
| Voice bins | 2..14 | average of these bins per frame |
| Normalization | p99 | across all buckets |

### WavBandEnergy Parser

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Input | WAV byte array | yes | PCM mono/stereo 8/16/24/32-bit LE |
| Output | `AmplitudeTrack` | yes | bucketMs as Double for fractional width |
| L+R merge | Average L+R to mono | yes | for stereo input |

### Demo Voice Asset

| Field | Value | Notes |
|-------|-------|-------|
| File | `demo_voice_prototype.wav` | CC asset from client video demo |
| Format | PCM, 2ch, 44100 Hz, 16-bit | ~41.5 s |
| Location | `composeResources/files/audio/` | coexists with Prelude |

### Visibility / Behavior

- `PlayerViewModel.load()` accepts `useBandEnergy` flag; default `false` preserves RMS behavior.
- `DebugEffectsPanel` provides runtime asset selector and band-energy toggle — no recompile needed for A/B.
- `AmplitudeTrack.bucketMs` changes from `Int` to `Double` so consumers divide `positionMs` by the actual fractional bucket width.
- Both parsers (RMS and band-energy) coexist; selection is per-load.

## Decisions

- **Stdlib-only FFT** — no external dependencies; radix-2 Cooley-Tukey is compact and sufficient for offline WAV analysis.
- **IIR smoothing factor 0.85** — matches the temporal smoothing of the HTML `AnalyserNode`.
- **p99 normalization** — avoids outlier spikes from transients while preserving dynamic range.
- **Voice bins 2..14** — corresponds to the frequency range the HTML prototype's `AnalyserNode` focuses on.
- **bucketMs as Double** — RMS and FFT parsers produce different actual bucket widths; Double avoids loss of precision.

## Context

- See `AGENTS.md`: un solo `.kt` autónomo, cero dependencias externas.
- See `SPEC.md` section 4.5: volume processing pipeline that the band-energy feeds into.
- Existing pattern: `audio/WavAmplitude.kt` — `parseWavAmplitudes()` is the RMS baseline.
- HTML prototype `AnalyserNode` is the reference behavior to replicate.

## Out of scope

- Real-time FFT during playback (all processing is offline pre-compute).
- Microphone capture or live audio input.
- Visual validation of band-energy vs HTML prototype (tracked separately as T14e).
- Trim/normalization of the demo WAV (tracked as T15c).
