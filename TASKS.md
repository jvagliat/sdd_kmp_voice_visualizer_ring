# TASKS — VoiceVisualizerRing

Marco de pruebas + componente `VoiceVisualizerRing` en KMP Compose.
Targets: Android, iOS (arm64 + simulator), JVM (Win desktop), wasmJs.

## Cómo usar este archivo
- Fuente de verdad del estado del trabajo. Editarlo al avanzar.
- Al retomar: leer este archivo y el último commit. No re-explicar nada al usuario.
- Al terminar una task: mover a Done con commit hash. Si queda parcial, dejar nota en la línea.

---

## Reglas duras (memoria persistente — NO re-preguntar)
- **No compilar yo.** Nunca `./gradlew ...`, `installDebug`, `run`, tests. El usuario compila y confirma. Pedírselo cuando haga falta antes de commit.
- **No rompas nada.** Cada commit debe dejar el repo compilando y la app arrancable en los 4 targets. Si un cambio es inseparable, preguntar.
- **Feedback continuo, español informal.** Sin silencios largos. Preguntar antes de cambios de config.
- **Commits:** `Module - Area: description` capitalizado, imperativo. Ver `.claude/rules/commits.md`.

## Decisiones ya tomadas (NO re-preguntar)
- `volume` del visualizer = RMS precomputado del WAV, indexado por `PlaybackProgress.currentPosition` del plugin. Plugin `kmp-audio-recorder-player` **no** expone amplitud en playback.
- Plugin: `io.github.hyochan:kmp-audio-recorder-player:1.0.0-alpha04` (ya en libs.versions.toml).
- `js` legacy eliminado; `wasmJs` es el target web.
- Asset de prueba: **Big Buck Bunny — "Prelude" de Jan Morgenstern, CC BY 3.0** en `composeApp/src/commonMain/composeResources/files/audio/Jan_Morgenstern_-_01_-_Prelude.wav` (~5.5 MB, puesto por el usuario).
- Spec del componente: `AGENTS.md` + `SPEC.md` son fuente de verdad. Un solo `.kt` en commonMain, cero deps fuera de `androidx.compose.*` + `kotlin.math.*`, Canvas + DrawScope + `withFrameMillis`, Path reutilizado, 60 FPS.
- **Desktop-first**: iteración actual prueba en JVM. Los actuals de androidMain/iosMain/wasmJsMain deben compilar pero pueden ser stubs mínimos. Android wiring (AppContextHolder etc.) queda para cuando toque Android real.
- **JVM player**: `javax.sound.sampled.Clip` (JDK stdlib). VLCJ descartado por dep pesada. Ver T5c.

---

## Done
- [x] **T1** `js` legacy eliminado, `webMain/` consolidado en `wasmJsMain/`. → `8ab2c2e`
- [x] **T2** Dep `kmp-audio-recorder-player:1.0.0-alpha04` agregada a commonMain. → `6629bac`
- [x] **T3a** Asset WAV dropeado en `composeApp/src/commonMain/composeResources/files/audio/Jan_Morgenstern_-_01_-_Prelude.wav` (CC BY 3.0, Blender/Peach). → incluido en `b58a87b`.
- [x] **T3b** Header WAV parseado: `audioFormat=1 (PCM), channels=2, bits=16, rate=44100, dataSize=5695488 bytes` → ~32.29 s. Parser T4 toma canal 0 en 16-bit LE. Info de referencia.
- [x] **T4** Parser WAV → RMS buckets en `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/audio/WavAmplitude.kt`. Soporta PCM mono/estéreo 8/16/24/32-bit LE, un pase, normalización al pico. → `1284f2b`
- [x] **T5b** `writeBytesToCache` expect/actual. JVM real (`java.io.tmpdir/kmp_voice_ring/`); android/ios/wasmJs stubs `NotImplementedError` (compilan, no se corren). → `b58a87b` (con asset).
- [x] **T5c** Desktop JVM player real: `AudioPlayerFactory.kt` expect + 4 actuals. JVM instancia `DesktopAudioRecorderPlayer` usando `javax.sound.sampled.Clip` (playback completo + ticker de `PlaybackProgress`; recording devuelve `Result.failure`). Android/iOS/wasmJs delegan en `createAudioRecorderPlayer()` del plugin. → `c5b986f`
- [x] **T5a** `PlayerViewModel` con `StateFlow<PlayerState>` (Idle/Loading/Ready/Playing/Paused/Error), `volume`, `positionMs`, `durationMs`. `load()` hace `Res.readBytes()` → `parseWavAmplitudes()` → `writeBytesToCache()`. Listener mapea `currentPosition / bucketMs` al índice de buckets. Al llegar a duration vuelve a Ready. → `da5a79c`
- [x] **T6** Test harness: `App.kt` con fondo oscuro, `DebugPanel` provisional (barra de volumen + texto state/pos/dur/vol monoespaciado) en slot 320.dp, botones Play/Pause/Stop, `LaunchedEffect` carga asset, `DisposableEffect` limpia. → `1b48f04`
- [x] **T6b** FPS meters: `FpsMeter` (ventana de 1s con `TimeSource.Monotonic`, stdlib only) + `volumeFps` en `PlayerViewModel` (tick dentro del listener), `frameFps` en `App` (via `withFrameMillis`), `drawFps` tickeado dentro de un `Canvas` stand-in que lee `volume`. Diagnóstico: volume ≈ 32 Hz (cuello en `updateIntervalMs` del plugin), frame = 60, draw sigue a volume sin drops.
- [x] **T6c** Volume tick rate subido a ~60 Hz en JVM: `PlayerViewModel.init` llama `player.setPlayerProperties(AudioRecorderPlayerProperties(updateIntervalMs = 16L))` + workaround en `jvmMain/main.kt` (`raiseWindowsTimerResolution`: daemon thread parkeado en `Thread.sleep(Long.MAX_VALUE)` que activa `timeBeginPeriod(1)` process-wide en Windows, no-op en otros OS). Sin el hack, el default timer granularity de Windows (~15.6ms) capaba cualquier `delay(16)` a ~32Hz. Verificado por el usuario: `fps volume ≈ 58`, `fps draw ≈ 58`, `fps frame = 60`.
- [x] **T7** `VoiceVisualizerRing` implementado en commonMain con 4 versiones preservadas (V1→V4) + dispatcher delgado en `VoiceVisualizerRing.kt`. App.kt swap del slot 320.dp por el ring + DebugPanel reubicado debajo. Verificado visualmente por el usuario en JVM Win desktop. Recorrido: V1 (stacks rellenos, descartado por "onion ring"), V2 (clipPath strokes, descartado por 11 bandas duras + escalón en radii sin normalizar CSS), V3 (Modifier.blur + radii normalizados + blobs filled, descartado porque el blur preserva masa central → ring lleno), V4 (Modifier.blur + blobs STROKED en los 3 canvases con stroke widths escalonados → halo gaussiano sin masa central + 3 phase offsets visibles superpuestos). Cada Vx documenta hipótesis/approach/drawPath count/problemas en su header.
- [x] **T10** Análisis en frío del prototipo HTML → `docs/html_analysis.md`. 13 efectos listados + tabla + elaboración en 5 bloques físicos (silueta, halo, multiplicidad, reactividad, smoothing). Síntesis: super-ellipse rotante de 8 s replicado 3 veces con phase offsets, halo gaussiano simétrico in/out, scale+opacity lerpeados desde FFT banda vocal media.
- [x] **T11** Contraste HTML vs V4 → `docs/html_vs_v4.md`. Esqueleto 1:1 (morph + rotación + phase offsets + scale/brightness). Halo reimplementado con `Modifier.blur` sobre strokes en vez de `box-shadow` apilados. Gap funcional principal: RMS total vs FFT banda vocal (origen de T14).
- [x] **T17** Selector de asset + toggle `band-energy` en `DebugEffectsPanel` (bottom-end) + re-load keyed en `LaunchedEffect(assetIndex, useBandEnergy)`. `PlayerViewModel.load()` hace stop-if-playing y resetea `positionMs`/`volume` antes de reparsear, para evitar que el listener mapee posición vieja sobre buckets nuevos. Verificado por el usuario en JVM desktop (captura adjunta).

## In progress
- [ ] **T15 — Preprocesar nuevo WAV demo del cliente.** Subtareas:
  - [x] **T15a** Asset copiado a `composeApp/src/commonMain/composeResources/files/audio/demo_voice_prototype.wav` (convive con el Prelude).
  - [x] **T15b** Specs verificadas: PCM1, 2 ch, 44100 Hz, 16-bit. Parser T4 lo come sin cambios.
  - [ ] **T15c** Trim/normalización — por ahora crudo; abrir si rompe la escala.

- [ ] **T14 — FFT banda vocal media (réplica prototipo HTML).** Implementado. Decisiones aplicadas: conviven ambos parsers, L+R → mono, normalización p99. Subtareas:
  - [x] **T14a** API: conviven `parseWavAmplitudes` (RMS) y `parseWavBandEnergies` (banda), toggleados por flag en el VM.
  - [x] **T14b** `audio/Fft.kt` — radix-2 Cooley–Tukey stdlib-only, tablas sin/cos precomputadas + bit-reverse, tamaño 256.
  - [x] **T14c** `audio/WavBandEnergy.kt` — L+R → mono, Blackman window (Web Audio default), FFT 256 / hop 128, smoothing IIR 0.85 por bin, dB `[-100,-30]` → byte 0..255, promedio bins 2..14 inclusive (13 bins), buckets 16 ms, normalización p99.
  - [x] **T14d** `PlayerViewModel.load(path, useBandEnergy = false)` — default conserva comportamiento RMS, flag cambia al band-energy.
  - [ ] **T14e** Verificación visual lado a lado con prototipo HTML — pendiente usuario.

- [ ] **T16 — Cargar el nuevo WAV en la App.** Subtareas:
  - [x] **T16a** `App.kt`: `ASSET_PATH = files/audio/demo_voice_prototype.wav`, `USE_BAND_ENERGY = true`.
  - [x] **T16b** Selector en DebugPanel — ver **T17**.
  - [ ] **T16c** Verificación end-to-end en JVM desktop — pendiente usuario.

## Pending
### T12 — iOS compatible? 
VoiceVisualizerRing con los imports de androidx es compatible con iOS?

### T18 — Relayout mobile del harness
El layout actual está pensado para desktop (1600×900 aprox): ring al centro con panel de métricas top-end y panel de controles bottom-end, ambos de 260.dp a la derecha. En mobile (pantalla angosta) eso no entra: los paneles comen el ring o se superponen.

Objetivo: que la app sea usable en Android/iOS sin reimplementar el ring ni perder los controles de debug.

Ideas (elegir en implementación):
- Detectar viewport angosto (e.g. `maxWidth < 600.dp` vía `BoxWithConstraints`) y apilar verticalmente: ring arriba (aspecto cuadrado), controles + métricas debajo en una `Column` scrolleable.
- En mobile los paneles de debug podrían ir plegados en un drawer / bottom sheet / tabs (métricas | selector | sliders).
- Play/Stop siempre visibles (barra fija abajo).
- Paneles desktop intactos cuando hay espacio: branchar por breakpoint, no reescribir.

Subtareas (por definir cuando se encare):
1. **T18a** Probar en qué targets pasa (Android phone retrato, iOS sim). Capturar estado actual para referencia.
2. **T18b** Introducir breakpoint (`BoxWithConstraints` en `App`) y layout mobile inicial — ring + controles apilados.
3. **T18c** Decidir si los debug panels van detrás de un toggle (ej. ícono que abre drawer) o siempre visibles debajo del ring.
4. **T18d** Verificación visual en ambos modos.

### T13 — Verificación end-to-end (por target, el usuario corre)
Al terminar T7, pedir al usuario:
- **Desktop (JVM):** `./gradlew :composeApp:run` — ventana levanta, botón Play reproduce, blobs reaccionan al audio.
- **Android:** `./gradlew :composeApp:installDebug` → Pixel u otro.
- **iOS:** Xcode → iPhone sim.
- **wasmJs:** `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`.
- Probar `lowPerformanceMode = true` (toggle temporal o default cambiado) y cambios dinámicos de `color`/`thickness`/`glowSpread`/`intensity`.

### T13 — Verificación end-to-end (por target, el usuario corre)
Al terminar T7, pedir al usuario:
- **Desktop (JVM):** `./gradlew :composeApp:run` — ventana levanta, botón Play reproduce, blobs reaccionan al audio.
- **Android:** `./gradlew :composeApp:installDebug` → Pixel u otro.
- **iOS:** Xcode → iPhone sim.
- **wasmJs:** `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`.
- Probar `lowPerformanceMode = true` (toggle temporal o default cambiado) y cambios dinámicos de `color`/`thickness`/`glowSpread`/`intensity`.

---

## Blockers / notas abiertas
- **wasmJs + filesystem:** diferido. Cuando toque activar wasmJs, hay que ver si `AudioSource.File(path)` del plugin acepta paths en ese target o toca blob URL. Plan B: "visualizer ok, playback no".
- **Android wiring:** diferido. Cuando toque Android real, el actual de `writeBytesToCache` necesita un `AppContextHolder` singleton rellenado desde `MainActivity.onCreate` antes de `setContent`. El patrón es deuda del marco de prueba, no del componente.
- **Android/iOS tick rate:** T6c sólo verificó el cambio en JVM. Los tickers nativos del plugin en Android/iOS pueden clampar internamente a un piso mayor que 16ms. Cuando toque correr en esos targets, verificar empíricamente con los meters y ver si hace falta interpolación del volume en el VM como fallback.

---

## Estado vivo (última actualización)
- Último commit del feature: T7 (`VoiceVisualizerRing` V4 activo + harness reorganizado).
- Pipeline completo en JVM desktop: audio + player + VM + harness + FPS meters + ring visual reactivo, verificado por el usuario.
- **Diagnóstico de performance:** `fps volume ≈ 58`, `fps frame = 60`, `fps draw ≈ 58`. El techo práctico en JVM Windows queda marcado por jitter del scheduler de coroutines, no por el plugin ni por Compose.
- **Desviación de spec consciente en V4:** el spec prohibía `Modifier.blur` y pedía glow por strokes apilados; las pruebas V1/V2 mostraron que stack-of-strokes no escala a glow gaussiano suave (11 bandas visibles) y V3/V4 caen en `Modifier.blur` con `BlurredEdgeTreatment.Unbounded`. Documentado en los headers de cada Vx. Si esta desviación no es aceptable, se puede revertir a V2 cambiando una línea en el dispatcher.
- **Próximo paso concreto:** T14e + T16c — usuario compila y verifica que el demo respira con la voz y no con los bajos. Después, T17 (selector en DebugPanel) para A/B en caliente.
- **Notas pendientes:**
  - `Greeting.kt` sigue existiendo sin referencias — opcional borrar.
  - Android/iOS/wasmJs: `writeBytesToCache` tira `NotImplementedError` en runtime. Compila, pero no correr en esos targets todavía.
  - Seek/volume de `Clip` mapea 0..1 → dB logarítmico; recording devuelve `Result.failure(UnsupportedOperationException)`.
