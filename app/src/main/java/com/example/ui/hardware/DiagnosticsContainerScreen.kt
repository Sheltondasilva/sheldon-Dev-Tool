package com.example.ui.hardware

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.monitor.PerformanceMonitorScreen
import com.example.ui.monitor.PerformanceMonitorViewModel

@Composable
fun DiagnosticsContainerScreen(
    monitorViewModel: PerformanceMonitorViewModel,
    deviceTestViewModel: DeviceTestViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Performance Monitor, 1: Hardware Test

    Column(modifier = modifier.fillMaxSize().testTag("diagnostics_container")) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("Performance Monitor", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_monitor_sub")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("Hardware Tests", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_hardware_sub")
                )
            }
        }

        when (selectedTab) {
            0 -> PerformanceMonitorScreen(
                viewModel = monitorViewModel,
                modifier = Modifier.fillMaxSize()
            )
            1 -> DeviceTestScreen(
                viewModel = deviceTestViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
