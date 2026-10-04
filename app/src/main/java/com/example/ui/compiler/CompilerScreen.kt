package com.example.ui.compiler

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compiler.SyntaxHighlighter
import com.example.model.CodeLanguage
import com.example.model.CodeSnippet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompilerScreen(
    viewModel: CompilerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val savedSnippets by viewModel.savedSnippets.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSnippetsSheet by remember { mutableStateOf(false) }
    var snippetNameInput by remember { mutableStateOf("") }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("compiler_screen")
    ) {
        // Top Toolbar: Language Selector & Actions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language Choice Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CodeLanguage.values().forEach { lang ->
                            val isSelected = lang == state.currentLanguage
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.testTag("lang_${lang.name.lowercase()}")
                            )
                        }
                    }

                    // Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.loadLanguageTemplate(state.currentLanguage) },
                            modifier = Modifier.testTag("btn_reset_template")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Template")
                        }

                        IconButton(
                            onClick = {
                                snippetNameInput = "${state.currentLanguage.displayName}_Script"
                                showSaveDialog = true
                            },
                            modifier = Modifier.testTag("btn_save_snippet")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save Snippet")
                        }

                        IconButton(
                            onClick = { showSnippetsSheet = true },
                            modifier = Modifier.testTag("btn_open_snippets")
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Saved Snippets")
                        }
                    }
                }

                // Script title label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        state.currentSnippetTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = { viewModel.executeCode() },
                        enabled = !state.isRunning,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_run_code")
                    ) {
                        if (state.isRunning) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Running...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Run Code", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Quick Symbol Toolbar
        QuickSymbolBar(
            onInsert = { sym ->
                viewModel.updateCode(state.codeText + sym)
            }
        )

        // Split Area: Code Editor (top) + Terminal Console (bottom)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Code Editor Box with Line Numbers
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0F141C))
            ) {
                CodeEditorField(
                    code = state.codeText,
                    language = state.currentLanguage,
                    onCodeChange = { viewModel.updateCode(it) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 2.dp)

            // Console Output Terminal
            ConsoleTerminalPanel(
                state = state,
                onTabSelect = { viewModel.setActiveOutputTab(it) },
                onUpdateStdin = { viewModel.updateStdin(it) },
                onCopy = {
                    val out = state.executionResult?.output ?: ""
                    clipboardManager.setText(AnnotatedString(out))
                    Toast.makeText(context, "Copied console output", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxWidth()
            )
        }
    }

    // Save Snippet Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Code Snippet") },
            text = {
                OutlinedTextField(
                    value = snippetNameInput,
                    onValueChange = { snippetNameInput = it },
                    label = { Text("Snippet Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("snippet_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentSnippet(snippetNameInput)
                        showSaveDialog = false
                    },
                    modifier = Modifier.testTag("confirm_save_snippet")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Saved Snippets Sheet
    if (showSnippetsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSnippetsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Saved Snippets Database",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                if (savedSnippets.isEmpty()) {
                    Text(
                        "No snippets saved yet. Click the disk icon to save your scripts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedSnippets) { snippet ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadSnippet(snippet)
                                        showSnippetsSheet = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(snippet.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "${snippet.language.displayName} • ${snippet.code.lines().size} lines",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSnippet(snippet) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Snippet", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSymbolBar(onInsert: (String) -> Unit) {
    val symbols = listOf("    ", "{", "}", "(", ")", "[", "]", ";", ":", "\"", "'", "=", "+", "-", "*", "/", "<", ">", ",", ".")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF161B22)
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(symbols) { sym ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF21262D),
                    modifier = Modifier.clickable { onInsert(sym) }
                ) {
                    Text(
                        text = if (sym == "    ") "TAB" else sym,
                        color = Color(0xFFC9D1D9),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeEditorField(
    code: String,
    language: CodeLanguage,
    onCodeChange: (String) -> Unit
) {
    val lines = code.lines()
    val lineCount = lines.size.coerceAtLeast(1)

    val lineNumbersText = remember(lineCount) {
        (1..lineCount).joinToString("\n")
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Line numbers gutter
        Box(
            modifier = Modifier
                .background(Color(0xFF0B0E14))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Text(
                text = lineNumbersText,
                color = Color(0xFF4A5568),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Code editor input
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFE2E8F0)
                ),
                cursorBrush = SolidColor(Color(0xFF00F0FF)),
                visualTransformation = {
                    androidx.compose.ui.text.input.TransformedText(
                        SyntaxHighlighter.highlight(it.text, language),
                        androidx.compose.ui.text.input.OffsetMapping.Identity
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("code_editor_input")
            )
        }
    }
}

@Composable
private fun ConsoleTerminalPanel(
    state: CompilerState,
    onTabSelect: (Int) -> Unit,
    onUpdateStdin: (String) -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val result = state.executionResult

    Column(
        modifier = modifier
            .background(Color(0xFF0B0E14))
    ) {
        // Terminal Header & Tabs
        Surface(
            color = Color(0xFF131822),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabRow(
                    selectedTabIndex = state.activeOutputTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(300.dp)
                ) {
                    Tab(
                        selected = state.activeOutputTab == 0,
                        onClick = { onTabSelect(0) },
                        text = { Text("Output", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = state.activeOutputTab == 1,
                        onClick = { onTabSelect(1) },
                        text = { Text("Diagnostics", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = state.activeOutputTab == 2,
                        onClick = { onTabSelect(2) },
                        text = { Text("Input (stdin)", fontSize = 12.sp) }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    result?.let { res ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (res.isSuccess) Color(0xFF0F5132) else Color(0xFF842029)
                        ) {
                            Text(
                                if (res.isSuccess) "${res.executionTimeMs}ms" else "Err: ${res.exitCode}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Console", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Terminal Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (state.activeOutputTab) {
                0 -> {
                    val outText = result?.output ?: "Terminal ready. Write code and press 'Run Code'."
                    Text(
                        text = outText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (result?.isSuccess == false) Color(0xFFFF6B6B) else Color(0xFF00FF66),
                        modifier = Modifier.testTag("terminal_output_text")
                    )
                }
                1 -> {
                    val diag = result?.compilerLog ?: "Compiler log: no build diagnostics recorded yet."
                    Text(
                        text = diag,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF90CDF4),
                        modifier = Modifier.testTag("compiler_diagnostics_text")
                    )
                }
                2 -> {
                    Column {
                        Text(
                            "Standard Input Stream (stdin):",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = state.stdinText,
                            onValueChange = onUpdateStdin,
                            placeholder = { Text("Enter input values separated by newline...", fontSize = 11.sp) },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White),
                            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("stdin_input")
                        )
                    }
                }
            }
        }
    }
}
