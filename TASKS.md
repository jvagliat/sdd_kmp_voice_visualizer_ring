# TASKS — VoiceVisualizerRing

KMP Compose. Targets: Android, iOS (arm64 + simulator), JVM (Win desktop), wasmJs.

## Cómo usar este archivo
- Fuente de verdad del estado activo. Historial completo en `DONE.md`.
- Al retomar: leer este archivo y el último commit. No re-explicar nada al usuario.
- Al terminar una task: moverla a `DONE.md` con commit hash.

---

## Reglas duras (NO re-preguntar)
- **No compilar yo.** Nunca `./gradlew ...`, `installDebug`, `run`, tests. El usuario compila y confirma.
- **No rompas nada.** Cada commit debe dejar el repo compilando y la app arrancable en los 4 targets.
- **Feedback continuo, español informal.** Sin silencios largos.
- **Commits:** `Module - Area: description` capitalizado, imperativo. Ver `.claude/rules/commits.md`.

## Decisiones ya tomadas (NO re-preguntar)
- `volume` = RMS precomputado del WAV indexado por posición. Plugin no expone amplitud en playback.
- Plugin: `io.github.hyochan:kmp-audio-recorder-player:1.0.0-alpha04`.
- `js` legacy eliminado; `wasmJs` es el target web.
- **JVM player:** `javax.sound.sampled.Clip` (JDK stdlib).
- **Spec del componente:** `AGENTS.md` + `SPEC.md` son fuente de verdad. Un solo `.kt` en commonMain, cero deps fuera de `androidx.compose.*` + `kotlin.math.*`.

---

## In progress

- [ ] **T15c** Trim/normalización del demo WAV — por ahora crudo; abrir si rompe la escala.
- [ ] **T14e** Verificación visual band-energy vs prototipo HTML — pendiente usuario.
- [ ] **T16c** Verificación end-to-end nuevo WAV en JVM desktop — pendiente usuario.
- [ ] **T18d** Verificación sin regresión desktop + prueba mobile (usuario compila y confirma).
- [ ] **T20** Verificación visual V5 (ver sección T20 en Pending — usuario compila y confirma).

---

## Pending

### T26 — V9: parametrizar smoothing (Stage 1 y Stage 2)

**Objetivo:** exponer las constantes de suavizado de V8 como parámetros del composable y agregar sliders en un nuevo bottom sheet.

**Cambios en `VoiceVisualizerRingV9.kt`** (fork de V8):
- `inputSmoothing: Float = 0.85f` — decay de Stage 1 (`PRE_SMOOTH_DECAY`). Rango útil: 0.0–0.95.
- `responsiveness: Float = 0.15f` — factor de Stage 2 (`LERP_FACTOR`). Rango útil: 0.01–1.0.
- `PRE_SMOOTH_NEW` se deriva como `1f - inputSmoothing` (no es parámetro independiente).
- Ambos se pasan al `LaunchedEffect` via `rememberUpdatedState` para que cambios en caliente sean efectivos sin reiniciar la animación.

**Cambios en `App.kt`**:
- Agregar estado `inputSmoothing` y `responsiveness` en `App()`.
- Agregar un tercer bottom sheet (icono `Equalizer` o similar) en Desktop y Mobile con dos `EffectSlider`:
  - `"inputSmoothing"`, rango `0f..0.95f`
  - `"responsiveness"`, rango `0.01f..1f`
- Dispatcher apunta a V9.

**Criterio de done:** sliders funcionales en ambos layouts, V9 activo, valores por defecto idénticos al comportamiento de V8.

---

### T25 — Refactor de documentación

**Objetivo:** dejar la base de código documentada para entrega al cliente.

**Tres cambios coordinados:**

1. **Crear `docs/bitacora_estrategias.md`**
   - Centralizar el historial de estrategias actualmente disperso en los headers de V1..V8.
   - Formato: una sección por versión — hipótesis, approach, drawPath count, problemas descubiertos, conclusión.
   - Incluye el contenido actual del bloque de comentarios de `VoiceVisualizerRing.kt` (V1..V8) y los headers de cada `experiments/VoiceVisualizerRingVx.kt`.

2. **Refactorizar `VoiceVisualizerRing.kt`**
   - Reemplazar el bloque V1..V8 por una referencia a `docs/bitacora_estrategias.md`.
   - Agregar documentación del contrato público: propósito del componente, comportamiento audio-reactivo, descripción de cada parámetro.
   - Estilo: comentarios de bloque (no KDoc), como el estilo actual del archivo.

3. **Documentar `VoiceVisualizerRingV8.kt` para el desarrollador cliente**
   - Sin referencias a versiones anteriores ni a la bitácora.
   - Cabecera: propósito y comportamiento visual del componente (qué ve el usuario).
   - Sección parámetros: qué controla cada uno, rangos típicos, efectos visuales.
   - Internals: una descripción por sección del código —
     - Constantes y keyframes: qué representa cada grupo
     - `VoiceVisualizerRingV8` composable: el loop de animación (`LaunchedEffect`), el filtro de suavizado, el modelo escala/brillo, la estructura de 3 canvases y el key-trick de Cloudy
     - `BlobsCanvasV8`: cómo lee estado, cómo calcula escala y brillo por capa
     - `buildBlobFrameV8`: interpolación de keyframes, normalización CSS de radios, construcción del path Bézier
   - Estilo: comentarios de bloque en español, sin KDoc.

**Criterio de done:** un desarrollador Kotlin/Compose sin contexto de este repo puede leer `VoiceVisualizerRingV8.kt` de arriba a abajo y entender qué hace cada parte antes de tocar una línea.

---

### T24 — Verificación audio wasmJs (HTMLAudioElement)

**Implementado:** `WasmAudioRecorderPlayer` reemplaza el timer ficticio por un `HTMLAudioElement` real.
- `external interface JsAudio : JsAny` + `js("new Audio(src)")` — sin dependencias nuevas.
- El ticker lee `currentTime` / `duration` / `ended` del elemento JS cada ~16 ms.
- La duración real se emite en `PlaybackProgress.duration`, el ViewModel la pica automáticamente.

**Pendiente usuario:**
- [ ] Compilar wasmJs y abrir en browser.
- [ ] Presionar Play — debe sonar el audio WAV.
- [ ] Verificar que el ring anima en sincronía con el audio.
- [ ] Verificar que el playback para solo al terminar (no antes, no loop).
- [ ] Verificar que Pause/Resume funcionan (pausa el audio y reanuda en la misma posición).

**Posibles problemas:**
- Si el browser bloquea autoplay sin interacción previa, `audio.play()` puede quedar pendiente — el ring no animará hasta que el usuario interactúe.
- `audio.duration` es `NaN` los primeros ticks (metadata aún cargando) — ya manejado con `rawDur.isNaN()`.

---

### T23 — Fix de build JS

**Pendiente de detalle** — el usuario lo especificará en una sesión separada.

---

### T21+T22 — Verificación visual V8 (smoothing + brillo dinámico)

**Implementado en V8** (`VoiceVisualizerRingV8.kt`) — dispatcher apunta a V8.

**Pendiente usuario:**
- [ ] Compilar y verificar que los picos ya no se ven "nerviosos" (movimiento líquido).
- [ ] Verificar que el glow baja en silencio y sube con el audio (respira).
- [ ] Comparar comportamiento con `lowPerformanceMode = true` y `false`.
- [ ] Confirmar sin regresión en Desktop (JVM) y wasmJs.

---

### T20 — Verificación visual VoiceVisualizerRingV5

**Implementado:** `VoiceVisualizerRingV5.kt` — Canvas único, multi-pass strokes sin `Modifier.blur`.
Dispatcher `VoiceVisualizerRing.kt` ya apunta a V5.

**Pendiente usuario:**
- [ ] Compilar y verificar que halos animan en Android (el bug de V4 era halos congelados).
- [ ] Revisar aspecto visual: glow suave en far halo (4p), near halo (3p), sharp ring encima.
- [ ] Comparar `lowPerformanceMode = true` vs `false`.
- [ ] Confirmar sin regresión en Desktop (JVM) y wasmJs.

---

### T19 — Validar ring en iOS sim

T12 confirmó compatibilidad de código. Verificar en runtime:
- `Modifier.blur` + `BlurredEdgeTreatment.Unbounded`: halo sin recortes.
- `withFrameMillis` / 60 FPS: medir con FpsMeter (`CADisplayLink`).
- `fps volume`: verificar que el ticker del plugin no clampea por encima de 16ms.
- Costo GPU en dispositivo físico.

Depende de: stub `writeBytesToCache` en iosMain (o bypass directo desde commonMain).

---

### T13 — Verificación end-to-end por target (usuario corre)

- **Desktop (JVM):** ventana levanta, Play reproduce, blobs reaccionan al audio.
- **Android:** instalar en dispositivo real.
- **iOS:** Xcode → iPhone sim.
- **wasmJs:** browser.
- Probar `lowPerformanceMode = true` y cambios dinámicos de `color`/`thickness`/`glowSpread`/`intensity`.

---

## Blockers / notas abiertas
- **wasmJs + filesystem:** `AudioSource.File(path)` puede no funcionar en wasmJs. Plan B: visualizer ok, playback no.
- **Android/iOS tick rate:** T6c solo verificó JVM. En Android/iOS el ticker del plugin puede clampar a >16ms. Verificar `fps volume` y considerar interpolación en el VM.
- **`Modifier.blur` en Android:** congelado en hardware layer. V5 lo reemplaza. Ver T20.

---

## Estado vivo
- Pipeline completo verificado: JVM desktop (audio + ring animado) y Android (ring animado, layout mobile).
- **V8 activo en dispatcher** (`VoiceVisualizerRing.kt` → `VoiceVisualizerRingV8`).
- V8 = V6Gml51Cloudy + T21 (filtro paso-bajo 2 etapas) + T22 (glow dinámico, floor 0.05f).
- V7 movido a `experiments/` — experimento de keyframes circulares, no era la rama principal.
- **Pendiente:** T21+T22 verificación visual (usuario compila), T20, T18d (mobile), T14e/T16c (audio).
- `Greeting.kt` sin referencias — opcional borrar.
