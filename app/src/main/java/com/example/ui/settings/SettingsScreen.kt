package com.example.ui.settings

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.personalize.PersonalizationViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    personalizeViewModel: PersonalizationViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    val loc = state.localized

    LaunchedEffect(state.notificationMessage) {
        state.notificationMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearNotificationMessage()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Language Selector Section
        item {
            LanguageSection(
                currentLang = state.language,
                onSelectLanguage = { viewModel.setLanguage(it) },
                title = loc.languageSection
            )
        }

        // 2. Editor Preferences
        item {
            EditorPreferencesSection(
                fontSize = state.editorFontSize,
                autoIndent = state.autoIndent,
                wordWrap = state.wordWrap,
                onFontSizeChange = { viewModel.setEditorFontSize(it) },
                onToggleAutoIndent = { viewModel.toggleAutoIndent(it) },
                onToggleWordWrap = { viewModel.toggleWordWrap(it) },
                title = loc.editorSection,
                fontSizeLabel = loc.fontSizeLabel
            )
        }

        // 3. Security & Cache Preferences
        item {
            SecurityPreferencesSection(
                deepScan = state.deepScanDefault,
                autoQuarantine = state.autoQuarantine,
                onToggleDeepScan = { viewModel.toggleDeepScan(it) },
                onToggleAutoQuarantine = { viewModel.toggleAutoQuarantine(it) },
                onClearCache = { viewModel.clearCache() },
                title = loc.securityConfigSection,
                clearCacheLabel = loc.clearCacheTitle
            )
        }

        // 4. Reset App Configuration Section
        item {
            ResetConfigurationSection(
                onOpenResetDialog = { showResetDialog = true },
                title = loc.resetSection,
                resetButtonLabel = loc.resetAllConfig
            )
        }

        // 5. About Dev Tools Build Info
        item {
            AboutDevToolsSection(title = loc.aboutTitle)
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(loc.resetConfirmTitle, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(loc.resetConfirmMessage)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllConfiguration(personalizeViewModel) {
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LanguageSection(
    currentLang: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    title: String
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
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        currentLang.displayName,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppLanguage.values()) { lang ->
                    val isSelected = lang == currentLang
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectLanguage(lang) },
                        label = {
                            Text("${lang.nativeName} (${lang.displayName})", fontSize = 12.sp)
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("lang_chip_${lang.code}")
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorPreferencesSection(
    fontSize: Int,
    autoIndent: Boolean,
    wordWrap: Boolean,
    onFontSizeChange: (Int) -> Unit,
    onToggleAutoIndent: (Boolean) -> Unit,
    onToggleWordWrap: (Boolean) -> Unit,
    title: String,
    fontSizeLabel: String
) {
    val fontSizes = listOf(11, 13, 15, 17, 19)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Font size selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$fontSizeLabel: ${fontSize}sp", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fontSizes.forEach { sz ->
                        OutlinedButton(
                            onClick = { onFontSizeChange(sz) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            colors = if (sz == fontSize) {
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text("${sz}sp", fontSize = 12.sp, fontWeight = if (sz == fontSize) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Auto indent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto Indentation", fontWeight = FontWeight.Medium)
                    Text("Indent next line on new brackets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = autoIndent, onCheckedChange = onToggleAutoIndent)
            }

            // Word wrap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Soft Word Wrap", fontWeight = FontWeight.Medium)
                    Text("Wrap long code lines inside the editor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = wordWrap, onCheckedChange = onToggleWordWrap)
            }
        }
    }
}

@Composable
private fun SecurityPreferencesSection(
    deepScan: Boolean,
    autoQuarantine: Boolean,
    onToggleDeepScan: (Boolean) -> Unit,
    onToggleAutoQuarantine: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    title: String,
    clearCacheLabel: String
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Default Deep Scan", fontWeight = FontWeight.Medium)
                    Text("Scan entire public external documents by default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = deepScan, onCheckedChange = onToggleDeepScan)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto-Remediate Known Threats", fontWeight = FontWeight.Medium)
                    Text("Immediately quarantine flagged critical scripts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = autoQuarantine, onCheckedChange = onToggleAutoQuarantine)
            }

            OutlinedButton(
                onClick = onClearCache,
                modifier = Modifier.fillMaxWidth().testTag("btn_clear_cache")
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(clearCacheLabel)
            }
        }
    }
}

@Composable
private fun ResetConfigurationSection(
    onOpenResetDialog: () -> Unit,
    title: String,
    resetButtonLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }

            Text(
                "Reset themes, wallpapers, compilers, local snippets, and scan records to initial setup state.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(
                onClick = onOpenResetDialog,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().testTag("btn_reset_app_config")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(resetButtonLabel)
            }
        }
    }
}

@Composable
private fun AboutDevToolsSection(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Text("Dev Tools Suite for Android", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Version 1.0.0 • Target SDK 36 (Android 16)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Runtimes: Java 21 JVM, Python 3.12, GCC C++17", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Database: Android Jetpack Room with SQLite", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Security Engine: EICAR & Heuristic Threat Scanner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
