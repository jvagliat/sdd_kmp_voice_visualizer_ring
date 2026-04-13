package com.iattraxia.kmp_voice_ring

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

// =============================================================================
//  V1 — Stacks rellenos + glow por strokes apilados (INTENTO DESCARTADO)
//
//  HIPÓTESIS INICIAL:
//  Cada capa es un blob filled con halo alrededor. Las 3 capas con phase
//  offset crean una forma orgánica con halo multicapa. El outline más nítido
//  va arriba en la capa primaria. Esto es la lectura literal de la spec.
//
//  APPROACH:
//  - Outer glow: 3 strokes anchos (spread 70/40/15 dp × 2) sobre el contorno,
//    pintados ANTES del fill. La mitad interior de cada stroke queda tapada
//    por el fill que viene después → sólo se ve la mitad EXTERIOR (el halo).
//  - Fill: drawPath(path, color = fillColor) — el blob relleno con la alpha
//    de la capa.
//  - Inner glow: 3 strokes anchos pintados DESPUÉS del fill, envueltos en
//    clipPath(path) (default = Intersect) → sólo la mitad INTERIOR queda
//    visible.
//  - Outline final en capa 0: stroke fino con `thickness.dp.toPx()`.
//  - Capas 1 y 2 = 1 outer + fill + 1 inner. Sin outline propio.
//
//  drawPath count:
//    Capa 0 full: 3 outer + 1 fill + 3 inner + 1 outline = 8
//    Capas 1, 2:  1 outer + 1 fill + 1 inner             = 3 c/u
//    Total full = 14
//    Total lowPerf = 4 (capa 0) + 1 (capa 1) + 1 (capa 2) = 6
//
//  PROBLEMAS DESCUBIERTOS:
//
//  1. **Onion ring (rodajas concéntricas).** Las capas 1 y 2 con su fill son
//     discos opacos (con alpha 0.6 × brightness, pero opacos al ojo)
//     superpuestos al ring principal. En vez de un anillo HUECO con halo, se
//     ve un objeto SÓLIDO con anillos concéntricos visibles, como rodajas de
//     cebolla. El prototipo del cliente es un ring hueco con glow alrededor
//     — no quería las capas filled visibles como discos.
//     La intuición que falló: pensé que el alpha bajo iba a "disolver" las
//     capas internas en el halo, pero el ojo distingue claramente los bordes
//     duros donde termina cada blob filled.
//
//  2. **(Latente) Bug del "escalón".** Mismo bug que V2 (radii overflowing
//     sin normalización CSS), pero quedó tapado por el visual problem #1.
//     Recién fue diagnóstico explícito en V2 cuando los fills se removieron
//     y el ring hueco dejó visible la intersección propia del path.
//
//  SIGUIENTE PASO → V2: quitar todos los fills y dejar SÓLO strokes para que
//  se vea como un ring hueco. Spoiler: arregla el onion ring pero introduce
//  el problema de las 11 bandas duras visibles.
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
fun VoiceVisualizerRingV1(
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
    val scratchRadii = remember { FloatArray(8) }

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

    Canvas(modifier) {
        if (size.minDimension <= 0f) return@Canvas

        val s = currentScale.floatValue
        val b = currentBright.floatValue.coerceIn(0f, 1f)
        val ms = elapsedMs.longValue

        val thicknessPx = thickness.dp.toPx()
        val spread15 = 15.dp.toPx() * glowSpread
        val spread40 = 40.dp.toPx() * glowSpread
        val spread70 = 70.dp.toPx() * glowSpread
        val spread30 = 30.dp.toPx() * glowSpread
        val spread20 = 20.dp.toPx() * glowSpread

        val base = size.minDimension * BASE_FRACTION
        val dx = (size.width - base) / 2f
        val dy = (size.height - base) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        scale(scaleX = s, scaleY = s, pivot = Offset(cx, cy)) {
            for (layer in (LAYER_COUNT - 1) downTo 0) {
                val rawProgress = (ms % CYCLE_MS).toFloat() / CYCLE_MS.toFloat()
                val layerProgress = (rawProgress + PHASE_OFFSETS[layer]) % 1f

                val rotation = buildBlobFrame(
                    path = paths[layer],
                    scratch = scratchRadii,
                    progress = layerProgress,
                    base = base,
                )

                val layerBright = (b * (1f - layer * 0.2f)).coerceIn(0f, 1f)

                translate(left = dx, top = dy) {
                    rotate(degrees = rotation, pivot = Offset(base / 2f, base / 2f)) {
                        drawBlobLayer(
                            path = paths[layer],
                            layer = layer,
                            color = color,
                            brightness = layerBright,
                            thicknessPx = thicknessPx,
                            spread15 = spread15,
                            spread40 = spread40,
                            spread70 = spread70,
                            spread30 = spread30,
                            spread20 = spread20,
                            lowPerf = lowPerformanceMode,
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// buildBlobFrame V1 — sin normalización CSS de los radios overflowing.
// El bug latente del "escalón" vive acá; quedó tapado por el onion ring.
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
    val k = BEZIER_K

    path.reset()
    path.moveTo(h1, 0f)
    path.lineTo(w - h2, 0f)
    path.cubicTo(w - h2 + h2 * k, 0f, w, v2 - v2 * k, w, v2)
    path.lineTo(w, h - v3)
    path.cubicTo(w, h - v3 + v3 * k, w - h3 + h3 * k, h, w - h3, h)
    path.lineTo(h4, h)
    path.cubicTo(h4 - h4 * k, h, 0f, h - v4 + v4 * k, 0f, h - v4)
    path.lineTo(0f, v1)
    path.cubicTo(0f, v1 - v1 * k, h1 - h1 * k, 0f, h1, 0f)
    path.close()

    return rotation
}

// -----------------------------------------------------------------------------
// Drawing V1: outer strokes ANTES del fill (la mitad interior queda tapada),
// fill, inner strokes clipados al interior, outline final en capa 0.
// El fill de las capas 1 y 2 es lo que produce el efecto "onion ring".
// -----------------------------------------------------------------------------
private fun DrawScope.drawBlobLayer(
    path: Path,
    layer: Int,
    color: Color,
    brightness: Float,
    thicknessPx: Float,
    spread15: Float,
    spread40: Float,
    spread70: Float,
    spread30: Float,
    spread20: Float,
    lowPerf: Boolean,
) {
    val fillAlpha = brightness * if (layer == 0) 1f else 0.6f
    val fillColor = color.copy(alpha = fillAlpha)

    if (layer == 0) {
        if (lowPerf) {
            // Capa 0 lowPerf: 1 outer + fill + 1 inner clipped + outline = 4
            drawPath(
                path = path,
                color = color.copy(alpha = 0.5f * brightness),
                style = Stroke(width = spread40 * 2f),
            )
            drawPath(path = path, color = fillColor)
            clipPath(path) {
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.5f * brightness),
                    style = Stroke(width = spread40 * 2f),
                )
            }
            drawPath(
                path = path,
                color = color.copy(alpha = 0.8f * brightness),
                style = Stroke(width = thicknessPx),
            )
        } else {
            // Capa 0 full: 3 outer + fill + 3 inner clipped + outline = 8
            // Outer: el más ancho/transparente primero. Los más finos van encima
            // y sobreescriben la franja cercana al borde con alpha mayor.
            drawPath(
                path = path,
                color = color.copy(alpha = 0.2f * brightness),
                style = Stroke(width = spread70 * 2f),
            )
            drawPath(
                path = path,
                color = color.copy(alpha = 0.4f * brightness),
                style = Stroke(width = spread40 * 2f),
            )
            drawPath(
                path = path,
                color = color.copy(alpha = 0.7f * brightness),
                style = Stroke(width = spread15 * 2f),
            )
            drawPath(path = path, color = fillColor)
            clipPath(path) {
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.2f * brightness),
                    style = Stroke(width = spread70 * 2f),
                )
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.4f * brightness),
                    style = Stroke(width = spread40 * 2f),
                )
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.7f * brightness),
                    style = Stroke(width = spread15 * 2f),
                )
            }
            drawPath(
                path = path,
                color = color.copy(alpha = 0.8f * brightness),
                style = Stroke(width = thicknessPx),
            )
        }
    } else {
        // Capas 1 y 2 — sin outline propio
        val spread = if (layer == 1) spread30 else spread20
        val glowAlpha = if (layer == 1) 0.3f else 0.2f
        val haloColor = color.copy(alpha = glowAlpha * brightness)
        val strokeWidth = spread * 2f

        if (lowPerf) {
            // lowPerf: sólo el fill (causa onion ring incluso acá)
            drawPath(path = path, color = fillColor)
        } else {
            // 1 outer + fill + 1 inner clipped = 3
            drawPath(
                path = path,
                color = haloColor,
                style = Stroke(width = strokeWidth),
            )
            drawPath(path = path, color = fillColor)
            clipPath(path) {
                drawPath(
                    path = path,
                    color = haloColor,
                    style = Stroke(width = strokeWidth),
                )
            }
        }
    }
}
