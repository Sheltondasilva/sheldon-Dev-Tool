package com.example.ui.security

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScanRecordEntity
import com.example.model.ThreatCategory
import com.example.model.ThreatItem
import com.example.model.ThreatSeverity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scanHistory by viewModel.scanHistory.collectAsState()
    val context = LocalContext.current
    var showTestThreatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.testThreatMessage) {
        state.testThreatMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearNotificationMessage()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("security_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Security Score & Overview Card
        item {
            SecurityScoreCard(
                score = state.securityScore,
                threatsCount = state.detectedThreats.size,
                isScanning = state.scanProgress.isScanning,
                onStartQuickScan = { viewModel.startScan(deepScan = false) },
                onStartDeepScan = { viewModel.startScan(deepScan = true) },
                onOpenTestThreatDialog = { showTestThreatDialog = true }
            )
        }

        // 2. Active Scan Progress Radar (Visible when scanning)
        if (state.scanProgress.isScanning) {
            item {
                ScanProgressCard(
                    progress = state.scanProgress.progress,
                    currentItem = state.scanProgress.currentItem,
                    scannedCount = state.scanProgress.scannedFilesCount,
                    threatsFound = state.scanProgress.threatsFoundCount
                )
            }
        }

        // 3. Clean Result Toast / Banner
        state.lastCleanResult?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "System Remediated Successfully",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Removed ${result.removedCount} threat files • Reclaimed ${result.bytesReclaimed / 1024} KB storage",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // 4. Detected Threats Section & Clean Button
        if (state.detectedThreats.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${state.detectedThreats.size} Threats Detected!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Button(
                                onClick = { viewModel.cleanAllThreats() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                enabled = !state.isCleaning,
                                modifier = Modifier.testTag("btn_clean_threats")
                            ) {
                                if (state.isCleaning) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cleaning...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Clean All", fontSize = 12.sp)
                                }
                            }
                        }
                        Text(
                            "Immediate action recommended: clean or isolate malicious scripts and files.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            items(state.detectedThreats, key = { it.id }) { threat ->
                ThreatItemCard(threat = threat)
            }
        }

        // 5. Scan History Section
        item {
            ScanHistorySection(history = scanHistory)
        }
    }

    // Safe Test Threat Creator Dialog
    if (showTestThreatDialog) {
        AlertDialog(
            onDismissRequest = { showTestThreatDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Test Threat Sample")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Create a non-destructive test file in storage to test real antivirus signature detection & cleaning:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.generateTestThreat("eicar")
                            showTestThreatDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("generate_eicar_test")
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("EICAR Antivirus Test Signature", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Standard AV verification signature (safe)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.generateTestThreat("miner")
                            showTestThreatDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("generate_miner_test")
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Crypto Miner Config Signature", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Simulated Stratum mining pool configuration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.generateTestThreat("script")
                            showTestThreatDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("generate_backdoor_test")
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Unauthorized Reverse Shell Script", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Sample script containing suspicious payload", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTestThreatDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SecurityScoreCard(
    score: Int,
    threatsCount: Int,
    isScanning: Boolean,
    onStartQuickScan: () -> Unit,
    onStartDeepScan: () -> Unit,
    onOpenTestThreatDialog: () -> Unit
) {
    val scoreColor = when {
        score >= 90 -> MaterialTheme.colorScheme.primary
        score >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Storage Threat Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Malware heuristics & EICAR verification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenTestThreatDialog) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = "Test Threat Generator",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Radial Health Meter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    color = scoreColor,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$score%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = scoreColor
                    )
                    Text(
                        if (threatsCount == 0) "Secure" else "At Risk",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartQuickScan,
                    enabled = !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_scan_now")
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Storage")
                }

                OutlinedButton(
                    onClick = onStartDeepScan,
                    enabled = !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_deep_scan")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Deep Scan")
                }
            }

            TextButton(
                onClick = onOpenTestThreatDialog,
                modifier = Modifier.fillMaxWidth().testTag("open_test_threat_dialog")
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Generate Test Threat File (Verify Antivirus)", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ScanProgressCard(
    progress: Float,
    currentItem: String,
    scannedCount: Int,
    threatsFound: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Scanning File System...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "$scannedCount files inspected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                "Inspecting: $currentItem",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (threatsFound > 0) {
                Text(
                    "⚠️ $threatsFound suspicious items flagged",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ThreatItemCard(threat: ThreatItem) {
    val badgeColor = when (threat.severity) {
        ThreatSeverity.CRITICAL -> MaterialTheme.colorScheme.error
        ThreatSeverity.HIGH -> Color(0xFFFF9800)
        ThreatSeverity.MEDIUM -> Color(0xFFFFC107)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    threat.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        threat.severity.label,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                threat.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Path: ${threat.filePath}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${threat.fileSize} B",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ScanHistorySection(history: List<ScanRecordEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Scan Audit History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (history.isEmpty()) {
                Text(
                    "No scan records yet. Run your first storage scan above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                history.take(5).forEach { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                record.scanMode,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${record.totalFilesScanned} items inspected • ${dateFormat.format(Date(record.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (record.threatsCleaned > 0) {
                            Text(
                                "Cleaned ${record.threatsCleaned}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (record.threatsFound > 0) {
                            Text(
                                "${record.threatsFound} Threats",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Safe",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }
}
