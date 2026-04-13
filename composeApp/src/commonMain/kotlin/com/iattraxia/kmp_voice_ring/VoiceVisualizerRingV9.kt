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
//  V9 — V8 con inputSmoothing y responsiveness como parámetros (T26).
//
//  Cambios respecto a V8:
//
//  Las constantes de suavizado dejan de ser hardcodeadas y se exponen como
//  parámetros del composable, permitiendo ajuste en caliente vía sliders:
//
//  - inputSmoothing: Float (default 0.85)
//      Decay de Stage 1 (filtro paso-bajo sobre el dato de volumen crudo).
//      0.0 = sin memoria (señal cruda, picos bruscos).
//      0.95 = muy inerte (responde lento a cambios de volumen).
//      PRE_SMOOTH_NEW se deriva como (1 - inputSmoothing).
//
//  - responsiveness: Float (default 0.15)
//      Factor de Stage 2 (lerp visual de current hacia target).
//      0.01 = muy suave (movimiento lento y orgánico).
//      1.0 = snap instantáneo al target.
//
//  Ambos se leen via rememberUpdatedState, por lo que cambios en caliente
//  (slider en el DebugPanel) tienen efecto inmediato sin reiniciar la animación.
//
//  El resto (keyframes, relativeMotion, Cloudy key-trick, layerFalloff,
//  glow dinámico, floor 0.05f) es idéntico a V8.
// =============================================================================

private const val CYCLE_MS_V9 = 8000L
private const val LAYER_COUNT_V9 = 3
private const val BEZIER_K_V9 = 0.5522847498f
private val EASING_V9 = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
private val PHASE_OFFSETS_V9 = floatArrayOf(0f, 0.3125f, 0.625f)
private val KEYFRAME_RADII_V9 = arrayOf(
    floatArrayOf(50f, 45f, 60f, 45f, 45f, 60f, 45f, 60f),
    floatArrayOf(45f, 60f, 45f, 55f, 60f, 45f, 60f, 45f),
    floatArrayOf(60f, 45f, 55f, 60f, 45f, 60f, 45f, 55f),
)
private val KEYFRAME_ROTATIONS_V9 = floatArrayOf(0f, 120f, 240f)
private const val BASE_FRACTION_V9 = 0.55f
private const val SILENCE_EPSILON_V9 = 0.005f
private const val BLUR_UPDATE_INTERVAL_MS_V9 = 66L
private val CYCLE_DURATIONS_V9 = longArrayOf(
    CYCLE_MS_V9,
    (CYCLE_MS_V9 * 1.09).toLong(),
    (CYCLE_MS_V9 * 1.18).toLong(),
)

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
    val volumeState = rememberUpdatedState(volume)
    val intensityState = rememberUpdatedState(intensity)
    val inputSmoothingState = rememberUpdatedState(inputSmoothing)
    val responsivenessState = rememberUpdatedState(responsiveness)

    val currentScale = remember { mutableFloatStateOf(1f) }
    val currentBright = remember { mutableFloatStateOf(0.05f) }
    val elapsedMs = remember { mutableLongStateOf(0L) }
    val blurKey = remember { mutableIntStateOf(0) }

    val preSmoothed = remember { mutableFloatStateOf(0f) }

    val paths = remember { Array(LAYER_COUNT_V9) { Path() } }
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
                if (v > SILENCE_EPSILON_V9) {
                    animMs += dt
                }
                elapsedMs.longValue = animMs

                val decay = inputSmoothingState.value.coerceIn(0f, 0.999f)
                preSmoothed.floatValue = preSmoothed.floatValue * decay + v * (1f - decay)

                val smoothedV = preSmoothed.floatValue
                val avg = smoothedV * 255f

                val targetScale = 1f + (avg / 500f) * intensityState.value
                val targetBright = 0.05f + (avg / 150f) * intensityState.value

                val lerp = responsivenessState.value.coerceIn(0.001f, 1f)
                currentScale.floatValue += (targetScale - currentScale.floatValue) * lerp
                currentBright.floatValue += (targetBright - currentBright.floatValue) * lerp

                if (nowMs - lastBlurUpdateMs >= BLUR_UPDATE_INTERVAL_MS_V9) {
                    blurKey.intValue++
                    lastBlurUpdateMs = nowMs
                }
            }
        }
    }

    Box(modifier = modifier) {
        val tick = blurKey.intValue

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

@Composable
private fun BlobsCanvasV9(
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
    layerFalloff: Float,
) {
    @Suppress("UNUSED_EXPRESSION")
    elapsedMs.longValue

    Canvas(modifier) {
        if (size.minDimension <= 0f) return@Canvas

        val s = currentScale.floatValue
        val b = currentBright.floatValue.coerceIn(0f, 1f)
        val ms = elapsedMs.longValue
        val strokePx = strokeWidthDp.dp.toPx()

        val base = size.minDimension * BASE_FRACTION_V9
        val dx = (size.width - base) / 2f
        val dy = (size.height - base) / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        scale(scaleX = s, scaleY = s, pivot = Offset(cx, cy)) {
            for (layer in (LAYER_COUNT_V9 - 1) downTo 0) {
                val cycleMs = if (relativeMotion) CYCLE_DURATIONS_V9[layer] else CYCLE_MS_V9
                val rawProgress = (ms % cycleMs).toFloat() / cycleMs.toFloat()
                val layerProgress = (rawProgress + PHASE_OFFSETS_V9[layer]) % 1f
                val rotation = buildBlobFrameV9(
                    path = paths[layer],
                    scratch = scratch,
                    progress = layerProgress,
                    base = base,
                )
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

private fun buildBlobFrameV9(
    path: Path,
    scratch: FloatArray,
    progress: Float,
    base: Float,
): Float {
    val segFloat = progress * 3f
    val segIdx = segFloat.toInt().coerceIn(0, 2)
    val localT = (segFloat - segIdx).coerceIn(0f, 1f)
    val eased = EASING_V9.transform(localT)

    val fromR = KEYFRAME_RADII_V9[segIdx]
    val toR = KEYFRAME_RADII_V9[(segIdx + 1) % 3]
    for (i in 0 until 8) {
        scratch[i] = fromR[i] + (toR[i] - fromR[i]) * eased
    }

    val fromRot = KEYFRAME_ROTATIONS_V9[segIdx]
    val toRot = if (segIdx == 2) 360f else KEYFRAME_ROTATIONS_V9[segIdx + 1]
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
    val k = BEZIER_K_V9

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
