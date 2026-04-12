# Contraste — prototipo HTML vs `VoiceVisualizerRingV4.kt`

Lectura pareada de cada efecto listado en [html_analysis.md](./html_analysis.md) contra lo que hace V4 hoy. El objetivo es identificar qué quedó fiel, qué quedó aproximado y qué falta.

Leyenda: ✅ equivalente · ≈ aproximado / con desviación · ❌ ausente.

## Tabla — efecto por efecto

| # | Efecto HTML | Estado en V4 | Cómo lo resuelve V4 (o qué falta) |
|---|---|---|---|
| 1 | Fondo radial oscuro | ❌ (responsabilidad del host) | V4 no pinta fondo; lo provee `App.kt`. Fuera del scope del componente. |
| 2 | Anillo duro 5 px | ✅ | Canvas "sharp" dibuja los 3 blobs como `Stroke(width = thickness.dp)`. |
| 3 | Glow outer gaussiano | ≈ | Se aproxima con **2 canvases blureados** (`Modifier.blur` 40·spread y 15·spread dp) sobre strokes escalados ×2.5 y ×1.6. Es **un blur gaussiano real**, no 3 shadows apilados — más limpio, pero el "escalón" de los 3 radios 15/40/70 no está: V4 tiene sólo 2 niveles de halo externo. |
| 4 | Glow inner (`inset box-shadow`) | ❌ | Compose no tiene `inset shadow`. V4 dibuja los blobs como **stroke** (no fill), así que el blur de esas capas crea halo a ambos lados del contorno de forma natural — ese es el reemplazo funcional. **Cualitativamente logra el efecto "halo simétrico dentro/fuera" pero sin el control fino de 3 radios inset distintos.** |
| 5 | 3 capas apiladas con glow escalonado | ≈ | En el prototipo cada capa tiene su propio `box-shadow` con alfas 0.3 / 0.2 escalonados. En V4 las 3 capas comparten la **misma** canvas stack (2 blureadas + 1 nítida); el escalonado por capa se hace dentro del canvas via `layerBright = b × (1 − layer×0.2)`. El resultado perceptual es similar (las capas de atrás brillan menos), pero la distribución del halo no es "3 glows independientes" sino "3 siluetas con la misma pipeline de halo". |
| 6 | Morph de border-radius (8 radios, 3 keyframes) | ✅ | `KEYFRAME_RADII` replica exacto los 3 arrays del CSS; `buildBlobFrame` construye el path bezier equivalente con `BEZIER_K = 0.5522847498f`. Incluye normalización CSS border-radius (cuando suma > ancho, se rescala) para evitar intersecciones por radios overflowing. |
| 7 | Rotación 0→360° en 8 s | ✅ | `KEYFRAME_ROTATIONS = [0, 120, 240]` + cierre a 360 en el último tramo, aplicado con `rotate(degrees, pivot)` en el canvas. |
| 8 | Phase offset por capa | ✅ | `PHASE_OFFSETS = [0, 0.3125, 0.625]` (equivalentes a `−2.5s` y `−5s` sobre 8s). Los 3 blobs se dibujan en **cada** canvas, así que los 3 contornos desfasados quedan visibles superpuestos en el sharp + difuminados pero presentes en los halos. |
| 9 | Blur sub-pixel global (0.3 px) | ❌ | No hay anti-alias explícito. Compose depende del AA nativo del renderer (Skia). En práctica, en JVM desktop el AA es bueno; si se ve aliasing duro, agregar un tercer `blur(1.dp)` sobre el ring sharp lo arreglaría. |
| 10 | Backdrop-filter 8 px | ❌ | No implementado. Compose no tiene equivalente directo. Poco visible en la demo original, baja prioridad. |
| 11 | Scale reactivo (1 → ~1.5) | ✅ | `targetScale = 1 + (avg/500) × intensity`, lerp 0.15 por frame — idéntico al JS. Aplicado con `scale()` del DrawScope sobre el canvas completo. |
| 12 | Brightness reactivo con escalón por capa | ✅ | `targetBright = 0.4 + (avg/150) × intensity`, lerp 0.15, y dentro del canvas se aplica `b × (1 − layer×0.2)` al alfa del color — fórmula 1:1 con el JS. |
| 13 | FFT banda vocal media | ❌ (por diseño) | V4 consume `volume: Float` ya precalculado. La fuente actual es **RMS completo del WAV** indexado por posición de playback (ver `WavAmplitude.kt`), no FFT con banda elegida. Decisión documentada en TASKS.md — el plugin de audio no expone amplitud en playback, así que se precomputa offline desde el WAV. Consecuencia perceptual: el ring reacciona a **energía total**, no a banda vocal; en una pista con bajos fuertes responderá más al kick que a la voz. |
| E | Triple smoothing (analyser 0.85 + lerp 0.15 + transition 50 ms) | ≈ | V4 tiene **sólo el lerp 0.15** (los otros dos son propios del pipeline web). En la práctica el RMS precomputado ya viene suavizado por el tamaño de bucket, así que la sensación "orgánica" se mantiene, pero con otra firma temporal. |

## Diferencias estructurales (no "efectos" pero relevantes)

1. **DOM vs Canvas.** El prototipo apila 3 elementos DOM; V4 hace 3 canvases en un `Box`. Consecuencia: V4 tiene control explícito sobre orden de draw y sobre qué canvas blurea, cosa que el DOM resolvía vía el propio layout.

2. **`Modifier.blur` vs `box-shadow` apilados.** Decisión registrada en el header de V4 como "desviación consciente de spec". Es más barato perceptualmente (un blur gaussiano real queda más suave que 3 gaussianas discretas apiladas) y escala mejor, a costa de no poder teñir cada halo de forma independiente (no hay `#00ffcc` en los radios externos como en el HTML).

3. **Fill vs Stroke en los halos.** V3 dibujaba los blobs filled en los canvases de halo; el blur preservaba la masa central y el anillo se veía relleno. V4 cambia a **stroke** en los 3 canvases, lo que emula el efecto `inset shadow` del prototipo sin necesidad de un inset real. Este es el cambio más importante entre V3 y V4 y es lo que hace que V4 "se parezca al prototipo".

4. **Fuente del volume.** HTML: `AnalyserNode.getByteFrequencyData()` live del audio que suena. V4: RMS precomputado del WAV indexado por `PlaybackProgress.currentPosition`. Determinista, cero latencia vs el audio, pero no funciona con audio externo/streaming/mic.

## Qué se ve bien y qué conviene revisar visualmente

Equivalencias confirmadas conceptualmente (falta ojo del usuario para confirmar perceptualmente):
- Silueta morpheada + rotación + phase offsets → deberían verse los 3 contornos cruzándose en el sharp.
- Halo doble-lado por stroke blureado → debería sentirse simétrico dentro/fuera.
- Pulso de scale y brillo amortiguado → debería sentirse "respirando", no saltón.

A observar con lupa comparando lado a lado:
- **Grosor del halo:** HTML tiene 3 niveles (15/40/70 px del `box-shadow`). V4 tiene 2 (blur 15 y 40 dp). Si el halo de V4 se corta más "cerca" del borde que el del HTML, agregar un tercer canvas con `blur(70.dp × glowSpread)` y stroke ×3.5 cubre el nivel faltante.
- **Tono del halo externo:** en HTML el radio 70 px usa `#00ffcc` (verdoso). V4 usa el `color` prop uniforme. Si se quiere fidelidad total, el canvas de halo lejano podría teñir con un hue-shift sutil.
- **Anti-alias del borde:** si el ring sharp se ve "pixelado" en algún target, agregar un `.blur(0.5.dp)` al canvas sharp replica el `filter: blur(0.3px)` del CSS.
- **Inner glow:** en HTML el `inset` ocupa 3 radios simétricos al outer. En V4 el "inner glow" es lo que el blur del stroke sangra hacia adentro — puede ser **más angosto** que el del prototipo. Si se nota, aumentar el `strokeWidthDp` multiplicador de los halos (hoy 2.5 y 1.6) lo compensa.

## Gaps conocidos vs prototipo — en orden de impacto

1. **RMS total en vez de FFT banda vocal** — el ring reaccionará distinto según el espectro de la pista.
2. **Halo de 2 niveles en vez de 3** — posible pérdida de extensión del glow.
3. **Sin hue-shift verdoso en el halo lejano** — fidelidad de color incompleta.
4. **Sin `backdrop-filter`** — irrelevante en esta demo, relevante si hay contenido atrás.
5. **Sin control de alfa por capa de halo** — se compensa con `alphaMul` por canvas, pero no es 1:1 con los 3 `box-shadow` alpha.

## Veredicto

V4 reproduce fielmente el esqueleto del prototipo: **silueta morpheada + rotación + 3 phase offsets + scale/brightness reactivos** están 1:1. El halo está reimplementado con una técnica distinta (blur gaussiano sobre strokes en vez de múltiples box-shadows con inset) que logra el mismo efecto cualitativo — borde nítido flotando en un halo simétrico — a cambio de menos granularidad de control. La única desviación funcional significativa es la fuente de volume (RMS vs FFT banda), que es una decisión de arquitectura documentada y no un bug de V4.
