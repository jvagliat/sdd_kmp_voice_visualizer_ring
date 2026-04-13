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
//  V7 — V6 con tres cambios:
//
//  1. blurRadius parametrizado: el caller controla el radio de blur del near
//     halo en dp. Far halo escala a blurRadius * 2.5f.
//
//  2. Keyframes circulares con protuberancia localizada:
//     En lugar de variar los 8 radios globalmente (V6: 45-60, V7prev: 30-75),
//     cada keyframe mantiene la mayoría de valores cerca de 50% (forma
//     circular) y eleva 2 valores opuestos a 65% en esquinas específicas.
//     Para evitar la normalización CSS (h1+h2 > 100), los pares opuestos
//     suman exactamente 100: si un radio es 65, el de la misma arista es 35.
//     Resultado: anillo casi circular con una "llamarada" localizada que
//     rota entre keyframes via la combinación de keyframe + KEYFRAME_ROTATIONS.
//
//  3. relativeMotion: cuando true, cada blob usa un CYCLE_MS distinto
//     (8000, 8720, 9440 ms). El desfasaje entre blobs varía en el tiempo →
//     movimiento relativo real, los anillos se "abren" y "cierran" entre sí.
//     Cuando false, mismo ciclo con phase offsets fijos (comportamiento V6).
//
//  Los phase offsets se mantienen en ambos modos para evitar saltos bruscos
//  al togglear relativeMotion en runtime.
// =============================================================================

private const val CYCLE_MS_V7 = 8000L
private const val LERP_FACTOR_V7 = 0.15f
private const val LAYER_COUNT_V7 = 3
private const val BEZIER_K_V7 = 0.5522847498f
private val EASING_V7 = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val PHASE_OFFSETS_V7 = floatArrayOf(0f, 0.3125f, 0.625f)

// Ciclos para relativeMotion=true. Separados ~9% entre sí para que el drift
// sea perceptible en ~30s sin ser demasiado rápido.
private val CYCLE_DURATIONS_V7 = longArrayOf(
    CYCLE_MS_V7,
    (CYCLE_MS_V7 * 1.09).toLong(),   // ~8720ms
    (CYCLE_MS_V7 * 1.18).toLong(),   // ~9440ms
)

// Keyframes: anillo circular con una protuberancia localizada.
// Cada par de valores que comparte arista suma 100 (sin overflow → sin
// normalización que aplanaría la protuberancia).
//   h = radios horizontales (top-left, top-right, bottom-right, bottom-left)
//   v = radios verticales   (top-left, top-right, bottom-right, bottom-left)
//
//  KF0: protuberancia en esquina top-right  (h2=65/h1=35, v2=65/v3=35)
//  KF1: protuberancia en esquina bottom-left (h4=65/h3=35, v4=65/v1=35)
//  KF2: protuberancia en esquina top-left   (h1=65/h2=35, v1=65/v4=35)
//
//  KEYFRAME_ROTATIONS gira 120° adicional por keyframe → la protuberancia
//  recorre el anillo completo en un ciclo.
private val KEYFRAME_RADII_V7 = arrayOf(
    floatArrayOf(35f, 65f, 50f, 48f, 50f, 65f, 35f, 48f),
    floatArrayOf(48f, 50f, 35f, 65f, 35f, 50f, 48f, 65f),
    floatArrayOf(65f, 35f, 48f, 50f, 65f, 48f, 50f, 35f),
)
private val KEYFRAME_ROTATIONS_V7 = floatArrayOf(0f, 120f, 240f)
private const val BASE_FRACTION_V7 = 0.55f
private const val SILENCE_EPSILON_V7 = 0.005f
private const val BLUR_UPDATE_INTERVAL_MS_V7 = 66L

@Composable
fun VoiceVisualizerRingV7(
    volume: Float,
    color: Color,
    intensity: Float = 1f,
    thickness: Float = 5f,
    glowSpread: Float = 1f,
    blurRadius: Float = 15f,
    relativeMotion: Boolean = false,
    lowPerformanceMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val volumeState = rememberUpdatedState(volume)
    val intensityState = rememberUpdatedState(intensity)

    val currentScale = remember { mutableFloatStateOf(1f) }
    val currentBright = remember { mutableFloatStateOf(0.4f) }
    val elapsedMs = remember { mutableLongStateOf(0L) }
    val blurKey = remember { mutableIntStateOf(0) }

    val paths = remember { Array(LAYER_COUNT_V7) { Path() } }
    val scratch = remember { FloatArray(8) }

    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        var lastFrameMs = -1L
        var animMs = 0L
        var lastBlurUpdateMs = 0L
        while (true) {
            withFrameMillis { nowMs ->
                val dt = if (lastFrameMs < 0L) 0L else nowMs - lastFrameMs
                lastFrameMs = nowMs

                val v = volumeState.value.coerceIn(0f, 1f)
                if (v > SILENCE_EPSILON_V7) animMs += dt
                elapsedMs.longValue = animMs

                val avg = v * 255f
                val targetScale = 1f + (avg / 500f) * intensityState.value
                val targetBright = 0.4f + (avg / 150f) * intensityState.value

                currentScale.floatValue += (targetScale - currentScale.floatValue) * LERP_FACTOR_V7
                currentBright.floatValue += (targetBright - currentBright.floatValue) * LERP_FACTOR_V7

                if (nowMs - lastBlurUpdateMs >= BLUR_UPDATE_INTERVAL_MS_V7) {
                    blurKey.intValue++
                    lastBlurUpdateMs = nowMs
                }
            }
        }
    }

    Box(modifier = modifier) {
        val tick = blurKey.intValue

        if (!lowPerformanceMode) {
            val farRadiusPx = with(density) { (blurRadius * 2.5f * glowSpread).dp.roundToPx() }
            key(tick) {
                BlobsCanvasV7(
                    modifier = Modifier.fillMaxSize().cloudy(radius = farRadiusPx),
                    color = color,
                    alphaMul = 0.45f,
                    strokeWidthDp = thickness * 2.5f,
                    paths = paths,
                    scratch = scratch,
                    currentScale = currentScale,
                    currentBright = currentBright,
                    elapsedMs = elapsedMs,
                    relativeMotion = relativeMotion,
                )
            }
        }

        val nearRadiusPx = with(density) {
            val base = if (lowPerformanceMode) blurRadius * 1.5f else blurRadius
            (base * glowSpread).dp.roundToPx()
        }
        key(tick) {
            BlobsCanvasV7(
                modifier = Modifier.fillMaxSize().cloudy(radius = nearRadiusPx),
                color = color,
                alphaMul = 0.65f,
                strokeWidthDp = thickness * 1.6f,
                paths = paths,
                scratch = scratch,
                currentScale = currentScale,
                currentBright = currentBright,
                elapsedMs = elapsedMs,
                relativeMotion = relativeMotion,
            )
        }

        BlobsCanvasV7(
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
        )
    }
}

@Composable
private fun BlobsCanvasV7(
    modifier: Modifier,
    color: Color,
    alphaMul: Float,
    strokeWidthDp: Float,
    paths: Array<Path>,
    scratch: FloatArray,
    currentScale: MutableFloatState,
    currentBright: MutableFloatState,
    elapsedMs: MutableLongState,
    relativeMotion: Boolean,
) {
    @Suppress("UNUSED_EXPRESSION")
    elapsedMs.longValue

    Canvas(modifier) {
        if (size.minDimension <= 0f) return@Canvas

        val s = currentScale.floatValue
        val b = currentBright.floatValue.coerceIn(0f, 1f)
        val ms = elapsedMs.longValue
        val strokePx = strokeWidthDp.dp.toPx()

        val base = size.minDimension * BASE_FRACTION_V7
        val dx = (size.width - base) / 2f
        val dy = (size.height - base) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        scale(scaleX = s, scaleY = s, pivot = Offset(cx, cy)) {
            for (layer in (LAYER_COUNT_V7 - 1) downTo 0) {
                val cycleMs = if (relativeMotion) CYCLE_DURATIONS_V7[layer] else CYCLE_MS_V7
                val rawProgress = (ms % cycleMs).toFloat() / cycleMs.toFloat()
                val layerProgress = (rawProgress + PHASE_OFFSETS_V7[layer]) % 1f

                val rotation = buildBlobFrameV7(
                    path = paths[layer],
                    scratch = scratch,
                    progress = layerProgress,
                    base = base,
                )
                val layerBright = (b * (1f - layer * 0.2f)).coerceIn(0f, 1f)
                val pathColor = color.copy(alpha = alphaMul * layerBright)

                translate(left = dx, top = dy) {
                    rotate(degrees = rotation, pivot = Offset(base / 2f, base / 2f)) {
                        drawPath(path = paths[layer], color = pathColor, style = Stroke(width = strokePx))
                    }
                }
            }
        }
    }
}

private fun buildBlobFrameV7(
    path: Path,
    scratch: FloatArray,
    progress: Float,
    base: Float,
): Float {
    val segFloat = progress * 3f
    val segIdx = segFloat.toInt().coerceIn(0, 2)
    val localT = (segFloat - segIdx).coerceIn(0f, 1f)
    val eased = EASING_V7.transform(localT)

    val fromR = KEYFRAME_RADII_V7[segIdx]
    val toR = KEYFRAME_RADII_V7[(segIdx + 1) % 3]
    for (i in 0 until 8) {
        scratch[i] = fromR[i] + (toR[i] - fromR[i]) * eased
    }

    val fromRot = KEYFRAME_ROTATIONS_V7[segIdx]
    val toRot = if (segIdx == 2) 360f else KEYFRAME_ROTATIONS_V7[segIdx + 1]
    val rotation = fromRot + (toRot - fromRot) * eased

    val w = base
    val h = base
    val h1 = scratch[0] * w / 100f; val h2 = scratch[1] * w / 100f
    val h3 = scratch[2] * w / 100f; val h4 = scratch[3] * w / 100f
    val v1 = scratch[4] * h / 100f; val v2 = scratch[5] * h / 100f
    val v3 = scratch[6] * h / 100f; val v4 = scratch[7] * h / 100f

    var factor = 1f
    if (h1 + h2 > w) factor = minOf(factor, w / (h1 + h2))
    if (h3 + h4 > w) factor = minOf(factor, w / (h3 + h4))
    if (v1 + v4 > h) factor = minOf(factor, h / (v1 + v4))
    if (v2 + v3 > h) factor = minOf(factor, h / (v2 + v3))

    val nh1 = h1 * factor; val nh2 = h2 * factor
    val nh3 = h3 * factor; val nh4 = h4 * factor
    val nv1 = v1 * factor; val nv2 = v2 * factor
    val nv3 = v3 * factor; val nv4 = v4 * factor
    val k = BEZIER_K_V7

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
