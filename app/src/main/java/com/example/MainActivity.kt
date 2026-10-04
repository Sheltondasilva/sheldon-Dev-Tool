package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.DevDatabase
import com.example.model.LocalizedStrings
import com.example.ui.compiler.CompilerScreen
import com.example.ui.compiler.CompilerViewModel
import com.example.ui.hardware.DeviceTestScreen
import com.example.ui.hardware.DeviceTestViewModel
import com.example.ui.personalize.PersonalizationScreen
import com.example.ui.personalize.PersonalizationViewModel
import com.example.ui.security.SecurityScreen
import com.example.ui.security.SecurityViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.DevToolsTheme

enum class DevTab(val testTag: String) {
    PERSONALIZE("tab_personalize"),
    TESTS("tab_tests"),
    SECURITY("tab_security"),
    COMPILER("tab_compiler"),
    SETTINGS("tab_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = remember { DevDatabase.getDatabase(context) }

            val personalizeVm: PersonalizationViewModel = viewModel(
                factory = PersonalizationViewModel.Factory(context)
            )
            val deviceTestVm: DeviceTestViewModel = viewModel(
                factory = DeviceTestViewModel.Factory(context)
            )
            val securityVm: SecurityViewModel = viewModel(
                factory = SecurityViewModel.Factory(context, database)
            )
            val compilerVm: CompilerViewModel = viewModel(
                factory = CompilerViewModel.Factory(database)
            )
            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(context, database)
            )

            val personalizeState by personalizeVm.uiState.collectAsState()
            val settingsState by settingsVm.uiState.collectAsState()
            val loc = settingsState.localized

            DevToolsTheme(
                preset = personalizeState.currentPreset,
                darkTheme = personalizeState.isDarkMode,
                dynamicColor = personalizeState.isDynamicColor
            ) {
                var currentTab by remember { mutableStateOf(DevTab.PERSONALIZE) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        DevToolsTopBar(
                            currentTab = currentTab,
                            loc = loc
                        )
                    },
                    bottomBar = {
                        DevToolsBottomBar(
                            currentTab = currentTab,
                            loc = loc,
                            onTabSelected = { currentTab = it }
                        )
                    }
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    when (currentTab) {
                        DevTab.PERSONALIZE -> PersonalizationScreen(
                            viewModel = personalizeVm,
                            modifier = modifier
                        )
                        DevTab.TESTS -> DeviceTestScreen(
                            viewModel = deviceTestVm,
                            modifier = modifier
                        )
                        DevTab.SECURITY -> SecurityScreen(
                            viewModel = securityVm,
                            modifier = modifier
                        )
                        DevTab.COMPILER -> CompilerScreen(
                            viewModel = compilerVm,
                            modifier = modifier
                        )
                        DevTab.SETTINGS -> SettingsScreen(
                            viewModel = settingsVm,
                            personalizeViewModel = personalizeVm,
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsTopBar(
    currentTab: DevTab,
    loc: LocalizedStrings
) {
    val tabTitle = when (currentTab) {
        DevTab.PERSONALIZE -> loc.tabPersonalize
        DevTab.TESTS -> loc.tabDeviceTest
        DevTab.SECURITY -> loc.tabSecurity
        DevTab.COMPILER -> loc.tabCompiler
        DevTab.SETTINGS -> loc.tabSettings
    }

    TopAppBar(
        title = {
            Text(
                text = "Dev Tools • $tabTitle",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun DevToolsBottomBar(
    currentTab: DevTab,
    loc: LocalizedStrings,
    onTabSelected: (DevTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        NavigationBarItem(
            selected = currentTab == DevTab.PERSONALIZE,
            onClick = { onTabSelected(DevTab.PERSONALIZE) },
            icon = { Icon(Icons.Default.Palette, contentDescription = "Personalization") },
            label = { Text(loc.tabPersonalize, maxLines = 1) },
            modifier = Modifier.testTag("nav_personalize")
        )

        NavigationBarItem(
            selected = currentTab == DevTab.TESTS,
            onClick = { onTabSelected(DevTab.TESTS) },
            icon = { Icon(Icons.Default.BuildCircle, contentDescription = "Hardware Test") },
            label = { Text(loc.tabDeviceTest, maxLines = 1) },
            modifier = Modifier.testTag("nav_tests")
        )

        NavigationBarItem(
            selected = currentTab == DevTab.SECURITY,
            onClick = { onTabSelected(DevTab.SECURITY) },
            icon = { Icon(Icons.Default.Security, contentDescription = "Security Scanner") },
            label = { Text(loc.tabSecurity, maxLines = 1) },
            modifier = Modifier.testTag("nav_security")
        )

        NavigationBarItem(
            selected = currentTab == DevTab.COMPILER,
            onClick = { onTabSelected(DevTab.COMPILER) },
            icon = { Icon(Icons.Default.Code, contentDescription = "Compilers") },
            label = { Text(loc.tabCompiler, maxLines = 1) },
            modifier = Modifier.testTag("nav_compiler")
        )

        NavigationBarItem(
            selected = currentTab == DevTab.SETTINGS,
            onClick = { onTabSelected(DevTab.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text(loc.tabSettings, maxLines = 1) },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}
