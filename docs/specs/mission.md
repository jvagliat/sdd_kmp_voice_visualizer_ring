# Mission

## Purpose

Un componente Kotlin Composable reutilizable que renderiza un visualizador de voz: 3 formas orgánicas (blobs) superpuestas con glow multicapa que reaccionan al volumen de audio en tiempo real. Los blobs se deforman continuamente (ciclo de 8 segundos) creando un visual orgánico de "respiración", y escalan/brillan proporcionalmente al input de audio.

Es una **traducción directa** de un prototipo HTML/JS Canvas al stack de Compose Multiplatform. El componente se usa como elemento visual central de un reproductor de audio.

## Primary Audience

Desarrolladores Kotlin/Compose que integran un componente audio-reactivo visualmente sofisticado en apps multiplataforma (Android, iOS, Desktop, Web). La API debe ser simple (un composable con parámetros) y la integración debe ser copy-paste de un solo archivo.

## What Success Looks Like

Un componente que replica fielmente la estética del prototipo HTML (3 rings orgánicos entrelazados con glow simétrico) a 60 FPS en gama media, con consumo de batería aceptable para sesiones prolongadas, compilando en los 4 targets KMP sin dependencias externas más allá de `androidx.compose.*`.

## Out of Scope

- Captura de audio / acceso al micrófono (el caller provee `volume: Float`).
- UI de ajustes / selector de color / controles del reproductor.
- ViewModel, Repository o capas arquitectónicas del host app.
- Tests unitarios del componente visual.
- Accesibilidad del canvas.
