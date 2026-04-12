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
//  V4 — Anillo hueco con strokes blureados + 3 phase offsets visibles
//
//  HIPÓTESIS:
//  V3 dibujaba los blobs FILLED en los canvases de halo. Como un fill cubre
//  toda la región interior, al blurearlo el blur preservaba la masa del
//  centro: el ring no se veía hueco, se veía como un disco con un borde
//  apenas más nítido. Además sólo el outline de blob[0] aparecía nítido, así
//  que el solape orgánico de las 3 capas con phase offset (visible en el
//  prototipo del cliente como rings desfasados que se intersectan) tampoco
//  estaba.
//
//  APPROACH:
//
//  1. Los 3 canvases dibujan los blobs como STROKE (anillo), no como fill.
//     Un stroke tiene grosor pero el interior queda transparente. Al blurear
//     un anillo se obtiene un halo a ambos lados del contorno SIN llenar el
//     centro — exactamente lo que queremos.
//
//  2. Los 3 blobs (con phase offset) se dibujan en TODOS los canvases. En el
//     canvas nítido los 3 contornos quedan visibles superpuestos, mostrando
//     el solape orgánico del prototipo. En los canvases blureados los 3
//     contornos se difuminan en un halo único pero conservando la firma de
//     las 3 morphs simultáneas.
//
//  3. Stroke widths escalonados:
//        - Far halo:   thickness × 2.5  → ancho difuso muy amplio
//        - Near halo:  thickness × 1.6  → halo intermedio
//        - Sharp:      thickness        → contorno crisp
//     El blur de cada capa amplía el ancho aparente vía gaussiano; el stroke
//     base más ancho en las capas blureadas le da más "mass" al halo.
//
//  4. Estado y loop COMPARTIDO con la misma estructura de V3 — sólo cambia
//     qué dibuja cada Canvas.
//
//  drawPath count:
//    Full: 3 (far halo) + 3 (near halo) + 3 (sharp ring) = 9
//    Low:  3 (near halo) + 3 (sharp ring) = 6
//    Plus: 2 GPU blurs por frame en full / 1 en lowPerf.
//
//  STATUS: ⏳ Pendiente verificación visual del usuario.
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
fun VoiceVisualizerRingV4(
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
        // === Halo lejano: stroke ancho + blur amplio ===
        if (!lowPerformanceMode) {
            BlobsCanvasStroked(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(
                        radius = (40f * glowSpread).dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
                color = color,
                alphaMul = 0.45f,
                strokeWidthDp = thickness * 2.5f,
                paths = paths,
                scratch = scratch,
                currentScale = currentScale,
                currentBright = currentBright,
                elapsedMs = elapsedMs,
            )
        }

        // === Halo cercano: stroke moderado + blur moderado ===
        val nearBlurDp = (if (lowPerformanceMode) 22f else 15f) * glowSpread
        BlobsCanvasStroked(
            modifier = Modifier
                .fillMaxSize()
                .blur(
                    radius = nearBlurDp.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                ),
            color = color,
            alphaMul = 0.65f,
            strokeWidthDp = thickness * 1.6f,
            paths = paths,
            scratch = scratch,
            currentScale = currentScale,
            currentBright = currentBright,
            elapsedMs = elapsedMs,
        )

        // === Ring nítido: 3 contornos superpuestos sin blur ===
        BlobsCanvasStroked(
            modifier = Modifier.fillMaxSize(),
            color = color,
            alphaMul = 0.85f,
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
// Dibuja los 3 blobs (con phase offset) como STROKE — anillo, no disco. El
// stroke no rellena el interior, así que el centro queda transparente y el
// blur sobre el stroke crea un halo a ambos lados del contorno sin masa
// central.
// -----------------------------------------------------------------------------
@Composable
private fun BlobsCanvasStroked(
    modifier: Modifier,
    color: Color,
    alphaMul: Float,
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
                            style = Stroke(width = strokePx),
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// buildBlobFrame V4 — idéntico a V3: interpola radios + rotación entre
// keyframes y normaliza al estilo CSS border-radius para evitar
// intersecciones propias por radii overflowing.
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
