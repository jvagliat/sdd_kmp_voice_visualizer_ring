package com.iattraxia.kmp_voice_ring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

private const val ASSET_PATH = "files/audio/Jan_Morgenstern_-_01_-_Prelude.wav"

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

        LaunchedEffect(Unit) { viewModel.load(ASSET_PATH) }
        LaunchedEffect(Unit) {
            while (true) withFrameMillis { frameFpsMeter.tick() }
        }
        DisposableEffect(Unit) { onDispose { viewModel.dispose() } }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1C)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    VoiceVisualizerRing(
                        volume = volume,
                        color = Color(0xFF00FFFF),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.togglePlayPause() },
                        enabled = state is PlayerState.Ready ||
                            state is PlayerState.Playing ||
                            state is PlayerState.Paused,
                    ) {
                        Text(if (state is PlayerState.Playing) "Pause" else "Play")
                    }
                    Button(
                        onClick = { viewModel.stop() },
                        enabled = state is PlayerState.Playing || state is PlayerState.Paused,
                    ) {
                        Text("Stop")
                    }
                }

                Box(
                    modifier = Modifier
                        .width(520.dp)
                        .wrapContentHeight()
                        .background(Color(0xFF111827)),
                    contentAlignment = Alignment.Center,
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
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        val volTxt = ((volume * 1000f).toInt() / 1000f).toString()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "state: ${state.label()}",
                    color = Color(0xFFE5E7EB),
                    fontFamily = FontFamily.Monospace,
                )
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
                Text(
                    "vol:  $volTxt",
                    color = Color(0xFFE5E7EB),
                    fontFamily = FontFamily.Monospace,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
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
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color(0xFF1F2937)),
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
                .background(Color(0xFF1F2937)),
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
