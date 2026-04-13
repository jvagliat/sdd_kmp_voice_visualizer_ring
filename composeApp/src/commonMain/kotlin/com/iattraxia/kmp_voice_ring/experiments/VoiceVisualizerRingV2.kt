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
import androidx.compose.ui.graphics.ClipOp
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
//  V2 — Anillo hueco con clipPath de strokes (INTENTO DESCARTADO)
//
//  HIPÓTESIS:
//  V1 (stacks rellenos) mostraba "rodajas concéntricas" — onion ring — porque
//  cada capa pintaba un blob FILLED encima de la anterior. Si removemos los
//  fills y dejamos sólo strokes para el contorno + glow, obtenemos un anillo
//  hueco como en el prototipo del cliente.
//
//  APPROACH:
//  - Cada subcapa de glow = Stroke ancho (width = 2 × spread) sobre el contorno
//    del blob, recortado:
//      • Outer glow: clipPath(path, Difference) → sólo la mitad EXTERIOR del
//        stroke queda visible (el halo AFUERA del anillo).
//      • Inner glow: clipPath(path, Intersect)  → sólo la mitad INTERIOR
//        (el halo DENTRO del anillo).
//  - Outline nítido al final, sólo en la capa 0.
//
//  drawPath count:
//    Capa 0 full: 3 outer + 3 inner + 1 outline = 7
//    Capas 1, 2:  1 outer + 1 inner             = 2 c/u
//    Total full = 11   Total lowPerf = 7
//
//  PROBLEMAS DESCUBIERTOS (ambos visibles en pantalla):
//
//  1. **11 capas perfectamente diferenciadas.** Cada stroke clipped es una
//     franja de alpha CONSTANTE a lo ancho de su grosor — no hay falloff
//     gaussiano. El ojo distingue las 11 bandas como escalones discretos. La
//     única forma de aproximar un degradado gaussiano por strokes apilados es
//     subir masivamente el número de subcapas (8-15× → coste de draw
//     prohibitivo). Conclusión: stack-of-strokes no escala a "glow suave".
//
//  2. **"Escalón" a las 12 y a las 6 del ring.** Bug en KEYFRAME_RADII: en
//     varias filas la suma de radios adyacentes supera 100 (ej. fila 33% top:
//     45+60=105; fila 66% bottom: 55+60=115). El `border-radius` CSS
//     auto-normaliza estos casos con un factor proporcional; este
//     buildBlobFrame no lo hace. Resultado: `path.lineTo(w - h2, 0f)` queda
//     con coordenadas que retroceden cuando h1 + h2 > w, creando una
//     intersección propia que Skia renderiza como un escalón vertical en el
//     borde superior e inferior.
//
//  SIGUIENTE PASO → V3: (a) normalizar los radios al estilo CSS y (b) usar
//  Modifier.blur (la única forma de obtener glow gaussiano real).
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
fun VoiceVisualizerRingV2(
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
// Construye el Path del blob a partir de los 8 radios interpolados entre
// keyframes adyacentes. NO normaliza overflow — esa es una de las razones por
// las que V2 muestra el "escalón". V3 corrige esto.
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
// Dibuja una capa del ring hueco. Outer glow vía clipPath(Difference); inner
// glow vía clipPath(Intersect); outline final sólo en capa 0.
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
    if (layer == 0) {
        if (lowPerf) {
            clipPath(path, clipOp = ClipOp.Difference) {
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.5f * brightness),
                    style = Stroke(width = spread40 * 2f),
                )
            }
            clipPath(path, clipOp = ClipOp.Intersect) {
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
            clipPath(path, clipOp = ClipOp.Difference) {
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
            clipPath(path, clipOp = ClipOp.Intersect) {
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
        val spread = if (layer == 1) spread30 else spread20
        val glowAlpha = if (layer == 1) 0.3f else 0.2f
        val haloColor = color.copy(alpha = glowAlpha * brightness)
        val strokeWidth = spread * 2f

        clipPath(path, clipOp = ClipOp.Difference) {
            drawPath(
                path = path,
                color = haloColor,
                style = Stroke(width = strokeWidth),
            )
        }
        clipPath(path, clipOp = ClipOp.Intersect) {
            drawPath(
                path = path,
                color = haloColor,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}
