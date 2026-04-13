package com.iattraxia.kmp_voice_ring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
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

        var assetIndex by remember { mutableStateOf(0) }
        var useBandEnergy by remember { mutableStateOf(DEFAULT_USE_BAND_ENERGY) }

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
                .background(Color(0xFF0A0F1C)),
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
                    assetPaths = ASSET_PATHS,
                    assetIndex = assetIndex,
                    onAssetIndexChange = { assetIndex = it },
                    useBandEnergy = useBandEnergy,
                    onUseBandEnergyChange = { useBandEnergy = it },
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
                    assetPaths = ASSET_PATHS,
                    assetIndex = assetIndex,
                    onAssetIndexChange = { assetIndex = it },
                    useBandEnergy = useBandEnergy,
                    onUseBandEnergyChange = { useBandEnergy = it },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onStop = { viewModel.stop() },
                )
            }
        }
    }
}

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
    assetPaths: List<String>,
    assetIndex: Int,
    onAssetIndexChange: (Int) -> Unit,
    useBandEnergy: Boolean,
    onUseBandEnergyChange: (Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
) {
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
                    color = Color(0xFF00FFFF),
                    intensity = intensity,
                    thickness = thickness,
                    glowSpread = glowSpread,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onTogglePlayPause,
                    enabled = state is PlayerState.Ready ||
                        state is PlayerState.Playing ||
                        state is PlayerState.Paused,
                ) {
                    Text(if (state is PlayerState.Playing) "Pause" else "Play")
                }
                Button(
                    onClick = onStop,
                    enabled = state is PlayerState.Playing || state is PlayerState.Paused,
                ) {
                    Text("Stop")
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
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
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            DebugEffectsPanel(
                intensity = intensity,
                onIntensityChange = onIntensityChange,
                thickness = thickness,
                onThicknessChange = onThicknessChange,
                glowSpread = glowSpread,
                onGlowSpreadChange = onGlowSpreadChange,
                assetPaths = assetPaths,
                assetIndex = assetIndex,
                onAssetIndexChange = onAssetIndexChange,
                useBandEnergy = useBandEnergy,
                onUseBandEnergyChange = onUseBandEnergyChange,
            )
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
    assetPaths: List<String>,
    assetIndex: Int,
    onAssetIndexChange: (Int) -> Unit,
    useBandEnergy: Boolean,
    onUseBandEnergyChange: (Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
) {
    var showSourceSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    color = Color(0xFF00FFFF),
                    intensity = intensity,
                    thickness = thickness,
                    glowSpread = glowSpread,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val volTxt = ((volume * 1000f).toInt() / 1000f).toString()

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "pos: ${positionMs.toString().padStart(6)} ms",
                        color = Color(0xFFE5E7EB),
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        "dur: ${durationMs.toString().padStart(6)} ms",
                        color = Color(0xFFE5E7EB),
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("vol: $volTxt", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
                    Text("fps vol: ${volumeFps.fmt1()}", color = Color(0xFF22D3EE), fontFamily = FontFamily.Monospace)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("fps frame: ${frameFps.fmt1()}", color = Color(0xFF22D3EE), fontFamily = FontFamily.Monospace)
                    Text("fps draw: ${drawFps.fmt1()}", color = Color(0xFF22D3EE), fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showSourceSheet = true }) {
                    Icon(
                        Icons.Filled.LibraryMusic,
                        contentDescription = "Source",
                        tint = Color(0xFF22D3EE),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val playEnabled = state is PlayerState.Ready ||
                        state is PlayerState.Playing ||
                        state is PlayerState.Paused
                    val stopEnabled = state is PlayerState.Playing || state is PlayerState.Paused

                    IconButton(
                        onClick = onTogglePlayPause,
                        enabled = playEnabled,
                    ) {
                        Icon(
                            if (state is PlayerState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state is PlayerState.Playing) "Pause" else "Play",
                            tint = if (playEnabled) Color(0xFF22D3EE) else Color(0xFF22D3EE).copy(alpha = 0.38f),
                        )
                    }
                    IconButton(
                        onClick = onStop,
                        enabled = stopEnabled,
                    ) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = "Stop",
                            tint = if (stopEnabled) Color(0xFF22D3EE) else Color(0xFF22D3EE).copy(alpha = 0.38f),
                        )
                    }
                }

                IconButton(onClick = { showSettingsSheet = true }) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Settings",
                        tint = Color(0xFF22D3EE),
                    )
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
                            checkedThumbColor = Color(0xFF22D3EE),
                            checkedTrackColor = Color(0x5522D3EE),
                            uncheckedThumbColor = Color(0xFFE5E7EB),
                            uncheckedTrackColor = Color(0x331F2937),
                        ),
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color(0xFF1A1F2E),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                EffectSlider("intensity", intensity, 0f..3f, onIntensityChange)
                Spacer(Modifier.height(6.dp))
                EffectSlider("thickness", thickness, 0f..30f, onThicknessChange)
                Spacer(Modifier.height(6.dp))
                EffectSlider("glowSpread", glowSpread, 0f..3f, onGlowSpreadChange)
            }
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
) {
    Column(
        modifier = Modifier.width(260.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text("state: ${state.label()}", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text(
            "pos:  ${positionMs.toString().padStart(6)} ms",
            color = Color(0xFFE5E7EB),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "dur:  ${durationMs.toString().padStart(6)} ms",
            color = Color(0xFFE5E7EB),
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(6.dp))
        val volTxt = ((volume * 1000f).toInt() / 1000f).toString()
        Text("vol:  $volTxt", color = Color(0xFFE5E7EB), fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(6.dp))
        Text(
            "fps volume: ${volumeFps.fmt1()}",
            color = Color(0xFF22D3EE),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "fps frame:  ${frameFps.fmt1()}",
            color = Color(0xFF22D3EE),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "fps draw:   ${drawFps.fmt1()}",
            color = Color(0xFF22D3EE),
            fontFamily = FontFamily.Monospace,
        )
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
                    .background(Color(0xFF22D3EE)),
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
                color = Color(0xFF22D3EE),
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
) {
    Column(horizontalAlignment = Alignment.Start) {
        assetPaths.forEachIndexed { idx, path ->
            val selected = idx == assetIndex
            val label = path.substringAfterLast('/')
            Button(
                onClick = { if (!selected) onAssetIndexChange(idx) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) Color(0xFF22D3EE) else Color(0x331F2937),
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
    assetPaths: List<String>,
    assetIndex: Int,
    onAssetIndexChange: (Int) -> Unit,
    useBandEnergy: Boolean,
    onUseBandEnergyChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.width(260.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        AssetSelector(
            assetPaths = assetPaths,
            assetIndex = assetIndex,
            onAssetIndexChange = onAssetIndexChange,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    checkedThumbColor = Color(0xFF22D3EE),
                    checkedTrackColor = Color(0x5522D3EE),
                    uncheckedThumbColor = Color(0xFFE5E7EB),
                    uncheckedTrackColor = Color(0x331F2937),
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        EffectSlider(
            label = "intensity",
            value = intensity,
            valueRange = 0f..3f,
            onValueChange = onIntensityChange,
        )
        Spacer(Modifier.height(6.dp))
        EffectSlider(
            label = "thickness",
            value = thickness,
            valueRange = 0f..30f,
            onValueChange = onThicknessChange,
        )
        Spacer(Modifier.height(6.dp))
        EffectSlider(
            label = "glowSpread",
            value = glowSpread,
            valueRange = 0f..3f,
            onValueChange = onGlowSpreadChange,
        )
    }
}

@Composable
private fun EffectSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    val valTxt = ((value * 100f).toInt() / 100f).toString()
    Text(
        "$label: $valTxt",
        color = Color(0xFFE5E7EB),
        fontFamily = FontFamily.Monospace,
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = Color(0xFF22D3EE),
            activeTrackColor = Color(0xFF22D3EE),
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
