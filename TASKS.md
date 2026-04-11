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

## In progress
- [ ] nada

## Pending

### T7 — `VoiceVisualizerRing.kt`
Nuevo archivo: `composeApp/src/commonMain/kotlin/com/iattraxia/kmp_voice_ring/VoiceVisualizerRing.kt`.
Spec literal de `AGENTS.md` §"API del Componente" y `SPEC.md` §4-7.
- Firma: `fun VoiceVisualizerRing(volume: Float, color: Color, intensity: Float = 1f, thickness: Float = 5f, glowSpread: Float = 1f, lowPerformanceMode: Boolean = false, modifier: Modifier = Modifier)`.
- Constantes top-level `private`: `CYCLE_MS=8000L`, `LERP_FACTOR=0.15f`, `LAYER_COUNT=3`, `EASING = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)`, `PHASE_OFFSETS = floatArrayOf(0f, 0.3125f, 0.625f)`, `KEYFRAME_RADII` (Array<FloatArray> 3×8), `KEYFRAME_ROTATIONS = floatArrayOf(0f, 120f, 240f)`.
- Loop: `LaunchedEffect(Unit) { while (true) withFrameMillis { ... } }`. Variables trackeadas en `remember { mutableStateOf(FrameSnapshot(...)) }`.
  - `average = volume.coerceIn(0f,1f) * 255f`
  - `targetScale = 1f + (average/500f)*intensity`
  - `targetBright = 0.4f + (average/150f)*intensity`
  - Lerp 0.15 en scale y bright.
- `Canvas(modifier)`: `remember { Array(3) { Path() } }`. Por frame: `path[i].reset()`, recalcular 8 radios interpolados entre keyframes, construir con 4 esquinas × Bézier cúbico (k≈0.5522847498), `withTransform { scale(currentScale); rotate(rotDeg) { ... } }`, dibujar glow sublayers (spec §4.3 normal/low-perf), fill del blob, stroke de borde **solo en capa 0** con `thickness.dp.toPx()`.
- `layerBright = (currentBright * (1f - i*0.2f)).coerceIn(0f, 1f)`.
- **PROHIBIDO:** `@Preview`, `Modifier.blur`, allocs en `DrawScope`, `android.graphics`, `CoreGraphics`, `Skia` directo.

### T10 — Verificación end-to-end (por target, el usuario corre)
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

---

## Estado vivo (última actualización)
- Último commit del feature: T6b (FPS meters).
- Pipeline de audio + player + VM + harness + FPS diagnóstico funcionando en JVM desktop, verificado por el usuario.
- **Diagnóstico de performance:** `fps volume ≈ 32`, `fps frame = 60`, `fps draw ≈ 32`. Compose no es el cuello; el ticker del plugin emite a ~32Hz con el `updateIntervalMs` default. Para llegar a 60Hz visuales hay que bajar `updateIntervalMs` a 16ms (el JVM actual tolera hasta 8ms).
- **Próximo paso concreto:** T7 — implementar `VoiceVisualizerRing.kt` según spec literal de `AGENTS.md` + `SPEC.md`. Después swap del `DebugPanel` por el ring (o conviven) en `App.kt`. El ajuste de `updateIntervalMs` va como task aparte cuando toque.
- **Notas pendientes:**
  - `Greeting.kt` sigue existiendo sin referencias — opcional borrar.
  - Android/iOS/wasmJs: `writeBytesToCache` tira `NotImplementedError` en runtime. Compila, pero no correr en esos targets todavía.
  - Seek/volume de `Clip` mapea 0..1 → dB logarítmico; recording devuelve `Result.failure(UnsupportedOperationException)`.
