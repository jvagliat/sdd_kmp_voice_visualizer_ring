# Plan: Smoothing + dynamic glow (V8)

## Group 1: New composable implementation

1. Create `VoiceVisualizerRingV8.kt` in `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/` as a fork of `VoiceVisualizerRingV6Gml51Cloudy.kt`.

2. Implement two-stage low-pass filter (T21 — smoothing):
   - Stage 1 (pre-smooth): `preSmoothed = preSmoothed × 0.85 + rawVolume × 0.15` — the raw volume never reaches `targetScale`/`targetBright` directly; peaks are dampened before entering the lerp stage.
   - Stage 2 (visual lerp): `currentScale += (targetScale - currentScale) × 0.15` and `currentBright += (targetBright - currentBright) × 0.15` — inherited from V6, now operating on the pre-smoothed signal.
   - Result: liquid, organic motion where opening and closing of the ring have equal inertia.

3. Implement dynamic brightness (T22 — glow breathing):
   - Lower `targetBright` floor from `0.4f` to `0.05f` — in silence the glow fades almost completely, leaving a subtle residual shimmer.
   - Initialize `currentBright` at `0.05f` to match.
   - The fade-out is smooth because it uses the pre-smoothed signal from Stage 1.

4. Inherit `relativeMotion` from V7: when `true`, blobs use distinct cycle durations (8000/8720/9440 ms).

---

## Group 2: Dispatcher and archive

5. Update `VoiceVisualizerRing.kt` dispatcher to delegate to V8, replacing V6 as the active version.

6. Move `VoiceVisualizerRingV7.kt` to `experiments/` directory (the circular keyframe experiment is archived).

---

## Group 3: Validation

7. Verify that volume peaks are smoothed (no jerky snaps in ring scale or brightness).
8. Confirm glow fades to near-invisible at `volume = 0f` and smoothly brightens with audio.
9. Run on JVM desktop and verify no regressions in Cloudy blur or animation fluidity.
