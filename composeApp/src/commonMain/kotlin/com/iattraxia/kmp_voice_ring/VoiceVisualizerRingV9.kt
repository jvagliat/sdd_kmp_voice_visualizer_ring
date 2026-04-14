package com.iattraxia.kmp_voice_ring

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.cloudy

// =============================================================================
//  VoiceVisualizerRingV9 — implementación del componente audio-reactivo.
//
//  ── 1. ARQUITECTURA Y ESTRATEGIA ─────────────────────────────────────────────
//
//  El efecto visual consiste en un anillo orgánico con glow difuso que responde
//  al volumen de audio. El anillo está formado por 3 blobs (formas Bézier con
//  radio variable en cada esquina) que se mueven con phase offsets entre sí:
//  cuando se superponen crean un "shimmer" fluido y orgánico.
//
//  El glow se logra blureando el contorno del blob. El problema central es que
//  todas las APIs de blur de la plataforma (Modifier.blur en Compose, RenderEffect
//  en Android, ImageFilter en Skia) capturan el contenido en un bitmap y lo
//  cachean. Si el Canvas dentro del blur anima a 60 fps, el blur se "congela"
//  porque el sistema no sabe que el contenido interno cambió.
//
//  Solución adoptada — Cloudy + key-trick:
//  Cloudy (skydoves/Cloudy) aplica el blur capturando un bitmap del Canvas.
//  Para que capture el estado actualizado, se destruye y recrea el composable
//  con Cloudy usando key(tick): cuando `tick` cambia, Compose descarta el
//  subtree viejo y crea uno nuevo que captura el estado actual.
//  El tick sube cada ~66 ms → los halos blureados actualizan a ~15 fps.
//  El contorno nítido (sin Cloudy) no necesita key() y anima a 60 fps completos.
//  El ojo no distingue el throttle del halo porque el blur lo hace inherentemente
//  difuso — la posición exacta del halo a 15 fps es imperceptible.
//
//  ── 2. PARÁMETROS Y EFECTOS VISUALES ─────────────────────────────────────────
//
//  El contrato completo (qué hace cada parámetro, rangos típicos) está en el
//  header de VoiceVisualizerRing.kt. Resumen de los efectos visuales:
//
//  volume          → escala del ring + intensidad del glow. Es la señal de
//                    entrada; el resto de parámetros controlan cómo se procesa.
//
//  inputSmoothing  → suaviza el volumen crudo antes de usarlo (Stage 1).
//                    Valores altos (~0.85) dan movimiento orgánico; valores
//                    bajos (~0.0) hacen que el ring reaccione de forma nerviosa
//                    a cada pico del audio.
//
//  responsiveness  → velocidad del lerp visual (Stage 2). Controla qué tan
//                    rápido el ring alcanza su forma/brillo objetivo.
//                    Con inputSmoothing alto y responsiveness bajo se obtiene
//                    el movimiento más fluido y orgánico.
//
//  thickness       → grosor del trazo. Los halos lo multiplican (×1.6 y ×2.5)
//                    para que el glow tenga masa visual suficiente.
//
//  glowSpread / blurRadius → tamaño del halo. glowSpread escala ambos radios
//                    proporcionalmente; blurRadius afina el halo cercano.
//
//  layerFalloff    → diferencia de brillo entre los 3 blobs superpuestos.
//                    0.0 = todos igual de brillantes (más denso).
//                    0.2 = el blob de atrás se ve más tenue (más profundidad).
//
//  relativeMotion  → cuando true, cada blob tiene una velocidad de ciclo
//                    ligeramente distinta. Los blobs se "abren" y "cierran"
//                    entre sí con el tiempo → movimiento más vivo y aleatorio.
//
//  lowPerformanceMode → omite el halo lejano (el Canvas más costoso) y ajusta
//                    el radio del halo cercano para dispositivos limitados.
//
//  ── 3. ORGANIZACIÓN DEL CÓDIGO ───────────────────────────────────────────────
//
//  El código se divide en tres piezas con responsabilidades separadas:
//
//  VoiceVisualizerRingV9  (composable público)
//    Orquestador. Mantiene el estado de animación y lanza el loop de frames.
//    Compone el layout: un Box con 3 instancias de BlobsCanvasV9 apiladas.
//
//  BlobsCanvasV9  (composable privado)
//    Un Canvas con su configuración particular de blur y stroke. Se instancia
//    3 veces (far halo, near halo, sharp ring) con distintos parámetros.
//    Existe como composable separado porque key() necesita envolver un subtree
//    completo para poder destruirlo y recrearlo — si los 3 canvases estuvieran
//    inline en el Box, no se podría aplicar key() a cada uno individualmente.
//
//  buildBlobFrameV9  (función pura)
//    Calcula el Path Bézier de un blob para un instante dado del ciclo de
//    animación. No tiene estado, no conoce Compose. Se puede leer, testear y
//    modificar de forma completamente independiente del resto del componente.
//
// =============================================================================

// ── Ciclo y capas ─────────────────────────────────────────────────────────────
// CYCLE_MS_V9: duración del ciclo de animación de un blob en modo lockstep.
// LAYER_COUNT_V9: número de blobs superpuestos.
// CYCLE_DURATIONS_V9: ciclos por blob en relativeMotion — el 9% y 18% de
//   diferencia hacen que los blobs se desincronicen y resincroniicen lentamente.
private const val CYCLE_MS_V9 = 8000L
private const val LAYER_COUNT_V9 = 3
private val CYCLE_DURATIONS_V9 = longArrayOf(
    CYCLE_MS_V9,
    (CYCLE_MS_V9 * 1.09).toLong(),
    (CYCLE_MS_V9 * 1.18).toLong(),
)

// ── Geometría del blob ────────────────────────────────────────────────────────
// BEZIER_K_V9: constante de Bézier para aproximar un cuarto de círculo
//   con curvas cúbicas (= 4/3 × tan(π/8) ≈ 0.5523). Los handles de control
//   se calculan como radio × k para que la curva sea lo más circular posible.
// BASE_FRACTION_V9: fracción del lado mínimo del componente que ocupa el blob.
//   0.55 → el blob cubre el 55% del espacio disponible, dejando margen para
//   que el glow no quede recortado en los bordes.
// SILENCE_EPSILON_V9: umbral por debajo del cual se considera silencio y el
//   reloj de animación se pausa (el ring no avanza, queda congelado).
private const val BEZIER_K_V9 = 0.5522847498f
private const val BASE_FRACTION_V9 = 0.55f
private const val SILENCE_EPSILON_V9 = 0.005f

// ── Animación de forma: easing, phases y keyframes ───────────────────────────
// EASING_V9: curva ease-in-out para la interpolación entre keyframes.
//   Los valores (0.42, 0, 0.58, 1) corresponden al preset "ease-in-out" de CSS.
// PHASE_OFFSETS_V9: desfasaje inicial de cada blob en el ciclo [0..1].
//   Con 3 blobs a 0%, 31.25% y 62.5% del ciclo, los blobs están repartidos
//   de forma aproximadamente uniforme → siempre hay variedad de formas visible.
// KEYFRAME_RADII_V9: 3 keyframes × 8 radios por keyframe, en porcentaje del
//   lado del blob. Los 8 radios corresponden a las 4 esquinas de la forma,
//   en el orden [top-left-H, top-right-H, bottom-right-H, bottom-left-H,
//   top-left-V, top-right-V, bottom-right-V, bottom-left-V].
//   "H" = extensión horizontal del radio de esa esquina; "V" = extensión vertical.
// KEYFRAME_ROTATIONS_V9: rotación del canvas para cada keyframe. Los 120°
//   entre keyframes hacen que el blob dé una vuelta completa por ciclo.
private val EASING_V9 = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val PHASE_OFFSETS_V9 = floatArrayOf(0f, 0.3125f, 0.625f)
private val KEYFRAME_RADII_V9 = arrayOf(
    floatArrayOf(50f, 45f, 60f, 45f, 45f, 60f, 45f, 60f),
    floatArrayOf(45f, 60f, 45f, 55f, 60f, 45f, 60f, 45f),
    floatArrayOf(60f, 45f, 55f, 60f, 45f, 60f, 45f, 55f),
)
private val KEYFRAME_ROTATIONS_V9 = floatArrayOf(0f, 120f, 240f)

// ── Blur / Cloudy ─────────────────────────────────────────────────────────────
// BLUR_UPDATE_INTERVAL_MS_V9: cada cuántos ms se incrementa blurKey.
//   66 ms ≈ 15 fps para los halos blureados. Más frecuente aumenta la carga
//   GPU de Cloudy; menos frecuente hace el halo visiblemente "saltarín".
private const val BLUR_UPDATE_INTERVAL_MS_V9 = 66L

@Composable
fun VoiceVisualizerRingV9(
    volume: Float,
    color: Color,
    intensity: Float = 1f,
    thickness: Float = 5f,
    glowSpread: Float = 1f,
    blurRadius: Float = 15f,
    relativeMotion: Boolean = false,
    layerFalloff: Float = 0.2f,
    inputSmoothing: Float = 0.85f,
    responsiveness: Float = 0.15f,
    lowPerformanceMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // rememberUpdatedState permite que el LaunchedEffect (que vive mientras
    // el composable existe) lea siempre el valor más reciente de cada parámetro
    // sin necesidad de reiniciarse cuando el caller recompone con nuevos valores.
    val volumeState = rememberUpdatedState(volume)
    val intensityState = rememberUpdatedState(intensity)
    val inputSmoothingState = rememberUpdatedState(inputSmoothing)
    val responsivenessState = rememberUpdatedState(responsiveness)

    // Estado de animación. MutableFloatState / MutableLongState son observables
    // por Compose: cuando cambian, solo se re-ejecuta el draw block del Canvas
    // que los lee, sin recomponer el árbol completo.
    val currentScale = remember { mutableFloatStateOf(1f) }
    val currentBright = remember { mutableFloatStateOf(0.05f) } // inicia en el floor del glow
    val elapsedMs = remember { mutableLongStateOf(0L) }         // tiempo de animación acumulado
    val blurKey = remember { mutableIntStateOf(0) }             // tick que controla el key-trick

    // Acumulador del filtro paso-bajo (Stage 1). Persiste entre frames.
    val preSmoothed = remember { mutableFloatStateOf(0f) }

    // paths y scratch se reutilizan frame a frame para evitar allocations.
    // Los Canvas se ejecutan en orden de declaración (Box es secuencial), así
    // que los 3 comparten estos arrays sin race conditions.
    val paths = remember { Array(LAYER_COUNT_V9) { Path() } }
    val scratch = remember { FloatArray(8) }

    val density = LocalDensity.current

    // ── Loop de animación ─────────────────────────────────────────────────────
    // LaunchedEffect(Unit) corre en el ciclo de frames de Compose (Choreographer
    // en Android, rAF en WASM, vsync en Desktop). Se ejecuta una vez y vive
    // hasta que el composable sale de la composición.
    LaunchedEffect(Unit) {
        var lastFrameMs = -1L
        var animMs = 0L
        var lastBlurUpdateMs = 0L
        while (true) {
            withFrameMillis { nowMs ->
                val dt = if (lastFrameMs < 0L) 0L else nowMs - lastFrameMs
                lastFrameMs = nowMs

                val v = volumeState.value.coerceIn(0f, 1f)

                // El reloj de animación solo avanza cuando hay audio.
                // En silencio el ring queda congelado en su forma actual.
                if (v > SILENCE_EPSILON_V9) {
                    animMs += dt
                }
                elapsedMs.longValue = animMs

                // Stage 1 — filtro paso-bajo exponencial sobre el volumen crudo.
                // Suaviza los picos de la señal antes de usarla. El decay es
                // el peso del valor anterior (cuánto "recuerda" el filtro).
                // (1 - decay) es el peso del dato nuevo (cuánto responde al audio).
                val decay = inputSmoothingState.value.coerceIn(0f, 0.999f)
                preSmoothed.floatValue = preSmoothed.floatValue * decay + v * (1f - decay)

                val smoothedV = preSmoothed.floatValue
                // Convertir a rango 0..255 para usar las mismas fórmulas de
                // escala y brillo que el prototipo HTML original.
                val avg = smoothedV * 255f

                // Escala: en silencio = 1.0 (tamaño base). En pico = ~1.51 con
                // intensity=1. avg/500 normaliza el rango 0..255 a 0..0.51.
                val targetScale = 1f + (avg / 500f) * intensityState.value
                // Brillo: floor de 0.05 → el glow no desaparece del todo en
                // silencio. En pico puede superar 1.0 (se clampea al dibujar).
                val targetBright = 0.05f + (avg / 150f) * intensityState.value

                // Stage 2 — lerp visual. Mueve el valor actual hacia el target
                // en cada frame. Con responsiveness bajo (~0.15) el movimiento
                // es suave; con 1.0 sería un snap instantáneo.
                val lerp = responsivenessState.value.coerceIn(0.001f, 1f)
                currentScale.floatValue += (targetScale - currentScale.floatValue) * lerp
                currentBright.floatValue += (targetBright - currentBright.floatValue) * lerp

                // Incrementar blurKey a intervalos controlados.
                // Cada cambio fuerza la recreación de los canvases blureados vía key().
                if (nowMs - lastBlurUpdateMs >= BLUR_UPDATE_INTERVAL_MS_V9) {
                    blurKey.intValue++
                    lastBlurUpdateMs = nowMs
                }
            }
        }
    }

    // ── Composición visual: 3 canvases apilados ───────────────────────────────
    Box(modifier = modifier) {
        // Leer blurKey aquí suscribe este scope de composición al estado.
        // Cuando blurKey cambia, el Box recompone y los key() internos detectan
        // el cambio → destruyen y recrean los subtrees de Cloudy.
        val tick = blurKey.intValue

        // Halo lejano: stroke ancho + blur amplio. Omitido en lowPerformanceMode.
        // alpha 0.45 y stroke ×2.5 le dan masa al halo exterior sin saturar el color.
        if (!lowPerformanceMode) {
            val farRadius = with(density) { (blurRadius * 2.5f * glowSpread).dp.roundToPx() }
            key(tick) {
                BlobsCanvasV9(
                    modifier = Modifier.fillMaxSize().cloudy(radius = farRadius),
                    color = color,
                    alphaMul = 0.45f,
                    strokeWidthDp = thickness * 2.5f,
                    paths = paths,
                    scratch = scratch,
                    currentScale = currentScale,
                    currentBright = currentBright,
                    elapsedMs = elapsedMs,
                    relativeMotion = relativeMotion,
                    layerFalloff = layerFalloff,
                )
            }
        }

        // Halo cercano: stroke moderado + blur moderado. Siempre presente.
        // En lowPerformanceMode hereda un radio 1.5× para compensar la ausencia
        // del halo lejano y mantener algo de profundidad en el glow.
        val nearRadius = with(density) {
            val base = if (lowPerformanceMode) blurRadius * 1.5f else blurRadius
            (base * glowSpread).dp.roundToPx()
        }
        key(tick) {
            BlobsCanvasV9(
                modifier = Modifier.fillMaxSize().cloudy(radius = nearRadius),
                color = color,
                alphaMul = 0.65f,
                strokeWidthDp = thickness * 1.6f,
                paths = paths,
                scratch = scratch,
                currentScale = currentScale,
                currentBright = currentBright,
                elapsedMs = elapsedMs,
                relativeMotion = relativeMotion,
                layerFalloff = layerFalloff,
            )
        }

        // Ring nítido: stroke base sin blur. Actualiza a 60 fps completos.
        // Sin key() porque no tiene Cloudy — se invalida por los state reads
        // normales de elapsedMs dentro del draw block.
        BlobsCanvasV9(
            modifier = Modifier.fillMaxSize(),
            color = color,
            alphaMul = 0.85f,
            strokeWidthDp = thickness,
            paths = paths,
            scratch = scratch,
            currentScale = currentScale,
            currentBright = currentBright,
            elapsedMs = elapsedMs,
            relativeMotion = relativeMotion,
            layerFalloff = layerFalloff,
        )
    }
}

// Dibuja los 3 blobs en un Canvas con la configuración de stroke y blur recibida.
// Se instancia 3 veces desde VoiceVisualizerRingV9 con distintos alphaMul y
// strokeWidthDp para obtener el efecto de capas (far halo / near halo / sharp).
@Composable
private fun BlobsCanvasV9(
    modifier: Modifier,
    color: Color,
    alphaMul: Float,        // alpha base de esta capa [0..1]
    strokeWidthDp: Float,   // grosor del trazo en dp
    paths: Array<Path>,
    scratch: FloatArray,
    currentScale: MutableFloatState,
    currentBright: MutableFloatState,
    elapsedMs: MutableLongState,
    relativeMotion: Boolean,
    layerFalloff: Float,
) {
    // Leer elapsedMs fuera del bloque Canvas (en el cuerpo del composable)
    // suscribe este composable al estado. Cuando elapsedMs cambia, Compose
    // re-ejecuta el draw block del Canvas en el siguiente frame.
    // Sin esta lectura el Canvas nunca se redibujaría (ningún state read propio).
    @Suppress("UNUSED_EXPRESSION")
    elapsedMs.longValue

    Canvas(modifier) {
        if (size.minDimension <= 0f) return@Canvas

        // Snapshot del estado en este frame. Leer aquí (dentro del draw block)
        // también suscribe el draw a los estados, lo que garantiza invalidación
        // correcta si alguno cambia entre recomposiciones.
        val s = currentScale.floatValue
        val b = currentBright.floatValue.coerceIn(0f, 1f)
        val ms = elapsedMs.longValue
        val strokePx = strokeWidthDp.dp.toPx()

        // El blob se dibuja dentro de un cuadrado de lado `base` centrado en
        // el Canvas. BASE_FRACTION_V9 (0.55) deja margen para que el glow
        // no quede recortado en los bordes del componente.
        val base = size.minDimension * BASE_FRACTION_V9
        val dx = (size.width - base) / 2f
        val dy = (size.height - base) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        // scale() aplica la respuesta de volumen: en pico el ring crece.
        // El pivot es el centro del Canvas para que la expansión sea uniforme.
        scale(scaleX = s, scaleY = s, pivot = Offset(cx, cy)) {
            // Dibujar de atrás hacia adelante (layer 2 → 0) para que el blob
            // del frente quede encima de los otros.
            for (layer in (LAYER_COUNT_V9 - 1) downTo 0) {
                // En relativeMotion cada blob tiene su propio período; en modo
                // normal todos comparten CYCLE_MS_V9 con offsets fijos.
                val cycleMs = if (relativeMotion) CYCLE_DURATIONS_V9[layer] else CYCLE_MS_V9
                val rawProgress = (ms % cycleMs).toFloat() / cycleMs.toFloat()
                // Aplicar el phase offset de la capa para desfasar los blobs.
                val layerProgress = (rawProgress + PHASE_OFFSETS_V9[layer]) % 1f

                // buildBlobFrameV9 escribe el path del blob en paths[layer]
                // y devuelve la rotación que debe aplicarse al dibujarlo.
                val rotation = buildBlobFrameV9(
                    path = paths[layer],
                    scratch = scratch,
                    progress = layerProgress,
                    base = base,
                )

                // Cada capa se atenúa según su índice y layerFalloff.
                // layer 0 (frente) = brillo completo; layer 2 (fondo) = más tenue.
                val layerBright = (b * (1f - layer * layerFalloff)).coerceIn(0f, 1f)
                val pathColor = color.copy(alpha = alphaMul * layerBright)

                translate(left = dx, top = dy) {
                    rotate(degrees = rotation, pivot = Offset(base / 2f, base / 2f)) {
                        drawPath(
                            path = paths[layer],
                            color = pathColor,
                            style = Stroke(width = strokePx),
                        )
                    }
                }
            }
        }
    }
}

// Calcula el Path Bézier del blob y la rotación del canvas para un instante
// del ciclo de animación dado por `progress` [0..1].
// El path se escribe en `path` (reutilizado) y la función retorna los grados
// de rotación que el caller debe aplicar antes de dibujar.
private fun buildBlobFrameV9(
    path: Path,
    scratch: FloatArray,  // buffer temporal de 8 floats para los radios interpolados
    progress: Float,      // posición en el ciclo [0..1]
    base: Float,          // lado del cuadrado de referencia en px
): Float {
    // El ciclo se divide en 3 segmentos iguales (0..1/3, 1/3..2/3, 2/3..1).
    // segIdx identifica en cuál de los 3 segmentos cae progress.
    // localT es la posición dentro del segmento [0..1].
    val segFloat = progress * 3f
    val segIdx = segFloat.toInt().coerceIn(0, 2)
    val localT = (segFloat - segIdx).coerceIn(0f, 1f)
    // Aplicar easing al t local para que la transición entre keyframes
    // desacelere al inicio y al final (más natural que una interpolación lineal).
    val eased = EASING_V9.transform(localT)

    // Interpolar los 8 radios entre el keyframe actual y el siguiente.
    // El segmento 2 hace wrap: su keyframe destino es el keyframe 0 (cierre del ciclo).
    val fromR = KEYFRAME_RADII_V9[segIdx]
    val toR = KEYFRAME_RADII_V9[(segIdx + 1) % 3]
    for (i in 0 until 8) {
        scratch[i] = fromR[i] + (toR[i] - fromR[i]) * eased
    }

    // Interpolar también la rotación del canvas entre keyframes.
    // El segmento 2 rota de 240° a 360° (= una vuelta completa por ciclo).
    val fromRot = KEYFRAME_ROTATIONS_V9[segIdx]
    val toRot = if (segIdx == 2) 360f else KEYFRAME_ROTATIONS_V9[segIdx + 1]
    val rotation = fromRot + (toRot - fromRot) * eased

    // Convertir radios de porcentaje a píxeles.
    // h1..h4: extensión horizontal de cada esquina (top-left, top-right,
    //         bottom-right, bottom-left).
    // v1..v4: extensión vertical de las mismas esquinas (mismo orden).
    val w = base
    val h = base
    val h1 = scratch[0] * w / 100f
    val h2 = scratch[1] * w / 100f
    val h3 = scratch[2] * w / 100f
    val h4 = scratch[3] * w / 100f
    val v1 = scratch[4] * h / 100f
    val v2 = scratch[5] * h / 100f
    val v3 = scratch[6] * h / 100f
    val v4 = scratch[7] * h / 100f

    // Normalización al estilo CSS border-radius.
    // Si la suma de los radios de dos esquinas adyacentes en el mismo lado
    // supera el largo de ese lado, los radios se reducen proporcionalmente.
    // Sin esta normalización el path tendría intersecciones propias que Skia
    // renderiza como artefactos ("escalones") en el contorno.
    var factor = 1f
    val topSum = h1 + h2
    if (topSum > w) factor = minOf(factor, w / topSum)
    val bottomSum = h3 + h4
    if (bottomSum > w) factor = minOf(factor, w / bottomSum)
    val leftSum = v1 + v4
    if (leftSum > h) factor = minOf(factor, h / leftSum)
    val rightSum = v2 + v3
    if (rightSum > h) factor = minOf(factor, h / rightSum)

    val nh1 = h1 * factor
    val nh2 = h2 * factor
    val nh3 = h3 * factor
    val nh4 = h4 * factor
    val nv1 = v1 * factor
    val nv2 = v2 * factor
    val nv3 = v3 * factor
    val nv4 = v4 * factor
    val k = BEZIER_K_V9

    // Construir el path Bézier de la forma. El blob es un cuadrilátero con las
    // 4 esquinas redondeadas por curvas cúbicas (una curva por esquina).
    // Cada esquina tiene radio horizontal (nh) y vertical (nv) independientes,
    // lo que permite formas asimétricas y orgánicas.
    // Los handles de control se calculan como radio × BEZIER_K para aproximar
    // la tangente de un arco circular en esa esquina.
    //
    // Orden de trazado (en sentido horario desde esquina top-left):
    //   top-left → top-right (línea horizontal superior)
    //   esquina top-right (curva)
    //   top-right → bottom-right (línea vertical derecha)
    //   esquina bottom-right (curva)
    //   bottom-right → bottom-left (línea horizontal inferior)
    //   esquina bottom-left (curva)
    //   bottom-left → top-left (línea vertical izquierda)
    //   esquina top-left (curva, cierra el path)
    path.reset()
    path.moveTo(nh1, 0f)
    path.lineTo(w - nh2, 0f)
    path.cubicTo(w - nh2 + nh2 * k, 0f, w, nv2 - nv2 * k, w, nv2)
    path.lineTo(w, h - nv3)
    path.cubicTo(w, h - nv3 + nv3 * k, w - nh3 + nh3 * k, h, w - nh3, h)
    path.lineTo(nh4, h)
    path.cubicTo(nh4 - nh4 * k, h, 0f, h - nv4 + nv4 * k, 0f, h - nv4)
    path.lineTo(0f, nv1)
    path.cubicTo(0f, nv1 - nv1 * k, nh1 - nh1 * k, 0f, nh1, 0f)
    path.close()

    return rotation
}
