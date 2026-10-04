package com.example.ui.monitor

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PerformanceMonitorScreen(
    viewModel: PerformanceMonitorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedChartTab by remember { mutableStateOf(0) } // 0: CPU, 1: RAM, 2: Battery

    LaunchedEffect(state.memoryFreedMessage) {
        state.memoryFreedMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.exportMessage) {
        state.exportMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("performance_monitor_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Monitor Control Toolbar
        item {
            MonitorControlCard(
                isMonitoring = state.isMonitoring,
                intervalMs = state.updateIntervalMs,
                isExporting = state.isExporting,
                onToggleMonitor = { viewModel.toggleMonitoring() },
                onIntervalChange = { viewModel.setUpdateInterval(it) },
                onOptimizeRam = { viewModel.optimizeMemory() },
                onExportCsv = { viewModel.exportLogsAsCsv() }
            )
        }

        // 2. Primary Metrics Row: CPU & RAM Gauges
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CPU Gauge Card
                MetricGaugeCard(
                    title = "CPU Utilization",
                    value = "${state.metrics.cpuUsagePct}%",
                    progress = state.metrics.cpuUsagePct / 100f,
                    detail1 = "${state.metrics.cpuCoresCount} Physical Cores",
                    detail2 = "${state.metrics.activeThreads} Active Threads",
                    accentColor = Color(0xFF00F0FF),
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f)
                )

                // RAM Gauge Card
                MetricGaugeCard(
                    title = "RAM Memory",
                    value = "${state.metrics.ramUsagePct}%",
                    progress = state.metrics.ramUsagePct / 100f,
                    detail1 = "${state.metrics.ramUsedMb} / ${state.metrics.ramTotalMb} MB",
                    detail2 = "Heap: ${state.metrics.jvmHeapUsedMb} MB",
                    accentColor = Color(0xFFBD93F9),
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Secondary Metrics Row: Battery & Storage / Network
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Battery Card
                MetricGaugeCard(
                    title = "Battery & Thermal",
                    value = "${state.metrics.batteryPct}%",
                    progress = state.metrics.batteryPct / 100f,
                    detail1 = "${state.metrics.batteryTempC}°C • ${state.metrics.batteryVoltageMv} mV",
                    detail2 = state.metrics.batteryStatus,
                    accentColor = if (state.metrics.batteryTempC > 40f) Color(0xFFFF5252) else Color(0xFF00FF66),
                    icon = Icons.Default.BatteryChargingFull,
                    modifier = Modifier.weight(1f)
                )

                // Storage & Network Card
                MetricGaugeCard(
                    title = "Storage & Net I/O",
                    value = "${state.metrics.storageUsagePct}%",
                    progress = state.metrics.storageUsagePct / 100f,
                    detail1 = "Free: ${state.metrics.storageFreeGb.toInt()} GB",
                    detail2 = "↓ ${(state.metrics.rxSpeedKbps).toInt()} • ↑ ${(state.metrics.txSpeedKbps).toInt()} KB/s",
                    accentColor = Color(0xFFFFD866),
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Live Real-Time Telemetry Graph
        item {
            RealtimeWaveformCard(
                selectedTab = selectedChartTab,
                onTabSelect = { selectedChartTab = it },
                cpuHistory = state.metrics.cpuHistory,
                ramHistory = state.metrics.ramHistory,
                batteryHistory = state.metrics.batteryHistory,
                currentCpu = state.metrics.cpuUsagePct,
                currentRam = state.metrics.ramUsagePct,
                currentTemp = state.metrics.batteryTempC
            )
        }

        // 5. CSV Export & Logging Card
        item {
            ExportLogsCard(
                recordedCount = state.recordedLogsCount,
                isExporting = state.isExporting,
                onExportCsv = { viewModel.exportLogsAsCsv() }
            )
        }

        // 6. Multi-Core CPU Spectrum
        item {
            CpuCoresBreakdownCard(
                cores = state.metrics.coreLoads
            )
        }

        // 7. Memory Optimizer & Trim Action
        item {
            MemoryOptimizerCard(
                jvmUsedMb = state.metrics.jvmHeapUsedMb,
                jvmTotalMb = state.metrics.jvmHeapTotalMb,
                availRamMb = state.metrics.ramAvailMb,
                isLowMemory = state.metrics.isLowMemory,
                onOptimize = { viewModel.optimizeMemory() }
            )
        }
    }
}

@Composable
private fun MonitorControlCard(
    isMonitoring: Boolean,
    intervalMs: Long,
    isExporting: Boolean,
    onToggleMonitor: () -> Unit,
    onIntervalChange: (Long) -> Unit,
    onOptimizeRam: () -> Unit,
    onExportCsv: () -> Unit
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isMonitoring) Color(0xFF00FF66) else Color.Gray)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isMonitoring) "Live Telemetry Active" else "Telemetry Paused",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onExportCsv,
                        enabled = !isExporting,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_export_csv_quick")
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV", fontSize = 11.sp)
                        }
                    }

                    FilledTonalButton(
                        onClick = onOptimizeRam,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_trim_ram")
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Trim", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onToggleMonitor,
                        modifier = Modifier.size(32.dp).testTag("btn_toggle_monitor")
                    ) {
                        Icon(
                            if (isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Monitor",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Interval selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sample Rate:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Pair("500ms", 500L), Pair("1.0s", 1000L), Pair("2.0s", 2000L)).forEach { (label, ms) ->
                        FilterChip(
                            selected = intervalMs == ms,
                            onClick = { onIntervalChange(ms) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportLogsCard(
    recordedCount: Int,
    isExporting: Boolean,
    onExportCsv: () -> Unit
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
                        Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Export Performance Logs (CSV)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        "$recordedCount Records",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                "Export complete historical performance snapshots (CPU utilization, core load, RAM, JVM heap, battery thermal/voltage, and network bandwidth) as a standard RFC 4180 CSV file saved to your device's Downloads directory and shared with your preferred spreadsheet app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("CSV Spreadsheet", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Downloads/DevTools", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
            }

            Button(
                onClick = onExportCsv,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_export_performance_csv")
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generating & Exporting CSV...")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export CSV File to Device Storage")
                }
            }
        }
    }
}

@Composable
private fun MetricGaugeCard(
    title: String,
    value: String,
    progress: Float,
    detail1: String,
    detail2: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }

            // Circular Radial Progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 6.dp,
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Text(
                        value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(detail1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(detail2, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RealtimeWaveformCard(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    cpuHistory: List<Float>,
    ramHistory: List<Float>,
    batteryHistory: List<Float>,
    currentCpu: Int,
    currentRam: Int,
    currentTemp: Float
) {
    val currentData = when (selectedTab) {
        0 -> cpuHistory
        1 -> ramHistory
        else -> batteryHistory
    }

    val (accentColor, label, curVal) = when (selectedTab) {
        0 -> Triple(Color(0xFF00F0FF), "CPU Utilization (%)", "$currentCpu%")
        1 -> Triple(Color(0xFFBD93F9), "RAM Memory (%)", "$currentRam%")
        else -> Triple(Color(0xFF00FF66), "Battery Temp (°C)", "${currentTemp}°C")
    }

    val maxVal = if (selectedTab == 2) 50f else 100f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chart Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Performance Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        curVal,
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelect(0) },
                    text = { Text("CPU", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelect(1) },
                    text = { Text("RAM", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { onTabSelect(2) },
                    text = { Text("Thermal", fontSize = 12.sp) }
                )
            }

            // Real-Time Waveform Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B0E14))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                    val w = size.width
                    val h = size.height

                    // Draw subtle grid lines
                    val gridPaintColor = Color(0xFF1B2332)
                    for (i in 1..3) {
                        val y = h * (i / 4f)
                        drawLine(
                            color = gridPaintColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    if (currentData.isNotEmpty()) {
                        val stepX = w / (currentData.size - 1).coerceAtLeast(1)
                        val path = Path()
                        val fillPath = Path()

                        for (i in currentData.indices) {
                            val v = currentData[i].coerceIn(0f, maxVal)
                            val x = i * stepX
                            val y = h - (v / maxVal * h)

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }

                        fillPath.lineTo(w, h)
                        fillPath.close()

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )

                        // Glowing line
                        drawPath(
                            path = path,
                            color = accentColor,
                            style = Stroke(width = 3.5f)
                        )

                        // Last point circle
                        val lastV = currentData.last().coerceIn(0f, maxVal)
                        val lastY = h - (lastV / maxVal * h)
                        drawCircle(
                            color = Color.White,
                            radius = 4.5f,
                            center = Offset(w, lastY)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("← Past 30 Seconds", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(label, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.SemiBold)
                Text("Now", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun CpuCoresBreakdownCard(cores: List<CoreLoad>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Multi-Core CPU Frequency & Load", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${cores.size} Cores", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cores.forEach { core ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Core ${core.coreIndex}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(54.dp)
                        )

                        LinearProgressIndicator(
                            progress = { core.loadPct / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape),
                            color = when {
                                core.loadPct > 80 -> Color(0xFFFF5252)
                                core.loadPct > 50 -> Color(0xFFFFD866)
                                else -> Color(0xFF00F0FF)
                            },
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Text(
                            "${core.loadPct}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryOptimizerCard(
    jvmUsedMb: Long,
    jvmTotalMb: Long,
    availRamMb: Long,
    isLowMemory: Boolean,
    onOptimize: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Memory Management & Trimmer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (isLowMemory) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)) {
                        Text("LOW MEMORY", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Text(
                "App JVM Heap: $jvmUsedMb MB allocated / $jvmTotalMb MB peak • Available RAM: $availRamMb MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onOptimize,
                modifier = Modifier.fillMaxWidth().testTag("optimize_memory_button")
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Run Garbage Collector & Trim Heap")
            }
        }
    }
}
