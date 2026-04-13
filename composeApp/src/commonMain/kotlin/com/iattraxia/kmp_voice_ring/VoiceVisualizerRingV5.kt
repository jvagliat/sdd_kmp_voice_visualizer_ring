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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

// =============================================================================
//  V5 — Fake-blur via multi-pass strokes en un Canvas único
//
//  PROBLEMA DE V4:
//  Modifier.blur crea un RenderEffect (hardware layer) en Android. Los state
//  reads del draw block no invalidan ese layer cacheado → halos blureados
//  congelados. Solo el sharp ring (sin blur) animaba.
//
//  APPROACH:
//  Un Canvas único sin Modifier.blur. Cada blob se dibuja en múltiples
//  pasadas con stroke width decreciente y alpha creciente → falloff gaussiano
//  aproximado. Sin hardware layers → sin caching → el draw block se
//  re-ejecuta correctamente en cada frame.
//
//  Grupos de pasadas (painter's algorithm, back → front):
//    Far halo  (solo full mode): 4 pasadas, extraWidth 40dp→0dp, alpha 0.05→0.45
//    Near halo: 3 pasadas, extraWidth 15dp→0dp, alpha 0.15→0.65
//    Sharp ring: 1 pasada, extraWidth 0dp, alpha 0.85
//
//  drawPath count (3 blobs × pasadas):
//    Full:  (4 + 3 + 1) × 3 = 24  (vs 9 draws + 2 GPU blurs en V4)
//    Low:   (3 + 1) × 3 = 12
//
//  Alternativas descartadas: ver header de VoiceVisualizerRing.kt.
//
//  STATUS: pendiente verificación visual usuario (T20).
// =============================================================================

// Animation constants — mismos que V4
private const val V5_CYCLE_MS = 8000L
private const val V5_LERP_FACTOR = 0.15f
private const val V5_LAYER_COUNT = 3
private const val V5_BEZIER_K = 0.5522847498f
private val V5_EASING = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val V5_PHASE_OFFSETS = floatArrayOf(0f, 0.3125f, 0.625f)
private val V5_KEYFRAME_RADII = arrayOf(
    floatArrayOf(50f, 45f, 60f, 45f, 45f, 60f, 45f, 60f),  //   0% / 100%
    floatArrayOf(45f, 60f, 45f, 55f, 60f, 45f, 60f, 45f),  //  33%
    floatArrayOf(60f, 45f, 55f, 60f, 45f, 60f, 45f, 55f),  //  66%
)
private val V5_KEYFRAME_ROTATIONS = floatArrayOf(0f, 120f, 240f)
private const val V5_BASE_FRACTION = 0.55f
private const val V5_SILENCE_EPSILON = 0.005f

// Multi-pass stroke params.
// Índice 0 = pasada más estrecha y más opaca (frente).
// Índice N-1 = pasada más ancha y más transparente (fondo).
// El loop de dibujo itera N-1 downTo 0 → painter's algorithm correcto.
private val FAR_EXTRA_DP = floatArrayOf(0f, 13.3f, 26.7f, 40f)
private val FAR_ALPHAS   = floatArrayOf(0.45f, 0.33f, 0.17f, 0.05f)
private val NEAR_EXTRA_DP = floatArrayOf(0f, 7.5f, 15f)
private val NEAR_ALPHAS   = floatArrayOf(0.65f, 0.40f, 0.15f)
private const val V5_SHARP_ALPHA = 0.85f

@Composable
fun VoiceVisualizerRingV5(
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

    val paths = remember { Array(V5_LAYER_COUNT) { Path() } }
    val scratch = remember { FloatArray(8) }
    val rotations = remember { FloatArray(V5_LAYER_COUNT) }

    LaunchedEffect(Unit) {
        var lastFrameMs = -1L
        var animMs = 0L
        while (true) {
            withFrameMillis { nowMs ->
                val dt = if (lastFrameMs < 0L) 0L else nowMs - lastFrameMs
                lastFrameMs = nowMs

                val v = volumeState.value.coerceIn(0f, 1f)
                if (v > V5_SILENCE_EPSILON) {
                    animMs += dt
                }
                elapsedMs.longValue = animMs

                val avg = v * 255f
                val targetScale = 1f + (avg / 500f) * intensityState.value
                val targetBright = 0.4f + (avg / 150f) * intensityState.value

                currentScale.floatValue +=
                    (targetScale - currentScale.floatValue) * V5_LERP_FACTOR
                currentBright.floatValue +=
                    (targetBright - currentBright.floatValue) * V5_LERP_FACTOR
            }
        }
    }

    Canvas(modifier) {
        if (size.minDimension <= 0f) return@Canvas

        val s = currentScale.floatValue
        val b = currentBright.floatValue.coerceIn(0f, 1f)
        val ms = elapsedMs.longValue
        val baseStrokePx = thickness.dp.toPx()

        val base = size.minDimension * V5_BASE_FRACTION
        val dx = (size.width - base) / 2f
        val dy = (size.height - base) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Compute all blob paths once per frame (reused across all passes)
        for (layer in 0 until V5_LAYER_COUNT) {
            val rawProgress = (ms % V5_CYCLE_MS).toFloat() / V5_CYCLE_MS.toFloat()
            val layerProgress = (rawProgress + V5_PHASE_OFFSETS[layer]) % 1f
            rotations[layer] = buildBlobFrameV5(paths[layer], scratch, layerProgress, base)
        }

        scale(scaleX = s, scaleY = s, pivot = Offset(cx, cy)) {
            // --- Far halo (full mode only) — widest (back) → narrowest (front) ---
            if (!lowPerformanceMode) {
                for (pi in FAR_EXTRA_DP.size - 1 downTo 0) {
                    val strokePx = baseStrokePx + (FAR_EXTRA_DP[pi] * glowSpread).dp.toPx()
                    val passAlpha = FAR_ALPHAS[pi]
                    for (layer in V5_LAYER_COUNT - 1 downTo 0) {
                        val layerBright = (b * (1f - layer * 0.2f)).coerceIn(0f, 1f)
                        drawBlobV5(
                            path = paths[layer],
                            rotation = rotations[layer],
                            dx = dx, dy = dy, base = base,
                            color = color.copy(alpha = passAlpha * layerBright),
                            strokePx = strokePx,
                        )
                    }
                }
            }

            // --- Near halo — widest (back) → narrowest (front) ---
            for (pi in NEAR_EXTRA_DP.size - 1 downTo 0) {
                val strokePx = baseStrokePx + (NEAR_EXTRA_DP[pi] * glowSpread).dp.toPx()
                val passAlpha = NEAR_ALPHAS[pi]
                for (layer in V5_LAYER_COUNT - 1 downTo 0) {
                    val layerBright = (b * (1f - layer * 0.2f)).coerceIn(0f, 1f)
                    drawBlobV5(
                        path = paths[layer],
                        rotation = rotations[layer],
                        dx = dx, dy = dy, base = base,
                        color = color.copy(alpha = passAlpha * layerBright),
                        strokePx = strokePx,
                    )
                }
            }

            // --- Sharp ring ---
            for (layer in V5_LAYER_COUNT - 1 downTo 0) {
                val layerBright = (b * (1f - layer * 0.2f)).coerceIn(0f, 1f)
                drawBlobV5(
                    path = paths[layer],
                    rotation = rotations[layer],
                    dx = dx, dy = dy, base = base,
                    color = color.copy(alpha = V5_SHARP_ALPHA * layerBright),
                    strokePx = baseStrokePx,
                )
            }
        }
    }
}

private fun DrawScope.drawBlobV5(
    path: Path,
    rotation: Float,
    dx: Float,
    dy: Float,
    base: Float,
    color: Color,
    strokePx: Float,
) {
    translate(left = dx, top = dy) {
        rotate(degrees = rotation, pivot = Offset(base / 2f, base / 2f)) {
            drawPath(path, color, style = Stroke(width = strokePx))
        }
    }
}

// Idéntico a V4 buildBlobFrame — interpola radios + rotación entre keyframes
// y normaliza al estilo CSS border-radius para evitar intersecciones propias.
private fun buildBlobFrameV5(
    path: Path,
    scratch: FloatArray,
    progress: Float,
    base: Float,
): Float {
    val segFloat = progress * 3f
    val segIdx = segFloat.toInt().coerceIn(0, 2)
    val localT = (segFloat - segIdx).coerceIn(0f, 1f)
    val eased = V5_EASING.transform(localT)

    val fromR = V5_KEYFRAME_RADII[segIdx]
    val toR = V5_KEYFRAME_RADII[(segIdx + 1) % 3]
    for (i in 0 until 8) {
        scratch[i] = fromR[i] + (toR[i] - fromR[i]) * eased
    }

    val fromRot = V5_KEYFRAME_ROTATIONS[segIdx]
    val toRot = if (segIdx == 2) 360f else V5_KEYFRAME_ROTATIONS[segIdx + 1]
    val rotation = fromRot + (toRot - fromRot) * eased

    val w = base
    val h = base
    val h1 = scratch[0] * w / 100f
    val h2 = scratch[1] * w / 100f
    val h3 = scratch[2] * w / 100f
    val h4 = scratch[3] * w / 100f
    val nv1Src = scratch[4] * h / 100f
    val nv2Src = scratch[5] * h / 100f
    val nv3Src = scratch[6] * h / 100f
    val nv4Src = scratch[7] * h / 100f

    var factor = 1f
    val topSum = h1 + h2
    if (topSum > w) factor = minOf(factor, w / topSum)
    val bottomSum = h3 + h4
    if (bottomSum > w) factor = minOf(factor, w / bottomSum)
    val leftSum = nv1Src + nv4Src
    if (leftSum > h) factor = minOf(factor, h / leftSum)
    val rightSum = nv2Src + nv3Src
    if (rightSum > h) factor = minOf(factor, h / rightSum)

    val nh1 = h1 * factor
    val nh2 = h2 * factor
    val nh3 = h3 * factor
    val nh4 = h4 * factor
    val nv1 = nv1Src * factor
    val nv2 = nv2Src * factor
    val nv3 = nv3Src * factor
    val nv4 = nv4Src * factor
    val k = V5_BEZIER_K

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
