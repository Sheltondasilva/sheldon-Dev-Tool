package com.example.ui.hardware

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeviceTestScreen(
    viewModel: DeviceTestViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val camPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissions()
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissions()
        if (it) viewModel.startMicTest()
    }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("device_test_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Overview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Hardware Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Test cameras, audio, mic, flashlight, charger & touch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 2. Flashlight (Torch) Test
        item {
            FlashlightTestCard(
                isTorchOn = state.isTorchOn,
                isStrobe = state.isTorchStrobeActive,
                hasTorch = state.hasTorch,
                onToggleTorch = { viewModel.toggleFlashlight() },
                onToggleStrobe = { viewModel.toggleStrobeFlashlight() }
            )
        }

        // 3. Charger & Battery Hardware Test
        item {
            ChargerTestCard(batteryInfo = state.batteryInfo)
        }

        // 4. Audio (Speaker / Output) Test
        item {
            AudioTestCard(
                isPlaying = state.isAudioTonePlaying,
                currentFreq = state.audioFrequencyHz,
                onPlayTone = { freq -> viewModel.playAudioTestTone(freq) },
                onStop = { viewModel.stopAudioTone() }
            )
        }

        // 5. Microphone (Audio Input) Test
        item {
            MicrophoneTestCard(
                hasPermission = state.hasMicPermission,
                isRecording = state.isRecordingMic,
                amplitude = state.micAmplitudeLevel,
                decibels = state.micDecibels,
                onStart = {
                    if (state.hasMicPermission) {
                        viewModel.startMicTest()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStop = { viewModel.stopMicTest() }
            )
        }

        // 6. Camera Hardware Test
        item {
            CameraTestCard(
                hasPermission = state.hasCameraPermission,
                cameras = state.cameras,
                onRequestPermission = { camPermissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        }

        // 7. Touch Screen & Digitizer Test
        item {
            TouchScreenTestCard()
        }
    }
}

@Composable
private fun FlashlightTestCard(
    isTorchOn: Boolean,
    isStrobe: Boolean,
    hasTorch: Boolean,
    onToggleTorch: () -> Unit,
    onToggleStrobe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FlashlightOn,
                        contentDescription = null,
                        tint = if (isTorchOn) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Flashlight & Torch Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isTorchOn) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        if (isTorchOn) (if (isStrobe) "STROBE" else "ACTIVE") else "OFF",
                        color = if (isTorchOn) Color(0xFFFFD700) else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text("Test camera LED flash unit and strobe pulse generator:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleTorch,
                    enabled = hasTorch,
                    colors = if (isTorchOn && !isStrobe) ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)) else ButtonDefaults.buttonColors(),
                    modifier = Modifier.weight(1f).testTag("btn_toggle_flashlight")
                ) {
                    Icon(Icons.Default.FlashlightOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isTorchOn && !isStrobe) "Turn Off" else "Toggle Torch")
                }

                OutlinedButton(
                    onClick = onToggleStrobe,
                    enabled = hasTorch,
                    colors = if (isStrobe) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFF5722).copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier.weight(1f).testTag("btn_strobe_flashlight")
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isStrobe) "Stop Strobe" else "SOS Strobe")
                }
            }
        }
    }
}

@Composable
private fun ChargerTestCard(batteryInfo: BatteryHardwareStatus?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Power,
                        contentDescription = null,
                        tint = if (batteryInfo?.isPlugged == true) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Charger & Power Port", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (batteryInfo?.isPlugged == true) Color(0xFF00FF66).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        if (batteryInfo?.isPlugged == true) "PLUGGED" else "UNPLUGGED",
                        color = if (batteryInfo?.isPlugged == true) Color(0xFF00FF66) else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            batteryInfo?.let {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HardwareDetailRow("Connection Type", it.plugType)
                    HardwareDetailRow("Charging State", it.status)
                    HardwareDetailRow("Battery Level", "${it.level}%")
                    HardwareDetailRow("Voltage Output", "${it.voltageMv} mV")
                    HardwareDetailRow("Thermal State", "${it.temperatureC}°C")
                    HardwareDetailRow("Cell Health", it.health)
                }
            }
        }
    }
}

@Composable
private fun AudioTestCard(
    isPlaying: Boolean,
    currentFreq: Int,
    onPlayTone: (Int) -> Unit,
    onStop: () -> Unit
) {
    val frequencies = listOf(
        Pair("440 Hz (Low)", 440),
        Pair("880 Hz (Mid)", 880),
        Pair("1000 Hz (High)", 1000)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Audio Speaker Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (isPlaying) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                        Text("PLAYING", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }

            Text("Synthesizes pure PCM stereo diagnostic frequencies to verify speaker hardware:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                frequencies.forEach { (label, freq) ->
                    val isThisFreq = isPlaying && currentFreq == freq
                    Button(
                        onClick = { onPlayTone(freq) },
                        modifier = Modifier.weight(1f).testTag("play_tone_$freq"),
                        colors = if (isThisFreq) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary) else ButtonDefaults.buttonColors(),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }

            if (isPlaying) {
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth().testTag("stop_audio_tone")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Stop Audio Playback")
                }
            }
        }
    }
}

@Composable
private fun MicrophoneTestCard(
    hasPermission: Boolean,
    isRecording: Boolean,
    amplitude: Float,
    decibels: Int,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Microphone Input Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (isRecording) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)) {
                        Text("$decibels dB", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }

            Text("Captures live sound input and monitors acoustic amplitude levels:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Live visualizer bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { amplitude },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(CircleShape),
                    color = if (amplitude > 0.7f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quiet (0 dB)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Active Speech", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Loud (90+ dB)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Button(
                onClick = if (isRecording) onStop else onStart,
                colors = if (isRecording) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                modifier = Modifier.fillMaxWidth().testTag("toggle_mic_test")
            ) {
                Icon(if (isRecording) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isRecording) "Stop Microphone Test" else "Start Microphone Test")
            }
        }
    }
}

@Composable
private fun CameraTestCard(
    hasPermission: Boolean,
    cameras: List<CameraHardwareInfo>,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera Sensor Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (hasPermission) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        if (hasPermission) "PERMITTED" else "PERMISSION NEEDED",
                        color = if (hasPermission) MaterialTheme.colorScheme.primary else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (!hasPermission) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth().testTag("request_camera_permission")
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Camera Permission")
                }
            }

            if (cameras.isEmpty()) {
                Text("Probing camera sensors on device...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                cameras.forEach { cam ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Camera ID ${cam.id}: ${cam.facing}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (cam.hasFlash) {
                                    Text("LED Flash Available", color = Color(0xFFFFB300), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Text("Sensor orientation: ${cam.orientation}° • ${cam.maxResolution}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TouchScreenTestCard() {
    val totalRows = 6
    val totalCols = 8
    val touchedCells = remember { mutableStateMapOf<Pair<Int, Int>, Boolean>() }
    var touchCount by remember { mutableStateOf(0) }
    var currentTouchPos by remember { mutableStateOf("Touch anywhere in the grid") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Touch Screen & Digitizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = { touchedCells.clear(); touchCount = 0; currentTouchPos = "Grid Reset" },
                    modifier = Modifier.testTag("reset_touch_grid")
                ) {
                    Text("Reset Grid", fontSize = 12.sp)
                }
            }

            Text("Drag your finger across all tiles to verify digitizer touch accuracy & dead zones:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Touch Screen Matrix Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F141C))
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val cellWidth = size.width / totalCols
                            val cellHeight = size.height / totalRows
                            val col = (change.position.x / cellWidth).toInt().coerceIn(0, totalCols - 1)
                            val row = (change.position.y / cellHeight).toInt().coerceIn(0, totalRows - 1)
                            touchedCells[Pair(col, row)] = true
                            touchCount = touchedCells.size
                            currentTouchPos = "X: ${change.position.x.toInt()}px, Y: ${change.position.y.toInt()}px"
                            change.consume()
                        }
                    }
                    .testTag("touch_screen_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellW = size.width / totalCols
                    val cellH = size.height / totalRows

                    for (r in 0 until totalRows) {
                        for (c in 0 until totalCols) {
                            val isFilled = touchedCells[Pair(c, r)] == true
                            val cellColor = if (isFilled) Color(0xFF00FF66).copy(alpha = 0.85f) else Color(0xFF1E2638)
                            drawRect(
                                color = cellColor,
                                topLeft = Offset(c * cellW + 2f, r * cellH + 2f),
                                size = Size(cellW - 4f, cellH - 4f)
                            )
                        }
                    }
                }
            }

            // Touch stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Coverage: ${(touchCount * 100 / (totalRows * totalCols))}% (${touchCount}/${totalRows * totalCols})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (touchCount == totalRows * totalCols) Color(0xFF00FF66) else MaterialTheme.colorScheme.primary
                )
                Text(
                    currentTouchPos,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
private fun HardwareDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
