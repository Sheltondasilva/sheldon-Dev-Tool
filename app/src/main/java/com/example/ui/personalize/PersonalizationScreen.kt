package com.example.ui.personalize

import android.app.WallpaperManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThemePreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PersonalizationScreen(
    viewModel: PersonalizationViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showWallpaperDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.wallpaperApplyMessage) {
        state.wallpaperApplyMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearWallpaperMessage()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("personalization_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Brand Header with Logo
        item {
            com.example.ui.common.DevToolsBrandHeader()
        }

        // Section: Hero / Header
        item {
            HeroHeaderCard(metrics = state.deviceMetrics, onOpenDevSettings = { viewModel.openDevSettings() })
        }

        // Section 1: Device Theme & Accents
        item {
            ThemeSelectorSection(
                currentPreset = state.currentPreset,
                onSelectPreset = { viewModel.setThemePreset(it) }
            )
        }

        // Section 2: Custom Developer Wallpapers
        item {
            WallpaperSection(
                selectedStyle = state.selectedWallpaper,
                isApplying = state.isApplyingWallpaper,
                onSelectStyle = { viewModel.selectWallpaperStyle(it) },
                onOpenApplyDialog = { showWallpaperDialog = true }
            )
        }

        // Section 3: App Icon Customizer
        item {
            AppIconSection(
                selectedIndex = state.selectedIconPackIndex,
                customSymbol = state.customIconSymbol,
                onSelectPack = { viewModel.selectIconPack(it) },
                onSelectSymbol = { viewModel.setCustomIconSymbol(it) }
            )
        }

        // Section 4: System Telemetry & Developer Shortcuts
        item {
            TelemetrySection(
                metrics = state.deviceMetrics,
                onRefresh = { viewModel.refreshMetrics() },
                onOpenSettings = { viewModel.openDevSettings() }
            )
        }
    }

    // Wallpaper Confirmation Dialog
    if (showWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply System Wallpaper", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Set '${state.selectedWallpaper.title}' to your device's background:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.applyWallpaperToDevice(WallpaperManager.FLAG_SYSTEM)
                            showWallpaperDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("apply_home_wallpaper")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Apply to Home Screen")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.applyWallpaperToDevice(WallpaperManager.FLAG_LOCK)
                            showWallpaperDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("apply_lock_wallpaper")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Apply to Lock Screen")
                    }

                    Button(
                        onClick = {
                            viewModel.applyWallpaperToDevice(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                            showWallpaperDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("apply_both_wallpaper")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Apply to Both Screens")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroHeaderCard(
    metrics: DeviceMetrics?,
    onOpenDevSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Dev Personalization",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        "Customize device theme, wallpapers & developer styling",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            metrics?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = onOpenDevSettings,
                        label = { Text("Android ${it.androidVersion} (API ${it.apiLevel})") },
                        leadingIcon = {
                            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    AssistChip(
                        onClick = onOpenDevSettings,
                        label = { Text("${it.availRamMb} MB Free") },
                        leadingIcon = {
                            Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectorSection(
    currentPreset: ThemePreset,
    onSelectPreset: (ThemePreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Device Theme Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Active: ${currentPreset.title}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ThemePreset.values()) { preset ->
                val isSelected = preset == currentPreset
                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { onSelectPreset(preset) }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .testTag("theme_preset_${preset.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = preset.previewBg)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(preset.previewPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(preset.previewAccent)
                            )
                            Spacer(Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = preset.previewPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            preset.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            preset.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperSection(
    selectedStyle: WallpaperStyle,
    isApplying: Boolean,
    onSelectStyle: (WallpaperStyle) -> Unit,
    onOpenApplyDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Developer Wallpapers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Generate & apply aesthetic developer backgrounds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onOpenApplyDialog,
                    modifier = Modifier.testTag("btn_apply_wallpaper")
                ) {
                    Icon(
                        Icons.Default.Wallpaper,
                        contentDescription = "Set Wallpaper",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Wallpaper Style Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WallpaperStyle.values()) { style ->
                    FilterChip(
                        selected = style == selectedStyle,
                        onClick = { onSelectStyle(style) },
                        label = { Text(style.title, fontSize = 12.sp) },
                        leadingIcon = if (style == selectedStyle) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }

            // Fast, Zero-Allocation GPU Canvas Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                WallpaperCanvasPreview(
                    style = selectedStyle,
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay Controls
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedStyle.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = onOpenApplyDialog,
                            enabled = !isApplying,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("set_wallpaper_button")
                        ) {
                            if (isApplying) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Applying...", fontSize = 11.sp)
                            } else {
                                Text("Set as Wallpaper", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperCanvasPreview(
    style: WallpaperStyle,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (style) {
            WallpaperStyle.MATRIX_RAIN -> {
                drawRect(color = Color(0xFF040B06))
                val colCount = 14
                val colWidth = w / colCount
                for (c in 0 until colCount) {
                    val streamHeight = ((c * 37) % h.toInt()).toFloat()
                    val x = c * colWidth + colWidth / 2f
                    // Draw fading stream
                    drawLine(
                        brush = Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF00FF66), Color.White)
                        ),
                        start = Offset(x, (streamHeight - 120f).coerceAtLeast(0f)),
                        end = Offset(x, streamHeight),
                        strokeWidth = 3f
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.5f,
                        center = Offset(x, streamHeight)
                    )
                }
            }

            WallpaperStyle.CIRCUIT_BOARD -> {
                drawRect(color = Color(0xFF080D1A))
                val cyan = Color(0xFF00F0FF)
                val pink = Color(0xFFFF007F)

                for (row in 1..4) {
                    val y = h * (row / 5f)
                    val path = Path().apply {
                        moveTo(20f, y)
                        lineTo(w * 0.35f, y)
                        lineTo(w * 0.5f, y + if (row % 2 == 0) 30f else -30f)
                        lineTo(w - 20f, y + if (row % 2 == 0) 30f else -30f)
                    }
                    drawPath(path, cyan, style = Stroke(width = 3f))
                    drawCircle(pink, radius = 6f, center = Offset(w * 0.35f, y))
                    drawCircle(pink, radius = 6f, center = Offset(w * 0.5f, y + if (row % 2 == 0) 30f else -30f))
                }
            }

            WallpaperStyle.DEV_TERMINAL -> {
                drawRect(color = Color(0xFF0F141C))
                // Window Header
                drawRect(color = Color(0xFF1B2332), topLeft = Offset(16f, 16f), size = Size(w - 32f, 32f))
                drawCircle(Color(0xFFFF5F56), radius = 5f, center = Offset(32f, 32f))
                drawCircle(Color(0xFFFFBD2E), radius = 5f, center = Offset(48f, 32f))
                drawCircle(Color(0xFF27C93F), radius = 5f, center = Offset(64f, 32f))

                // Terminal text bar lines
                val colors = listOf(Color(0xFF88C0D0), Color(0xFFA3BE8C), Color(0xFFEBCB8B), Color(0xFFB48EAD), Color.White)
                for (i in 0..4) {
                    val y = 68f + i * 26f
                    val lineWidth = (w * 0.4f) + ((i * 47) % (w * 0.45f).toInt())
                    drawRoundRect(
                        color = colors[i % colors.size],
                        topLeft = Offset(24f, y),
                        size = Size(lineWidth, 8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }

            WallpaperStyle.HEX_GRID -> {
                drawRect(color = Color(0xFF0B0C10))
                val gridColor = Color(0xFF1F2833)
                val glowColor = Color(0xFF66FCF1)

                val radius = 32f
                val dx = radius * 1.732f
                val dy = radius * 1.5f

                var row = 0
                var y = 20f
                while (y < h + radius) {
                    val offsetX = if (row % 2 == 0) 0f else dx / 2f
                    var x = offsetX
                    while (x < w + radius) {
                        val path = Path()
                        for (i in 0 until 6) {
                            val angle = 2.0 * PI / 6.0 * i
                            val px = (x + radius * cos(angle)).toFloat()
                            val py = (y + radius * sin(angle)).toFloat()
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        path.close()
                        val isGlow = (row + (x / dx).toInt()) % 7 == 0
                        drawPath(path, if (isGlow) glowColor else gridColor, style = Stroke(width = if (isGlow) 2.5f else 1.5f))
                        x += dx
                    }
                    y += dy
                    row++
                }
            }

            WallpaperStyle.MINIMAL_CODE -> {
                drawRect(color = Color(0xFF090A0F))
                // Central Code Symbol
                val center = Offset(w / 2f, h / 2f - 20f)
                val purple = Color(0xFFBD93F9)
                val pink = Color(0xFFFF79C6)

                // Draw brackets </>
                // Left bracket <
                drawLine(purple, start = Offset(center.x - 70f, center.y - 40f), end = Offset(center.x - 110f, center.y), strokeWidth = 8f)
                drawLine(purple, start = Offset(center.x - 110f, center.y), end = Offset(center.x - 70f, center.y + 40f), strokeWidth = 8f)

                // Slash /
                drawLine(pink, start = Offset(center.x + 20f, center.y - 45f), end = Offset(center.x - 20f, center.y + 45f), strokeWidth = 8f)

                // Right bracket >
                drawLine(purple, start = Offset(center.x + 70f, center.y - 40f), end = Offset(center.x + 110f, center.y), strokeWidth = 8f)
                drawLine(purple, start = Offset(center.x + 110f, center.y), end = Offset(center.x + 70f, center.y + 40f), strokeWidth = 8f)
            }
        }
    }
}

@Composable
private fun AppIconSection(
    selectedIndex: Int,
    customSymbol: String,
    onSelectPack: (Int) -> Unit,
    onSelectSymbol: (String) -> Unit
) {
    val symbols = listOf("</>", "⚡", "🛡️", "⚙️", "👾", "🚀", "💻", "λ")
    val iconThemes = listOf(
        Pair("Cyber Neon", Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFFFF007F)))),
        Pair("Terminal Green", Brush.linearGradient(listOf(Color(0xFF00FF66), Color(0xFF04200E)))),
        Pair("Dracula Violet", Brush.linearGradient(listOf(Color(0xFFBD93F9), Color(0xFF6272A4)))),
        Pair("Amber Sunset", Brush.linearGradient(listOf(Color(0xFFFFD866), Color(0xFFFF6188))))
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "App Icon Customization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Personalize launcher style & developer emblem",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Live Icon Preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(6.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(iconThemes[selectedIndex.coerceIn(0, iconThemes.size - 1)].second),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customSymbol,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Dev Tools",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Symbol Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select Emblem:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(symbols) { s ->
                        OutlinedButton(
                            onClick = { onSelectSymbol(s) },
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = if (customSymbol == s) {
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text(s, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Theme Gradients Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select Icon Gradient Palette:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    iconThemes.forEachIndexed { index, (name, brush) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(brush)
                                .clickable { onSelectPack(index) }
                                .border(
                                    width = if (selectedIndex == index) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedIndex == index) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetrySection(
    metrics: DeviceMetrics?,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
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
                Text(
                    "Device Telemetry & Specs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Telemetry")
                }
            }

            metrics?.let {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricRow(label = "Model & Hardware", value = "${it.manufacturer} ${it.deviceModel}")
                    MetricRow(label = "OS Platform", value = "Android ${it.androidVersion} (API ${it.apiLevel})")
                    MetricRow(label = "Processor Architecture", value = "${it.cpuArch} (${it.cpuCores} cores)")
                    MetricRow(label = "RAM Status", value = "${it.availRamMb} MB free / ${it.totalRamMb} MB total")
                    MetricRow(
                        label = "Battery",
                        value = "${it.batteryPct}% ${if (it.isCharging) "(Charging)" else ""} • ${it.batteryTempC}°C"
                    )
                }
            }

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().testTag("open_developer_options")
            ) {
                Icon(Icons.Default.Build, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Android Developer Settings")
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
