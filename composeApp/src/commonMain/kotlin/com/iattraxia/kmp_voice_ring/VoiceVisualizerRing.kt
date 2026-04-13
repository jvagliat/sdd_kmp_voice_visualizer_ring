package com.iattraxia.kmp_voice_ring

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// =============================================================================
//  VoiceVisualizerRing — componente audio-reactivo de anillo orgánico con glow.
//
//  Dibuja un anillo de forma orgánica que responde al volumen de entrada:
//  se expande con el audio y se contrae en silencio. El anillo está formado
//  por 3 blobs Bézier desfasados que se superponen y generan un efecto de
//  "shimmer" fluido. El glow se implementa con dos halos blureados (Cloudy)
//  apilados detrás del contorno nítido.
//
//  Parámetros:
//
//  volume          Nivel de volumen normalizado [0.0, 1.0]. Típicamente el RMS
//                  del frame de audio en curso. Controla la escala del ring
//                  y la intensidad del glow.
//
//  color           Color base del ring y del glow. Se aplica con distintos
//                  valores de alpha en cada capa (halo lejano, cercano, nítido).
//
//  intensity       Multiplicador de la respuesta a `volume`. Con 1.0 (default)
//                  el ring usa su rango completo de escala y brillo. Valores
//                  menores reducen la sensibilidad; mayores la amplifican.
//
//  thickness       Grosor base del trazo del ring en dp (default 5). Los halos
//                  blureados usan múltiplos de este valor (×1.6 y ×2.5).
//
//  glowSpread      Multiplicador sobre los radios de blur de Cloudy. 1.0 =
//                  radios por defecto; valores mayores amplían el halo sin
//                  cambiar el trazo nítido.
//
//  blurRadius      Radio base del halo cercano en dp (default 15). El halo
//                  lejano escala a blurRadius×2.5. En lowPerformanceMode el
//                  halo lejano se omite y el cercano usa blurRadius×1.5.
//
//  relativeMotion  false (default): los 3 blobs comparten el mismo ciclo de
//                  8 segundos con phase offsets fijos (0%, 31.25%, 62.5%).
//                  true: cada blob usa un ciclo ligeramente distinto (8000 /
//                  8720 / 9440 ms) → el desfasaje entre blobs varía en el
//                  tiempo, creando movimiento relativo real entre los anillos.
//
//  layerFalloff    Cuánto se atenúa el brillo en cada capa respecto a la
//                  anterior. 0.0 = todas las capas con igual brillo. 0.2
//                  (default) = cada capa pierde un 20% de brillo adicional.
//                  La capa 0 (frente) siempre tiene el brillo máximo.
//
//  inputSmoothing  Decay del filtro paso-bajo que suaviza el volumen crudo
//                  antes de aplicarlo (default 0.85). Rango útil: 0.0–0.95.
//                  0.0 = sin suavizado (señal cruda, picos bruscos).
//                  0.95 = muy inerte (responde lento a cambios de volumen).
//
//  responsiveness  Factor de lerp visual que mueve el ring hacia su target
//                  en cada frame (default 0.15). Rango útil: 0.01–1.0.
//                  0.01 = movimiento muy suave y orgánico.
//                  1.0 = snap instantáneo al target.
//
//  lowPerformanceMode
//                  true: omite el halo lejano para reducir la carga GPU en
//                  dispositivos con limitaciones. false (default): renderiza
//                  los 3 canvases (far halo + near halo + sharp ring).
//
//  modifier        Modifier estándar de Compose. El componente ocupa todo el
//                  espacio disponible dentro del modifier aplicado.
//
//  Historia de implementaciones: docs/bitacora_estrategias.md
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
    inputSmoothing: Float = 0.85f,
    responsiveness: Float = 0.15f,
    lowPerformanceMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    VoiceVisualizerRingV9(
        volume = volume,
        color = color,
        intensity = intensity,
        thickness = thickness,
        glowSpread = glowSpread,
        blurRadius = blurRadius,
        relativeMotion = relativeMotion,
        layerFalloff = layerFalloff,
        inputSmoothing = inputSmoothing,
        responsiveness = responsiveness,
        lowPerformanceMode = lowPerformanceMode,
        modifier = modifier,
    )
}
