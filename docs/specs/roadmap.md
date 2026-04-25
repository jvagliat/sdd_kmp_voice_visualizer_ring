# Roadmap

Phases are intentionally small — each maps to a coherent unit of work from the actual development.

---

## Phase 1: Project Setup (2026-04-10)

### Task 0 — Scaffolding KMP + cleanup [x]

**Goal:** Importar el proyecto KMP dummy, limpiar targets legacy, agregar dependencia de audio.

- [x] Importar proyecto KMP dummy inicial
- [x] Eliminar target `js` legacy, consolidar `webMain` → `wasmJsMain`
- [x] Agregar dependencia `kmp-audio-recorder-player:1.0.0-alpha04`

---

## Phase 2: Audio Infrastructure + Test Harness (2026-04-11)

### Task 1 — Infraestructura de audio y test harness [x]

**Goal:** Pipeline de audio completo (WAV → RMS → playback → volume tick) con test harness visual.

- [x] Parser WAV → RMS buckets (`WavAmplitude.kt`)
- [x] `writeBytesToCache` expect/actual con test asset
- [x] `DesktopAudioRecorderPlayer` con `javax.sound.sampled.Clip`
- [x] `PlayerViewModel` con `StateFlow<PlayerState>`, volume, position, duration
- [x] Test harness: `App.kt` con DebugPanel, Play/Pause/Stop
- [x] `FpsMeter` + volume/frame/draw FPS meters
- [x] Volume tick rate ~60Hz en JVM Windows

### Task 2 — Análisis del prototipo HTML [x]

**Goal:** Documentar los 13 efectos visuales del prototipo y contrastar con la implementación.

- [x] Análisis en frío del prototipo HTML → `docs/html_analysis.md`
- [x] Contraste HTML vs V4 → `docs/html_vs_v4.md`

---

## Phase 3: Componente Visualizador — Iteración (2026-04-11 → 2026-04-12)

### Task 3 — VoiceVisualizerRing V1→V4 (primera iteración) [x]

**Goal:** Implementar el componente composable. Cuatro versiones iteradas, cada una diagnosticando y resolviendo problemas de la anterior.

- [x] V1: stacks rellenos (onion ring — fills son discos, no anillos)
- [x] V2: anillo hueco con strokes + clipPath (bandas duras + bug escalón de radii)
- [x] V3: Modifier.blur + radii CSS-normalizados (centro lleno por fills)
- [x] V4: Modifier.blur + strokes en todos los canvases (halos congelados en Android)

### Task 4 — Fake-blur multi-pass (V5) [x]

**Goal:** Alternativa portable sin Modifier.blur ni dependencias externas.

- [x] Canvas único, N pasadas de strokes con width decreciente y alpha creciente
- [x] Sin congelamiento en ningún target
- [x] DrawPath: Full 24 | Low 12

### Task 5 — Cloudy blur library (V6) [x]

**Goal:** Blur gaussiano real vía Cloudy con key-trick para evitar congelamiento.

- [x] Integrar `skydoves/Cloudy` con `key(blurTick)` a ~15fps
- [x] 3 Canvas apilados: far halo + near halo con Cloudy, sharp ring sin blur
- [x] DrawPath: Full 9 | Low 6

### Task 6 — Keyframes circulares + relativeMotion (V7, experimento) [x]

**Goal:** Explorar estética de keyframes circulares con protuberancia localizada.

- [x] Keyframes circulares con llamarada localizada
- [x] `blurRadius` parametrizado
- [x] `relativeMotion`: CYCLE_MS distinto por blob → movimiento relativo real
- [x] Archivado. `relativeMotion` migrado a V8.

### Task 7 — Smoothing de picos + brillo dinámico (V8) [x]

**Goal:** Movimiento orgánico equivalente al prototipo HTML.

- [x] Filtro paso-bajo 2 etapas: pre-smooth (0.85) + lerp visual (0.15)
- [x] Brillo dinámico: floor `targetBright` 0.05f → glow se desvanece en silencio
- [x] `relativeMotion` heredado de V7

---

## Phase 4: Audio Enhancement (2026-04-12)

### Task 8 — FFT banda vocal + demo voice asset [x]

**Goal:** Replicar el AnalyserNode del prototipo HTML con FFT offline sobre el WAV.

- [x] FFT radix-2 Cooley-Tukey + `WavBandEnergy` (Blackman, bins 2-14, IIR 0.85, p99)
- [x] Asset `demo_voice_prototype.wav`
- [x] Toggle `useBandEnergy` en ViewModel
- [x] Selector de asset en DebugPanel

---

## Phase 5: Parametrización + Mobile Layout (2026-04-12 → 2026-04-13)

### Task 9 — Parametrización de smoothing (V9) [x]

**Goal:** Exponer constantes de suavizado como parámetros ajustables en caliente.

- [x] `inputSmoothing` y `responsiveness` como parámetros del composable
- [x] Sliders en bottom sheet
- [x] `rememberUpdatedState` para cambios instantáneos

### Task 10 — Layout mobile + platform detection [x]

**Goal:** Layout responsive para dispositivos móviles con color picker y Android wiring.

- [x] `isMobilePlatform()` expect/actual
- [x] `MobileLayout` con ring expandido + métricas + bottom sheets
- [x] Color picker con 5 presets
- [x] Android wiring: `writeBytesToCache` real vía `AppContextHolder`
- [x] `safeDrawingPadding` para system bars

---

## Phase 6: Web + Deploy (2026-04-13)

### Task 11 — Web (wasmJs) audio playback [x]

**Goal:** Audio playback funcional en web con deploy a Firebase Hosting.

- [x] `WasmAudioRecorderPlayer` con `HTMLAudioElement`
- [x] Timer leyendo `currentTime` / `duration` / `ended` cada ~16ms
- [x] Fix de build: split `newAudio` en dos funciones
- [x] Firebase Hosting config para deploy web

---

## Phase 7: Documentation (2026-04-13)

### Task 12 — Documentación y cleanup [x]

**Goal:** Dejar el repo documentado para entrega.

- [x] Mover versiones experimentales a `experiments/`
- [x] Bitácora centralizada en `docs/bitacora_estrategias.md`
- [x] Documentación de API pública en `VoiceVisualizerRing.kt`
- [x] Documentación para desarrollador cliente en V9
