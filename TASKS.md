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
