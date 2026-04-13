package com.iattraxia.kmp_voice_ring

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// =============================================================================
//  VoiceVisualizerRing — API pública del componente. Dispatcher de versiones.
//
//  Mantenemos múltiples implementaciones versionadas para preservar el
//  aprendizaje de cada intento y poder comparar visualmente o retomar un
//  approach descartado sin perder contexto:
//
//   - VoiceVisualizerRingV1 — stacks rellenos.
//       Descartado: onion ring (rodajas concéntricas en lugar de anillo hueco).
//
//   - VoiceVisualizerRingV2 — stacks recortados con clipPath.
//       Descartado: 11 bandas duras visibles + "escalón" en el contorno por
//       radii overflowing sin normalización CSS.
//
//   - VoiceVisualizerRingV3 — radii CSS-normalizados + Modifier.blur,
//       blobs FILLED en los halos.
//       Descartado: el centro del ring queda lleno (los fills blureados
//       preservan masa central). Tampoco se ven los 3 phase offsets porque
//       sólo blob[0] aparece nítido.
//
//   - VoiceVisualizerRingV4 — mismo blur + radii normalizados, pero los
//       blobs van como STROKE en todos los canvases y los 3 phase offsets se
//       dibujan también en el ring nítido.
//       Problema en Android: Modifier.blur crea un RenderEffect (hardware
//       layer) que cachea el contenido. Los state reads del draw block no
//       invalidan ese layer → los halos blureados quedan congelados. Solo se
//       actualizan cuando cambia el radio del blur (parámetro del modifier).
//       Intentos de fix fallidos (ver historial de commits):
//         1. Mover state reads al cuerpo del composable (fuera del draw block):
//            el draw block quedó sin reads propios → nunca se re-ejecuta solo.
//         2. Leer estado en el cuerpo Y en el draw block: fuerza recomposición
//            60 fps del composable entero → 1000+ frames skipped, app
//            inutilizable.
//       Conclusión: Modifier.blur no es viable para contenido que anima 60 fps
//       en Android. Solución definitiva en V5.
//
//   - VoiceVisualizerRingV5 — reemplaza Modifier.blur con fake-blur via
//       multi-pass strokes dentro de un Canvas único.
//       Approach: cada blob se dibuja N veces con stroke width decreciente
//       (más ancho primero) y alpha creciente → falloff gaussiano aproximado.
//       Sin hardware layers → sin caching → anima correctamente en todos los
//       targets (Android, iOS, JVM Desktop, wasmJs).
//       Grupos de pasadas: far halo (4p), near halo (3p), sharp ring (1p).
//       24 drawPath calls en full mode, 12 en lowPerformanceMode.
//       Alternativas descartadas para Android:
//         A. drawIntoCanvas + BlurMaskFilter: funciona en Desktop (Skia puro),
//            silenciosamente ignorado en Android hardware canvas para drawPath.
//         B. graphicsLayer { renderEffect = BlurEffect(...) } (API 31+):
//            misma raíz que Modifier.blur → mismo problema de caching.
//            Además requiere expect/actual para Desktop.
//         C. Render a offscreen bitmap + blur bitmap + drawBitmap: correcto
//            pero requiere allocar bitmap(s) por frame y blur software;
//            overkill para este efecto.
//
//   - VoiceVisualizerRingV6Gml51Cloudy — V4 con Cloudy en lugar de Modifier.blur.
//       Usa key(blurTick) para forzar recreación de los canvases blureados a
//       ~15fps. Ring nítido actualiza a 60fps completos.
//
//   - VoiceVisualizerRingV8 — V6 con smoothing de picos y brillo dinámico (T21+T22).
//       T21: filtro paso-bajo en dos etapas (preSmoothed + lerp visual) →
//            movimiento líquido y orgánico en apertura y cierre.
//       T22: floor de targetBright bajado a 0.05f → glow respira con el volumen,
//            se desvanece casi por completo en silencio.
//
//   Experimentos archivados en experiments/:
//   - VoiceVisualizerRingV7 — keyframes circulares con protuberancia localizada.
//       Descartado como rama principal; V6 es la base canónica.
//
//  Cada Vx arranca con un header propio documentando hipótesis, approach,
//  drawPath count, problemas descubiertos y siguiente paso.
//
//  Para comparar versiones cambiá la línea de despacho de abajo. Las firmas
//  son idénticas → swap puro.
// =============================================================================

@Composable
fun VoiceVisualizerRing(
    volume: Float,
    color: Color,
    intensity: Float = 1f,
    thickness: Float = 5f,
    glowSpread: Float = 1f,
    blurRadius: Float = 15f,
    relativeMotion: Boolean = false,
    layerFalloff: Float = 0.2f,
    lowPerformanceMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    VoiceVisualizerRingV8(
        volume = volume,
        color = color,
        intensity = intensity,
        thickness = thickness,
        glowSpread = glowSpread,
        blurRadius = blurRadius,
        relativeMotion = relativeMotion,
        layerFalloff = layerFalloff,
        lowPerformanceMode = lowPerformanceMode,
        modifier = modifier,
    )
}
