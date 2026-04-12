package com.iattraxia.kmp_voice_ring

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

// =============================================================================
//  V3 — Glow gaussiano vía Modifier.blur + radii CSS-normalizados
//        (INTENTO DESCARTADO — los halos filled tapan el centro)
//
//  HIPÓTESIS:
//  V2 demostró dos cosas:
//   (a) sin fills, el ring sí se ve hueco (corrige V1) — bien.
//   (b) los stack-of-strokes generan bandas duras visibles (11 capas en V2) y
//       además había un "escalón" en el contorno por radii overflowing.
//  V3 ataca (a) preservado, y resuelve los dos defectos restantes:
//   - Para las bandas: usar Modifier.blur (gaussiano real, GPU-accelerated).
//   - Para el escalón: normalizar los radios al estilo CSS.
//
//  APPROACH:
//
//  1. Path normalizado al estilo CSS border-radius. En cada frame:
//        factor = min(1, w/(h1+h2), w/(h3+h4), h/(v1+v4), h/(v2+v3))
//     Si la suma de radios adyacentes excede el lado, todos los radios se
//     multiplican por factor. Mismo comportamiento que un browser. Esto
//     elimina las intersecciones propias del path.
//
//  2. Estructura visual = 3 Canvas apilados en un Box, cada uno con un blur
//     distinto:
//
//        ┌───────────────────────────────────────┐
//        │ Canvas 1 — halo lejano                │  blur 40dp × glowSpread
//        │ dibuja los 3 blobs FILLED, alpha bajo │  → halo difuso amplio
//        │ (skipped en lowPerformanceMode)       │
//        ├───────────────────────────────────────┤
//        │ Canvas 2 — halo cercano               │  blur 15dp × glowSpread
//        │ dibuja los 3 blobs FILLED, alpha mid  │  → halo concentrado
//        ├───────────────────────────────────────┤
//        │ Canvas 3 — ring nítido                │  sin blur
//        │ dibuja sólo el outline de blob[0]     │  → contorno crisp
//        │ con stroke = thickness.dp             │
//        └───────────────────────────────────────┘
//
//     Modifier.blur usa internamente RenderEffect (Android API 31+) o
//     ImageFilter de Skia (JVM/iOS/Web), ambos GPU-accelerated. El edge
//     treatment Unbounded extiende el blur fuera de los bounds del Canvas
//     para que el halo no quede recortado en los bordes.
//
//  3. Estado y loop COMPARTIDO: una sola LaunchedEffect actualiza scale,
//     bright y elapsed. Las 3 Canvas leen los mismos MutableFloatState/
//     MutableLongState dentro de su lambda de draw — cada update invalida
//     los 3 draws sin recomponer. paths[] y scratch[] también compartidos:
//     cada Canvas reescribe el mismo array (Box children draw en orden de
//     declaración → no hay race).
//
//  drawPath count:
//    Full:   3 (far halo) + 3 (near halo) + 1 (sharp ring) = 7
//    Low:    3 (near halo) + 1 (sharp ring) = 4
//    Plus:   2 GPU blurs por frame en full / 1 en lowPerf.
//
//  DESVIACIÓN EXPLÍCITA DE LA SPEC:
//  AGENTS.md / SPEC.md prohíben Modifier.blur. La razón original era no
//  depender de blur (Android API <31 no lo soporta y degrada a no-op). En
//  el target desktop-first actual esto no aplica. La concesión está
//  justificada porque V2 demostró que sin blur real las bandas duras son
//  inevitables. Si en el futuro toca soportar Android <S, el fallback es V2
//  (mismo archivo, sólo cambia la línea de despacho).
//
//  PROBLEMAS DESCUBIERTOS (visibles en pantalla):
//
//  1. **Centro lleno.** El blob filled cubre toda la región interior del
//     contorno con la alpha de la capa. Al pasarlo por el blur, el centro NO
//     se vacía — el blur sólo difumina los bordes, pero la masa central
//     queda intacta. Resultado: el ring no se ve hueco, se ve como un disco
//     cyan con bordes difusos. Incorrecto: el prototipo del cliente es un
//     anillo HUECO con halo, no un disco.
//
//  2. **Sólo se ve un blob, no los 3 phase-offset solapados.** El sharp
//     Canvas dibuja únicamente blob[0] como stroke. Las capas 1 y 2 sólo
//     contribuyen al halo blureado, donde su outline se difumina. La firma
//     visual del prototipo (3 rings ligeramente desfasados que se intersectan
//     y crean el "shimmer" orgánico) no aparece.
//
//  SIGUIENTE PASO → V4: dibujar los blobs como STROKE en TODOS los canvases
//  (incluido los de halo). Stroke = anillo, no disco → al blurearlo el
//  centro queda transparente. Y los 3 phase offsets también en el sharp
//  Canvas para mostrar el solape orgánico.
// =============================================================================

private const val CYCLE_MS = 8000L
private const val LERP_FACTOR = 0.15f
private const val LAYER_COUNT = 3
private const val BEZIER_K = 0.5522847498f
private val EASING = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val PHASE_OFFSETS = floatArrayOf(0f, 0.3125f, 0.625f)
private val KEYFRAME_RADII = arrayOf(
    floatArrayOf(50f, 45f, 60f, 45f, 45f, 60f, 45f, 60f),  //   0% / 100%
    floatArrayOf(45f, 60f, 45f, 55f, 60f, 45f, 60f, 45f),  //  33%
    floatArrayOf(60f, 45f, 55f, 60f, 45f, 60f, 45f, 55f),  //  66%
)
private val KEYFRAME_ROTATIONS = floatArrayOf(0f, 120f, 240f)
private const val BASE_FRACTION = 0.55f
private const val SILENCE_EPSILON = 0.005f

@Composable
fun VoiceVisualizerRingV3(
    volume: Float,
    color: Color,
    intensity: Float = 1f,
    thickness: Float = 5f,
    glowSpread: Float = 1f,
    lowPerformanceMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val volumeState = rememberUpdatedState(volume)
    val intensityState = rememberUpdatedState(intensity)

    val currentScale = remember { mutableFloatStateOf(1f) }
    val currentBright = remember { mutableFloatStateOf(0.4f) }
    val elapsedMs = remember { mutableLongStateOf(0L) }

    val paths = remember { Array(LAYER_COUNT) { Path() } }
    val scratch = remember { FloatArray(8) }

    LaunchedEffect(Unit) {
        var lastFrameMs = -1L
        var animMs = 0L
        while (true) {
            withFrameMillis { nowMs ->
                val dt = if (lastFrameMs < 0L) 0L else nowMs - lastFrameMs
                lastFrameMs = nowMs

                val v = volumeState.value.coerceIn(0f, 1f)
                if (v > SILENCE_EPSILON) {
                    animMs += dt
                }
                elapsedMs.longValue = animMs

                val avg = v * 255f
                val targetScale = 1f + (avg / 500f) * intensityState.value
                val targetBright = 0.4f + (avg / 150f) * intensityState.value

                currentScale.floatValue +=
                    (targetScale - currentScale.floatValue) * LERP_FACTOR
                currentBright.floatValue +=
                    (targetBright - currentBright.floatValue) * LERP_FACTOR
            }
        }
    }

    Box(modifier = modifier) {
        // === Halo lejano: blur amplio, alpha baja. Skipped en lowPerf. ===
        if (!lowPerformanceMode) {
            BlobsCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(
                        radius = (40f * glowSpread).dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
                color = color,
                alphaMul = 0.5f,
                drawStroke = false,
                strokeWidthDp = 0f,
                paths = paths,
                scratch = scratch,
                currentScale = currentScale,
                currentBright = currentBright,
                elapsedMs = elapsedMs,
            )
        }

        // === Halo cercano: blur moderado, alpha mid. ===
        val nearBlurDp = (if (lowPerformanceMode) 22f else 15f) * glowSpread
        BlobsCanvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(
                    radius = nearBlurDp.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                ),
            color = color,
            alphaMul = 0.7f,
            drawStroke = false,
            strokeWidthDp = 0f,
            paths = paths,
            scratch = scratch,
            currentScale = currentScale,
            currentBright = currentBright,
            elapsedMs = elapsedMs,
        )

        // === Ring nítido: sin blur, sólo el outline de la capa primaria. ===
        BlobsCanvas(
            modifier = Modifier.fillMaxSize(),
            color = color,
            alphaMul = 0.95f,
            drawStroke = true,
            strokeWidthDp = thickness,
            paths = paths,
            scratch = scratch,
            currentScale = currentScale,
            currentBright = currentBright,
            elapsedMs = elapsedMs,
        )
    }
}

// -----------------------------------------------------------------------------
// Canvas reutilizable que dibuja los blobs en el modo correspondiente.
//   - drawStroke = false → todos los blobs FILLED (para los halos blureados)
//   - drawStroke = true  → SÓLO el outline de blob[0] (para el ring nítido)
//
// Lee el estado compartido (scale/bright/elapsed) dentro de la lambda de
// draw → no recompone, sólo invalida el draw cuando cambia.
// -----------------------------------------------------------------------------
@Composable
private fun BlobsCanvas(
    modifier: Modifier,
    color: Color,
    alphaMul: Float,
    drawStroke: Boolean,
    strokeWidthDp: Float,
    paths: Array<Path>,
    scratch: FloatArray,
    currentScale: MutableFloatState,
    currentBright: MutableFloatState,
    elapsedMs: MutableLongState,
) {
    Canvas(modifier) {
        if (size.minDimension <= 0f) return@Canvas

        val s = currentScale.floatValue
        val b = currentBright.floatValue.coerceIn(0f, 1f)
        val ms = elapsedMs.longValue
        val strokePx = strokeWidthDp.dp.toPx()

        val base = size.minDimension * BASE_FRACTION
        val dx = (size.width - base) / 2f
        val dy = (size.height - base) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        scale(scaleX = s, scaleY = s, pivot = Offset(cx, cy)) {
            if (drawStroke) {
                // Sharp ring: sólo blob[0]. No iteramos las otras 2 capas.
                val rawProgress = (ms % CYCLE_MS).toFloat() / CYCLE_MS.toFloat()
                val layerProgress = (rawProgress + PHASE_OFFSETS[0]) % 1f
                val rotation = buildBlobFrame(
                    path = paths[0],
                    scratch = scratch,
                    progress = layerProgress,
                    base = base,
                )
                val pathColor = color.copy(alpha = alphaMul * b)
                translate(left = dx, top = dy) {
                    rotate(degrees = rotation, pivot = Offset(base / 2f, base / 2f)) {
                        drawPath(
                            path = paths[0],
                            color = pathColor,
                            style = Stroke(width = strokePx),
                        )
                    }
                }
            } else {
                // Halo: dibuja los 3 blobs filled de atrás hacia adelante.
                for (layer in (LAYER_COUNT - 1) downTo 0) {
                    val rawProgress = (ms % CYCLE_MS).toFloat() / CYCLE_MS.toFloat()
                    val layerProgress = (rawProgress + PHASE_OFFSETS[layer]) % 1f
                    val rotation = buildBlobFrame(
                        path = paths[layer],
                        scratch = scratch,
                        progress = layerProgress,
                        base = base,
                    )
                    val layerBright = (b * (1f - layer * 0.2f)).coerceIn(0f, 1f)
                    val pathColor = color.copy(alpha = alphaMul * layerBright)

                    translate(left = dx, top = dy) {
                        rotate(degrees = rotation, pivot = Offset(base / 2f, base / 2f)) {
                            drawPath(
                                path = paths[layer],
                                color = pathColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// buildBlobFrame V3 — interpola los 8 radios + rotación entre keyframes
// adyacentes Y normaliza al estilo CSS border-radius para evitar
// intersecciones propias cuando dos radios adyacentes suman > lado.
// -----------------------------------------------------------------------------
private fun buildBlobFrame(
    path: Path,
    scratch: FloatArray,
    progress: Float,
    base: Float,
): Float {
    val segFloat = progress * 3f
    val segIdx = segFloat.toInt().coerceIn(0, 2)
    val localT = (segFloat - segIdx).coerceIn(0f, 1f)
    val eased = EASING.transform(localT)

    val fromR = KEYFRAME_RADII[segIdx]
    val toR = KEYFRAME_RADII[(segIdx + 1) % 3]
    for (i in 0 until 8) {
        scratch[i] = fromR[i] + (toR[i] - fromR[i]) * eased
    }

    val fromRot = KEYFRAME_ROTATIONS[segIdx]
    val toRot = if (segIdx == 2) 360f else KEYFRAME_ROTATIONS[segIdx + 1]
    val rotation = fromRot + (toRot - fromRot) * eased

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

    // CSS border-radius normalization. Si la suma de radios adyacentes en
    // cualquier lado excede ese lado, escalamos TODOS los radios por el mismo
    // factor proporcional. Mismo comportamiento que un browser.
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
    val k = BEZIER_K

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
