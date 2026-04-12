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
//       dibujan también en el ring nítido. Activo.
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
    lowPerformanceMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    VoiceVisualizerRingV4(
        volume = volume,
        color = color,
        intensity = intensity,
        thickness = thickness,
        glowSpread = glowSpread,
        lowPerformanceMode = lowPerformanceMode,
        modifier = modifier,
    )
}
