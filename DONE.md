# DONE — VoiceVisualizerRing

Historial de tareas completadas. No se lee en el flujo normal de trabajo — consultar solo para arqueología o referencia.

---

- [x] **T1** `js` legacy eliminado, `webMain/` consolidado en `wasmJsMain/`. → `8ab2c2e`
- [x] **T2** Dep `kmp-audio-recorder-player:1.0.0-alpha04` agregada a commonMain. → `6629bac`
- [x] **T3a** Asset WAV dropeado en `files/audio/Jan_Morgenstern_-_01_-_Prelude.wav` (CC BY 3.0, Blender/Peach). → `b58a87b`
- [x] **T3b** Header WAV parseado: PCM1, 2ch, 16-bit, 44100Hz, ~32.29s. Info de referencia.
- [x] **T4** Parser WAV → RMS buckets en `audio/WavAmplitude.kt`. PCM mono/estéreo 8/16/24/32-bit LE, normalización al pico. → `1284f2b`
- [x] **T5a** `PlayerViewModel` con `StateFlow<PlayerState>`, `volume`, `positionMs`, `durationMs`. → `da5a79c`
- [x] **T5b** `writeBytesToCache` expect/actual. JVM real; android/ios/wasmJs stubs. → `b58a87b`
- [x] **T5c** Desktop JVM player: `DesktopAudioRecorderPlayer` con `javax.sound.sampled.Clip`. → `c5b986f`
- [x] **T6** Test harness: `App.kt` con DebugPanel, botones Play/Pause/Stop, carga de asset. → `1b48f04`
- [x] **T6b** FPS meters: `FpsMeter` stdlib-only + `volumeFps`/`frameFps`/`drawFps` en VM y App.
- [x] **T6c** Volume tick rate ~60Hz en JVM: `updateIntervalMs=16` + `raiseWindowsTimerResolution` (Windows `timeBeginPeriod(1)`).
- [x] **T7** `VoiceVisualizerRing` V1→V4 en commonMain + dispatcher. Verificado en JVM desktop.
- [x] **T10** Análisis prototipo HTML → `docs/html_analysis.md`. 13 efectos, 5 bloques físicos.
- [x] **T11** Contraste HTML vs V4 → `docs/html_vs_v4.md`.
- [x] **T12** Compatibilidad de código con iOS confirmada. Validación empírica en T19.
- [x] **T14a–d** FFT banda vocal: `audio/Fft.kt` (radix-2 Cooley–Tukey) + `audio/WavBandEnergy.kt` (Blackman, bins 2–14, IIR 0.85, p99). Toggle en VM.
- [x] **T15a–b** Asset `demo_voice_prototype.wav` copiado y specs verificadas (PCM1, 2ch, 44100Hz, 16-bit).
- [x] **T16a–b** App carga demo WAV + selector de asset en DebugPanel (T17).
- [x] **T17** Selector de asset + toggle band-energy en DebugEffectsPanel. Verificado en JVM desktop.
- [x] **T18a** `expect/actual isMobilePlatform()` — true en android/ios, false en jvm/wasmJs. → `f6d5c03`
- [x] **T18b** `material-icons-core` verificado en deps. → `f6d5c03`
- [x] **T18c** `MobileLayout` en App.kt: ring expandido + métricas 2×3 + barra iconos + BottomSheets. → `f6d5c03`
- [x] **T18 (Android wiring)** `writeBytesToCache` Android real vía `AppContextHolder`. → `3320e28`
