# Tech & Architecture

## Language & Runtime

- **Kotlin** — Kotlin Multiplatform (KMP).
- **Targets:** Android, iOS (arm64 + simulator), JVM (Desktop/Windows), wasmJs (Web).
- **UI Framework:** Compose Multiplatform (Compose Multiplatform + Jetpack Compose runtime).

## Key Dependencies

- **Compose Multiplatform** — UI toolkit compartido entre plataformas.
- **`io.github.hyochan:kmp-audio-recorder-player:1.0.0-alpha04`** — plugin de audio para playback cross-platform (excepto JVM que usa `javax.sound.sampled.Clip`).
- **`skydoves/Cloudy`** — blur library para el efecto glow en V6/V8/V9 (GPU-accelerated con fallback nativo).
- **No otras dependencias externas.** El componente visualizador usa solo `androidx.compose.*` + `kotlin.math.*`.

## Architecture

### Componente visualizador (objetivo central del proyecto)

```
VoiceVisualizerRing.kt          # Dispatcher → apunta a la versión activa (V9)
VoiceVisualizerRingV9.kt        # Versión activa: V8 + inputSmoothing/responsiveness params
VoiceVisualizerRingV8.kt        # V6 Cloudy + smoothing 2 etapas + brillo dinámico
VoiceVisualizerRingV6Cloudy.kt  # Cloudy blur + key(tick) trick
experiments/
  VoiceVisualizerRingV1-V5.kt   # Versiones descartadas, documentadas en bitácora
  VoiceVisualizerRingV7.kt      # Experimento de keyframes circulares (archivado)
```

Un solo archivo `.kt` autónomo por versión. Renderizado exclusivamente vía `DrawScope` (Canvas). Sin Composables internos, sin layouts, sin Material.

### Pipeline de audio

```
WAV file → WavAmplitude.kt (RMS buckets precomputados)
         → WavBandEnergy.kt (FFT + banda vocal, opcional)
         → PlayerViewModel (StateFlow<PlayerState>)
         → AudioPlayerFactory (expect/actual por plataforma)
            ├── Desktop: javax.sound.sampled.Clip
            ├── Android: kmp-audio-recorder-player
            ├── iOS: kmp-audio-recorder-player (stub)
            └── wasmJs: HTMLAudioElement
```

`volume` = RMS precomputado del WAV indexado por posición de playback. El plugin no expone amplitud en playback — se precomputa offline.

### App de demo / test harness

```
App.kt                          # Entry point con DesktopLayout / MobileLayout
debug/
  FpsMeter.kt                   # FPS meter stdlib-only
player/
  PlayerViewModel.kt            # MVVM, StateFlow<PlayerState>
  AudioPlayerFactory.kt         # Factory expect/actual
platform/
  CacheFile.kt                  # writeBytesToCache expect/actual
```

Layout responsive: `isMobilePlatform()` detecta Android/iOS vs Desktop/Web. MobileLayout expande el ring, DesktopLayout muestra debug panel lateral.

## Conventions

- **Commits:** `Module - Area: description` (imperativo, capitalizado). Ver `.claude/rules/commits.md`.
- **Tareas:** `TASKS.md` = estado activo, `DONE.md` = historial. Agente no compila — el usuario compila y confirma.
- **Documentación:** comentarios de bloque en español, sin KDoc, en los archivos del componente.
- **Sin compilación por agente:** nunca `./gradlew`, `installDebug`, `run`, tests. El usuario compila.

## Key Technical Decisions

1. **Blur vía Cloudy library, no Modifier.blur.** `Modifier.blur` congela la animación en Android (hardware layer caching). Cloudy + `key(tick)` a ~15fps evita el problema.
2. **Volumen precomputado, no en tiempo real.** El plugin de audio no expone amplitud durante playback. Se parsea el WAV offline y se indexa por posición.
3. **Glow aproximado, no gaussiano exacto.** CSS `box-shadow` produce gaussiano nativo. Compose Canvas no tiene `drawGlow()`. Se aproxima con Cloudy blur sobre strokes, o multi-pass strokes en V5 (portable, sin deps).
4. **Radii CSS-normalizados.** Los keyframes del prototipo tienen radios que suman >100%. Se normalizan como lo hace un browser para evitar intersecciones en el path.
5. **Single file component.** Una constraint deliberada: un solo `.kt` autónomo, cero dependencias externas (excepto en versiones que usan Cloudy).

## Gaps / Future Considerations

- **iOS sim validation** pendiente (T19 en TASKS.md).
- **wasmJs audio** funcional pero con posible bloqueo de autoplay en browsers.
- **V9 parametrización** activa pero pendiente verificación visual del usuario.
- **FFT banda vocal** implementado pero el default es RMS completo.
