package com.iattraxia.kmp_voice_ring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.iattraxia.kmp_voice_ring.debug.FpsMeter
import com.iattraxia.kmp_voice_ring.player.PlayerState
import com.iattraxia.kmp_voice_ring.player.PlayerViewModel
import com.iattraxia.kmp_voice_ring.player.createPlayer

private val ASSET_PATHS = listOf(
    "files/audio/demo_voice_prototype.wav",
    "files/audio/Jan_Morgenstern_-_01_-_Prelude.wav",
)
private const val DEFAULT_USE_BAND_ENERGY = true

data class RingColorPreset(val label: String, val color: Color)

private val COLOR_PRESETS = listOf(
    RingColorPreset("Ámbar",   Color(0xFFFF9500)),
    RingColorPreset("Cian",    Color(0xFF00FFFF)),
    RingColorPreset("Menta",   Color(0xFF30D68A)),
    RingColorPreset("Violeta", Color(0xFFBF5AF2)),
    RingColorPreset("Hielo",   Color(0xFF5AC8FA)),
)
private const val DEFAULT_COLOR_INDEX = 1 // Cian

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val player = remember { createPlayer() }
        val viewModel = remember { PlayerViewModel(player, scope) }

        val state by viewModel.state.collectAsState()
        val volume by viewModel.volume.collectAsState()
        val positionMs by viewModel.positionMs.collectAsState()
        val durationMs by viewModel.durationMs.collectAsState()
        val volumeFps by viewModel.volumeFps.collectAsState()

        val frameFpsMeter = remember { FpsMeter() }
        val drawFpsMeter = remember { FpsMeter() }
        val frameFps by frameFpsMeter.fps.collectAsState()
        val drawFps by drawFpsMeter.fps.collectAsState()

        var intensity by remember { mutableStateOf(1f) }
        var thickness by remember { mutableStateOf(5f) }
        var glowSpread by remember { mutableStateOf(1f) }
        var blurRadius by remember { mutableStateOf(15f) }
        var relativeMotion by remember { mutableStateOf(false) }
        var layerFalloff by remember { mutableStateOf(0.2f) }

        var assetIndex by remember { mutableStateOf(0) }
        var useBandEnergy by remember { mutableStateOf(DEFAULT_USE_BAND_ENERGY) }
        var colorIndex by remember { mutableStateOf(DEFAULT_COLOR_INDEX) }

        LaunchedEffect(assetIndex, useBandEnergy) {
            viewModel.load(ASSET_PATHS[assetIndex], useBandEnergy = useBandEnergy)
        }
        LaunchedEffect(Unit) {
            while (true) withFrameMillis { frameFpsMeter.tick() }
        }
        DisposableEffect(Unit) { onDispose { viewModel.dispose() } }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1C))
                .safeDrawingPadding(),
        ) {
            val useMobileLayout = isMobilePlatform() || (maxWidth < maxHeight)

            if (useMobileLayout) {
                MobileLayout(
                    state = state,
                    volume = volume,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    volumeFps = volumeFps,
                    frameFps = frameFps,
                    drawFps = drawFps,
                    drawFpsMeter = drawFpsMeter,
                    intensity = intensity,
                    onIntensityChange = { intensity = it },
                    thickness = thickness,
                    onThicknessChange = { thickness = it },
                    glowSpread = glowSpread,
                    onGlowSpreadChange = { glowSpread = it },
                    blurRadius = blurRadius,
                    onBlurRadiusChange = { blurRadius = it },
                    relativeMotion = relativeMotion,
                    onRelativeMotionChange = { relativeMotion = it },
                    layerFalloff = layerFalloff,
                    onLayerFalloffChange = { layerFalloff = it },
                    assetPaths = ASSET_PATHS,
                    assetIndex = assetIndex,
                    onAssetIndexChange = { assetIndex = it },
                    useBandEnergy = useBandEnergy,
                    onUseBandEnergyChange = { useBandEnergy = it },
                    colorPresets = COLOR_PRESETS,
                    colorIndex = colorIndex,
                    onColorIndexChange = { colorIndex = it },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onStop = { viewModel.stop() },
                )
            } else {
                DesktopLayout(
                    state = state,
                    volume = volume,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    volumeFps = volumeFps,
                    frameFps = frameFps,
                    drawFps = drawFps,
                    drawFpsMeter = drawFpsMeter,
                    intensity = intensity,
                    onIntensityChange = { intensity = it },
                    thickness = thickness,
                    onThicknessChange = { thickness = it },
                    glowSpread = glowSpread,
                    onGlowSpreadChange = { glowSpread = it },
                    blurRadius = blurRadius,
                    onBlurRadiusChange = { blurRadius = it },
                    relativeMotion = relativeMotion,
                    onRelativeMotionChange = { relativeMotion = it },
                    layerFalloff = layerFalloff,
                    onLayerFalloffChange = { layerFalloff = it },
                    assetPaths = ASSET_PATHS,
                    assetIndex = assetIndex,
                    onAssetIndexChange = { assetIndex = it },
                    useBandEnergy = useBandEnergy,
                    onUseBandEnergyChange = { useBandEnergy = it },
                    colorPresets = COLOR_PRESETS,
                    colorIndex = colorIndex,
                    onColorIndexChange = { colorIndex = it },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onStop = { viewModel.stop() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopLayout(
    state: PlayerState,
    volume: Float,
    positionMs: Long,
    durationMs: Long,
    volumeFps: Float,
    frameFps: Float,
    drawFps: Float,
    drawFpsMeter: FpsMeter,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    thickness: Float,
    onThicknessChange: (Float) -> Unit,
    glowSpread: Float,
    onGlowSpreadChange: (Float) -> Unit,
    blurRadius: Float,
    onBlurRadiusChange: (Float) -> Unit,
    relativeMotion: Boolean,
    onRelativeMotionChange: (Boolean) -> Unit,
    layerFalloff: Float,
    onLayerFalloffChange: (Float) -> Unit,
    assetPaths: List<String>,
    assetIndex: Int,
    onAssetIndexChange: (Int) -> Unit,
    useBandEnergy: Boolean,
    onUseBandEnergyChange: (Boolean) -> Unit,
    colorPresets: List<RingColorPreset>,
    colorIndex: Int,
    onColorIndexChange: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
) {
    val accentColor = colorPresets[colorIndex].color
    var showSourceSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                VoiceVisualizerRing(
                    volume = volume,
                    color = accentColor,
                    intensity = intensity,
                    thickness = thickness,
                    glowSpread = glowSpread,
                    blurRadius = blurRadius,
                    relativeMotion = relativeMotion,
                    layerFalloff = layerFalloff,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showSourceSheet = true }) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = "Source", tint = accentColor)
                }
                Button(
                    onClick = onTogglePlayPause,
                    enabled = state is PlayerState.Ready ||
                        state is PlayerState.Playing ||
                        state is PlayerState.Paused,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f)),
                ) {
                    Text(
                        if (state is PlayerState.Playing) "Pause" else "Play",
                        color = accentColor,
                    )
                }
                Button(
                    onClick = onStop,
                    enabled = state is PlayerState.Playing || state is PlayerState.Paused,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f)),
                ) {
                    Text("Stop", color = accentColor)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            DebugPanel(
                state = state,
                volume = volume,
                positionMs = positionMs,
                durationMs = durationMs,
                volumeFps = volumeFps,
                frameFps = frameFps,
                drawFps = drawFps,
                drawFpsMeter = drawFpsMeter,
                accentColor = accentColor,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            DebugEffectsPanel(
                intensity = intensity,
                onIntensityChange = onIntensityChange,
                thickness = thickness,
                onThicknessChange = onThicknessChange,
                glowSpread = glowSpread,
                onGlowSpreadChange = onGlowSpreadChange,
                blurRadius = blurRadius,
                onBlurRadiusChange = onBlurRadiusChange,
                relativeMotion = relativeMotion,
                onRelativeMotionChange = onRelativeMotionChange,
                layerFalloff = layerFalloff,
                onLayerFalloffChange = onLayerFalloffChange,
                colorPresets = colorPresets,
                colorIndex = colorIndex,
                onColorIndexChange = onColorIndexChange,
                accentColor = accentColor,
            )
        }
    }

    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            containerColor = Color(0xFF1A1F2E),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AssetSelector(
                    assetPaths = assetPaths,
                    assetIndex = assetIndex,
                    onAssetIndexChange = onAssetIndexChange,
                    accentColor = accentColor,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "band-energy",
                        color = Color(0xFFE5E7EB),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = useBandEnergy,
                        onCheckedChange = onUseBandEnergyChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor,
                            checkedTrackColor = accentColor.copy(alpha = 0.33f),
                            uncheckedThumbColor = Color(0xFFE5E7EB),
                            uncheckedTrackColor = Color(0x331F2937),
                        ),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileLayout(
    state: PlayerState,
    volume: Float,
    positionMs: Long,
    durationMs: Long,
    volumeFps: Float,
    frameFps: Float,
    drawFps: Float,
    drawFpsMeter: FpsMeter,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    thickness: Float,
    onThicknessChange: (Float) -> Unit,
    glowSpread: Float,
    onGlowSpreadChange: (Float) -> Unit,
    blurRadius: Float,
    onBlurRadiusChange: (Float) -> Unit,
    relativeMotion: Boolean,
    onRelativeMotionChange: (Boolean) -> Unit,
    layerFalloff: Float,
    onLayerFalloffChange: (Float) -> Unit,
    assetPaths: List<String>,
    assetIndex: Int,
    onAssetIndexChange: (Int) -> Unit,
    useBandEnergy: Boolean,
    onUseBandEnergyChange: (Boolean) -> Unit,
    colorPresets: List<RingColorPreset>,
    colorIndex: Int,
    onColorIndexChange: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
) {
    val accentColor = colorPresets[colorIndex].color
    var showSourceSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                volume
                drawFpsMeter.tick()
            }
            VoiceVisualizerRing(
                volume = volume,
                color = accentColor,
                intensity = intensity,
                thickness = thickness,
                glowSpread = glowSpread,
                blurRadius = blurRadius,
                relativeMotion = relativeMotion,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(16.dp))

        val volTxt = ((volume * 1000f).toInt() / 1000f).toString()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("pos: ${positionMs.toString().padStart(6)} ms", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
                Text("vol: $volTxt", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
                Text("dur: ${durationMs.toString().padStart(6)} ms", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp), horizontalAlignment = Alignment.End) {
                Text("fps vol:   ${volumeFps.fmt1()}", color = accentColor, fontFamily = FontFamily.Monospace)
                Text("fps frame: ${frameFps.fmt1()}", color = accentColor, fontFamily = FontFamily.Monospace)
                Text("fps draw:  ${drawFps.fmt1()}", color = accentColor, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { showSourceSheet = true }) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = "Source", tint = accentColor)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val playEnabled = state is PlayerState.Ready ||
                    state is PlayerState.Playing ||
                    state is PlayerState.Paused
                val stopEnabled = state is PlayerState.Playing || state is PlayerState.Paused

                IconButton(onClick = onTogglePlayPause, enabled = playEnabled) {
                    Icon(
                        if (state is PlayerState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state is PlayerState.Playing) "Pause" else "Play",
                        tint = if (playEnabled) accentColor else accentColor.copy(alpha = 0.38f),
                    )
                }
                IconButton(onClick = onStop, enabled = stopEnabled) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = "Stop",
                        tint = if (stopEnabled) accentColor else accentColor.copy(alpha = 0.38f),
                    )
                }
            }

            Row {
                IconButton(onClick = { showColorSheet = true }) {
                    Icon(Icons.Filled.Palette, contentDescription = "Color", tint = accentColor)
                }
                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Settings", tint = accentColor)
                }
            }
        }
    }

    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            containerColor = Color(0xFF1A1F2E),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AssetSelector(
                    assetPaths = assetPaths,
                    assetIndex = assetIndex,
                    onAssetIndexChange = onAssetIndexChange,
                    accentColor = accentColor,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "band-energy",
                        color = Color(0xFFE5E7EB),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = useBandEnergy,
                        onCheckedChange = onUseBandEnergyChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor,
                            checkedTrackColor = accentColor.copy(alpha = 0.33f),
                            uncheckedThumbColor = Color(0xFFE5E7EB),
                            uncheckedTrackColor = Color(0x331F2937),
                        ),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showColorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColorSheet = false },
            containerColor = Color(0xFF1A1F2E),
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("color", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(16.dp))
                ColorChipRow(
                    presets = colorPresets,
                    selectedIndex = colorIndex,
                    onSelect = { onColorIndexChange(it); showColorSheet = false },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color(0xFF1A1F2E),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                EffectSlider("intensity", intensity, 0f..3f, onIntensityChange, accentColor)
                Spacer(Modifier.height(6.dp))
                EffectSlider("thickness", thickness, 0f..30f, onThicknessChange, accentColor)
                Spacer(Modifier.height(6.dp))
                EffectSlider("glowSpread", glowSpread, 0f..3f, onGlowSpreadChange, accentColor)
                Spacer(Modifier.height(6.dp))
                EffectSlider("blurRadius", blurRadius, 2f..60f, onBlurRadiusChange, accentColor)
                Spacer(Modifier.height(6.dp))
                EffectSlider("layerFalloff", layerFalloff, 0f..1.0f, onLayerFalloffChange, accentColor)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "relativeMotion",
                        color = Color(0xFFE5E7EB),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = relativeMotion,
                        onCheckedChange = onRelativeMotionChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentColor,
                            checkedTrackColor = accentColor.copy(alpha = 0.33f),
                            uncheckedThumbColor = Color(0xFFE5E7EB),
                            uncheckedTrackColor = Color(0x331F2937),
                        ),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ColorChipRow(
    presets: List<RingColorPreset>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEachIndexed { idx, preset ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .then(
                        if (selected) Modifier.border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        else Modifier
                    )
                    .padding(if (selected) 3.dp else 0.dp)
                    .clip(CircleShape)
                    .background(preset.color)
                    .clickable { onSelect(idx) },
            )
        }
    }
}

@Composable
private fun DebugPanel(
    state: PlayerState,
    volume: Float,
    positionMs: Long,
    durationMs: Long,
    volumeFps: Float,
    frameFps: Float,
    drawFps: Float,
    drawFpsMeter: FpsMeter,
    accentColor: Color,
) {
    Column(
        modifier = Modifier.width(260.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text("state: ${state.label()}", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text("pos:  ${positionMs.toString().padStart(6)} ms", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
        Text("dur:  ${durationMs.toString().padStart(6)} ms", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        val volTxt = ((volume * 1000f).toInt() / 1000f).toString()
        Text("vol:  $volTxt", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text("fps volume: ${volumeFps.fmt1()}", color = accentColor, fontFamily = FontFamily.Monospace)
        Text("fps frame:  ${frameFps.fmt1()}", color = accentColor, fontFamily = FontFamily.Monospace)
        Text("fps draw:   ${drawFps.fmt1()}", color = accentColor, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color(0x331F2937)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(volume.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(accentColor),
            )
        }
        Spacer(Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color(0x331F2937)),
        ) {
            val v = volume.coerceIn(0f, 1f)
            drawFpsMeter.tick()
            drawCircle(
                color = accentColor,
                radius = 4f + v * 8f,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
    }
}

@Composable
private fun AssetSelector(
    assetPaths: List<String>,
    assetIndex: Int,
    onAssetIndexChange: (Int) -> Unit,
    accentColor: Color,
) {
    Column(horizontalAlignment = Alignment.Start) {
        assetPaths.forEachIndexed { idx, path ->
            val selected = idx == assetIndex
            val label = path.substringAfterLast('/')
            Button(
                onClick = { if (!selected) onAssetIndexChange(idx) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) accentColor else Color(0x331F2937),
                    contentColor = if (selected) Color(0xFF0A0F1C) else Color(0xFFE5E7EB),
                ),
            ) {
                Text(label, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DebugEffectsPanel(
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    thickness: Float,
    onThicknessChange: (Float) -> Unit,
    glowSpread: Float,
    onGlowSpreadChange: (Float) -> Unit,
    blurRadius: Float,
    onBlurRadiusChange: (Float) -> Unit,
    relativeMotion: Boolean,
    onRelativeMotionChange: (Boolean) -> Unit,
    layerFalloff: Float,
    onLayerFalloffChange: (Float) -> Unit,
    colorPresets: List<RingColorPreset>,
    colorIndex: Int,
    onColorIndexChange: (Int) -> Unit,
    accentColor: Color,
) {
    Column(
        modifier = Modifier.width(260.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        EffectSlider("intensity", intensity, 0f..3f, onIntensityChange, accentColor)
        Spacer(Modifier.height(6.dp))
        EffectSlider("thickness", thickness, 0f..30f, onThicknessChange, accentColor)
        Spacer(Modifier.height(6.dp))
        EffectSlider("glowSpread", glowSpread, 0f..3f, onGlowSpreadChange, accentColor)
        Spacer(Modifier.height(6.dp))
        EffectSlider("blurRadius", blurRadius, 2f..60f, onBlurRadiusChange, accentColor)
        Spacer(Modifier.height(6.dp))
        EffectSlider("layerFalloff", layerFalloff, 0f..1.0f, onLayerFalloffChange, accentColor)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "relativeMotion",
                color = Color(0xFFE5E7EB),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = relativeMotion,
                onCheckedChange = onRelativeMotionChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.33f),
                    uncheckedThumbColor = Color(0xFFE5E7EB),
                    uncheckedTrackColor = Color(0x331F2937),
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        ColorChipRow(
            presets = colorPresets,
            selectedIndex = colorIndex,
            onSelect = onColorIndexChange,
        )
    }
}

@Composable
private fun EffectSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
) {
    val valTxt = ((value * 100f).toInt() / 100f).toString()
    Text("$label: $valTxt", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = accentColor,
            activeTrackColor = accentColor,
            inactiveTrackColor = Color(0x331F2937),
        ),
    )
}

private fun Float.fmt1(): String {
    val x = (this * 10f).toInt() / 10f
    return x.toString()
}

private fun PlayerState.label(): String = when (this) {
    PlayerState.Idle -> "Idle"
    PlayerState.Loading -> "Loading"
    PlayerState.Ready -> "Ready"
    PlayerState.Playing -> "Playing"
    PlayerState.Paused -> "Paused"
    is PlayerState.Error -> "Error(${this.msg})"
}
