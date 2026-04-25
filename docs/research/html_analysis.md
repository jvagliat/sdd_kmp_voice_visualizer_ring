# Análisis del prototipo HTML — `html_para_desarrollador_final.html`

Análisis en frío del prototipo, sin referencia al proyecto KMP. Enfoque: qué efectos visuales están implementados, cómo se combinan, y qué "física" (percibida) producen.

## 1. Efectos implementados — lectura directa del código

El prototipo dibuja un círculo reactivo a audio compuesto por **tres elementos DOM idénticos** (`.circle`) apilados en el mismo punto dentro de un `.container` de 120×120 px. No hay canvas; todo es CSS + un `AudioContext` con `AnalyserNode` para sacar el nivel.

Efectos que veo, en orden de impacto visual:

1. **Fondo de cámara oscura.** `radial-gradient` azul noche (`#1c2244 → #0a0d1d`) centrado. Función: dar rango dinámico al glow cian; sin fondo oscuro, los `box-shadow` cian se lavan.

2. **Anillo con borde cian y grosor fijo.** `border: 5px solid rgba(0, 255, 255, 0.8)`. Es el único elemento "duro" del conjunto; todo lo demás es halo, blur o morph.

3. **Glow cian multi-capa (double-sided).** Cada `.circle` aplica **6 `box-shadow` simultáneos**: tres hacia afuera (15 / 40 / 70 px) y tres hacia adentro (`inset` 15 / 40 / 70 px), con alfa y tono decrecientes (`#00ffff` → `#00ffcc`). Resultado: un halo gaussiano aproximado por tres gaussianas stackeadas, simétrico dentro/fuera del borde. Es el efecto dominante.

4. **Tres capas con glow escalonado.** Los hijos 2 y 3 sobrescriben el `box-shadow` con versiones más tenues (0.3 y 0.2 de alfa, radios 30 y 20 px). Apiladas sobre la primera, el halo total gana "grosor perceptual" sin duplicar la masa del borde duro (porque ese borde se solapa pixel a pixel entre las tres).

5. **Morphing orgánico del contorno (`@keyframes wave`).** Los 8 valores de `border-radius` (`50% 45% 60% 45% / 45% 60% 45% 60%`, etc.) interpolan entre 3 keyframes cada 8 s con `ease-in-out`. En CSS, los 8 números controlan los radios horizontales/verticales de las 4 esquinas de forma independiente, lo que produce un **super-ellipse asimétrico** que respira entre tres formas distintas. Nunca se sale de algo "casi-redondo".

6. **Rotación acoplada al morph.** Cada keyframe del `wave` mete también `rotate(0 / 120 / 240 / 360deg)`. El contorno morpheado gira junto con la deformación, así que la cresta del blob "viaja" alrededor del centro en vez de latir en un eje fijo.

7. **Desincronización por phase offset entre capas.** Las tres `.circle` comparten `animation: wave 8s infinite`, pero los hijos 2 y 3 aplican `animation-delay: -2.5s` y `-5s`. Eso las mete en el mismo ciclo pero desfasadas 1/3 y 2/3. Como cada una tiene su propia rotación + su propia deformación en ese instante, los tres contornos **no coinciden** pixel a pixel — se intersectan. Ese cruce es lo que da el aspecto de "3 anillos orgánicos entrelazados" y no "un anillo triplicado".

8. **Backdrop-filter blur (8 px).** Sobre el interior de cada círculo. En la práctica, como el fondo es un gradiente casi uniforme y no hay contenido detrás, el efecto visible es mínimo — aporta una sutileza de "vidrio" si hubiera algo atrás, pero en esta demo es casi decorativo.

9. **Filtro `blur(0.3px)` global sobre el borde.** Un smoothing sub-pixel que mata el aliasing del borde duro. Esencialmente: anti-alias forzado sobre el stroke de 5 px.

10. **Escala reactiva (`--scale`).** JS calcula `targetScale = 1 + (avg / 500)` donde `avg` es el promedio de bins FFT 2..14. Se aplica al `.container` con `transform: scale(var(--scale))`. Rango típico: ~1.00 (silencio) a ~1.5 (picos). La animación llega con un lerp de factor 0.15 por frame (`currentScale += (target - currentScale) * 0.15`), o sea inercia/ease-out exponencial hacia el target.

11. **Brillo reactivo (`--bright`).** Paralelo al scale: `targetBright = 0.4 + avg/150`. Aplica como `opacity` de cada `.circle`, pero con **gradiente entre capas**: la capa 1 recibe el valor full, las capas 2 y 3 lo reciben multiplicado por `(1 - index*0.2)` (×0.8 y ×0.6 respectivamente). Mismo lerp 0.15.

12. **Transition CSS de 50 ms sobre `transform` y `opacity`.** Redundante con el lerp del JS, pero suma un micro-smoothing entre actualizaciones de frame.

13. **FFT y banda elegida.** `fftSize = 256` (128 bins útiles), `smoothingTimeConstant = 0.85` (smoothing temporal del propio analyser, muy alto), promedio de bins `2..14`. Eso cubre aprox la banda vocal baja/media (≈86 Hz a 600 Hz para 44.1 kHz). Es por qué reacciona mucho a voz y poco a sibilantes o bajos puros.

## 2. Resumen de la "física"

### 2a. Tabla — efecto ↔ descripción

| # | Efecto | Descripción breve |
|---|---|---|
| 1 | Fondo radial oscuro | Contraste para que el glow cian resalte. |
| 2 | Anillo duro 5 px | Única silueta nítida del conjunto. |
| 3 | Glow outer × 3 niveles | 3 box-shadows externos (15/40/70 px) aproximan una gaussiana ancha. |
| 4 | Glow inner × 3 niveles | 3 box-shadows `inset` (15/40/70 px) — halo hacia adentro del borde. |
| 5 | 3 capas apiladas | Hijos 2 y 3 con glows más tenues suman "masa" al halo sin triplicar el borde. |
| 6 | Morph de border-radius | 8 radios independientes interpolando entre 3 keyframes → super-ellipse respirando. |
| 7 | Rotación 0°→360° en 8 s | Cada keyframe rota 120° — la cresta del blob viaja alrededor del centro. |
| 8 | Phase offset por capa | `animation-delay` −2.5 s y −5 s → las 3 capas nunca coinciden, se intersectan. |
| 9 | Blur sub-pixel global (0.3 px) | Anti-alias forzado sobre el borde duro. |
| 10 | Backdrop-filter 8 px | "Vidrio" sobre el interior (poco visible en esta demo). |
| 11 | Scale reactivo (1.0 → ~1.5) | `1 + avg/500`, lerp 0.15 — pulso del contenedor entero. |
| 12 | Brightness reactivo por capa | `opacity` con escalón entre capas (×1.0, ×0.8, ×0.6), lerp 0.15. |
| 13 | FFT banda media | Promedio bins 2..14 de FFT 256, `smoothingTimeConstant 0.85`. |

### 2b. Elaboración — qué "sensación física" produce cada bloque

**Bloque A — la silueta (efectos 2, 6, 7).**
El anillo no es un círculo: es un super-ellipse cuyos 8 radios de esquina se mueven de forma independiente en un loop de 8 s. Sumado a la rotación de 360° dentro del mismo loop, la sensación es la de una membrana orgánica que no tiene eje privilegiado — como una gota de mercurio vista desde arriba, o un iris respirando. Nunca pierde la topología de anillo porque los radios se mantienen en rangos `45–60%` (nunca `0%` ni `>100%`).

**Bloque B — el halo (efectos 3, 4, 5, 10).**
La clave es que hay glow a los dos lados del borde: el `box-shadow` externo y el `inset` hacia adentro son simétricos en radios (15/40/70) y en alfa. Eso hace que el borde duro quede "flotando" dentro de una nube cian que sangra igual hacia afuera y hacia adentro. Visualmente se parece a una descarga de plasma confinada a un tubo toroidal: ves el filamento (el borde) y el resplandor a su alrededor, pero el centro del anillo sigue relativamente vacío porque el glow interior es radial desde el borde, no desde el centro. Las 3 capas apiladas con glows más tenues agregan extensión al halo sin oscurecer el centro ni engrosar el filamento.

**Bloque C — la multiplicidad (efecto 8).**
El `animation-delay` negativo desfasado meticulosamente es lo que convierte el "una forma morpheada" en "tres formas que coexisten y se cruzan". En cualquier instante `t`, la capa 1 está en el progreso `t/8`, la 2 en `(t+2.5)/8` y la 3 en `(t+5)/8`. Como el morph incluye rotación, las tres siluetas están en rotaciones distintas y con radios distintos. El ojo lee eso como **interferencia**: tres membranas elásticas compartiendo un mismo espacio pero moviéndose desfasadas. Ese patrón de cruces es lo que evita que el resultado se sienta plano.

**Bloque D — la reactividad al audio (efectos 11, 12, 13).**
El audio controla dos canales independientes: escala del contenedor completo y opacidad por capa. El scale tiene rango acotado (×1..×1.5) y el lerp 0.15 da un half-life de ~5 frames (~80 ms a 60 fps), así que el pulso se siente "orgánico" más que "saltón". El brightness tiene escalón entre capas: la capa frontal reacciona full (0.4–2.1), las de atrás reaccionan amortiguadas, lo que produce un efecto de profundidad — el halo exterior parpadea más suave que el borde central. Eligieron la banda media (≈86–600 Hz) del FFT porque es donde vive el cuerpo de la voz; usar el full-spectrum promedio haría que el ring respondiera más a ruido de fondo.

**Bloque E — el smoothing.**
Hay **tres capas de smoothing temporal** apiladas: `analyser.smoothingTimeConstant = 0.85` (filtro IIR en el analyser antes de exponer el dato), lerp 0.15 por frame en JS (IIR en el render loop), y `transition: 0.05s ease-out` en CSS (interp entre cambios de `--scale` y `--bright`). Es por qué el prototipo nunca se ve "nervioso" — un pico de audio tarda ~100–150 ms en llegar a su scale full. Es una decisión estética deliberada: suaviza a costa de latencia perceptiva.

---

**Síntesis de una línea.** Un super-ellipse rotante de 8 s replicado 3 veces con phase offsets de 1/3, cada réplica rodeada de un halo gaussiano simétrico dentro/fuera, todo con scale + opacity lerpeados desde el nivel FFT de la banda vocal media.
