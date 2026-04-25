# Bitácora de estrategias — VoiceVisualizerRing

Historia de implementaciones del componente, desde el primer intento hasta la versión activa.
Cada entrada documenta hipótesis de partida, approach técnico, costo de draw, problemas encontrados
y por qué se descartó o evolucionó.

Los archivos de las versiones descartadas viven en `experiments/`.
La versión activa es la que despacha `VoiceVisualizerRing.kt`.

---

## V1 — Stacks rellenos

**Archivo:** `experiments/VoiceVisualizerRingV1.kt`

**Hipótesis:** cada capa es un blob filled con halo de strokes anchos a ambos lados del contorno.
Las 3 capas con phase offset crean una forma orgánica multicapa. El outline nítido va encima,
en la capa primaria.

**Approach:**
- Outer glow: 3 strokes anchos pintados antes del fill → la mitad exterior del stroke es el halo
- Fill: `drawPath` relleno con alpha de capa → tapa la mitad interior del outer stroke
- Inner glow: 3 strokes dentro de `clipPath(Intersect)` → solo la mitad interior queda visible
- Outline nítido (stroke fino) solo en capa 0; capas 1 y 2 sin outline propio

**DrawPath count:** Full: 14 | Low: 6

**Problemas:**

1. **Onion ring (rodajas concéntricas).** Las capas 1 y 2 son discos opacos superpuestos al ring
   principal. En lugar de un anillo hueco, el resultado es un objeto sólido con anillos concéntricos
   visibles. El alpha bajo no "disuelve" las capas: el ojo distingue claramente los bordes duros donde
   termina cada blob filled. El prototipo del cliente es un ring hueco con glow, no un disco sólido.

2. **(Latente) Bug del escalón.** Radii overflowing sin normalización CSS, tapado visualmente por
   el problema #1. Diagnosticado explícitamente en V2 cuando se removieron los fills.

**Conclusión:** la intuición de "fill + glow = ring" no funciona. Los fills son discos, no anillos.

---

## V2 — Anillo hueco con strokes + clipPath

**Archivo:** `experiments/VoiceVisualizerRingV2.kt`

**Hipótesis:** quitar todos los fills y dejar solo strokes resuelve el onion ring de V1.

**Approach:**
- Outer glow: stroke ancho + `clipPath(Difference)` → mitad exterior visible (halo fuera del ring)
- Inner glow: stroke ancho + `clipPath(Intersect)` → mitad interior visible (halo dentro del ring)
- Outline nítido al final, solo capa 0

**DrawPath count:** Full: 11 | Low: 7

**Problemas:**

1. **11 bandas duras visibles.** Cada stroke clipped tiene alpha constante a lo ancho del stroke —
   no hay falloff gaussiano. El ojo distingue los 11 escalones discretos. Para aproximar un glow suave
   por strokes apilados harían falta 8-15× más subcapas: coste de draw prohibitivo.

2. **Escalón en contorno (bug de radii overflowing).** En varias filas de `KEYFRAME_RADII` la suma
   de radios adyacentes supera 100 (ej. 45+60=105, 55+60=115). Sin normalización CSS, la coordenada
   `w - h2` retrocede cuando `h1 + h2 > w`, creando una intersección propia en el path que Skia
   renderiza como un escalón vertical visible a las 12 y las 6 del ring.

**Conclusión:** stack-of-strokes no escala a glow suave. Hace falta blur gaussiano real.
Y hay que normalizar los radios al estilo CSS.

---

## V3 — Modifier.blur + radii CSS-normalizados + blobs filled

**Archivo:** `experiments/VoiceVisualizerRingV3.kt`

**Hipótesis:** V2 confirmó que sin fills el ring es hueco ✓. Quedan dos problemas: bandas duras y
escalón. V3 ataca ambos: usar `Modifier.blur` (blur gaussiano real, GPU-accelerated) y normalizar los
radios al estilo CSS.

**Normalización CSS:** en cada frame se calcula `factor = min(1, w/(h1+h2), w/(h3+h4), h/(v1+v4),
h/(v2+v3))`. Si la suma de radios adyacentes excede el lado, todos se multiplican por `factor`.
Mismo comportamiento que un browser. Elimina las intersecciones propias del path.

**Estructura:** 3 Canvas apilados en `Box`. Far halo y near halo dibujan blobs FILLED + `Modifier.blur`.
Sharp ring dibuja solo el outline de blob[0] como stroke, sin blur.

**DrawPath count:** Full: 7 | Low: 4

**Problemas:**

1. **Centro lleno.** El blob filled cubre toda la región interior. Al blurearlo, la masa central se
   mantiene — el blur solo difumina los bordes. Resultado: se ve un disco con bordes difusos, no un
   anillo hueco.

2. **Solo se ve un blob.** El sharp canvas dibuja únicamente blob[0]. Las capas 1 y 2 solo contribuyen
   al halo blureado, donde su outline se difumina. La firma visual del prototipo (3 rings desfasados
   que se intersectan y crean el "shimmer" orgánico) no aparece.

**Conclusión:** los halos necesitan strokes, no fills. Y los 3 phase offsets deben dibujarse en todos
los canvases.

---

## V4 — Modifier.blur + strokes en todos los canvases + 3 phase offsets

**Archivo:** `experiments/VoiceVisualizerRingV4.kt`

**Hipótesis:** cambiar los fills por strokes en los canvases de halo resuelve el centro lleno.
Dibujar los 3 blobs en todos los canvases muestra el solape orgánico del prototipo.

**Approach:**
- Todos los canvases (far halo, near halo, sharp) dibujan los 3 blobs como STROKE
- Stroke widths escalonados: far = `thickness×2.5`, near = `thickness×1.6`, sharp = `thickness`
- Blur GPU-accelerated (Modifier.blur) en far y near
- Normalización CSS de radios heredada de V3

**DrawPath count:** Full: 9 | Low: 6

**Problema:** `Modifier.blur` crea un `RenderEffect` (hardware layer) en Android que cachea el
contenido del Canvas. Los state reads dentro del draw block no invalidan ese layer → los halos
blureados quedan congelados en el primer frame. Solo el sharp ring (sin blur) animaba.

Intentos de fix fallidos:
- Mover state reads al cuerpo del composable (fuera del draw block): el draw block quedó sin reads
  propios → nunca se re-ejecuta solo.
- Leer estado en el cuerpo Y en el draw block: fuerza recomposición a 60 fps del composable entero
  → 1000+ frames skipped, app inutilizable.

Alternativas descartadas:
- `drawIntoCanvas + BlurMaskFilter`: funciona en Desktop (Skia puro), silenciosamente ignorado en
  Android hardware canvas para `drawPath`.
- `graphicsLayer { renderEffect = BlurEffect(...) }` (API 31+): misma raíz que `Modifier.blur`,
  mismo problema de caching. Además requiere expect/actual para Desktop.
- Render en offscreen bitmap + blur bitmap + drawBitmap: correcto pero requiere alocar bitmap(s)
  por frame y blur software; overkill para este efecto.

**Conclusión:** `Modifier.blur` no es viable para contenido que anima a 60 fps en Android.

---

## V5 — Fake-blur por multi-pass strokes en Canvas único

**Archivo:** `experiments/VoiceVisualizerRingV5.kt`

**Hipótesis:** sin ningún blur de sistema, aproximar el falloff gaussiano dibujando cada blob N veces
con stroke width decreciente y alpha creciente, todo en un Canvas único sin hardware layers.

**Approach:**
- Un solo Canvas, sin `Modifier.blur`, sin caching
- Cada blob se dibuja N veces (painter's algorithm, back-to-front): más ancho + más transparente
  primero, más fino + más opaco al final
- Far halo: 4 pasadas (extraWidth 40dp→0dp, alpha 0.05→0.45)
- Near halo: 3 pasadas (extraWidth 15dp→0dp, alpha 0.15→0.65)
- Sharp ring: 1 pasada (alpha 0.85)

**DrawPath count:** Full: 24 | Low: 12

**Resultado:** funciona correctamente en todos los targets sin congelamiento. El glow no es un
gaussiano perfecto pero es visualmente aceptable. Se archivó como alternativa portable: no depende
de ninguna librería externa (cero deps fuera de `androidx.compose.*`).

---

## V6 — Cloudy en lugar de Modifier.blur

**Archivo:** `VoiceVisualizerRingV6Gml51Cloudy.kt`

**Hipótesis:** `skydoves/Cloudy` usa `RenderEffect` en Android 31+ y un fallback C++ nativo
(NEON/SIMD) en versiones anteriores, más Skia Metal (iOS) y Skia GPU (Desktop/WASM). Resuelve la
cobertura de plataformas que limitaba `Modifier.blur`.

**Problema de Cloudy:** captura el bitmap del Canvas y lo cachea. Si el contenido anima a 60 fps,
Cloudy se congela en el primer frame porque no detecta que el Canvas interno cambió.

**Solución — key-trick:** usar `key(blurTick)` para forzar la recreación completa del subtree con
Cloudy a ~15 fps (cada 66 ms). Cuando el valor del key cambia, Compose descarta el subtree viejo y
crea uno nuevo desde cero, incluyendo una instancia fresca de Cloudy que captura el estado actual
del Canvas. El sharp ring (sin Cloudy) continúa actualizando a 60 fps completos.

**Estructura:** 3 Canvas apilados en Box. Far halo y near halo envueltos en `key(tick)` con
`.cloudy(radius)`. Sharp ring sin `key()`.

**DrawPath count:** Full: 9 | Low: 6

---

## V7 — Keyframes circulares con protuberancia localizada

**Archivo:** `experiments/VoiceVisualizerRingV7.kt`
**Estado:** archivado — experimento de estética de keyframes, no era la dirección principal.

**Fork de V6.** Tres cambios explorados:

1. **Keyframes circulares:** en lugar de variar los 8 radios globalmente (rango 45-60), cada
   keyframe mantiene la mayoría de valores en ~50% (forma casi circular) y eleva dos esquinas
   opuestas a 65%. Los pares opuestos suman exactamente 100 (si uno es 65, el de la misma arista
   es 35) → sin normalización necesaria. Resultado: anillo casi circular con una "llamarada"
   localizada que rota entre keyframes.

2. **`blurRadius` parametrizado:** el caller controla el radio de blur; far halo escala a
   `blurRadius × 2.5f`.

3. **`relativeMotion`:** cuando true, cada blob usa un `CYCLE_MS` distinto (8000 / 8720 / 9440 ms).
   El desfasaje varía en el tiempo → movimiento relativo real, los anillos se "abren" y "cierran"
   entre sí. Este parámetro se incorporó a V8.

**Conclusión:** los keyframes de llamarada localizada no fueron la dirección visual elegida.
V6 se mantuvo como base canónica.

---

## V8 — Smoothing de picos + brillo dinámico

**Archivo:** `VoiceVisualizerRingV8.kt`

**Fork de V6Gml51Cloudy.** Dos mejoras que dan movimiento orgánico y glow reactivo.

**Filtro paso-bajo en dos etapas:**

- Stage 1 (pre-smooth): el volumen crudo nunca llega directo a los targets. Pasa primero por un
  exponential moving average: `preSmoothed = preSmoothed × 0.85 + v × 0.15`. Suaviza los picos
  bruscos — la apertura y el cierre del ring tienen la misma inercia.
- Stage 2 (lerp visual): lerp de `currentScale`/`currentBright` hacia sus targets con factor 0.15,
  heredado de V6. Ahora actúa sobre la señal ya suavizada por Stage 1.

Resultado: movimiento líquido y orgánico, equivalente al prototipo HTML.

**Brillo dinámico:** floor de `targetBright` bajado de 0.4f a 0.05f → en silencio el glow se
desvanece casi por completo pero deja un brillo residual sutil. `currentBright` inicializado en
0.05f en consonancia. El fade-out del glow también es suave porque usa la señal de Stage 1.

---

## V9 — inputSmoothing y responsiveness como parámetros (activa)

**Archivo:** `VoiceVisualizerRingV9.kt`

**Fork de V8.** Las constantes de suavizado dejan de ser hardcodeadas y se exponen como parámetros
del composable, permitiendo ajuste en caliente vía sliders en el DebugPanel:

- `inputSmoothing: Float = 0.85f` — decay del filtro paso-bajo (Stage 1). 0.0 = sin memoria
  (señal cruda). 0.95 = muy inerte (responde lento). `PRE_SMOOTH_NEW` se deriva como
  `1 - inputSmoothing`.
- `responsiveness: Float = 0.15f` — factor de lerp visual (Stage 2). 0.01 = movimiento muy
  suave. 1.0 = snap instantáneo al target.

Ambos se leen via `rememberUpdatedState` → cambios en caliente tienen efecto inmediato sin
reiniciar la animación.

El resto (keyframes, relativeMotion, Cloudy key-trick, layerFalloff, glow dinámico, floor 0.05f)
es idéntico a V8.
